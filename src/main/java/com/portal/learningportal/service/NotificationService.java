package com.portal.learningportal.service;

import com.portal.learningportal.dto.NotificationResponse;
import com.portal.learningportal.entity.Assignment;
import com.portal.learningportal.entity.Quiz;
import com.portal.learningportal.entity.EnrollmentStatus;
import com.portal.learningportal.entity.Notification;
import com.portal.learningportal.entity.Resource;
import com.portal.learningportal.entity.Submission;
import com.portal.learningportal.entity.User;
import com.portal.learningportal.exception.ForbiddenException;
import com.portal.learningportal.exception.ResourceNotFoundException;
import com.portal.learningportal.repository.EnrollmentRepository;
import com.portal.learningportal.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CurrentUserService currentUserService;

    public NotificationService(
            NotificationRepository notificationRepository,
            EnrollmentRepository enrollmentRepository,
            CurrentUserService currentUserService
    ) {
        this.notificationRepository = notificationRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional
    public void notifyResourcePublished(Resource resource) {
        String title = "New course material";
        String body = "%s — %s".formatted(resource.getCourse().getCode(), resource.getTitle());
        String link = "/resources/" + resource.getId();
        notifyActiveEnrolledStudents(resource.getCourse().getId(), title, body, link);
    }

    @Transactional
    public void notifyNewAssignment(Assignment assignment) {
        if (!Boolean.TRUE.equals(assignment.getIsPublished())) {
            return;
        }
        String title = "New assignment";
        String body = "%s — %s (due %s)".formatted(
                assignment.getCourse().getCode(),
                assignment.getTitle(),
                assignment.getDueAt());
        String link = "/assignments/" + assignment.getId();
        notifyActiveEnrolledStudents(assignment.getCourse().getId(), title, body, link);
    }

    @Transactional
    public void notifyQuizPublished(Quiz quiz) {
        if (!Boolean.TRUE.equals(quiz.getIsPublished())) {
            return;
        }
        String title = "New practice quiz";
        String body = "%s — %s".formatted(quiz.getCourse().getCode(), quiz.getTitle());
        String link = "/student/quizzes/" + quiz.getId();
        notifyActiveEnrolledStudents(quiz.getCourse().getId(), title, body, link);
    }

    @Transactional
    public void notifySubmissionGraded(Submission submission) {
        Assignment assignment = submission.getAssignment();
        String title = "Grade posted";
        String body = "%s — %s/%d marks for %s".formatted(
                assignment.getCourse().getCode(),
                submission.getMarks(),
                assignment.getMaxMarks(),
                assignment.getTitle());
        String link = "/assignments/" + assignment.getId();
        notificationRepository.save(Notification.builder()
                .user(submission.getStudent())
                .title(title)
                .body(body)
                .link(link)
                .build());
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> listMine() {
        User user = currentUserService.requireAuthenticatedUser();
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(NotificationResponse::from)
                .toList();
    }

    @Transactional
    public NotificationResponse markRead(Long notificationId) {
        User user = currentUserService.requireAuthenticatedUser();
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        if (!notification.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("Access denied");
        }
        notification.setIsRead(true);
        return NotificationResponse.from(notificationRepository.save(notification));
    }

    private void notifyActiveEnrolledStudents(Long courseId, String title, String body, String link) {
        List<User> students = enrollmentRepository.findActiveStudentsByCourseId(courseId, EnrollmentStatus.ACTIVE);
        for (User student : students) {
            notificationRepository.save(Notification.builder()
                    .user(student)
                    .title(title)
                    .body(body)
                    .link(link)
                    .build());
        }
    }
}