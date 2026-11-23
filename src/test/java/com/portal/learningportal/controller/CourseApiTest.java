package com.portal.learningportal.controller;

import com.jayway.jsonpath.JsonPath;
import com.portal.learningportal.repository.UserRepository;
import com.portal.learningportal.support.ApiTestSupport;
import com.portal.learningportal.support.DemoCredentials;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Sql(scripts = "/db/demo-users.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
class CourseApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Test
    void studentCannotSeeOtherStudentsCourse() throws Exception {
        long teacher1Id = userRepository.findByUsername("teacher1").orElseThrow().getId();
        long student1Id = userRepository.findByUsername("student1").orElseThrow().getId();
        long student2Id = userRepository.findByUsername("student2").orElseThrow().getId();

        String adminToken = ApiTestSupport.loginAndGetToken(mockMvc, "admin");

        MvcResult createCourse = mockMvc.perform(post("/api/courses")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code":"CS-DSA-101",
                                  "title":"Data Structures & Algorithms",
                                  "description":"CS core",
                                  "teacherId":%d
                                }
                                """.formatted(teacher1Id)))
                .andExpect(status().isCreated())
                .andReturn();
        long courseId = ((Number) JsonPath.read(createCourse.getResponse().getContentAsString(), "$.id")).longValue();

        MvcResult createBatch = mockMvc.perform(post("/api/courses/" + courseId + "/batches")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"CS-DSA-A-2025",
                                  "academicYear":"2025-26"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        long batchId = ((Number) JsonPath.read(createBatch.getResponse().getContentAsString(), "$.id")).longValue();

        mockMvc.perform(post("/api/enrollments")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"studentId":%d,"batchId":%d}
                                """.formatted(student1Id, batchId)))
                .andExpect(status().isCreated());

        String student2Token = ApiTestSupport.loginAndGetToken(mockMvc, "student2");

        mockMvc.perform(get("/api/courses/" + courseId)
                        .header("Authorization", "Bearer " + student2Token))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/courses")
                        .header("Authorization", "Bearer " + student2Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void teacherCannotUpdateAnotherTeachersCourse() throws Exception {
        long teacher1Id = userRepository.findByUsername("teacher1").orElseThrow().getId();
        long teacher2Id = userRepository.findByUsername("teacher2").orElseThrow().getId();

        String adminToken = ApiTestSupport.loginAndGetToken(mockMvc, "admin");

        MvcResult createCourse = mockMvc.perform(post("/api/courses")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code":"CS-WEB-201",
                                  "title":"Web Technologies",
                                  "teacherId":%d
                                }
                                """.formatted(teacher1Id)))
                .andExpect(status().isCreated())
                .andReturn();
        long courseId = ((Number) JsonPath.read(createCourse.getResponse().getContentAsString(), "$.id")).longValue();

        String teacher2Token = ApiTestSupport.loginAndGetToken(mockMvc, "teacher2");

        mockMvc.perform(put("/api/courses/" + courseId)
                        .header("Authorization", "Bearer " + teacher2Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title":"Hacked title",
                                  "description":"",
                                  "teacherId":%d
                                }
                                """.formatted(teacher2Id)))
                .andExpect(status().isForbidden());
    }

    @Test
    void roleFilteredCourseList() throws Exception {
        String teacher1Token = ApiTestSupport.loginAndGetToken(mockMvc, "teacher1");
        mockMvc.perform(get("/api/courses")
                        .header("Authorization", "Bearer " + teacher1Token))
                .andExpect(status().isOk());
        // Catalog may be empty until admin creates courses; login + endpoint must succeed.
    }
}