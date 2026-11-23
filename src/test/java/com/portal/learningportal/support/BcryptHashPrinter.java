package com.portal.learningportal.support;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/** Run manually to print a BCrypt hash for demo-users.sql */
public final class BcryptHashPrinter {

    private BcryptHashPrinter() {
    }

    public static void main(String[] args) {
        System.out.println(new BCryptPasswordEncoder().encode(DemoCredentials.PASSWORD));
    }
}