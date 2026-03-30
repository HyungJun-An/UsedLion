package com.example.usedlion.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.usedlion.entity.Report;

public interface ReportRepository extends JpaRepository<Report, Integer> {

    List<Report> findByTargetId(Integer targetId);
}
