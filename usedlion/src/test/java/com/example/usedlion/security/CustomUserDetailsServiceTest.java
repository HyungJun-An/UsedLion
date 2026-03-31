package com.example.usedlion.security;

import com.example.usedlion.dto.UserInformation;
import com.example.usedlion.repository.UserInformationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserInformationRepository repo;

    @InjectMocks
    private CustomUserDetailsService service;

    private UserInformation testUser;

    @BeforeEach
    void setUp() {
        testUser = new UserInformation();
        testUser.setId(1L);
        testUser.setEmail("test@test.com");
        testUser.setPassword("$2a$10$encodedPasswordHash"); // BCrypt encoded "test"
        testUser.setUsername("testuser");
        testUser.setNickname("테스터");
        testUser.setProvider("local");
        testUser.setRole("USER");
        testUser.setCreatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("로컬 계정 이메일로 UserDetails 정상 반환")
    void loadUserByUsername_localUser_returnsUserDetails() {
        // given
        when(repo.findByEmail("test@test.com")).thenReturn(testUser);

        // when
        UserDetails result = service.loadUserByUsername("test@test.com");

        // then
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("test@test.com");
        assertThat(result.getPassword()).isEqualTo("$2a$10$encodedPasswordHash");
        verify(repo, times(1)).findByEmail("test@test.com");
    }

    @Test
    @DisplayName("존재하지 않는 이메일 → UsernameNotFoundException")
    void loadUserByUsername_userNotFound_throwsException() {
        // given
        when(repo.findByEmail("notexist@test.com")).thenReturn(null);

        // when & then
        assertThatThrownBy(() -> service.loadUserByUsername("notexist@test.com"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("notexist@test.com");
    }

    @Test
    @DisplayName("소셜 계정(provider != local) → UsernameNotFoundException")
    void loadUserByUsername_googleProvider_throwsException() {
        // given
        testUser.setProvider("google");
        when(repo.findByEmail("test@test.com")).thenReturn(testUser);

        // when & then
        assertThatThrownBy(() -> service.loadUserByUsername("test@test.com"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("test@test.com");
    }

    @Test
    @DisplayName("반환된 UserDetails의 권한은 ROLE_USER")
    void loadUserByUsername_returnsCorrectAuthority() {
        // given
        when(repo.findByEmail("test@test.com")).thenReturn(testUser);

        // when
        UserDetails result = service.loadUserByUsername("test@test.com");

        // then
        assertThat(result.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_USER");
    }
}
