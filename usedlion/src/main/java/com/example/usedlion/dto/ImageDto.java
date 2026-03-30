package com.example.usedlion.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class ImageDto {

    private Integer imageId;
    private Integer postId;
    private byte[] file;

}
