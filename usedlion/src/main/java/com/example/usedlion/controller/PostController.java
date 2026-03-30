package com.example.usedlion.controller;

import java.io.IOException;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.example.usedlion.dto.PostDetailDto;
import com.example.usedlion.dto.PostImage;
import com.example.usedlion.dto.ReplyDetailDto;
import com.example.usedlion.dto.ReplyTree;
import com.example.usedlion.dto.SaleStatus;
import com.example.usedlion.entity.Image;
import com.example.usedlion.entity.Post;
import com.example.usedlion.entity.UserInformation;
import com.example.usedlion.service.ImageService;
import com.example.usedlion.service.PostService;
import com.example.usedlion.service.ReplyService;
import com.example.usedlion.service.UserPostLikeService;
import com.example.usedlion.service.UserService;

@Controller
@RequestMapping("/post")
@CrossOrigin
public class PostController {
    private final PostService postService;
    private final ReplyService replyService;
    private final UserService userService;
    private final ImageService imageService;
    private final UserPostLikeService userPostLikeService;

    public PostController(PostService postService, ReplyService replyService, UserService userService,
            ImageService imageService,
            UserPostLikeService userPostLikeService) {
        this.userPostLikeService = userPostLikeService;
        this.userService = userService;
        this.replyService = replyService;
        this.postService = postService;
        this.imageService = imageService;
    }

    @PostMapping("/{id}")
    public String updatePost(@PathVariable Integer id,
            @RequestParam("title") String title,
            @RequestParam("price") Integer price,
            @RequestParam(value = "deleteImages", required = false) List<Integer> deleteIndices,
            @RequestParam("content") String content,
            @RequestParam("status") SaleStatus status,
            @RequestParam("file") List<MultipartFile> files,
            Principal principal) throws IOException {
        Post post = postService.getPostByPostId(id);
        post.setTitle(title);
        post.setPrice(price);
        post.setContent(content);
        post.setStatus(status);
        System.out.println("post.getStatus() = " + post.getStatus());

        if (deleteIndices != null) {
            for (Integer index : deleteIndices) {
                imageService.deleteImage(post.getPostId(), index);
            }
        }

        postService.update(id, post);
        for (MultipartFile file : files) {
            if (!file.isEmpty()) {
                imageService.uploadImage(post.getPostId(), file);
            }
        }
        return "redirect:/post/" + id;
    }

    @GetMapping("/{postId}")
    public String getPostDetail(@PathVariable Integer postId,
            @RequestParam(value = "deleteImages", required = false) List<Integer> deleteIndices,
            Model model, Principal principal) {
        PostDetailDto postDetailDto = postService.getPostDetailByPostId(postId);
        List<ReplyDetailDto> replyDto = replyService.getReplyByPostId(postId);
        List<ReplyTree> replyTree = replyService.MakeTree(replyDto);
        List<Image> postImages = imageService.getImagesByPostId(postId);

        List<String> imageList = new ArrayList<>();
        for (Image image : postImages) {
            String base64Image = Base64.getEncoder().encodeToString(image.getFile());
            imageList.add(base64Image);
        }

        boolean isLiked = userPostLikeService.isPostLiked(postId,
                userService.getUserByEmail(principal.getName()).getUserId());

        model.addAttribute("isLiked", isLiked);

        int viewCount = postDetailDto.getView();
        postDetailDto.setView(viewCount + 1);
        Post post = postService.getPostByPostId(postId);
        post.setView(postDetailDto.getView());
        postService.update(postId, post);

        model.addAttribute("post", postDetailDto);
        model.addAttribute("replyTree", replyTree);
        model.addAttribute("imageList", imageList);
        model.addAttribute("currentUsername", userService.getUserByEmail(principal.getName()).getUsername());

        int currentUserId = userService.getUserByEmail(principal.getName()).getUserId();
        model.addAttribute("currentUserId", currentUserId);
        return "postDetail";
    }

    @GetMapping("/create")
    public String showCreatePostForm(Model model) {
        model.addAttribute("update", false);
        return "createPost";
    }

    @PostMapping("/create")
    public String createPost(@RequestParam("title") String title,
            @RequestParam("price") Integer price,
            @RequestParam("content") String content,
            @RequestParam("status") SaleStatus status,
            @RequestParam("file") List<MultipartFile> files,
            Principal principal) throws IOException {

        String username = userService.getUserByEmail(principal.getName()).getUsername(); // 로그인한 사용자의 ID 또는 username

        Post post = new Post();

        post.setTitle(title);
        post.setPrice(price);
        post.setContent(content);
        post.setStatus(status);
        post.setCreated_at(LocalDateTime.now());
        post.setView(0);
        post.setUserId(userService.getUserByUsername(username).getUserId()); // 사용자 정보에서 userId 설정

        postService.createPost(post);
        for (MultipartFile file : files) {
            if (!file.isEmpty()) {
                imageService.uploadImage(post.getPostId(), file);
            }
        }
        return "redirect:/post/" + post.getPostId(); // 생성된 게시물의 상세 페이지로 리다이렉트
    }

    @GetMapping("/search/detail")
    public String searchDetail(
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "username", required = false) String username,
            @RequestParam(value = "content", required = false) String content,
            @RequestParam(value = "region", required = false) String region,
            Principal principal, Model model) {
        String filter = null;
        String condition = null;
        List<PostDetailDto> posts = postService.searchPosts(title, username, content, region);
        List<PostImage> postImages = postService.makePostImage(posts);

        if (title != null && !title.isEmpty()) {
            filter = "제목";
            condition = title;
        } else if (username != null && !username.isEmpty()) {
            filter = "닉네임";
            condition = username;
        } else if (content != null && !content.isEmpty()) {
            filter = "내용";
            condition = content;
        }
        if (region != null && !region.isEmpty()) {
            model.addAttribute("region", region);
        }

        model.addAttribute("posts", postImages);
        model.addAttribute("username", username);
        model.addAttribute("filter", filter);
        model.addAttribute("condition", condition);

        return "dashboard";
    }

    @GetMapping("/edit/{postId}")
    public String updatePost(@PathVariable Integer postId, Model model) {
        PostDetailDto postDetailDto = postService.getPostDetailByPostId(postId);
        List<Image> postImages = imageService.getImagesByPostId(postId);
        List<String> imageList = new ArrayList<>();
        for (Image image : postImages) {
            String base64Image = Base64.getEncoder().encodeToString(image.getFile());
            imageList.add(base64Image);
        }

        model.addAttribute("post", postDetailDto);
        model.addAttribute("imageList", imageList);
        model.addAttribute("update", true);

        return "createPost";
    }

    @PostMapping("/delete/{postId}")
    public String deletePost(@PathVariable Integer postId) {
        postService.deletePost(postId);
        return "redirect:/dashboard";
    }

}
