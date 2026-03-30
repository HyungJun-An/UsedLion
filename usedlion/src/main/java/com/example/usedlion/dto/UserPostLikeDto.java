package com.example.usedlion.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class UserPostLikeDto {

    private Integer id;
    private Integer userId;
    private Integer postId;

}
