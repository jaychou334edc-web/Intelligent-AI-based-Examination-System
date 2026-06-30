package com.aes.exam.exam;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
    "aes.auth.dev-seed.enabled=true"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ExamFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void teacherCanPublishAndStudentCanSubmitExam() throws Exception {
        String teacherToken = login("teacher", "Teacher@123456");
        Long questionId = createQuestion();

        String createResponse = mockMvc.perform(post("/api/teacher/exams")
                .header(HttpHeaders.AUTHORIZATION, bearer(teacherToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"自动化测试考试","description":"Phase 3 flow","durationMinutes":30}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status", is("draft")))
            .andReturn()
            .getResponse()
            .getContentAsString();
        Long examId = objectMapper.readTree(createResponse).path("data").path("id").asLong();

        mockMvc.perform(post("/api/teacher/exams/" + examId + "/questions")
                .header(HttpHeaders.AUTHORIZATION, bearer(teacherToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"questionIds\":[" + questionId + "]}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.questionCount", is(1)));

        mockMvc.perform(post("/api/teacher/exams/" + examId + "/publish")
                .header(HttpHeaders.AUTHORIZATION, bearer(teacherToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status", is("published")));

        String studentToken = login("student", "Student@123456");

        mockMvc.perform(get("/api/student/exams")
                .header(HttpHeaders.AUTHORIZATION, bearer(studentToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()", greaterThanOrEqualTo(1)));

        mockMvc.perform(get("/api/student/exams/" + examId)
                .header(HttpHeaders.AUTHORIZATION, bearer(studentToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.questions.length()", is(1)))
            .andExpect(jsonPath("$.data.questions[0].answer", nullValue()))
            .andExpect(jsonPath("$.data.submissionStartedAt", notNullValue()));

        mockMvc.perform(post("/api/student/exams/" + examId + "/answers")
                .header(HttpHeaders.AUTHORIZATION, bearer(studentToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"questionId\":" + questionId + ",\"answer\":\"A\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status", is("saved")));

        mockMvc.perform(post("/api/student/exams/" + examId + "/submit")
                .header(HttpHeaders.AUTHORIZATION, bearer(studentToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"answers\":[{\"questionId\":" + questionId + ",\"answer\":\"A\"}]}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status", is("submitted")));

        mockMvc.perform(post("/api/student/exams/" + examId + "/answers")
                .header(HttpHeaders.AUTHORIZATION, bearer(studentToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"questionId\":" + questionId + ",\"answer\":\"B\"}"))
            .andExpect(status().isBadRequest());
    }

    private Long createQuestion() {
        Long teacherId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE username = 'teacher'", Long.class);
        jdbcTemplate.update("""
            INSERT INTO questions (question_type, stem, analysis, score, difficulty, knowledge_point, status,
                                   version, created_at, updated_at, created_by, updated_by, is_deleted)
            VALUES ('single_choice', 'Phase 3 测试题？', '', ?, 'normal', 'Phase 3', 'active',
                    1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, ?, ?, 0)
            """, BigDecimal.valueOf(2), teacherId, teacherId);
        Long questionId = jdbcTemplate.queryForObject("SELECT MAX(id) FROM questions", Long.class);
        jdbcTemplate.update("""
            INSERT INTO question_options (question_id, option_key, option_text, is_correct, sort_order, created_at, updated_at)
            VALUES (?, 'A', '正确选项', 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """, questionId);
        jdbcTemplate.update("""
            INSERT INTO question_answers (question_id, answer_text, match_rule, created_at, updated_at)
            VALUES (?, 'A', 'exact', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """, questionId);
        return questionId;
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
}
