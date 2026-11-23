package com.itzetech.dpdms.common;

import java.security.SecureRandom;

public final class PasswordPolicy {

    public static final String MESSAGE = "Use at least 8 characters, with letters and a number.";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String LETTERS = "abcdefghjkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String DIGITS = "23456789";

    private PasswordPolicy() {
    }

    public static boolean isValid(String pw) {
        if (pw == null || pw.length() < 8 || pw.length() > 100) {
            return false;
        }
        boolean letter = false;
        boolean digit = false;
        for (char c : pw.toCharArray()) {
            letter |= Character.isLetter(c);
            digit |= Character.isDigit(c);
        }
        return letter && digit;
    }

    public static void require(String pw) {
        if (!isValid(pw)) {
            throw ApiException.badRequest("WEAK_PASSWORD", MESSAGE);
        }
    }

    /** A readable random password for invitations: always has letters and digits. */
    public static String temporary() {
        StringBuilder sb = new StringBuilder("Tmp-");
        for (int i = 0; i < 8; i++) {
            sb.append(i % 3 == 2 ? DIGITS.charAt(RANDOM.nextInt(DIGITS.length())) : LETTERS.charAt(RANDOM.nextInt(LETTERS.length())));
        }
        return sb.toString();
    }
}
