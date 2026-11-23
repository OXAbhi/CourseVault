package com.portal.learningportal.controller;

import com.portal.learningportal.dto.CloudinaryUploadResult;
import com.portal.learningportal.entity.CloudinaryResourceType;
import com.portal.learningportal.service.CloudinaryService;
import com.portal.learningportal.support.ApiTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Sql(scripts = {"/db/demo-users.sql", "/db/demo-cs-catalog.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
class AssignmentApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CloudinaryService cloudinaryService;

  @Test
  void studentSubmitReplaceGrade_andDeadlineEnforced() throws Exception {
        when(cloudinaryService.upload(any(), eq(CloudinaryResourceType.raw)))
                .thenReturn(
                        CloudinaryUploadResult.builder()
                                .publicId("submissions/attempt1")
                                .secureUrl("https://res.cloudinary.com/demo/raw/upload/v1/submissions/attempt1.pdf")
                                .cloudinaryResourceType(CloudinaryResourceType.raw)
                                .build(),
                        CloudinaryUploadResult.builder()
                                .publicId("submissions/attempt2")
                                .secureUrl("https://res.cloudinary.com/demo/raw/upload/v1/submissions/attempt2.pdf")
                                .cloudinaryResourceType(CloudinaryResourceType.raw)
                                .build());

        String teacherToken = ApiTestSupport.loginAndGetToken(mockMvc, "teacher1");
        long courseId = ((Number) com.jayway.jsonpath.JsonPath.read(
                mockMvc.perform(get("/api/courses")
                                .header("Authorization", "Bearer " + teacherToken))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                "$[0].id")).longValue();

        Instant dueAt = Instant.now().plusSeconds(3600 * 24);
        MvcResult createAssignment = mockMvc.perform(multipart("/api/courses/" + courseId + "/assignments")
                        .param("title", "DSA Problem Set 1")
                        .param("description", "Graph traversal exercises")
                        .param("dueAt", dueAt.toString())
                        .param("maxMarks", "20")
                        .param("published", "true")
                        .with(request -> {
                            request.setMethod("POST");
                            request.addHeader("Authorization", "Bearer " + teacherToken);
                            return request;
                        }))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.published").value(true))
                .andReturn();

        long assignmentId = ((Number) com.jayway.jsonpath.JsonPath.read(
                createAssignment.getResponse().getContentAsString(), "$.id")).longValue();

        MockMultipartFile file1 = new MockMultipartFile(
                "file",
                "ps1.pdf",
                "application/pdf",
                "%PDF-1.4 one".getBytes());
        String student1Token = ApiTestSupport.loginAndGetToken(mockMvc, "student1");

        MvcResult firstSubmit = mockMvc.perform(multipart("/api/assignments/" + assignmentId + "/submissions")
                        .file(file1)
                        .with(request -> {
                            request.setMethod("POST");
                            request.addHeader("Authorization", "Bearer " + student1Token);
                            return request;
                        }))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.secureUrl").isNotEmpty())
                .andReturn();

        long submissionId = ((Number) com.jayway.jsonpath.JsonPath.read(
                firstSubmit.getResponse().getContentAsString(), "$.id")).longValue();

        MockMultipartFile file2 = new MockMultipartFile(
                "file",
                "ps1-revised.pdf",
                "application/pdf",
                "%PDF-1.4 two".getBytes());

        mockMvc.perform(multipart("/api/assignments/" + assignmentId + "/submissions")
                        .file(file2)
                        .with(request -> {
                            request.setMethod("POST");
                            request.addHeader("Authorization", "Bearer " + student1Token);
                            return request;
                        }))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(submissionId))
                .andExpect(jsonPath("$.status").value("SUBMITTED"));

        String student2Token = ApiTestSupport.loginAndGetToken(mockMvc, "student2");
        mockMvc.perform(multipart("/api/assignments/" + assignmentId + "/submissions")
                        .file(file1)
                        .with(request -> {
                            request.setMethod("POST");
                            request.addHeader("Authorization", "Bearer " + student2Token);
                            return request;
                        }))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/assignments/" + assignmentId + "/submissions")
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].studentUsername").value("student1"));

        mockMvc.perform(put("/api/submissions/" + submissionId + "/grade")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"marks":18,"feedback":"Strong work on BFS."}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("GRADED"))
                .andExpect(jsonPath("$.marks").value(18));

        mockMvc.perform(get("/api/assignments/" + assignmentId)
                        .header("Authorization", "Bearer " + student1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mySubmission.marks").value(18))
                .andExpect(jsonPath("$.mySubmission.feedback").value("Strong work on BFS."));
    }

    @Test
    void submitAfterDeadlineRejected() throws Exception {
        when(cloudinaryService.upload(any(), eq(CloudinaryResourceType.raw)))
                .thenReturn(CloudinaryUploadResult.builder()
                        .publicId("submissions/late")
                        .secureUrl("https://res.cloudinary.com/demo/raw/upload/v1/submissions/late.pdf")
                        .cloudinaryResourceType(CloudinaryResourceType.raw)
                        .build());

        String teacherToken = ApiTestSupport.loginAndGetToken(mockMvc, "teacher1");
        long courseId = ((Number) com.jayway.jsonpath.JsonPath.read(
                mockMvc.perform(get("/api/courses")
                                .header("Authorization", "Bearer " + teacherToken))
                        .andReturn().getResponse().getContentAsString(),
                "$[0].id")).longValue();

        Instant pastDue = Instant.now().minusSeconds(3600);
        MvcResult createAssignment = mockMvc.perform(multipart("/api/courses/" + courseId + "/assignments")
                        .param("title", "Past deadline lab")
                        .param("dueAt", pastDue.toString())
                        .param("maxMarks", "10")
                        .param("published", "true")
                        .with(request -> {
                            request.setMethod("POST");
                            request.addHeader("Authorization", "Bearer " + teacherToken);
                            return request;
                        }))
                .andExpect(status().isCreated())
                .andReturn();

        long assignmentId = ((Number) com.jayway.jsonpath.JsonPath.read(
                createAssignment.getResponse().getContentAsString(), "$.id")).longValue();

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "late.pdf",
                "application/pdf",
                "%PDF-1.4".getBytes());
        String student1Token = ApiTestSupport.loginAndGetToken(mockMvc, "student1");

        mockMvc.perform(multipart("/api/assignments/" + assignmentId + "/submissions")
                        .file(file)
                        .with(request -> {
                            request.setMethod("POST");
                            request.addHeader("Authorization", "Bearer " + student1Token);
                            return request;
                        }))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Submission deadline has passed"));
    }
}