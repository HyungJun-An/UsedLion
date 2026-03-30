package com.example.usedlion.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.usedlion.entity.UserInformation;

public interface UserRepository extends JpaRepository<UserInformation, Integer> {

    UserInformation findByUsername(String nickname);

    UserInformation findByEmail(String email);
}
