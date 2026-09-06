package com.example.dynamicform.form.api;

import com.example.dynamicform.auth.security.CurrentUser;
import com.example.dynamicform.common.api.PageResponse;
import com.example.dynamicform.form.api.dto.CreateFormRequest;
import com.example.dynamicform.form.api.dto.FormResponse;
import com.example.dynamicform.form.api.dto.FormSummaryResponse;
import com.example.dynamicform.form.api.dto.UpdateDraftFormRequest;
import com.example.dynamicform.form.application.FormQueryService;
import com.example.dynamicform.form.application.FormService;
import com.example.dynamicform.form.domain.FormStatus;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(
        name = "Forms",
        description = "Dynamic form creation, versioning and lifecycle management"
)
@RestController
@RequestMapping("/api/forms")
public class FormController {

    private final FormService formService;
    private final FormQueryService formQueryService;

    public FormController(FormService formService,
                          FormQueryService formQueryService) {
        this.formService = formService;
        this.formQueryService = formQueryService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FormResponse create(@Valid @RequestBody CreateFormRequest request) {
        return formService.create(request);
    }

    @GetMapping
    public PageResponse<FormSummaryResponse> list(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) FormStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "updatedAt") String sort,
            @RequestParam(defaultValue = "desc") String direction) {
        CurrentUser currentUser = CurrentUser.from(jwt);
        return formQueryService.list(
                currentUser.admin(),
                status,
                page,
                size,
                sort,
                direction
        );
    }

    @GetMapping("/{formId}")
    public FormResponse get(@PathVariable Long formId,
                            @AuthenticationPrincipal Jwt jwt) {
        return formQueryService.get(formId, CurrentUser.from(jwt).admin());
    }

    @PutMapping("/{formId}")
    public FormResponse updateDraft(@PathVariable Long formId,
                                    @Valid @RequestBody UpdateDraftFormRequest request) {
        return formService.updateDraft(formId, request);
    }

    @PostMapping("/{formId}/publish")
    public FormResponse publish(@PathVariable Long formId) {
        return formService.publish(formId);
    }

    @PostMapping("/{formId}/close")
    public FormResponse close(@PathVariable Long formId,
                              @AuthenticationPrincipal Jwt jwt) {
        return formService.close(formId, Long.valueOf(jwt.getSubject()));
    }
}
