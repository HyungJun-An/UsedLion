package com.example.usedlion.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.usedlion.entity.UserPostLike;
import com.example.usedlion.repository.UserPostLikeRepository;

@Service
public class UserPostLikeService {
    private final UserPostLikeRepository userPostLikeRepository;

    public UserPostLikeService(UserPostLikeRepository userPostLikeRepository) {
        this.userPostLikeRepository = userPostLikeRepository;
    }

    public void likePost(Integer postId, Integer userId) {
        userPostLikeRepository.save(UserPostLike
                .builder()
                .postId(postId)
                .userId(userId)
                .build());
    }

    public void unlikePost(Integer postId, Integer userId) {

        UserPostLike userPostLike = userPostLikeRepository
                .findByUserIdAndPostId(userId, postId);

        userPostLikeRepository.delete(userPostLike);
    }

    public boolean isPostLiked(Integer postId, Integer userId) {
        return userPostLikeRepository
                .findByUserIdAndPostId(userId, postId) != null;
    }

    public List<UserPostLike> getUserPostLikeByUserId(Integer userId) {
        return userPostLikeRepository.findByUserId(userId);
    }

}
