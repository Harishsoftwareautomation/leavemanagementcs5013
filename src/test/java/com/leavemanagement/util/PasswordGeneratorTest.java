package com.leavemanagement.util;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordGeneratorTest {

    @Test
    void passwordIsTwelveReadableCharacters() {
        for (int i = 0; i < 200; i++) {
            String password = PasswordGenerator.generate();
            assertThat(password).hasSize(12);
            assertThat(password).matches("[A-Za-z2-9]+");
            // No look-alike characters
            assertThat(password).doesNotContain("0", "O", "1", "l", "I");
        }
    }

    @Test
    void passwordsAreRandom() {
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            seen.add(PasswordGenerator.generate());
        }
        assertThat(seen).hasSize(1000);
    }
}
