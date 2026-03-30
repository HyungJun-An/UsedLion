package com.example.usedlion.entity;

import java.time.LocalDateTime;

import com.example.usedlion.dto.SaleStatus;

import groovy.transform.builder.Builder;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer postId;
    private Integer userId;
    private Integer view;
    @Lob
    private byte[] file;
    private String title;
    private Integer price;
    private String content;
    private LocalDateTime created_at;
    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "ENUM('ONSALE','RESERVED','SOLDOUT')")
    private SaleStatus status;

}
