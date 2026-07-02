package com.aes.exam.common.exception;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aes.exam.common.api.ApiResponse;
import com.aes.exam.common.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest
@AutoConfigureMockMvc
@Import(GlobalExceptionHandlerTest.FailureController.class)
@ActiveProfiles("test")
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void validationFailureUsesUnifiedResponse() throws Exception {
        mockMvc.perform(post("/api/system/diagnostics/mapping")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"displayName":"","email":"not-an-email"}
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success", is(false)))
            .andExpect(jsonPath("$.code", is("VALIDATION_FAILED")))
            .andExpect(jsonPath("$.message", containsString("displayName")));
    }

    @Test
    void businessFailureUsesErrorCode() throws Exception {
        mockMvc.perform(get("/test/failures/business"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success", is(false)))
            .andExpect(jsonPath("$.code", is("BUSINESS_ERROR")))
            .andExpect(jsonPath("$.message", is("业务规则不满足")));
    }

    @Test
    void authFailureUsesErrorCode() throws Exception {
        mockMvc.perform(get("/test/failures/auth"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.success", is(false)))
            .andExpect(jsonPath("$.code", is("UNAUTHORIZED")));
    }

    @Test
    void unknownFailureIsHiddenAsInternalServerError() throws Exception {
        mockMvc.perform(get("/test/failures/unknown"))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.success", is(false)))
            .andExpect(jsonPath("$.code", is("INTERNAL_SERVER_ERROR")))
            .andExpect(jsonPath("$.message", is("系统内部错误")));
    }

    @Test
    void missingApiResourceUsesUnifiedNotFoundResponse() throws Exception {
        mockMvc.perform(get("/api/not-existing-resource"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.success", is(false)))
            .andExpect(jsonPath("$.code", is("NOT_FOUND")))
            .andExpect(jsonPath("$.message", is("请求资源不存在")));
    }

    @Test
    void unsupportedHttpMethodUsesUnifiedMethodNotAllowedResponse() throws Exception {
        mockMvc.perform(get("/api/auth/login"))
            .andExpect(status().isMethodNotAllowed())
            .andExpect(jsonPath("$.success", is(false)))
            .andExpect(jsonPath("$.code", is("METHOD_NOT_ALLOWED")))
            .andExpect(jsonPath("$.message", is("请求方法不被支持")));
    }

    @RestController
    @RequestMapping("/test/failures")
    static class FailureController {

        @GetMapping("/business")
        ApiResponse<Void> business() {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "业务规则不满足");
        }

        @GetMapping("/auth")
        ApiResponse<Void> auth() {
            throw new AuthException(ErrorCode.UNAUTHORIZED);
        }

        @GetMapping("/unknown")
        ApiResponse<Void> unknown() {
            throw new IllegalStateException("sensitive detail");
        }
    }
}
