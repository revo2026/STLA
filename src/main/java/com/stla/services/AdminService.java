package com.stla.services;

import com.stla.data.repositories.InstructorRepositoryImpl;
import com.stla.domain.models.Instructor;
import com.stla.patterns.observer.AppEvent;
import com.stla.patterns.observer.EventBus;

import java.util.List;

/**
 * Service for admin-specific operations.
 * Controllers call this service; service delegates to repositories.
 */
public class AdminService {

    private final InstructorRepositoryImpl instructorRepo = new InstructorRepositoryImpl();
    private final WalletService walletService = new WalletService();

    /**
     * Get all instructors with PENDING verification status.
     */
    public List<Instructor> getPendingInstructorVerifications() {
        return instructorRepo.findPendingVerificationRequests();
    }

    /**
     * Approve an instructor's verification.
     */
    public void approveInstructorVerification(String instructorId, String adminProfileId) {
        instructorRepo.updateVerificationStatus(instructorId, "VERIFIED", null);

        EventBus.getInstance().publish(new AppEvent(
            AppEvent.EventType.INSTRUCTOR_VERIFIED,
            adminProfileId, instructorId, "Instructor verification approved"
        ));

        System.out.println("[AdminService] Instructor " + instructorId + " verification approved.");
    }

    /**
     * Reject an instructor's verification with a reason.
     */
    public void rejectInstructorVerification(String instructorId, String reason, String adminProfileId) {
        instructorRepo.updateVerificationStatus(instructorId, "REJECTED", reason);

        String rejectionMsg = "Your instructor verification was rejected. Reason: "
                + (reason != null ? reason : "No reason provided.");
        EventBus.getInstance().publish(new AppEvent(
            AppEvent.EventType.INSTRUCTOR_REJECTED,
            adminProfileId, instructorId, rejectionMsg
        ));

        System.out.println("[AdminService] Instructor " + instructorId + " verification rejected.");
    }

    public List<com.stla.domain.models.WalletTransaction> getWithdrawalHistory() {
        return walletService.getAllWithdrawalHistory();
    }
}
