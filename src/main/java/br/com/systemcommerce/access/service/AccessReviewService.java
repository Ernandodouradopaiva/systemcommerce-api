package br.com.systemcommerce.access.service;

import br.com.systemcommerce.access.entity.AccessReview;
import br.com.systemcommerce.access.entity.AccessReviewDecision;
import br.com.systemcommerce.access.entity.AccessReviewItem;
import br.com.systemcommerce.access.entity.AccessAuditEvent;
import br.com.systemcommerce.access.repository.AccessReviewDecisionRepository;
import br.com.systemcommerce.access.repository.AccessReviewItemRepository;
import br.com.systemcommerce.access.repository.AccessReviewRepository;
import br.com.systemcommerce.access.repository.UserGroupAssignmentRepository;
import br.com.systemcommerce.organization.service.OrganizationService;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import br.com.systemcommerce.shared.security.CurrentUser;
import br.com.systemcommerce.user.repository.UserRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AccessReviewService {

    private final AccessReviewRepository reviewRepository;
    private final AccessReviewItemRepository itemRepository;
    private final AccessReviewDecisionRepository decisionRepository;
    private final UserRepository userRepository;
    private final UserGroupAssignmentRepository userGroupAssignmentRepository;
    private final AccessAuditEventService accessAuditEventService;

    @Transactional
    public AccessReview create(String title, String notes) {
        AccessReview review = new AccessReview();
        review.setCode("REV-" + System.currentTimeMillis());
        review.setTitle(title);
        review.setNotes(notes);
        review.setOrganizationId(OrganizationService.DEFAULT_ID);
        review.setStatus(AccessReview.Status.DRAFT);
        review.setReviewerUserId(CurrentUser.id().orElse(null));
        review.setScheduledAt(Instant.now());
        review.setActive(true);
        AccessReview saved = reviewRepository.save(review);

        userRepository.findAll().stream()
                .filter(u -> Boolean.TRUE.equals(u.getActive()))
                .limit(200)
                .forEach(u -> {
                    AccessReviewItem item = new AccessReviewItem();
                    item.setReview(saved);
                    item.setUserId(u.getId());
                    item.setDecision(AccessReviewItem.Decision.PENDING);
                    item.setActive(true);
                    itemRepository.save(item);
                });

        accessAuditEventService.record(
                "ACCESS_REVIEW_CREATE",
                AccessAuditEvent.Result.SUCCESS,
                CurrentUser.id().orElse(null),
                null,
                null,
                null,
                null,
                null,
                title,
                null,
                saved.getCode(),
                OrganizationService.DEFAULT_ID,
                null);
        return saved;
    }

    @Transactional
    public AccessReviewItem decideItem(UUID itemId, AccessReviewItem.Decision decision, String notes) {
        AccessReviewItem item = itemRepository
                .findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Item de revisão", itemId));
        if (item.getReview().getStatus() == AccessReview.Status.COMPLETED
                || item.getReview().getStatus() == AccessReview.Status.CANCELLED) {
            throw new BusinessRuleException("Revisão já finalizada");
        }
        item.getReview().setStatus(AccessReview.Status.IN_PROGRESS);
        item.setDecision(decision);
        item.setDecidedBy(CurrentUser.requireId());
        item.setDecidedAt(Instant.now());
        item.setNotes(notes);
        AccessReviewItem saved = itemRepository.save(item);

        AccessReviewDecision d = new AccessReviewDecision();
        d.setReviewItemId(saved.getId());
        d.setDecision(decision.name());
        d.setDecidedBy(CurrentUser.requireId());
        d.setReason(notes);
        decisionRepository.save(d);

        if (decision == AccessReviewItem.Decision.REMOVE && saved.getUserId() != null) {
            userGroupAssignmentRepository.findByUserIdAndActiveTrue(saved.getUserId()).forEach(a -> {
                if (saved.getGroupId() == null || saved.getGroupId().equals(a.getGroup().getId())) {
                    a.setStatus(br.com.systemcommerce.access.entity.UserGroupAssignment.Status.INACTIVE);
                    a.setActive(false);
                    userGroupAssignmentRepository.save(a);
                }
            });
        }
        return saved;
    }

    @Transactional
    public AccessReview complete(UUID reviewId) {
        AccessReview review = reviewRepository
                .findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Revisão", reviewId));
        review.setStatus(AccessReview.Status.COMPLETED);
        review.setCompletedAt(Instant.now());
        review.setNextReviewAt(Instant.now().plus(90, ChronoUnit.DAYS));
        return reviewRepository.save(review);
    }

    @Transactional(readOnly = true)
    public List<AccessReview> list() {
        return reviewRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<AccessReviewItem> items(UUID reviewId) {
        return itemRepository.findByReviewId(reviewId);
    }
}
