package com.jp.vocab.shared.auth;

import java.util.Locale;

public final class UserRole {

    public static final String USER = "USER";
    public static final String ADMIN = "ADMIN";

    private UserRole() {
    }

    public static String normalize(String role) {
        if (ADMIN.equals(role == null ? "" : role.trim().toUpperCase(Locale.ROOT))) {
            return ADMIN;
        }
        return USER;
    }
}
