package com.portal.learningportal.service;

import com.portal.learningportal.dto.AssignmentResponse;
import com.portal.learningportal.dto.AssignmentUpdateRequest;
import com.portal.learningportal.dto.CloudinaryUploadResult;
import com.portal.learningportal.dto.GradeSubmissionRequest;
import com.portal.learningportal.dto.SubmissionResponse;
import com.portal.learningportal.entity.Assignment;
import com.portal.learningportal.entity.CloudinaryResourceType;
import com.portal.learningportal.entity.Course;
import com.portal.learningportal.entity.Role;
import com.portal.learningportal.entity.Submission;
import com.portal.learningportal.entity.SubmissionStatus;
import com.portal.learningportal.entity.User;
import com.portal.learningportal.exception.BadRequestException;
import com.portal.learningportal.exception.ForbiddenException;
import com.portal.learningportal.exception.ResourceNotFoundException;
import com.portal.learningportal.repository.AssignmentRepository;
import com.portal.learningportal.repository.SubmissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

@Service
public class AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final SubmissionRepository submissionRepository;
    private final CurrentUserService currentUserService;
    private final CourseAccessService courseAccessService;
    private final CloudinaryService cloudinaryService;
    private final UploadFileValidator uploadFileValidator;
    private final NotificationService notificationService;

    public AssignmentService(
            AssignmentRepository assignmentRepository,
            SubmissionRepository submissionRepository,
            CurrentUserService currentUserService,
            CourseAccessService courseAccessService,
            CloudinaryService cloudinaryService,
            UploadFileValidator uploadFileValidator,
            NotificationService notificationService
    ) {
        this.assignmentRepository = assignmentRepository;
        this.submissionRepository = submissionRepository;
        this.currentUserService = currentUserService;
        this.courseAccessService = courseAccessService;
        this.cloudinaryService = cloudinaryService;
        this.uploadFileValidator = uploadFileValidator;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public List<AssignmentResponse> listCourseAssignments(Long courseId) {
        User user = currentUserService.requireAuthenticatedUser();
        courseAccessService.requireReadableCourse(user, courseId);
        return assignmentRepository.findByCourseIdWithDetails(courseId).stream()
                .filter(a -> canViewAssignmentMetadata(user, a))
                .sorted(Comparator.comparing(Assignment::getDueAt))
                .map(a -> toResponse(user, a))
                .toList();
    }

    @Transactional
    public AssignmentResponse createAssignment(
            Long courseId,
            String title,
            String description,
            Instant dueAt,
            int maxMarks,
            boolean published,
            MultipartFile attachment
    ) {
        User user = currentUserService.requireAnyRole(Role.TEACHER, Role.ADMIN);
        Course course = courseAccessService.requireManageableCourse(user, courseId);
        if (user.getRole() == Role.TEACHER && !course.getTeacher().getId().equals(user.getId())) {
            throw new ForbiddenException("Access denied");
        }
        if (maxMarks < 1) {
            throw new BadRequestException("maxMarks must be at least 1");
        }

        String attachmentPublicId = null;
        String attachmentSecureUrl = null;
        if (attachment != null && !attachment.isEmpty()) {
            uploadFileValidator.validate(attachment);
            CloudinaryUploadResult upload = cloudinaryService.upload(attachment, CloudinaryResourceType.raw);
            attachmentPublicId = upload.getPublicId();
            attachmentSecureUrl = upload.getSecureUrl();
        }

        Assignment assignment = Assignment.builder()
                .title(title)
                .description(description)
                .dueAt(dueAt)
                .maxMarks(maxMarks)
                .attachmentPublicId(attachmentPublicId)
                .attachmentSecureUrl(attachmentSecureUrl)
                .isPublished(published)
                .course(course)
                .createdBy(user)
                .build();

        Long id = assignmentRepository.save(assignment).getId();
        Assignment saved = assignmentRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found"));
        notificationService.notifyNewAssignment(saved);
        return toResponse(user, saved);
    }

    @Transactional(readOnly = true)
    public AssignmentResponse getAssignment(Long assignmentId) {
        User user = currentUserService.requireAuthenticatedUser();
        Assignment assignment = requireReadableAssignment(user, assignmentId);
        return toResponse(user, assignment);
    }

    @Transactional
    public AssignmentResponse updateAssignment(Long assignmentId, AssignmentUpdateRequest request) {
        User user = currentUserService.requireAnyRole(Role.TEACHER, Role.ADMIN);
        Assignment assignment = assignmentRepository.findByIdWithDetails(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found"));
        requireAssignmentTeacher(user, assignment);

        boolean wasPublished = Boolean.TRUE.equals(assignment.getIsPublished());
        assignment.setTitle(request.getTitle());
        assignment.setDescription(request.getDescription());
        assignment.setDueAt(request.getDueAt());
        assignment.setMaxMarks(request.getMaxMarks());
        assignment.setIsPublished(request.getPublished());
        Assignment saved = assignmentRepository.save(assignment);
        if (Boolean.TRUE.equals(request.getPublished()) && !wasPublished) {
            notificationService.notifyNewAssignment(saved);
        }
        return toResponse(user, saved);
    }

    @Transactional
    public SubmissionResponse submitOrReplace(Long assignmentId, MultipartFile file) {
        User student = currentUserService.requireRole(Role.STUDENT);
        Assignment assignment = assignmentRepository.findByIdWithDetails(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found"));
        if (!Boolean.TRUE.equals(assignment.getIsPublished())) {
            throw new ForbiddenException("Access denied");
        }
        if (!courseAccessService.canReadCourse(student, assignment.getCourse().getId())) {
            throw new ForbiddenException("Access denied");
        }
        if (Instant.now().isAfter(assignment.getDueAt())) {
            throw new BadRequestException("Submission deadline has passed");
        }

        uploadFileValidator.validate(file);
        CloudinaryUploadResult upload = cloudinaryService.upload(file, CloudinaryResourceType.raw);

        var existing = submissionRepository.findByAssignmentIdAndStudentIdWithDetails(
                assignmentId, student.getId());
        if (existing.isPresent()) {
            Submission submission = existing.get();
            cloudinaryService.deleteAsset(submission.getPublicId(), CloudinaryResourceType.raw);
            submission.setPublicId(upload.getPublicId());
            submission.setSecureUrl(upload.getSecureUrl());
            submission.setSubmittedAt(Instant.now());
            submission.setStatus(SubmissionStatus.SUBMITTED);
            submission.setMarks(null);
            submission.setFeedback(null);
            Submission saved = submissionRepository.save(submission);
            return SubmissionResponse.from(saved, true);
        }

        Submission submission = Submission.builder()
                .publicId(upload.getPublicId())
                .secureUrl(upload.getSecureUrl())
                .assignment(assignment)
                .student(student)
                .status(SubmissionStatus.SUBMITTED)
                .build();
        Submission saved = submissionRepository.save(submission);
        return SubmissionResponse.from(saved, true);
    }

    @Transactional(readOnly = true)
    public List<SubmissionResponse> listSubmissions(Long assignmentId) {
        User user = currentUserService.requireAnyRole(Role.TEACHER, Role.ADMIN);
        Assignment assignment = assignmentRepository.findByIdWithDetails(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found"));
        requireAssignmentTeacher(user, assignment);

        return submissionRepository.findByAssignmentIdWithDetails(assignmentId).stream()
                .sorted(Comparator.comparing(Submission::getSubmittedAt))
                .map(s -> SubmissionResponse.from(s, true))
                .toList();
    }

    @Transactional
    public SubmissionResponse gradeSubmission(Long submissionId, GradeSubmissionRequest request) {
        User user = currentUserService.requireAnyRole(Role.TEACHER, Role.ADMIN);
        Submission submission = submissionRepository.findByIdWithDetails(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Submission not found"));
        Assignment assignment = submission.getAssignment();
        requireAssignmentTeacher(user, assignment);

        if (request.getMarks() > assignment.getMaxMarks()) {
            throw new BadRequestException("Marks cannot exceed assignment max marks");
        }

        submission.setMarks(request.getMarks());
        submission.setFeedback(request.getFeedback());
        submission.setStatus(SubmissionStatus.GRADED);
        Submission saved = submissionRepository.save(submission);
        notificationService.notifySubmissionGraded(saved);
        return SubmissionResponse.from(saved, true);
    }

    private Assignment requireReadableAssignment(User user, Long assignmentId) {
        Assignment assignment = assignmentRepository.findByIdWithDetails(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found"));
        if (!canViewAssignmentMetadata(user, assignment)) {
            throw new ForbiddenException("Access denied");
        }
        if (!courseAccessService.canReadCourse(user, assignment.getCourse().getId())) {
            throw new ForbiddenException("Access denied");
        }
        return assignment;
    }

    private boolean canViewAssignmentMetadata(User user, Assignment assignment) {
        if (user.getRole() == Role.STUDENT) {
            return Boolean.TRUE.equals(assignment.getIsPublished());
        }
        return true;
    }

    private void requireAssignmentTeacher(User user, Assignment assignment) {
        if (user.getRole() == Role.ADMIN) {
            return;
        }
        if (user.getRole() == Role.TEACHER
                && assignment.getCourse().getTeacher().getId().equals(user.getId())) {
            return;
        }
        throw new ForbiddenException("Access denied");
    }

    private AssignmentResponse toResponse(User user, Assignment assignment) {
        boolean includeAttachment = courseAccessService.canReadCourse(user, assignment.getCourse().getId());
        SubmissionResponse mySubmission = null;
        if (user.getRole() == Role.STUDENT) {
            mySubmission = submissionRepository.findByAssignmentIdAndStudentIdWithDetails(
                            assignment.getId(), user.getId())
                    .map(s -> SubmissionResponse.from(s, true))
                    .orElse(null);
        }
        return AssignmentResponse.from(assignment, includeAttachment, mySubmission);
    }
}