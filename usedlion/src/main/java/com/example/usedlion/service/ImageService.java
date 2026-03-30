package com.example.usedlion.service;

import java.io.IOException;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.usedlion.entity.Image;
import com.example.usedlion.repository.ImageRepository;

@Service
public class ImageService {
    private final ImageRepository imageRepository;

    public ImageService(ImageRepository imageRepository) {
        this.imageRepository = imageRepository;
    }

    public void uploadImage(Integer postId, MultipartFile file) throws IOException {
        Image image = new Image();
        image.setPostId(postId);
        image.setFile(file.getBytes());

        imageRepository.save(image);
    }

    public List<Image> getImagesByPostId(Integer postId) {
        return imageRepository.findByPostId(postId);
    }

    public void deleteImage(Integer imageId) {
        Image image = imageRepository.findById(imageId)
                .orElseThrow(() -> new RuntimeException("Image not found"));
        imageRepository.delete(image);
    }

    public void deleteImage(Integer postId, Integer index) {
        List<Image> images = getImagesByPostId(postId);
        if (index >= 0 && index < images.size()) {
            deleteImage(images.get(index).getImageId());
        }
    }

}
