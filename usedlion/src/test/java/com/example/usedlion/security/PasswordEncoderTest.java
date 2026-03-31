package com.example.usedlion.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordEncoderTest {

    private final PasswordEncoder encoder = new BCryptPasswordEncoder();

    @Test
    @DisplayName("'test' 비밀번호를 BCrypt로 인코딩하면 원문과 달라야 한다")
    void encode_plainPassword_isDifferentFromRaw() {
        String raw = "test";
        String encoded = encoder.encode(raw);
        assertThat(encoded).isNotEqualTo(raw);
    }

    @Test
    @DisplayName("BCrypt 인코딩된 'test'는 matches()로 검증되어야 한다")
    void matches_encodedPassword_returnsTrue() {
        String raw = "test";
        String encoded = encoder.encode(raw);
        assertThat(encoder.matches(raw, encoded)).isTrue();
    }

    @Test
    @DisplayName("틀린 비밀번호는 matches()가 false를 반환해야 한다")
    void matches_wrongPassword_returnsFalse() {
        String encoded = encoder.encode("test");
        assertThat(encoder.matches("wrongpassword", encoded)).isFalse();
    }

    @Test
    @DisplayName("같은 평문이라도 인코딩할 때마다 결과가 달라야 한다 (salt)")
    void encode_sameRaw_producesDifferentHashes() {
        String raw = "test";
        String encoded1 = encoder.encode(raw);
        String encoded2 = encoder.encode(raw);
        assertThat(encoded1).isNotEqualTo(encoded2);
        // 하지만 둘 다 matches는 통과해야 함
        assertThat(encoder.matches(raw, encoded1)).isTrue();
        assertThat(encoder.matches(raw, encoded2)).isTrue();
    }
}
