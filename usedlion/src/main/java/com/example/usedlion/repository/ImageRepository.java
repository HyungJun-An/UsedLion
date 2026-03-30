package com.example.usedlion.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.usedlion.entity.Image;

public interface ImageRepository extends JpaRepository<Image, Integer> {

    public List<Image> findByPostId(Integer postId);
}
