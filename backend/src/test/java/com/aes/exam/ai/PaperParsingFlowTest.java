package com.aes.exam.ai;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
    "aes.auth.dev-seed.enabled=true",
    "aes.upload-dir=./target/test-uploads"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PaperParsingFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void teacherCanUploadParseReviewAndImportDocx() throws Exception {
        String token = loginAsTeacher();
        Long paperId = uploadPaper(token);

        String parseResponse = mockMvc.perform(post("/api/ai/parse-paper")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"paperId\":" + paperId + "}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.questions.length()", is(2)))
            .andExpect(jsonPath("$.data.questions[0].questionType", is("single_choice")))
            .andReturn()
            .getResponse()
            .getContentAsString();

        JsonNode questions = objectMapper.readTree(parseResponse).path("data").path("questions");
        String importPayload = objectMapper.writeValueAsString(new ImportPayload(paperId, questions));

        mockMvc.perform(post("/api/questions/import")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(importPayload))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.importedCount", is(2)));

        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM questions WHERE source_paper_id = ?", Integer.class, paperId);
        org.assertj.core.api.Assertions.assertThat(count).isEqualTo(2);

        mockMvc.perform(get("/api/ai/parse-result/" + paperId)
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.questions[0].reviewStatus", is("approved")));
    }

    @Test
    void studentCannotUseTeacherParsingApis() throws Exception {
        String studentToken = login("student", "Student@123456");
        mockMvc.perform(get("/api/ai/parse-result/1")
                .header(HttpHeaders.AUTHORIZATION, bearer(studentToken)))
            .andExpect(status().isForbidden());
    }

    private Long uploadPaper(String token) throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "sample.docx",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            sampleDocx()
        );

        String response = mockMvc.perform(multipart("/api/papers")
                .file(file)
                .param("title", "测试试卷")
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id", greaterThanOrEqualTo(1)))
            .andReturn()
            .getResponse()
            .getContentAsString();

        return objectMapper.readTree(response).path("data").path("id").asLong();
    }

    private byte[] sampleDocx() throws Exception {
        try (XWPFDocument document = new XWPFDocument();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            document.createParagraph().createRun().setText("1. 下列哪个选项是 Java 后端框架？（5分）");
            document.createParagraph().createRun().setText("A. Spring Boot");
            document.createParagraph().createRun().setText("B. Photoshop");
            document.createParagraph().createRun().setText("C. Excel");
            document.createParagraph().createRun().setText("D. Word");
            document.createParagraph().createRun().setText("答案：A");
            document.createParagraph().createRun().setText("2. 简述在线考试系统为什么需要权限控制。（10分）");
            document.createParagraph().createRun().setText("答案：防止越权访问，保护考试数据。");
            document.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private String loginAsTeacher() throws Exception {
        return login("teacher", "Teacher@123456");
    }

    private String login(String username, String password) throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        return objectMapper.readTree(response).path("data").path("token").asText();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private record ImportPayload(Long paperId, JsonNode questions) {
    }
}
