package com.example.dynamicform.submission.api;

import com.example.dynamicform.common.api.PageResponse;
import com.example.dynamicform.submission.api.dto.AdminSubmissionResponse;
import com.example.dynamicform.submission.api.dto.SubmitFormRequest;
import com.example.dynamicform.submission.api.dto.SubmissionResponse;
import com.example.dynamicform.submission.application.SubmissionQueryService;
import com.example.dynamicform.submission.application.SubmissionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/forms/{formId}/submissions")
public class SubmissionController {

    private final SubmissionService submissionService;
    private final SubmissionQueryService submissionQueryService;

    public SubmissionController(SubmissionService submissionService,
                                SubmissionQueryService submissionQueryService) {
        this.submissionService = submissionService;
        this.submissionQueryService = submissionQueryService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SubmissionResponse submit(@PathVariable Long formId,
                                     @AuthenticationPrincipal Jwt jwt,
                                     @Valid @RequestBody SubmitFormRequest request) {
        Long userId = Long.valueOf(jwt.getSubject());
        return submissionService.submit(formId, userId, request);
    }

    @GetMapping
    public PageResponse<AdminSubmissionResponse> listForAdmin(
            @PathVariable Long formId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "submittedAt") String sort,
            @RequestParam(defaultValue = "desc") String direction) {
        return submissionQueryService.listForAdmin(formId, page, size, sort, direction);
    }
}
