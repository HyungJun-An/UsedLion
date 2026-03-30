package com.example.usedlion.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.usedlion.entity.UserPostLike;

public interface UserPostLikeRepository extends JpaRepository<UserPostLike, Integer> {
    UserPostLike findByUserIdAndPostId(Integer userId, Integer postId);

    List<UserPostLike> findByUserId(Integer userId);
}
