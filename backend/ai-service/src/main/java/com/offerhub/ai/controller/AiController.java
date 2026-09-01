package com.offerhub.ai.controller;

import com.offerhub.ai.dto.*;
import com.offerhub.ai.service.AccuracyService;
import com.offerhub.ai.service.ExpertAssignmentService;
import com.offerhub.ai.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/ai")
public class AiController {

    private final RecommendationService recommendationService;
    private final ExpertAssignmentService expertAssignmentService;
    private final AccuracyService accuracyService;

    @PostMapping("/recommend")
    public ResponseEntity<ApiResponse<RecommendResponse>> recommend(@RequestBody RecommendRequest request) {
        RecommendResponse response = recommendationService.recommend(
                request.getSubscriberId(), request.getCampaignType());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/assign-expert")
    public ResponseEntity<ApiResponse<AssignExpertResponse>> assignExpert(@RequestBody AssignExpertRequest request) {
        AssignExpertResponse response = expertAssignmentService.assign(request.getCaseId(), request.getSegment());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/misclassification")
    public ResponseEntity<ApiResponse<Void>> misclassification(@RequestBody MisclassificationRequest request) {
        accuracyService.recordMisclassification(
                request.getCampaignNo(), request.getOriginalSegment(), request.getCorrectedSegment());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/accuracy")
    public ResponseEntity<ApiResponse<AccuracyResponse>> accuracy() {
        return ResponseEntity.ok(ApiResponse.success(accuracyService.getAccuracy()));
    }
}
