package com.example.usedlion.dto;

import jakarta.persistence.criteria.CriteriaBuilder.In;

public class PostImage {
    private PostDetailDto post;
    private String img;
    private Integer reportCount;

    public PostImage(PostDetailDto post, String img, Integer reportCount) {
        this.post = post;
        this.img = img;
        this.reportCount = reportCount;
    }

    public PostDetailDto getPost() {
        return post;
    }

    public String getImg() {
        return img;
    }

    public Integer getReportCount() {
        return reportCount;
    }

}
