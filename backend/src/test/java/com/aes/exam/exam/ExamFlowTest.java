package com.aes.exam.exam;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aes.exam.common.config.AesProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import org.junit.jupiter.api.AfterEach;
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

    @Autowired
    private AesProperties aesProperties;

    @AfterEach
    void resetGradingConfig() {
        aesProperties.getGrading().setAutoGradeFillBlank(false);
    }

    @Test
    void teacherCanPublishAndStudentCanSubmitExam() throws Exception {
        String teacherToken = login("teacher", "Teacher@123456");
        Long questionId = createQuestion();
        Long fillBlankQuestionId = createFillBlankQuestion();
        Long subjectiveQuestionId = createSubjectiveQuestion();

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
                .content("{\"questionIds\":[" + questionId + "," + fillBlankQuestionId + "," + subjectiveQuestionId + "]}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.questionCount", is(3)));

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
            .andExpect(jsonPath("$.data.questions.length()", is(3)))
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
                .content("{\"answers\":[{\"questionId\":" + questionId + ",\"answer\":\"A\"},{\"questionId\":"
                    + fillBlankQuestionId + ",\"answer\":\"Spring Boot\"},{\"questionId\":"
                    + subjectiveQuestionId + ",\"answer\":\"需要权限控制保护考试数据\"}]}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status", is("submitted")))
            .andExpect(jsonPath("$.data.totalScore", is(2.0)));

        String teacherDetail = mockMvc.perform(get("/api/teacher/grading/submissions")
                .header(HttpHeaders.AUTHORIZATION, bearer(teacherToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()", greaterThanOrEqualTo(1)))
            .andReturn()
            .getResponse()
            .getContentAsString();
        Long submissionId = objectMapper.readTree(teacherDetail).path("data").get(0).path("submissionId").asLong();

        mockMvc.perform(get("/api/teacher/grading/submissions/" + submissionId)
                .header(HttpHeaders.AUTHORIZATION, bearer(teacherToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.totalScore", is(2.0)))
            .andExpect(jsonPath("$.data.answers[0].gradingStatus", is("auto_graded")))
            .andExpect(jsonPath("$.data.answers[1].gradingStatus", is("pending")))
            .andExpect(jsonPath("$.data.answers[2].gradingStatus", is("pending")));

        mockMvc.perform(post("/api/teacher/grading/submissions/" + submissionId + "/answers")
                .header(HttpHeaders.AUTHORIZATION, bearer(teacherToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"questionId\":" + fillBlankQuestionId + ",\"score\":3,\"teacherComment\":\"填空正确\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.totalScore", is(5.0)))
            .andExpect(jsonPath("$.data.answers[1].gradingStatus", is("manual_graded")));

        mockMvc.perform(post("/api/teacher/grading/submissions/" + submissionId + "/answers")
                .header(HttpHeaders.AUTHORIZATION, bearer(teacherToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"questionId\":" + subjectiveQuestionId + ",\"score\":8,\"teacherComment\":\"要点基本完整\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.totalScore", is(13.0)))
            .andExpect(jsonPath("$.data.answers[2].gradingStatus", is("manual_graded")));

        mockMvc.perform(get("/api/student/results/" + submissionId)
                .header(HttpHeaders.AUTHORIZATION, bearer(studentToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.totalScore", is(13.0)));

        mockMvc.perform(post("/api/student/monitoring/exams/" + examId + "/events")
                .header(HttpHeaders.AUTHORIZATION, bearer(studentToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"eventType":"browser_blur","eventLevel":"warning","eventData":{"questionIndex":1,"remainingSeconds":1200}}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status", is("recorded")));

        mockMvc.perform(post("/api/student/monitoring/exams/" + examId + "/events")
                .header(HttpHeaders.AUTHORIZATION, bearer(studentToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"eventType":"copy_attempt","eventLevel":"critical","eventData":{"questionIndex":2}}
                    """))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/teacher/monitoring/exams/" + examId + "/events")
                .header(HttpHeaders.AUTHORIZATION, bearer(teacherToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()", is(2)))
            .andExpect(jsonPath("$.data[0].eventType", is("copy_attempt")));

        mockMvc.perform(get("/api/teacher/monitoring/exams/" + examId + "/analytics")
                .header(HttpHeaders.AUTHORIZATION, bearer(teacherToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.examId", is(examId.intValue())))
            .andExpect(jsonPath("$.data.submittedCount", is(1)))
            .andExpect(jsonPath("$.data.participantCount", greaterThanOrEqualTo(1)))
            .andExpect(jsonPath("$.data.averageScore", is(13.0)))
            .andExpect(jsonPath("$.data.passRate", is(100.0)))
            .andExpect(jsonPath("$.data.scoreDistribution[4].count", is(1)))
            .andExpect(jsonPath("$.data.questionAccuracy.length()", is(3)))
            .andExpect(jsonPath("$.data.eventCounts.length()", is(2)));

        mockMvc.perform(post("/api/student/exams/" + examId + "/answers")
                .header(HttpHeaders.AUTHORIZATION, bearer(studentToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"questionId\":" + questionId + ",\"answer\":\"B\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void fillBlankCanBeAutoGradedWhenConfigured() throws Exception {
        aesProperties.getGrading().setAutoGradeFillBlank(true);
        String teacherToken = login("teacher", "Teacher@123456");
        Long fillBlankQuestionId = createFillBlankQuestion();

        String createResponse = mockMvc.perform(post("/api/teacher/exams")
                .header(HttpHeaders.AUTHORIZATION, bearer(teacherToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"填空自动评分测试","description":"Phase 4 fill blank","durationMinutes":30}
                    """))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
        Long examId = objectMapper.readTree(createResponse).path("data").path("id").asLong();

        mockMvc.perform(post("/api/teacher/exams/" + examId + "/questions")
                .header(HttpHeaders.AUTHORIZATION, bearer(teacherToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"questionIds\":[" + fillBlankQuestionId + "]}"))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/teacher/exams/" + examId + "/publish")
                .header(HttpHeaders.AUTHORIZATION, bearer(teacherToken)))
            .andExpect(status().isOk());

        String studentToken = login("student", "Student@123456");

        mockMvc.perform(get("/api/student/exams/" + examId)
                .header(HttpHeaders.AUTHORIZATION, bearer(studentToken)))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/student/exams/" + examId + "/submit")
                .header(HttpHeaders.AUTHORIZATION, bearer(studentToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"answers\":[{\"questionId\":" + fillBlankQuestionId + ",\"answer\":\"Spring Boot\"}]}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status", is("submitted")))
            .andExpect(jsonPath("$.data.totalScore", is(3.0)));

        String teacherDetail = mockMvc.perform(get("/api/teacher/grading/submissions")
                .header(HttpHeaders.AUTHORIZATION, bearer(teacherToken)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
        Long submissionId = objectMapper.readTree(teacherDetail).path("data").get(0).path("submissionId").asLong();

        mockMvc.perform(get("/api/teacher/grading/submissions/" + submissionId)
                .header(HttpHeaders.AUTHORIZATION, bearer(teacherToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.totalScore", is(3.0)))
            .andExpect(jsonPath("$.data.answers[0].gradingStatus", is("auto_graded")))
            .andExpect(jsonPath("$.data.answers[0].isCorrect", is(true)));
    }

    @Test
    void publishedExamScheduleCanBeUpdated() throws Exception {
        String teacherToken = login("teacher", "Teacher@123456");
        Long questionId = createQuestion();

        String createResponse = mockMvc.perform(post("/api/teacher/exams")
                .header(HttpHeaders.AUTHORIZATION, bearer(teacherToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"发布时间修正测试","description":"错误设置到明天","durationMinutes":30,
                     "startTime":"2099-01-01T09:00:00","endTime":"2099-01-01T10:00:00"}
                    """))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
        Long examId = objectMapper.readTree(createResponse).path("data").path("id").asLong();

        mockMvc.perform(post("/api/teacher/exams/" + examId + "/questions")
                .header(HttpHeaders.AUTHORIZATION, bearer(teacherToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"questionIds\":[" + questionId + "]}"))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/teacher/exams/" + examId + "/publish")
                .header(HttpHeaders.AUTHORIZATION, bearer(teacherToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status", is("published")));

        mockMvc.perform(put("/api/teacher/exams/" + examId)
                .header(HttpHeaders.AUTHORIZATION, bearer(teacherToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"发布时间修正测试","description":"修正为立即可考","durationMinutes":45,
                     "startTime":null,"endTime":null}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status", is("published")))
            .andExpect(jsonPath("$.data.durationMinutes", is(45)))
            .andExpect(jsonPath("$.data.startTime", nullValue()))
            .andExpect(jsonPath("$.data.endTime", nullValue()));
    }

    @Test
    void objectiveQuestionWithoutCorrectAnswerFallsBackToManualReview() throws Exception {
        String teacherToken = login("teacher", "Teacher@123456");
        Long questionId = createQuestionWithoutAnswer();

        String createResponse = mockMvc.perform(post("/api/teacher/exams")
                .header(HttpHeaders.AUTHORIZATION, bearer(teacherToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"缺答案选择题测试","description":"manual fallback","durationMinutes":30}
                    """))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
        Long examId = objectMapper.readTree(createResponse).path("data").path("id").asLong();

        mockMvc.perform(post("/api/teacher/exams/" + examId + "/questions")
                .header(HttpHeaders.AUTHORIZATION, bearer(teacherToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"questionIds\":[" + questionId + "]}"))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/teacher/exams/" + examId + "/publish")
                .header(HttpHeaders.AUTHORIZATION, bearer(teacherToken)))
            .andExpect(status().isOk());

        String studentToken = login("student", "Student@123456");

        mockMvc.perform(get("/api/student/exams/" + examId)
                .header(HttpHeaders.AUTHORIZATION, bearer(studentToken)))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/student/exams/" + examId + "/submit")
                .header(HttpHeaders.AUTHORIZATION, bearer(studentToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"answers\":[{\"questionId\":" + questionId + ",\"answer\":\"B\"}]}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.totalScore", is(0.0)));

        String teacherDetail = mockMvc.perform(get("/api/teacher/grading/submissions")
                .header(HttpHeaders.AUTHORIZATION, bearer(teacherToken)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
        Long submissionId = objectMapper.readTree(teacherDetail).path("data").get(0).path("submissionId").asLong();

        mockMvc.perform(get("/api/teacher/grading/submissions/" + submissionId)
                .header(HttpHeaders.AUTHORIZATION, bearer(teacherToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.answers[0].gradingStatus", is("pending")))
            .andExpect(jsonPath("$.data.answers[0].correctAnswer", nullValue()));

        mockMvc.perform(post("/api/teacher/grading/submissions/" + submissionId + "/answers")
                .header(HttpHeaders.AUTHORIZATION, bearer(teacherToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"questionId\":" + questionId + ",\"score\":2,\"teacherComment\":\"人工复核给分\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.totalScore", is(2.0)))
            .andExpect(jsonPath("$.data.answers[0].gradingStatus", is("manual_graded")));
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

    private Long createQuestionWithoutAnswer() {
        Long teacherId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE username = 'teacher'", Long.class);
        jdbcTemplate.update("""
            INSERT INTO questions (question_type, stem, analysis, score, difficulty, knowledge_point, status,
                                   version, created_at, updated_at, created_by, updated_by, is_deleted)
            VALUES ('single_choice', '这道选择题缺少标准答案，教师需要复核。', '', ?, 'normal', 'Manual Review', 'active',
                    1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, ?, ?, 0)
            """, BigDecimal.valueOf(2), teacherId, teacherId);
        Long questionId = jdbcTemplate.queryForObject("SELECT MAX(id) FROM questions", Long.class);
        jdbcTemplate.update("""
            INSERT INTO question_options (question_id, option_key, option_text, is_correct, sort_order, created_at, updated_at)
            VALUES (?, 'A', '选项 A', 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """, questionId);
        jdbcTemplate.update("""
            INSERT INTO question_options (question_id, option_key, option_text, is_correct, sort_order, created_at, updated_at)
            VALUES (?, 'B', '选项 B', 0, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """, questionId);
        return questionId;
    }

    private Long createFillBlankQuestion() {
        Long teacherId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE username = 'teacher'", Long.class);
        jdbcTemplate.update("""
            INSERT INTO questions (question_type, stem, analysis, score, difficulty, knowledge_point, status,
                                   version, created_at, updated_at, created_by, updated_by, is_deleted)
            VALUES ('fill_blank', 'Spring 官方推荐的 Java Web 框架是____。', '', ?, 'normal', 'Phase 4', 'active',
                    1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, ?, ?, 0)
            """, BigDecimal.valueOf(3), teacherId, teacherId);
        Long questionId = jdbcTemplate.queryForObject("SELECT MAX(id) FROM questions", Long.class);
        jdbcTemplate.update("""
            INSERT INTO question_answers (question_id, answer_text, match_rule, created_at, updated_at)
            VALUES (?, 'Spring Boot', 'exact', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """, questionId);
        return questionId;
    }

    private Long createSubjectiveQuestion() {
        Long teacherId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE username = 'teacher'", Long.class);
        jdbcTemplate.update("""
            INSERT INTO questions (question_type, stem, analysis, score, difficulty, knowledge_point, status,
                                   version, created_at, updated_at, created_by, updated_by, is_deleted)
            VALUES ('subjective', '简述为什么在线考试系统需要权限控制。', '', ?, 'normal', 'Phase 4', 'active',
                    1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, ?, ?, 0)
            """, BigDecimal.valueOf(8), teacherId, teacherId);
        Long questionId = jdbcTemplate.queryForObject("SELECT MAX(id) FROM questions", Long.class);
        jdbcTemplate.update("""
            INSERT INTO question_answers (question_id, answer_text, match_rule, created_at, updated_at)
            VALUES (?, '保护考试数据，防止越权访问。', 'manual', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
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
