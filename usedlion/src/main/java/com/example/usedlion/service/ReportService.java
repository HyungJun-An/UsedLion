package com.example.usedlion.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.usedlion.entity.Report;
import com.example.usedlion.repository.ReportRepository;

@Service
public class ReportService {
    private final ReportRepository reportRepository;

    public ReportService(ReportRepository reportRepository) {
        this.reportRepository = reportRepository;
    }

    public void createReport(Report report) {
        reportRepository.save(report);
    }

    public List<Report> getByUserId(Integer userId) {

        return reportRepository.findByTargetId(userId);
    }

}
