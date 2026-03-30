package com.example.usedlion.security;

import com.example.usedlion.dto.UserInformation;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class CustomUserDetails implements UserDetails {
    private final UserInformation user;

    public CustomUserDetails(UserInformation user) {
        this.user = user;
    }

    public UserInformation getUser() {
        return user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() { //
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole()));
    }

    @Override public String getPassword() { return user.getPassword(); } // DB에 저장된 해쉬(암호화) 비밀번호
    @Override public String getUsername() { return user.getEmail(); } // 로그인폼에서 입력한 username과 비교

    // 나머지는 기본값: 계정 만료/잠김 여부 등
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked()  { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }


}
