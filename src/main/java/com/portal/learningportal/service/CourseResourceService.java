package com.portal.learningportal.service;

import com.portal.learningportal.dto.CloudinaryUploadResult;
import com.portal.learningportal.dto.ResourceAccessResponse;
import com.portal.learningportal.dto.ResourceResponse;
import com.portal.learningportal.entity.CloudinaryResourceType;
import com.portal.learningportal.entity.Course;
import com.portal.learningportal.entity.Resource;
import com.portal.learningportal.entity.ResourceType;
import com.portal.learningportal.entity.Role;
import com.portal.learningportal.entity.Tag;
import com.portal.learningportal.entity.User;
import com.portal.learningportal.exception.ForbiddenException;
import com.portal.learningportal.exception.ResourceNotFoundException;
import com.portal.learningportal.repository.ResourceRepository;
import com.portal.learningportal.repository.TagRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
@Service
public class CourseResourceService {

    private final ResourceRepository resourceRepository;
    private final TagRepository tagRepository;
    private final CurrentUserService currentUserService;
    private final CourseAccessService courseAccessService;
    private final CloudinaryService cloudinaryService;
    private final UploadFileValidator uploadFileValidator;
    private final NotificationService notificationService;

    public CourseResourceService(
            ResourceRepository resourceRepository,
            TagRepository tagRepository,
            CurrentUserService currentUserService,
            CourseAccessService courseAccessService,
            CloudinaryService cloudinaryService,
            UploadFileValidator uploadFileValidator,
            NotificationService notificationService
    ) {
        this.resourceRepository = resourceRepository;
        this.tagRepository = tagRepository;
        this.currentUserService = currentUserService;
        this.courseAccessService = courseAccessService;
        this.cloudinaryService = cloudinaryService;
        this.uploadFileValidator = uploadFileValidator;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public List<ResourceResponse> listCourseResources(Long courseId) {
        User user = currentUserService.requireAuthenticatedUser();
        courseAccessService.requireReadableCourse(user, courseId);
        List<Resource> resources = resourceRepository.findByCourseIdWithDetails(courseId);
        return resources.stream()
                .filter(r -> canViewResourceMetadata(user, r))
                .sorted(Comparator.comparing(Resource::getCreatedAt).reversed())
                .map(r -> ResourceResponse.from(r, false))
                .toList();
    }

    @Transactional
    public ResourceResponse uploadResource(
            Long courseId,
            String title,
            String description,
            ResourceType resourceType,
            String tagsCsv,
            boolean published,
            MultipartFile file
    ) {
        User user = currentUserService.requireAnyRole(Role.TEACHER, Role.ADMIN);
        Course course = courseAccessService.requireManageableCourse(user, courseId);
        if (user.getRole() == Role.TEACHER && !course.getTeacher().getId().equals(user.getId())) {
            throw new ForbiddenException("Access denied");
        }

        CloudinaryResourceType cloudinaryType = resolveCloudinaryType(resourceType);
        uploadFileValidator.validate(file);
        CloudinaryUploadResult upload = cloudinaryService.upload(file, cloudinaryType);

        Resource resource = Resource.builder()
                .title(title)
                .description(description)
                .resourceType(resourceType)
                .publicId(upload.getPublicId())
                .secureUrl(upload.getSecureUrl())
                .cloudinaryResourceType(upload.getCloudinaryResourceType())
                .mimeType(file.getContentType())
                .fileSize(file.getSize())
                .isPublished(published)
                .course(course)
                .uploadedBy(user)
                .tags(resolveTags(tagsCsv))
                .build();

        Long id = resourceRepository.save(resource).getId();
        Resource saved = resourceRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found"));
        if (published) {
            notificationService.notifyResourcePublished(saved);
        }
        return ResourceResponse.from(saved, true);
    }

    @Transactional(readOnly = true)
    public ResourceResponse getResource(Long resourceId) {
        User user = currentUserService.requireAuthenticatedUser();
        Resource resource = requireReadableResource(user, resourceId);
        boolean includeUrl = canIncludeSecureUrl(user, resource);
        return ResourceResponse.from(resource, includeUrl);
    }

    @Transactional(readOnly = true)
    public ResourceAccessResponse getResourceAccess(Long resourceId) {
        User user = currentUserService.requireAuthenticatedUser();
        Resource resource = requireReadableResource(user, resourceId);
        if (!canIncludeSecureUrl(user, resource)) {
            throw new ForbiddenException("Access denied");
        }
        return ResourceAccessResponse.builder()
                .secureUrl(resource.getSecureUrl())
                .resourceType(resource.getResourceType())
                .cloudinaryResourceType(resource.getCloudinaryResourceType())
                .mimeType(resource.getMimeType())
                .build();
    }

    @Transactional
    public void deleteResource(Long resourceId) {
        User user = currentUserService.requireAuthenticatedUser();
        Resource resource = resourceRepository.findByIdWithDetails(resourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found"));
        if (user.getRole() == Role.ADMIN) {
            // allowed
        } else if (user.getRole() == Role.TEACHER
                && resource.getCourse().getTeacher().getId().equals(user.getId())) {
            // allowed
        } else {
            throw new ForbiddenException("Access denied");
        }
        cloudinaryService.deleteAsset(resource.getPublicId(), resource.getCloudinaryResourceType());
        resourceRepository.delete(resource);
    }

    private Resource requireReadableResource(User user, Long resourceId) {
        Resource resource = resourceRepository.findByIdWithDetails(resourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found"));
        if (!canViewResourceMetadata(user, resource)) {
            throw new ForbiddenException("Access denied");
        }
        return resource;
    }

    private boolean canViewResourceMetadata(User user, Resource resource) {
        if (!courseAccessService.canReadCourse(user, resource.getCourse().getId())) {
            return false;
        }
        if (user.getRole() == Role.STUDENT) {
            return Boolean.TRUE.equals(resource.getIsPublished());
        }
        return true;
    }

    private boolean canIncludeSecureUrl(User user, Resource resource) {
        return canViewResourceMetadata(user, resource);
    }

    private CloudinaryResourceType resolveCloudinaryType(ResourceType resourceType) {
        return resourceType == ResourceType.LECTURE
                ? CloudinaryResourceType.video
                : CloudinaryResourceType.raw;
    }

    private Set<Tag> resolveTags(String tagsCsv) {
        if (tagsCsv == null || tagsCsv.isBlank()) {
            return new HashSet<>();
        }
        Set<Tag> tags = new HashSet<>();
        for (String name : Arrays.stream(tagsCsv.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList()) {
            String slug = toSlug(name);
            Tag tag = tagRepository.findBySlug(slug).orElseGet(() -> tagRepository.save(
                    Tag.builder().name(name).slug(slug).build()));
            tags.add(tag);
        }
        return tags;
    }

    private static String toSlug(String name) {
        return name.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
    }
}