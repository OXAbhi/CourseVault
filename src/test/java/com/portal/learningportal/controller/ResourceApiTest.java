package com.portal.learningportal.controller;

import com.portal.learningportal.dto.CloudinaryUploadResult;
import com.portal.learningportal.entity.CloudinaryResourceType;
import com.portal.learningportal.repository.UserRepository;
import com.portal.learningportal.service.CloudinaryService;
import com.portal.learningportal.support.ApiTestSupport;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Sql(scripts = {"/db/demo-users.sql", "/db/demo-cs-catalog.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
class ResourceApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private CloudinaryService cloudinaryService;

    @Test
    void enrolledStudentGetsSecureUrl_unenrolledStudentForbidden() throws Exception {
        when(cloudinaryService.upload(any(), eq(CloudinaryResourceType.raw)))
                .thenReturn(CloudinaryUploadResult.builder()
                        .publicId("notes/dsa-unit1")
                        .secureUrl("https://res.cloudinary.com/demo/raw/upload/v1/notes/dsa-unit1.pdf")
                        .cloudinaryResourceType(CloudinaryResourceType.raw)
                        .build());

        long teacher1Id = userRepository.findByUsername("teacher1").orElseThrow().getId();
        String adminToken = ApiTestSupport.loginAndGetToken(mockMvc, "admin");

        MvcResult createCourse = mockMvc.perform(post("/api/courses")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code":"CS-JAVA-301",
                                  "title":"Advanced Java",
                                  "teacherId":%d
                                }
                                """.formatted(teacher1Id)))
                .andExpect(status().isCreated())
                .andReturn();

        long courseId = ((Number) com.jayway.jsonpath.JsonPath.read(
                createCourse.getResponse().getContentAsString(), "$.id")).longValue();

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "unit1.pdf",
                "application/pdf",
                "%PDF-1.4 test".getBytes());

        String teacherToken = ApiTestSupport.loginAndGetToken(mockMvc, "teacher1");

        MvcResult upload = mockMvc.perform(multipart("/api/courses/" + courseId + "/resources")
                        .file(file)
                        .param("title", "Unit 1 Notes")
                        .param("resourceType", "NOTE")
                        .param("tags", "java,algorithms")
                        .param("published", "true")
                        .with(request -> {
                            request.setMethod("POST");
                            request.addHeader("Authorization", "Bearer " + teacherToken);
                            return request;
                        }))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.secureUrl").isNotEmpty())
                .andReturn();

        long resourceId = ((Number) com.jayway.jsonpath.JsonPath.read(
                upload.getResponse().getContentAsString(), "$.id")).longValue();

        long student1Id = userRepository.findByUsername("student1").orElseThrow().getId();
        String batchName = "CS-JAVA-A-2025";
        mockMvc.perform(post("/api/courses/" + courseId + "/batches")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s","academicYear":"2025-26"}
                                """.formatted(batchName)))
                .andExpect(status().isCreated());

        long batchId = ((Number) com.jayway.jsonpath.JsonPath.read(
                mockMvc.perform(get("/api/courses/" + courseId + "/batches")
                                .header("Authorization", "Bearer " + adminToken))
                        .andReturn().getResponse().getContentAsString(),
                "$[0].id")).longValue();

        mockMvc.perform(post("/api/enrollments")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"studentId":%d,"batchId":%d}
                                """.formatted(student1Id, batchId)))
                .andExpect(status().isCreated());

        String student1Token = ApiTestSupport.loginAndGetToken(mockMvc, "student1");
        mockMvc.perform(get("/api/resources/" + resourceId)
                        .header("Authorization", "Bearer " + student1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.secureUrl").isNotEmpty());

        String student2Token = ApiTestSupport.loginAndGetToken(mockMvc, "student2");
        mockMvc.perform(get("/api/resources/" + resourceId)
                        .header("Authorization", "Bearer " + student2Token))
                .andExpect(status().isForbidden());
    }

    @Test
    void invalidFileTypeRejectedBeforeCloudinary() throws Exception {
        String teacherToken = ApiTestSupport.loginAndGetToken(mockMvc, "teacher1");
        long courseId = ((Number) com.jayway.jsonpath.JsonPath.read(
                mockMvc.perform(get("/api/courses")
                                .header("Authorization", "Bearer " + teacherToken))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                "$[0].id")).longValue();

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "malware.exe",
                "application/octet-stream",
                "bad".getBytes());

        mockMvc.perform(multipart("/api/courses/" + courseId + "/resources")
                        .file(file)
                        .param("title", "Bad file")
                        .param("resourceType", "NOTE")
                        .with(request -> {
                            request.setMethod("POST");
                            request.addHeader("Authorization", "Bearer " + teacherToken);
                            return request;
                        }))
                .andExpect(status().isUnsupportedMediaType());

        verify(cloudinaryService, never()).upload(any(), any());
    }
}