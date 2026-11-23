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
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Sql(scripts = {"/db/demo-users.sql", "/db/demo-cs-catalog.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
class NotificationSearchApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CloudinaryService cloudinaryService;

    @Test
    void publishedResourceNotifiesEnrolledStudent_andSearchIsScoped() throws Exception {
        when(cloudinaryService.upload(any(), eq(CloudinaryResourceType.raw)))
                .thenReturn(CloudinaryUploadResult.builder()
                        .publicId("notes/graph-notify")
                        .secureUrl("https://res.cloudinary.com/demo/raw/upload/v1/notes/graph-notify.pdf")
                        .cloudinaryResourceType(CloudinaryResourceType.raw)
                        .build());

        String teacherToken = ApiTestSupport.loginAndGetToken(mockMvc, "teacher1");
        long courseId = ((Number) com.jayway.jsonpath.JsonPath.read(
                mockMvc.perform(get("/api/courses")
                                .header("Authorization", "Bearer " + teacherToken))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                "$[0].id")).longValue();

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "graphs.pdf",
                "application/pdf",
                "%PDF-1.4 graphs".getBytes());

        mockMvc.perform(multipart("/api/courses/" + courseId + "/resources")
                        .file(file)
                        .param("title", "Graph Algorithms Notes notify-m6-unique")
                        .param("resourceType", "NOTE")
                        .param("tags", "graphs,dsa")
                        .param("published", "true")
                        .with(request -> {
                            request.setMethod("POST");
                            request.addHeader("Authorization", "Bearer " + teacherToken);
                            return request;
                        }))
                .andExpect(status().isCreated());

        String student1Token = ApiTestSupport.loginAndGetToken(mockMvc, "student1");
        MvcResult student1Notifications = mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + student1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.title=='New course material' "
                        + "&& @.body =~ /.*notify-m6-unique.*/)]").isArray())
                .andReturn();

        var notificationIds = com.jayway.jsonpath.JsonPath.<List<Number>>read(
                student1Notifications.getResponse().getContentAsString(),
                "$[?(@.body =~ /.*notify-m6-unique.*/)].id");
        long notificationId = notificationIds.get(0).longValue();

        mockMvc.perform(patch("/api/notifications/" + notificationId + "/read")
                        .header("Authorization", "Bearer " + student1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.read").value(true));

        mockMvc.perform(get("/api/search")
                        .header("Authorization", "Bearer " + student1Token)
                        .param("q", "notify-m6-unique")
                        .param("type", "NOTE")
                        .param("tag", "graphs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Graph Algorithms Notes notify-m6-unique"));

        String student2Token = ApiTestSupport.loginAndGetToken(mockMvc, "student2");
        mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + student2Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        mockMvc.perform(get("/api/search")
                        .header("Authorization", "Bearer " + student2Token)
                        .param("q", "notify-m6-unique"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void assignmentAndGradeNotifications() throws Exception {
        when(cloudinaryService.upload(any(), eq(CloudinaryResourceType.raw)))
                .thenReturn(CloudinaryUploadResult.builder()
                        .publicId("submissions/tree")
                        .secureUrl("https://res.cloudinary.com/demo/raw/upload/v1/submissions/tree.pdf")
                        .cloudinaryResourceType(CloudinaryResourceType.raw)
                        .build());

        String teacherToken = ApiTestSupport.loginAndGetToken(mockMvc, "teacher1");
        long courseId = ((Number) com.jayway.jsonpath.JsonPath.read(
                mockMvc.perform(get("/api/courses")
                                .header("Authorization", "Bearer " + teacherToken))
                        .andReturn().getResponse().getContentAsString(),
                "$[0].id")).longValue();

        Instant dueAt = Instant.now().plusSeconds(86400);
        MvcResult assignment = mockMvc.perform(multipart("/api/courses/" + courseId + "/assignments")
                        .param("title", "Tree Homework")
                        .param("dueAt", dueAt.toString())
                        .param("maxMarks", "15")
                        .param("published", "true")
                        .with(request -> {
                            request.setMethod("POST");
                            request.addHeader("Authorization", "Bearer " + teacherToken);
                            return request;
                        }))
                .andExpect(status().isCreated())
                .andReturn();

        long assignmentId = ((Number) com.jayway.jsonpath.JsonPath.read(
                assignment.getResponse().getContentAsString(), "$.id")).longValue();

        String student1Token = ApiTestSupport.loginAndGetToken(mockMvc, "student1");
        mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + student1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.title=='New assignment')]").exists());

        MockMultipartFile file = new MockMultipartFile(
                "file", "trees.pdf", "application/pdf", "%PDF-1.4".getBytes());
        MvcResult submit = mockMvc.perform(multipart("/api/assignments/" + assignmentId + "/submissions")
                        .file(file)
                        .with(request -> {
                            request.setMethod("POST");
                            request.addHeader("Authorization", "Bearer " + student1Token);
                            return request;
                        }))
                .andExpect(status().isCreated())
                .andReturn();

        long submissionId = ((Number) com.jayway.jsonpath.JsonPath.read(
                submit.getResponse().getContentAsString(), "$.id")).longValue();

        mockMvc.perform(put("/api/submissions/" + submissionId + "/grade")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"marks\":14,\"feedback\":\"Nice trees.\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + student1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.title=='Grade posted')]").exists());
    }
}