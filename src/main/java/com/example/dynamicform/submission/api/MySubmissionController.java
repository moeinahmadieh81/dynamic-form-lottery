package com.example.dynamicform.submission.api;

import com.example.dynamicform.common.api.PageResponse;
import com.example.dynamicform.submission.api.dto.MySubmissionResponse;
import com.example.dynamicform.submission.application.SubmissionQueryService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me/submissions")
public class MySubmissionController {

    private final SubmissionQueryService submissionQueryService;

    public MySubmissionController(SubmissionQueryService submissionQueryService) {
        this.submissionQueryService = submissionQueryService;
    }

    @GetMapping
    public PageResponse<MySubmissionResponse> listMine(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "submittedAt") String sort,
            @RequestParam(defaultValue = "desc") String direction) {
        return submissionQueryService.listMine(
                Long.valueOf(jwt.getSubject()),
                page,
                size,
                sort,
                direction
        );
    }
}
