package com.example.dynamicform.lottery.application;

import com.example.dynamicform.audit.application.AuditLogService;
import com.example.dynamicform.form.application.FormLotteryService;
import com.example.dynamicform.lottery.api.dto.AdminLotteryWinnerResponse;
import com.example.dynamicform.lottery.api.dto.CreateLotteryRequest;
import com.example.dynamicform.lottery.api.dto.LotteryResponse;
import com.example.dynamicform.lottery.api.dto.LotteryWinnerResponse;
import com.example.dynamicform.lottery.api.dto.PublicLotteryResultResponse;
import com.example.dynamicform.lottery.api.dto.PublicLotteryWinnerResponse;
import com.example.dynamicform.lottery.domain.DuplicateLotteryException;
import com.example.dynamicform.lottery.domain.LotteryNotFoundException;
import com.example.dynamicform.lottery.domain.LotteryOperationException;
import com.example.dynamicform.lottery.domain.LotteryStatus;
import com.example.dynamicform.lottery.domain.WinnerSelector;
import com.example.dynamicform.lottery.infrastructure.persistence.LotteryEntity;
import com.example.dynamicform.lottery.infrastructure.persistence.LotteryEntryEntity;
import com.example.dynamicform.lottery.infrastructure.persistence.LotteryEntryRepository;
import com.example.dynamicform.lottery.infrastructure.persistence.LotteryRepository;
import com.example.dynamicform.lottery.infrastructure.persistence.LotteryWinnerEntity;
import com.example.dynamicform.lottery.infrastructure.persistence.LotteryWinnerRepository;
import com.example.dynamicform.submission.domain.SubmissionStatus;
import com.example.dynamicform.submission.infrastructure.persistence.SubmissionEntity;
import com.example.dynamicform.submission.infrastructure.persistence.SubmissionRepository;
import com.example.dynamicform.user.infrastructure.persistence.UserEntity;
import com.example.dynamicform.user.infrastructure.persistence.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class LotteryService {

    private static final Set<LotteryStatus> NON_CANCELLED_STATUSES = Set.of(
            LotteryStatus.READY,
            LotteryStatus.RUNNING,
            LotteryStatus.COMPLETED
    );

    private static final Set<LotteryStatus> COMPLETED_ONLY = Set.of(LotteryStatus.COMPLETED);

    private final FormLotteryService formLotteryService;
    private final SubmissionRepository submissionRepository;
    private final LotteryRepository lotteryRepository;
    private final LotteryEntryRepository entryRepository;
    private final LotteryWinnerRepository winnerRepository;
    private final WinnerSelector winnerSelector;
    private final AuditLogService auditLogService;
    private final UserRepository userRepository;

    public LotteryService(FormLotteryService formLotteryService,
                          SubmissionRepository submissionRepository,
                          LotteryRepository lotteryRepository,
                          LotteryEntryRepository entryRepository,
                          LotteryWinnerRepository winnerRepository,
                          WinnerSelector winnerSelector,
                          AuditLogService auditLogService,
                          UserRepository userRepository) {
        this.formLotteryService = formLotteryService;
        this.submissionRepository = submissionRepository;
        this.lotteryRepository = lotteryRepository;
        this.entryRepository = entryRepository;
        this.winnerRepository = winnerRepository;
        this.winnerSelector = winnerSelector;
        this.auditLogService = auditLogService;
        this.userRepository = userRepository;
    }

    @Transactional
    public LotteryResponse create(Long formId, Long actorId, CreateLotteryRequest request) {
        formLotteryService.lockAndRequireClosed(formId);

        if (lotteryRepository.existsByFormIdAndStatusIn(formId, NON_CANCELLED_STATUSES)) {
            throw new DuplicateLotteryException(formId);
        }

        List<SubmissionEntity> eligibleSubmissions = submissionRepository
                .findAllByFormIdAndStatusAndUserIdIsNotNullOrderByIdAsc(
                        formId,
                        SubmissionStatus.SUBMITTED
                );

        if (eligibleSubmissions.isEmpty()) {
            throw new LotteryOperationException("There are no eligible authenticated submissions for this form");
        }

        if (request.winnerCount() > eligibleSubmissions.size()) {
            throw new IllegalArgumentException(
                    "winnerCount cannot exceed eligible participant count (" + eligibleSubmissions.size() + ")"
            );
        }

        LotteryEntity lottery = LotteryEntity.ready(formId, request.winnerCount(), actorId);

        try {
            lotteryRepository.saveAndFlush(lottery);
        } catch (DataIntegrityViolationException ex) {
            if (containsConstraint(ex, "uq_lotteries_form_active")) {
                throw new DuplicateLotteryException(formId);
            }
            throw ex;
        }

        List<LotteryEntryEntity> entries = eligibleSubmissions.stream()
                .map(submission -> LotteryEntryEntity.snapshot(
                        lottery.getId(),
                        submission.getId(),
                        submission.getUserId()
                ))
                .toList();
        entryRepository.saveAll(entries);
        entryRepository.flush();

        auditLogService.record(
                actorId,
                "LOTTERY_CREATED",
                "LOTTERY",
                lottery.getId(),
                Map.of(
                        "formId", formId,
                        "winnerCount", request.winnerCount(),
                        "participantCount", entries.size()
                )
        );

        return toResponse(lottery, entries, List.of());
    }

    @Transactional
    public LotteryResponse run(Long lotteryId, Long actorId) {
        LotteryEntity lottery = lotteryRepository.findByIdForUpdate(lotteryId)
                .orElseThrow(() -> new LotteryNotFoundException(lotteryId));

        if (lottery.getStatus() != LotteryStatus.READY) {
            throw new LotteryOperationException(
                    "Lottery must be READY to run; current status is " + lottery.getStatus()
            );
        }

        formLotteryService.lockAndRequireClosed(lottery.getFormId());

        List<LotteryEntryEntity> entries = entryRepository.findAllByLotteryIdOrderByIdAsc(lotteryId);
        if (entries.size() < lottery.getWinnerCount()) {
            throw new LotteryOperationException("Lottery snapshot does not contain enough participants");
        }

        lottery.start();

        List<LotteryEntryEntity> selectedEntries = winnerSelector.select(
                entries,
                lottery.getWinnerCount()
        );

        List<LotteryWinnerEntity> winners = new ArrayList<>();
        for (int i = 0; i < selectedEntries.size(); i++) {
            LotteryEntryEntity selected = selectedEntries.get(i);
            winners.add(LotteryWinnerEntity.selected(
                    lotteryId,
                    selected.getId(),
                    i + 1
            ));
        }
        winnerRepository.saveAll(winners);
        winnerRepository.flush();

        lottery.complete();
        formLotteryService.markDrawn(lottery.getFormId());

        List<Long> winnerUserIds = selectedEntries.stream()
                .map(LotteryEntryEntity::getUserId)
                .toList();

        auditLogService.record(
                actorId,
                "LOTTERY_COMPLETED",
                "LOTTERY",
                lotteryId,
                Map.of(
                        "formId", lottery.getFormId(),
                        "participantCount", entries.size(),
                        "winnerCount", winners.size(),
                        "winnerUserIds", winnerUserIds
                )
        );

        return toResponse(lottery, entries, winners);
    }

    @Transactional(readOnly = true)
    public LotteryResponse get(Long lotteryId) {
        LotteryEntity lottery = lotteryRepository.findById(lotteryId)
                .orElseThrow(() -> new LotteryNotFoundException(lotteryId));
        List<LotteryEntryEntity> entries = entryRepository.findAllByLotteryIdOrderByIdAsc(lotteryId);
        List<LotteryWinnerEntity> winners = winnerRepository.findAllByLotteryIdOrderByPositionAsc(lotteryId);
        return toResponse(lottery, entries, winners);
    }

    @Transactional(readOnly = true)
    public PublicLotteryResultResponse getForForm(Long formId,
                                                  Long currentUserId,
                                                  boolean admin) {
        Collection<LotteryStatus> visibleStatuses = admin
                ? NON_CANCELLED_STATUSES
                : COMPLETED_ONLY;

        LotteryEntity lottery = lotteryRepository
                .findFirstByFormIdAndStatusInOrderByCreatedAtDesc(formId, visibleStatuses)
                .orElseThrow(() -> new LotteryNotFoundException("for form " + formId));

        List<LotteryEntryEntity> entries = entryRepository.findAllByLotteryIdOrderByIdAsc(lottery.getId());
        List<LotteryWinnerEntity> winners = winnerRepository.findAllByLotteryIdOrderByPositionAsc(lottery.getId());

        Map<Long, LotteryEntryEntity> entriesById = entries.stream()
                .collect(Collectors.toMap(LotteryEntryEntity::getId, Function.identity()));

        List<Long> winnerUserIds = winners.stream()
                .map(winner -> entriesById.get(winner.getLotteryEntryId()))
                .filter(entry -> entry != null)
                .map(LotteryEntryEntity::getUserId)
                .distinct()
                .toList();

        Map<Long, UserEntity> usersById = userRepository.findAllById(winnerUserIds).stream()
                .collect(Collectors.toMap(UserEntity::getId, Function.identity()));

        List<PublicLotteryWinnerResponse> winnerResponses = winners.stream()
                .map(winner -> {
                    LotteryEntryEntity entry = entriesById.get(winner.getLotteryEntryId());
                    if (entry == null) {
                        throw new IllegalStateException("Lottery winner references a missing lottery entry");
                    }
                    UserEntity user = usersById.get(entry.getUserId());
                    if (user == null) {
                        throw new IllegalStateException("Lottery winner references a missing user");
                    }
                    return new PublicLotteryWinnerResponse(
                            winner.getPosition(),
                            user.getDisplayName(),
                            entry.getUserId().equals(currentUserId),
                            winner.getSelectedAt()
                    );
                })
                .toList();

        return new PublicLotteryResultResponse(
                lottery.getId(),
                lottery.getFormId(),
                lottery.getStatus(),
                lottery.getWinnerCount(),
                entries.size(),
                lottery.getCompletedAt(),
                winnerResponses
        );
    }

    @Transactional(readOnly = true)
    public List<AdminLotteryWinnerResponse> getWinnersForAdmin(Long lotteryId) {
        if (!lotteryRepository.existsById(lotteryId)) {
            throw new LotteryNotFoundException(lotteryId);
        }

        List<LotteryWinnerEntity> winners = winnerRepository.findAllByLotteryIdOrderByPositionAsc(lotteryId);
        List<Long> entryIds = winners.stream()
                .map(LotteryWinnerEntity::getLotteryEntryId)
                .toList();

        Map<Long, LotteryEntryEntity> entriesById = entryRepository.findAllById(entryIds).stream()
                .collect(Collectors.toMap(LotteryEntryEntity::getId, Function.identity()));

        List<Long> userIds = entriesById.values().stream()
                .map(LotteryEntryEntity::getUserId)
                .distinct()
                .toList();

        Map<Long, UserEntity> usersById = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(UserEntity::getId, Function.identity()));

        return winners.stream()
                .map(winner -> {
                    LotteryEntryEntity entry = entriesById.get(winner.getLotteryEntryId());
                    if (entry == null) {
                        throw new IllegalStateException("Lottery winner references a missing lottery entry");
                    }
                    UserEntity user = usersById.get(entry.getUserId());
                    if (user == null) {
                        throw new IllegalStateException("Lottery winner references a missing user");
                    }
                    return new AdminLotteryWinnerResponse(
                            winner.getPosition(),
                            user.getId(),
                            user.getEmail(),
                            user.getDisplayName(),
                            entry.getSubmissionId(),
                            winner.getSelectedAt()
                    );
                })
                .toList();
    }

    private LotteryResponse toResponse(LotteryEntity lottery,
                                       List<LotteryEntryEntity> entries,
                                       List<LotteryWinnerEntity> winners) {
        Map<Long, LotteryEntryEntity> entriesById = new HashMap<>();
        for (LotteryEntryEntity entry : entries) {
            entriesById.put(entry.getId(), entry);
        }

        List<LotteryWinnerResponse> winnerResponses = winners.stream()
                .map(winner -> {
                    LotteryEntryEntity entry = entriesById.get(winner.getLotteryEntryId());
                    if (entry == null) {
                        throw new IllegalStateException("Lottery winner references a missing lottery entry");
                    }
                    return new LotteryWinnerResponse(
                            winner.getPosition(),
                            entry.getUserId(),
                            entry.getSubmissionId(),
                            winner.getSelectedAt()
                    );
                })
                .toList();

        return new LotteryResponse(
                lottery.getId(),
                lottery.getFormId(),
                lottery.getStatus(),
                lottery.getWinnerCount(),
                entries.size(),
                lottery.getCreatedBy(),
                lottery.getCreatedAt(),
                lottery.getStartedAt(),
                lottery.getCompletedAt(),
                winnerResponses
        );
    }

    private boolean containsConstraint(DataIntegrityViolationException ex, String constraint) {
        String message = ex.getMostSpecificCause().getMessage();
        return message != null && message.contains(constraint);
    }
}
