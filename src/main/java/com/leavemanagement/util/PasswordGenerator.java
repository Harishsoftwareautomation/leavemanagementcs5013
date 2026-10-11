package com.leavemanagement.util;

import java.security.SecureRandom;

/**
 * Generates random temporary passwords
 */
public class PasswordGenerator {
    // No look-alike characters (0/O, 1/l/I) so passwords are easy to read out
    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    public static String generate() {
        StringBuilder sb = new StringBuilder(12);
        for (int i = 0; i < 12; i++) {
            sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        }
        return sb.toString();
    }
}
