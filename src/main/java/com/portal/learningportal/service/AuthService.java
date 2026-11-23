package com.portal.learningportal.service;

import com.portal.learningportal.dto.ChangePasswordRequest;
import com.portal.learningportal.dto.LoginRequest;
import com.portal.learningportal.dto.LoginResponse;
import com.portal.learningportal.dto.ProfileUpdateRequest;
import com.portal.learningportal.dto.UserProfileResponse;
import com.portal.learningportal.entity.User;
import com.portal.learningportal.exception.ConflictException;
import com.portal.learningportal.exception.ResourceNotFoundException;
import com.portal.learningportal.exception.UnauthorizedException;
import com.portal.learningportal.repository.UserRepository;
import com.portal.learningportal.security.JwtService;
import com.portal.learningportal.security.UserPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsernameOrEmail(request.getUsername())
                .orElseThrow(() -> new UnauthorizedException("Invalid username or password"));

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new UnauthorizedException("Invalid username or password");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid username or password");
        }

        String token = jwtService.generateToken(user);
        return LoginResponse.builder()
                .accessToken(token)
                .role(user.getRole())
                .profile(UserProfileResponse.from(user))
                .build();
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getCurrentUser() {
        return UserProfileResponse.from(getAuthenticatedUser());
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        User user = getAuthenticatedUser();
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Current password is incorrect");
        }
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Transactional
    public UserProfileResponse updateProfile(ProfileUpdateRequest request) {
        User user = getAuthenticatedUser();
        if (!user.getEmail().equalsIgnoreCase(request.getEmail())
                && userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email is already in use");
        }
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        return UserProfileResponse.from(userRepository.save(user));
    }

  private User getAuthenticatedUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof UserPrincipal userPrincipal)) {
            throw new UnauthorizedException("Authentication required");
        }
        return userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}