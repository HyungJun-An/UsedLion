package com.example.usedlion.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReplyDto {

    private Integer id;
    private Integer userId;
    private Integer ref;
    private Integer level;
    private Integer postId;
    private String content;
    private LocalDateTime created_at;

}
