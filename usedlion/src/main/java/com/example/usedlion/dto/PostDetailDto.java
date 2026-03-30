package com.example.usedlion.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PostDetailDto {

    private Integer postId;
    private Integer userId;
    private Integer view;
    private byte[] file;
    private String title;
    private Integer price;
    private String content;
    private LocalDateTime created_at;
    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "ENUM('ONSALE', 'RESERVED', 'SOLDOUT')")
    private SaleStatus status;
    private String email;
    private String username;
    private String region;
}
