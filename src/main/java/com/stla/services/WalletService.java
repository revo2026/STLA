package com.stla.services;

import com.stla.data.repositories.WalletRepository;
import com.stla.domain.models.*;
import com.stla.patterns.observer.AppEvent;
import com.stla.patterns.observer.EventBus;
import com.stla.patterns.strategy.WithdrawStrategy;
import com.stla.patterns.strategy.WithdrawStrategyFactory;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Instructor wallet: instant withdrawals (no admin approval).
 * Admin views history only.
 */
public class WalletService {

    private final WalletRepository repo = new WalletRepository();

    public Optional<InstructorWallet> getWallet(String instructorId) {
        return repo.findWalletByInstructorId(instructorId);
    }

    public List<WalletTransaction> getTransactions(String instructorId) {
        return repo.findTransactionsByInstructorId(instructorId);
    }

    public List<WalletTransaction> getWithdrawalHistory(String instructorId) {
        return repo.findWithdrawalTransactionsByInstructor(instructorId);
    }

    public List<WalletTransaction> getAllWithdrawalHistory() {
        return repo.findAllWithdrawalTransactions();
    }

    public List<InstructorPayoutSummary> getAllInstructorsForPayout() {
        return repo.findAllInstructorsForPayout();
    }

    public Map<String, BigDecimal> getMonthlyEarnings(String instructorId) {
        return repo.getMonthlyEarnings(instructorId);
    }

    public Map<String, BigDecimal> getCourseRevenue(String instructorId) {
        return repo.getCourseRevenue(instructorId);
    }

    public Optional<AdminWallet> getAdminWallet() {
        return repo.findAdminWallet();
    }

    public List<AdminWalletTransaction> getAdminTransactions() {
        return repo.findAdminTransactions();
    }

    public BigDecimal getTotalInstructorEarningsPaid() {
        return repo.getTotalInstructorEarningsPaid();
    }

    public BigDecimal getTotalInstructorWithdrawn() {
        return repo.getTotalInstructorWithdrawn();
    }

    public BigDecimal getAdminSpendableBalance(AdminWallet wallet) {
        return repo.getAdminSpendableBalance(wallet);
    }

    /**
     * Instant withdrawal: deduct balance, record completed transaction, notify instructor.
     */
    public void withdrawNow(String instructorId, BigDecimal amount, String method, String methodDetails) throws Exception {
        if (method == null || method.isBlank()) {
            throw new IllegalArgumentException("Withdrawal method is required");
        }
        WithdrawStrategy strategy = WithdrawStrategyFactory.getStrategy(method);
        String validationError = strategy.validate(methodDetails);
        if (validationError != null) throw new IllegalArgumentException(validationError);

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than 0");
        }

        InstructorWallet wallet = repo.findWalletByInstructorId(instructorId)
                .orElseThrow(() -> new IllegalStateException("Wallet not found"));
        if (amount.compareTo(wallet.getAvailableBalance()) > 0) {
            throw new IllegalArgumentException("Insufficient balance. Available: $" + wallet.getAvailableBalance());
        }

        String txnId = repo.executeInstantWithdrawalTx(instructorId, amount, method, methodDetails);

        EventBus.getInstance().publish(new AppEvent(
                AppEvent.EventType.WITHDRAWAL_COMPLETED,
                instructorId, txnId,
                "Withdrawal completed successfully."));
    }
}
