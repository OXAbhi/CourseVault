package com.portal.learningportal.service;

import com.portal.learningportal.dto.UserCreateRequest;
import com.portal.learningportal.dto.UserResponse;
import com.portal.learningportal.dto.UserStatusPatchRequest;
import com.portal.learningportal.dto.UserUpdateRequest;
import com.portal.learningportal.entity.Role;
import com.portal.learningportal.entity.User;
import com.portal.learningportal.exception.ConflictException;
import com.portal.learningportal.exception.ForbiddenException;
import com.portal.learningportal.exception.ResourceNotFoundException;
import com.portal.learningportal.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserAdminService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CurrentUserService currentUserService;

    public UserAdminService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            CurrentUserService currentUserService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> listUsers() {
        currentUserService.requireRole(Role.ADMIN);
        return userRepository.findAll().stream().map(UserResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public UserResponse getUser(Long id) {
        currentUserService.requireRole(Role.ADMIN);
        return UserResponse.from(findUserOrThrow(id));
    }

    @Transactional
    public UserResponse createUser(UserCreateRequest request) {
        currentUserService.requireRole(Role.ADMIN);
        if (request.getRole() == Role.ADMIN) {
            throw new ForbiddenException("Cannot create admin accounts via API");
        }
        if (request.getRole() != Role.TEACHER && request.getRole() != Role.STUDENT) {
            throw new ForbiddenException("Only teacher or student accounts can be created");
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ConflictException("Username already exists");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email already exists");
        }
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .isActive(true)
                .build();
        return UserResponse.from(userRepository.save(user));
    }

    @Transactional
    public UserResponse updateUser(Long id, UserUpdateRequest request) {
        currentUserService.requireRole(Role.ADMIN);
        User user = findUserOrThrow(id);
        if (!user.getEmail().equalsIgnoreCase(request.getEmail()) && userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email already exists");
        }
        user.setEmail(request.getEmail());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        return UserResponse.from(userRepository.save(user));
    }

    @Transactional
    public UserResponse patchUserStatus(Long id, UserStatusPatchRequest request) {
        currentUserService.requireRole(Role.ADMIN);
        User user = findUserOrThrow(id);
        if (user.getRole() == Role.ADMIN) {
            throw new ForbiddenException("Cannot deactivate admin via this endpoint");
        }
        user.setIsActive(request.getActive());
        return UserResponse.from(userRepository.save(user));
    }

    private User findUserOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}