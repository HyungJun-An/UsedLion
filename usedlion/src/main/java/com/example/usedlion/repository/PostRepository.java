package com.example.usedlion.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.example.usedlion.dto.PostDetailDto;
import com.example.usedlion.entity.Post;
import com.example.usedlion.entity.PostDetail;

public interface PostRepository extends JpaRepository<Post, Integer> {

    List<Post> findByUserId(Integer userId);

    Post getPostByPostId(Integer postId);

    @Query("SELECT new com.example.usedlion.dto.PostDetailDto(p.postId, p.userId, p.view, p.file, p.title, p.price, p.content, p.created_at, p.status,f.email,f.username,f.region) from Post p Join UserInformation f ON p.userId = f.userId WHERE p.postId = ?1")
    PostDetailDto getPostDetailByPostId(Integer postId);

    @Query("SELECT new com.example.usedlion.dto.PostDetailDto(p.postId, p.userId, p.view, p.file, p.title, p.price, p.content, p.created_at, p.status,f.email,f.username,f.region) from Post p Join UserInformation f ON p.userId = f.userId")
    List<PostDetailDto> getAllPostDetail();

    @Query("SELECT new com.example.usedlion.dto.PostDetailDto(p.postId, p.userId, p.view, p.file, p.title, p.price, p.content, p.created_at, p.status,f.email,f.username,f.region) from Post p Join UserInformation f ON p.userId = f.userId WHERE p.title like %?1%")
    List<PostDetailDto> findByTitleContaining(String title);

    @Query("SELECT new com.example.usedlion.dto.PostDetailDto(p.postId, p.userId, p.view, p.file, p.title, p.price, p.content, p.created_at, p.status,f.email,f.username,f.region) from Post p Join UserInformation f ON p.userId = f.userId WHERE f.username like %?1%")
    List<PostDetailDto> findByNicknameContaining(String username);

    @Query("SELECT new com.example.usedlion.dto.PostDetailDto(p.postId, p.userId, p.view, p.file, p.title, p.price, p.content, p.created_at, p.status,f.email,f.username,f.region) from Post p Join UserInformation f ON p.userId = f.userId WHERE p.content like %?1%")
    List<PostDetailDto> findByContentContaining(String content);

    @Query("SELECT new com.example.usedlion.dto.PostDetailDto(p.postId, p.userId, p.view, p.file, p.title, p.price, p.content, p.created_at, p.status,f.email,f.username,f.region) from Post p Join UserInformation f ON p.userId = f.userId WHERE f.region like %?1%")
    List<PostDetailDto> findByRegionContaining(String region);

    @Query("SELECT new com.example.usedlion.dto.PostDetailDto(p.postId, p.userId, p.view, p.file, p.title, p.price, p.content, p.created_at, p.status,f.email,f.username,f.region) from Post p Join UserInformation f ON p.userId = f.userId WHERE f.region like %?1% AND p.title like %?2%")
    List<PostDetailDto> findByRegionAndTitleContaining(String region, String title);

    @Query("SELECT new com.example.usedlion.dto.PostDetailDto(p.postId, p.userId, p.view, p.file, p.title, p.price, p.content, p.created_at, p.status,f.email,f.username,f.region) from Post p Join UserInformation f ON p.userId = f.userId WHERE f.region like %?1% AND f.username like %?2%")
    List<PostDetailDto> findByRegionAndUsernameContaining(String region, String username);

    @Query("SELECT new com.example.usedlion.dto.PostDetailDto(p.postId, p.userId, p.view, p.file, p.title, p.price, p.content, p.created_at, p.status,f.email,f.username,f.region) from Post p Join UserInformation f ON p.userId = f.userId WHERE f.region like %?1% AND p.content like %?2%")
    List<PostDetailDto> findByRegionAndContentContaining(String region, String content);

}
