package com.portal.learningportal.service;

import com.portal.learningportal.entity.Role;
import com.portal.learningportal.entity.User;
import com.portal.learningportal.exception.ForbiddenException;
import com.portal.learningportal.exception.ResourceNotFoundException;
import com.portal.learningportal.exception.UnauthorizedException;
import com.portal.learningportal.repository.UserRepository;
import com.portal.learningportal.security.UserPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CurrentUserService {

    private final UserRepository userRepository;

    public CurrentUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public User requireAuthenticatedUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof UserPrincipal userPrincipal)) {
            throw new UnauthorizedException("Authentication required");
        }
        return userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    @Transactional(readOnly = true)
    public User requireRole(Role role) {
        User user = requireAuthenticatedUser();
        if (user.getRole() != role) {
            throw new ForbiddenException("Access denied");
        }
        return user;
    }

    @Transactional(readOnly = true)
    public User requireAnyRole(Role... roles) {
        User user = requireAuthenticatedUser();
        for (Role role : roles) {
            if (user.getRole() == role) {
                return user;
            }
        }
        throw new ForbiddenException("Access denied");
    }
}