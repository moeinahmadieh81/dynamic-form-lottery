package com.example.dynamicform.lottery.api;

import com.example.dynamicform.auth.security.CurrentUser;
import com.example.dynamicform.lottery.api.dto.AdminLotteryWinnerResponse;
import com.example.dynamicform.lottery.api.dto.CreateLotteryRequest;
import com.example.dynamicform.lottery.api.dto.LotteryResponse;
import com.example.dynamicform.lottery.api.dto.PublicLotteryResultResponse;
import com.example.dynamicform.lottery.application.LotteryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class LotteryController {

    private final LotteryService lotteryService;

    public LotteryController(LotteryService lotteryService) {
        this.lotteryService = lotteryService;
    }

    @PostMapping("/forms/{formId}/lotteries")
    @ResponseStatus(HttpStatus.CREATED)
    public LotteryResponse create(@PathVariable Long formId,
                                  @AuthenticationPrincipal Jwt jwt,
                                  @Valid @RequestBody CreateLotteryRequest request) {
        return lotteryService.create(formId, Long.valueOf(jwt.getSubject()), request);
    }

    @PostMapping("/lotteries/{lotteryId}/run")
    public LotteryResponse run(@PathVariable Long lotteryId,
                               @AuthenticationPrincipal Jwt jwt) {
        return lotteryService.run(lotteryId, Long.valueOf(jwt.getSubject()));
    }

    @GetMapping("/lotteries/{lotteryId}")
    public LotteryResponse get(@PathVariable Long lotteryId) {
        return lotteryService.get(lotteryId);
    }

    @GetMapping("/forms/{formId}/lottery")
    public PublicLotteryResultResponse getForForm(@PathVariable Long formId,
                                                  @AuthenticationPrincipal Jwt jwt) {
        CurrentUser currentUser = CurrentUser.from(jwt);
        return lotteryService.getForForm(formId, currentUser.id(), currentUser.admin());
    }

    @GetMapping("/lotteries/{lotteryId}/winners")
    public List<AdminLotteryWinnerResponse> getWinners(@PathVariable Long lotteryId) {
        return lotteryService.getWinnersForAdmin(lotteryId);
    }
}
