package com.example.usedlion.security;

import com.example.usedlion.dto.UserInformation;
import com.example.usedlion.repository.UserInformationRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserInformationRepository repo;

    public CustomUserDetailsService(UserInformationRepository repo) {
        this.repo = repo;
    }

    @Override
    public UserDetails loadUserByUsername(String email)
            throws UsernameNotFoundException {
        UserInformation user = repo.findByEmail(email);
        if (user == null || !"local".equals(user.getProvider())) {
            throw new UsernameNotFoundException(email + " not found");
        }
        return new CustomUserDetails(user);
    }
}
