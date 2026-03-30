package com.example.usedlion.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PostDto {

    private Integer id;

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

}
