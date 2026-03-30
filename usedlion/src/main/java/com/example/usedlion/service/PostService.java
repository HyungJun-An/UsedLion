package com.example.usedlion.service;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.example.usedlion.dto.PostDetailDto;
import com.example.usedlion.dto.PostImage;
import com.example.usedlion.entity.Image;
import com.example.usedlion.entity.Post;
import com.example.usedlion.entity.Report;
import com.example.usedlion.repository.PostRepository;

@Service
public class PostService {
    private final PostRepository postRepository;
    private final ImageService imageService;
    private final ReportService reportService;

    public PostService(PostRepository postRepository, ImageService imageService, ReportService reportService) {
        this.imageService = imageService;
        this.postRepository = postRepository;
        this.reportService = reportService;
    }

    public Post createPost(Post post) {
        return postRepository.save(post);
    }

    public List<Post> getPostById(Integer userId) {
        return postRepository.findByUserId(userId);
    }

    public Post getPostByPostId(Integer postId) {
        return postRepository.getPostByPostId(postId);
    }

    public PostDetailDto getPostDetailByPostId(Integer postId) {
        return postRepository.getPostDetailByPostId(postId);
    }

    public Post update(Integer id, Post updatedPost) {
        Post post = postRepository.findById(id).orElseThrow(() -> new RuntimeException("Post not found"));
        post.setTitle(updatedPost.getTitle());
        post.setContent(updatedPost.getContent());
        post.setPrice(updatedPost.getPrice());
        post.setFile(updatedPost.getFile());
        post.setStatus(updatedPost.getStatus());
        return postRepository.save(post);
    }

    public List<PostDetailDto> getAllPostDetail() {
        return postRepository.getAllPostDetail();
    }

    public void deletePost(Integer postId) {
        Post post = postRepository.findById(postId).orElseThrow(() -> new RuntimeException("Post not found"));
        postRepository.delete(post);
    }

    public List<Post> getPostsByUserId(Integer userId) {
        return postRepository.findAll().stream()
                .filter(post -> post.getUserId().equals(userId))
                .collect(Collectors.toList());
    }

    public List<PostDetailDto> searchPosts(String title, String username, String content, String region) {

        if (title != null && !title.isEmpty()) {
            if (region != null && !region.isEmpty()) {
                return postRepository.findByRegionAndTitleContaining(region, title);
            } else
                return postRepository.findByTitleContaining(title);
        } else if (username != null && !username.isEmpty()) {
            if (region != null && !region.isEmpty()) {
                return postRepository.findByRegionAndUsernameContaining(region, username);
            } else
                return postRepository.findByNicknameContaining(username);
        } else if (content != null && !content.isEmpty()) {
            if (region != null && !region.isEmpty()) {
                return postRepository.findByRegionAndContentContaining(region, content);
            } else
                return postRepository.findByContentContaining(content);

        }
        if (region != null && !region.isEmpty()) {
            return postRepository.findByRegionContaining(region);
        }
        return postRepository.getAllPostDetail();
    }

    public List<PostImage> makePostImage(List<PostDetailDto> posts) {
        List<PostImage> postImages = new ArrayList<>();
        for (PostDetailDto post : posts) {
            List<Report> reports = reportService.getByUserId(post.getUserId());
            List<Image> images = imageService.getImagesByPostId(post.getPostId());
            String base64File = null;
            if (images != null && !images.isEmpty()) {
                Image image = images.get(0);
                byte[] imageBytes = image.getFile();
                base64File = Base64.getEncoder().encodeToString(imageBytes);
            }
            PostImage pImage = new PostImage(post, base64File, reports.size());
            postImages.add(pImage);
        }
        return postImages;

    }

}
