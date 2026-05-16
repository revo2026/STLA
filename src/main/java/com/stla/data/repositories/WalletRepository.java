package com.stla.data.repositories;

import com.stla.core.database.DatabaseConnection;
import com.stla.domain.enums.WithdrawalStatus;
import com.stla.domain.models.AdminWallet;
import com.stla.domain.models.AdminWalletTransaction;
import com.stla.domain.models.*;

import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

/**
 * Repository for instructor wallet, transactions, and withdrawal requests.
 * All wallet balance operations use transactional safety.
 */
public class WalletRepository {

    private Connection getConn() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    // ==================== WALLET ====================

    public Optional<InstructorWallet> findWalletByInstructorId(String instructorId) {
        String sql = "SELECT * FROM instructor_wallets WHERE instructor_id=?::uuid";
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, instructorId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapWallet(rs));
            }
        } catch (SQLException e) { System.err.println("Wallet find error: " + e.getMessage()); }
        return Optional.empty();
    }

    public void deductAvailableBalance(String walletId, BigDecimal amount) throws SQLException {
        String sql = "UPDATE instructor_wallets SET available_balance = available_balance - ?, pending_balance = pending_balance + ?, updated_at = now() WHERE id = ?::uuid AND available_balance >= ?";
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setBigDecimal(1, amount);
            ps.setBigDecimal(2, amount);
            ps.setString(3, walletId);
            ps.setBigDecimal(4, amount);
            int rows = ps.executeUpdate();
            if (rows == 0) throw new SQLException("Insufficient balance or wallet not found");
        }
    }

    public void refundAvailableBalance(String walletId, BigDecimal amount) {
        String sql = "UPDATE instructor_wallets SET available_balance = available_balance + ?, pending_balance = pending_balance - ?, updated_at = now() WHERE id = ?::uuid";
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setBigDecimal(1, amount); ps.setBigDecimal(2, amount); ps.setString(3, walletId);
            ps.executeUpdate();
        } catch (SQLException e) { System.err.println("Refund error: " + e.getMessage()); }
    }

    public void completeWithdrawalBalance(String walletId, BigDecimal amount) {
        String sql = "UPDATE instructor_wallets SET pending_balance = pending_balance - ?, total_withdrawn = total_withdrawn + ?, updated_at = now() WHERE id = ?::uuid";
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setBigDecimal(1, amount); ps.setBigDecimal(2, amount); ps.setString(3, walletId);
            ps.executeUpdate();
        } catch (SQLException e) { System.err.println("Complete balance error: " + e.getMessage()); }
    }

    // ==================== TRANSACTIONS ====================

    public void ensureInstructorWallet(Connection conn, String instructorId) throws SQLException {
        String sql = "INSERT INTO instructor_wallets (instructor_id) VALUES (?::uuid) ON CONFLICT (instructor_id) DO NOTHING";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, instructorId);
            ps.executeUpdate();
        }
    }

    public InstructorWallet findWalletByInstructorIdConn(Connection conn, String instructorId) throws SQLException {
        String sql = "SELECT * FROM instructor_wallets WHERE instructor_id=?::uuid";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, instructorId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapWallet(rs);
            }
        }
        throw new SQLException("Instructor wallet not found");
    }

    public void creditInstructorRevenue(Connection conn, String instructorId, BigDecimal amount, String paymentId, String courseId, String note) throws SQLException {
        ensureInstructorWallet(conn, instructorId);
        InstructorWallet wallet = findWalletByInstructorIdConn(conn, instructorId);
        BigDecimal before = wallet.getAvailableBalance();
        BigDecimal after = before.add(amount);

        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE instructor_wallets SET available_balance=available_balance+?, total_earned=total_earned+?, updated_at=now() WHERE instructor_id=?::uuid")) {
            ps.setBigDecimal(1, amount);
            ps.setBigDecimal(2, amount);
            ps.setString(3, instructorId);
            ps.executeUpdate();
        }

        WalletTransaction txn = new WalletTransaction();
        txn.setWalletId(wallet.getId());
        txn.setType("earning");
        txn.setAmount(amount);
        txn.setBalanceBefore(before);
        txn.setBalanceAfter(after);
        txn.setDescription(note);
        txn.setStatus("completed");
        txn.setRelatedCourseId(courseId);
        addTransactionConn(conn, txn, instructorId, paymentId, 1);
    }

    public String addTransactionConn(Connection conn, WalletTransaction t, String instructorId, String paymentId, int direction) throws SQLException {
        String sql = """
            INSERT INTO wallet_transactions (wallet_id, instructor_id, payment_id, transaction_type, status, amount, direction, balance_before, balance_after, note)
            VALUES (?::uuid, ?::uuid, ?::uuid, ?::wallet_transaction_type, 'completed'::txn_status, ?, ?, ?, ?, ?)
            RETURNING id
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, t.getWalletId());
            ps.setString(2, instructorId);
            if (paymentId != null) ps.setString(3, paymentId);
            else ps.setNull(3, Types.OTHER);
            ps.setString(4, t.getType() != null ? t.getType() : "earning");
            ps.setBigDecimal(5, t.getAmount());
            ps.setInt(6, direction);
            ps.setBigDecimal(7, t.getBalanceBefore());
            ps.setBigDecimal(8, t.getBalanceAfter());
            ps.setString(9, t.getDescription());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("id");
            }
        }
        throw new SQLException("Failed to add wallet transaction");
    }

    // ==================== ADMIN WALLET ====================

    public AdminWallet getOrCreateAdminWallet(Connection conn) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM admin_wallet ORDER BY created_at LIMIT 1");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return mapAdminWallet(rs);
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().contains("admin_wallet")) {
                throw e;
            }
        }
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO admin_wallet (currency) VALUES ('USD') RETURNING *")) {
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapAdminWallet(rs);
            }
        }
        throw new SQLException("Admin wallet unavailable");
    }

    public Optional<AdminWallet> findAdminWallet() {
        try (Connection c = getConn();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM admin_wallet ORDER BY created_at LIMIT 1");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return Optional.of(mapAdminWallet(rs));
        } catch (SQLException e) {
            System.err.println("Admin wallet error: " + e.getMessage());
        }
        return Optional.empty();
    }

    public void recordCoursePurchaseRevenue(Connection conn, String paymentId, BigDecimal grossAmount,
                                            BigDecimal commission, BigDecimal instructorShare) throws SQLException {
        AdminWallet wallet;
        try {
            wallet = getOrCreateAdminWallet(conn);
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().toLowerCase().contains("admin_wallet")) {
                throw new SQLException("admin_wallet table not found — run payment_enrollment_wallet_migration.sql", e);
            }
            throw e;
        }
        BigDecimal before = wallet.getAvailableBalance() != null ? wallet.getAvailableBalance() : BigDecimal.ZERO;
        BigDecimal after = before.add(commission);

        String updateSql = """
            UPDATE admin_wallet SET
              total_revenue = total_revenue + ?,
              total_commissions = total_commissions + ?,
              paid_instructor_payouts = paid_instructor_payouts + ?,
              available_balance = available_balance + ?,
              updated_at = now()
            WHERE id = ?::uuid
            """;
        try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
            ps.setBigDecimal(1, grossAmount);
            ps.setBigDecimal(2, commission);
            ps.setBigDecimal(3, instructorShare);
            ps.setBigDecimal(4, commission);
            ps.setString(5, wallet.getId());
            ps.executeUpdate();
        }

        insertAdminTransaction(conn, wallet.getId(), paymentId, "PLATFORM_COMMISSION", commission, before, after,
                "Platform commission from course sale");
    }

    /**
     * Instant instructor withdrawal — no admin approval, no withdrawal_requests row.
     */
    public String executeInstantWithdrawalTx(String instructorId, BigDecimal amount,
                                             String method, String methodDetails) throws SQLException {
        try (Connection conn = getConn()) {
            conn.setAutoCommit(false);
            try {
                ensureInstructorWallet(conn, instructorId);
                InstructorWallet wallet = findWalletByInstructorIdConn(conn, instructorId);
                if (amount.compareTo(wallet.getAvailableBalance()) > 0) {
                    throw new SQLException("Insufficient balance");
                }
                BigDecimal before = wallet.getAvailableBalance() != null ? wallet.getAvailableBalance() : BigDecimal.ZERO;
                BigDecimal after = before.subtract(amount);

                String updateSql = """
                    UPDATE instructor_wallets
                    SET available_balance = available_balance - ?,
                        total_withdrawn = total_withdrawn + ?,
                        updated_at = now()
                    WHERE instructor_id = ?::uuid AND available_balance >= ?
                    """;
                try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                    ps.setBigDecimal(1, amount);
                    ps.setBigDecimal(2, amount);
                    ps.setString(3, instructorId);
                    ps.setBigDecimal(4, amount);
                    if (ps.executeUpdate() == 0) throw new SQLException("Insufficient balance");
                }

                String note = "Withdrawal via " + method
                        + (methodDetails != null && !methodDetails.isBlank() ? " • " + methodDetails : "");
                WalletTransaction txn = new WalletTransaction();
                txn.setWalletId(wallet.getId());
                txn.setType("withdrawal");
                txn.setAmount(amount);
                txn.setBalanceBefore(before);
                txn.setBalanceAfter(after);
                txn.setDescription(note);
                txn.setStatus("completed");
                String txnId = addTransactionConn(conn, txn, instructorId, null, -1);
                conn.commit();
                return txnId;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public List<WalletTransaction> findAllWithdrawalTransactions() {
        String sql = """
            SELECT wt.id, wt.wallet_id, wt.transaction_type, wt.status, wt.amount,
                   wt.direction, wt.balance_before, wt.balance_after, wt.note, wt.created_at,
                   p.full_name AS instructor_name, p.email AS instructor_email
            FROM wallet_transactions wt
            JOIN instructor_wallets iw ON iw.id = wt.wallet_id
            JOIN instructors i ON i.id = iw.instructor_id
            JOIN profiles p ON p.id = i.profile_id
            WHERE wt.transaction_type::text = 'withdrawal'
            ORDER BY wt.created_at DESC
            LIMIT 200
            """;
        return queryWalletTransactions(sql, null);
    }

    public List<WalletTransaction> findWithdrawalTransactionsByInstructor(String instructorId) {
        String sql = """
            SELECT wt.id, wt.wallet_id, wt.transaction_type, wt.status, wt.amount,
                   wt.direction, wt.balance_before, wt.balance_after, wt.note, wt.created_at
            FROM wallet_transactions wt
            JOIN instructor_wallets iw ON iw.id = wt.wallet_id
            WHERE iw.instructor_id = ?::uuid AND wt.transaction_type::text = 'withdrawal'
            ORDER BY wt.created_at DESC
            LIMIT 50
            """;
        return queryWalletTransactions(sql, instructorId);
    }

    private List<WalletTransaction> queryWalletTransactions(String sql, String instructorId) {
        List<WalletTransaction> list = new ArrayList<>();
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement(sql)) {
            if (instructorId != null) ps.setString(1, instructorId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapTransaction(rs));
            }
        } catch (SQLException e) {
            System.err.println("Wallet transactions query error: " + e.getMessage());
        }
        return list;
    }

    public BigDecimal getTotalInstructorEarningsPaid() {
        String sql = "SELECT COALESCE(SUM(total_earned), 0) FROM instructor_wallets";
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getBigDecimal(1);
        } catch (SQLException e) {
            System.err.println("Total instructor earnings error: " + e.getMessage());
        }
        return BigDecimal.ZERO;
    }

    /** Sum of all instructor cash-outs (total_withdrawn across wallets). */
    public BigDecimal getTotalInstructorWithdrawn() {
        String sql = "SELECT COALESCE(SUM(total_withdrawn), 0) FROM instructor_wallets";
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getBigDecimal(1);
        } catch (SQLException e) {
            System.err.println("Total instructor withdrawn error: " + e.getMessage());
        }
        return BigDecimal.ZERO;
    }

    /** Commission treasury — prefer available_balance; fall back to total_commissions if out of sync. */
    public BigDecimal getAdminSpendableBalance(AdminWallet wallet) {
        if (wallet == null) return BigDecimal.ZERO;
        BigDecimal available = wallet.getAvailableBalance() != null ? wallet.getAvailableBalance() : BigDecimal.ZERO;
        BigDecimal commissions = wallet.getTotalCommissions() != null ? wallet.getTotalCommissions() : BigDecimal.ZERO;
        if (available.compareTo(BigDecimal.ZERO) == 0 && commissions.compareTo(BigDecimal.ZERO) > 0) {
            return commissions;
        }
        return available;
    }

    private void insertAdminTransaction(Connection conn, String walletId, String paymentId, String type,
                                        BigDecimal amount, BigDecimal before, BigDecimal after, String note) throws SQLException {
        String sql = """
            INSERT INTO admin_wallet_transactions (admin_wallet_id, payment_id, transaction_type, amount, balance_before, balance_after, note)
            VALUES (?::uuid, ?::uuid, ?, ?, ?, ?, ?)
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, walletId);
            if (paymentId != null) ps.setString(2, paymentId);
            else ps.setNull(2, Types.OTHER);
            ps.setString(3, type);
            ps.setBigDecimal(4, amount);
            ps.setBigDecimal(5, before);
            ps.setBigDecimal(6, after);
            ps.setString(7, note);
            ps.executeUpdate();
        }
    }

    public List<AdminWalletTransaction> findAdminTransactions() {
        List<AdminWalletTransaction> list = new ArrayList<>();
        String sql = "SELECT * FROM admin_wallet_transactions ORDER BY created_at DESC LIMIT 100";
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapAdminTxn(rs));
        } catch (SQLException e) {
            System.err.println("Admin transactions error: " + e.getMessage());
        }
        return list;
    }

    /** Uses its own connection so a missing platform_settings table cannot abort a purchase transaction. */
    public int getCommissionPercent() {
        String sql = "SELECT setting_value FROM platform_settings WHERE setting_key='commission_percent' LIMIT 1";
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return Integer.parseInt(rs.getString("setting_value"));
        } catch (SQLException ignored) {}
        return com.stla.app.AppConfig.getInstance().getPlatformCommissionPercent();
    }

    public List<WalletTransaction> findTransactionsByInstructorId(String instructorId) {
        String sql = """
            SELECT wt.*, p.course_id
            FROM wallet_transactions wt
            JOIN instructor_wallets iw ON iw.id = wt.wallet_id
            LEFT JOIN payments p ON p.id = wt.payment_id
            WHERE iw.instructor_id = ?::uuid
            ORDER BY wt.created_at DESC LIMIT 100
            """;
        List<WalletTransaction> list = new ArrayList<>();
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, instructorId);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) list.add(mapTransaction(rs)); }
        } catch (SQLException e) { System.err.println("Transactions find error: " + e.getMessage()); }
        return list;
    }

    public String addTransaction(WalletTransaction t) {
        try (Connection c = getConn()) {
            String instructorId = resolveInstructorId(c, t.getWalletId());
            if (instructorId != null) {
                int direction = t.getAmount() != null && t.getAmount().signum() < 0 ? -1 : 1;
                return addTransactionConn(c, t, instructorId, null, direction);
            }
        } catch (SQLException e) {
            System.err.println("Add transaction error: " + e.getMessage());
        }
        return null;
    }

    private String resolveInstructorId(Connection c, String walletId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT instructor_id FROM instructor_wallets WHERE id=?::uuid")) {
            ps.setString(1, walletId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("instructor_id");
            }
        }
        return null;
    }

    // ==================== WITHDRAWALS ====================

    public List<WithdrawalRequest> findWithdrawalsByInstructorId(String instructorId) {
        String sql = "SELECT * FROM withdrawal_requests WHERE instructor_id = ?::uuid ORDER BY requested_at DESC";
        List<WithdrawalRequest> list = new ArrayList<>();
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, instructorId);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) list.add(mapWithdrawal(rs)); }
        } catch (SQLException e) { System.err.println("Withdrawals find error: " + e.getMessage()); }
        return list;
    }

    public List<WithdrawalRequest> findAllWithdrawals() {
        String sql = """
            SELECT wr.id AS withdrawal_id, wr.instructor_id, wr.wallet_id, wr.amount, wr.method,
                   wr.method_details, wr.status, wr.reviewed_by_admin_id, wr.review_note,
                   wr.requested_at, wr.reviewed_at, wr.completed_at,
                   p.full_name AS instructor_name, p.email AS instructor_email,
                   iw.available_balance AS wallet_available_balance,
                   iw.pending_balance AS wallet_pending_balance,
                   (SELECT COUNT(*)::int FROM withdrawal_requests wr2
                    WHERE wr2.instructor_id = wr.instructor_id AND wr2.status = 'completed') AS completed_withdrawals_count
            FROM withdrawal_requests wr
            JOIN instructors i ON i.id = wr.instructor_id
            JOIN profiles p ON p.id = i.profile_id
            LEFT JOIN instructor_wallets iw ON iw.instructor_id = wr.instructor_id
            ORDER BY wr.requested_at DESC
            """;
        List<WithdrawalRequest> list = new ArrayList<>();
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapWithdrawalAdmin(rs));
        } catch (SQLException e) {
            System.err.println("All withdrawals error: " + e.getMessage());
            e.printStackTrace();
        }
        return list;
    }

    public List<InstructorPayoutSummary> findAllInstructorsForPayout() {
        String sql = """
            SELECT i.id AS instructor_id, p.full_name, p.email,
                   COALESCE(iw.available_balance, 0) AS available_balance,
                   COALESCE(iw.pending_balance, 0) AS pending_balance,
                   COALESCE(iw.total_earned, 0) AS total_earned,
                   COALESCE(iw.total_withdrawn, 0) AS total_withdrawn,
                   (SELECT COUNT(*)::int FROM withdrawal_requests wr
                    WHERE wr.instructor_id = i.id AND wr.status = 'pending') AS pending_withdrawal_count
            FROM instructors i
            JOIN profiles p ON p.id = i.profile_id
            LEFT JOIN instructor_wallets iw ON iw.instructor_id = i.id
            ORDER BY p.full_name
            """;
        List<InstructorPayoutSummary> list = new ArrayList<>();
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapInstructorPayout(rs));
        } catch (SQLException e) {
            System.err.println("Instructors payout list error: " + e.getMessage());
            e.printStackTrace();
        }
        return list;
    }

    public List<String> findAllAdminProfileIds() {
        List<String> ids = new ArrayList<>();
        String sql = "SELECT profile_id FROM admins";
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) ids.add(rs.getString("profile_id"));
        } catch (SQLException e) { System.err.println("Admin profiles error: " + e.getMessage()); }
        return ids;
    }

    public String createWithdrawalRequest(WithdrawalRequest wr) {
        try (Connection c = getConn()) {
            c.setAutoCommit(false);
            try {
                String id = createWithdrawalRequestConn(c, wr);
                c.commit();
                return id;
            } catch (SQLException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        } catch (SQLException e) {
            System.err.println("Create withdrawal error: " + e.getMessage());
            return null;
        }
    }

    private String createWithdrawalRequestConn(Connection conn, WithdrawalRequest wr) throws SQLException {
        String sql = """
            INSERT INTO withdrawal_requests (instructor_id, wallet_id, amount, method, method_details, status, review_note)
            VALUES (?::uuid, ?::uuid, ?, ?::withdraw_method_type, ?::jsonb, 'pending', ?)
            RETURNING id
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, wr.getInstructorId());
            ps.setString(2, wr.getWalletId());
            ps.setBigDecimal(3, wr.getAmount());
            ps.setString(4, wr.getMethod());
            ps.setString(5, toJsonbDetails(wr.getMethodDetails()));
            if (wr.getReviewNote() != null && !wr.getReviewNote().isBlank()) ps.setString(6, wr.getReviewNote());
            else ps.setNull(6, Types.VARCHAR);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("id");
            }
        }
        throw new SQLException("Failed to create withdrawal request");
    }

    private void deductAvailableBalanceConn(Connection conn, String walletId, BigDecimal amount) throws SQLException {
        String sql = """
            UPDATE instructor_wallets
            SET available_balance = available_balance - ?, pending_balance = pending_balance + ?, updated_at = now()
            WHERE id = ?::uuid AND available_balance >= ?
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, amount);
            ps.setBigDecimal(2, amount);
            ps.setString(3, walletId);
            ps.setBigDecimal(4, amount);
            if (ps.executeUpdate() == 0) throw new SQLException("Insufficient balance or wallet not found");
        }
    }

    private void refundAvailableBalanceConn(Connection conn, String walletId, BigDecimal amount) throws SQLException {
        String sql = """
            UPDATE instructor_wallets
            SET available_balance = available_balance + ?, pending_balance = pending_balance - ?, updated_at = now()
            WHERE id = ?::uuid
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, amount);
            ps.setBigDecimal(2, amount);
            ps.setString(3, walletId);
            ps.executeUpdate();
        }
    }

    private void completeWithdrawalBalanceConn(Connection conn, String walletId, BigDecimal amount) throws SQLException {
        String sql = """
            UPDATE instructor_wallets
            SET pending_balance = pending_balance - ?, total_withdrawn = total_withdrawn + ?, updated_at = now()
            WHERE id = ?::uuid
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, amount);
            ps.setBigDecimal(2, amount);
            ps.setString(3, walletId);
            ps.executeUpdate();
        }
    }

    private void addWithdrawalTransactionConn(Connection conn, InstructorWallet wallet, String instructorId,
                                                BigDecimal amount, String withdrawalId, String status,
                                                String note, int direction) throws SQLException {
        BigDecimal before = wallet.getAvailableBalance();
        BigDecimal after = direction < 0 ? before.subtract(amount) : before.add(amount);
        String sql = """
            INSERT INTO wallet_transactions (wallet_id, instructor_id, withdrawal_request_id, transaction_type, status, amount, direction, balance_before, balance_after, note)
            VALUES (?::uuid, ?::uuid, ?::uuid, 'withdrawal'::wallet_transaction_type, ?::txn_status, ?, ?, ?, ?, ?)
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, wallet.getId());
            ps.setString(2, instructorId);
            ps.setString(3, withdrawalId);
            ps.setString(4, status);
            ps.setBigDecimal(5, amount);
            ps.setInt(6, direction);
            ps.setBigDecimal(7, before);
            ps.setBigDecimal(8, after);
            ps.setString(9, note);
            ps.executeUpdate();
        }
    }

    public String executeWithdrawalRequestTx(String instructorId, BigDecimal amount, String method,
                                             String methodDetails, String notes) throws SQLException {
        try (Connection conn = getConn()) {
            conn.setAutoCommit(false);
            try {
                InstructorWallet wallet = findWalletByInstructorIdConn(conn, instructorId);
                if (amount.compareTo(wallet.getAvailableBalance()) > 0) {
                    throw new SQLException("Insufficient balance");
                }
                deductAvailableBalanceConn(conn, wallet.getId(), amount);
                wallet = findWalletByInstructorIdConn(conn, instructorId);

                WithdrawalRequest wr = new WithdrawalRequest();
                wr.setInstructorId(instructorId);
                wr.setWalletId(wallet.getId());
                wr.setAmount(amount);
                wr.setMethod(method);
                wr.setMethodDetails(methodDetails);
                wr.setReviewNote(notes);
                String wrId = createWithdrawalRequestConn(conn, wr);

                addWithdrawalTransactionConn(conn, wallet, instructorId, amount, wrId, "pending",
                        "Withdrawal request via " + method, -1);
                conn.commit();
                return wrId;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public void executeApproveWithdrawalTx(String withdrawalId, String adminId) throws SQLException {
        try (Connection conn = getConn()) {
            conn.setAutoCommit(false);
            try {
                WithdrawalRequest wr = findWithdrawalByIdConn(conn, withdrawalId);
                if (!"pending".equals(wr.getStatus().getValue())) {
                    throw new SQLException("Only pending withdrawals can be approved");
                }
                updateWithdrawalStatusConn(conn, withdrawalId, "approved", adminId, null);
                InstructorWallet wallet = findWalletByInstructorIdConn(conn, wr.getInstructorId());
                addWithdrawalTransactionConn(conn, wallet, wr.getInstructorId(), wr.getAmount(), withdrawalId,
                        "completed", "Withdrawal approved by admin", -1);
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public void executeRejectWithdrawalTx(String withdrawalId, String adminId, String reason) throws SQLException {
        try (Connection conn = getConn()) {
            conn.setAutoCommit(false);
            try {
                WithdrawalRequest wr = findWithdrawalByIdConn(conn, withdrawalId);
                if (!"pending".equals(wr.getStatus().getValue())) {
                    throw new SQLException("Only pending withdrawals can be rejected");
                }
                updateWithdrawalStatusConn(conn, withdrawalId, "rejected", adminId, reason);
                InstructorWallet wallet = findWalletByInstructorIdConn(conn, wr.getInstructorId());
                refundAvailableBalanceConn(conn, wallet.getId(), wr.getAmount());
                wallet = findWalletByInstructorIdConn(conn, wr.getInstructorId());
                addWithdrawalTransactionConn(conn, wallet, wr.getInstructorId(), wr.getAmount(), withdrawalId,
                        "completed", "Withdrawal rejected — funds returned. Reason: " + reason, 1);
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public void executeCompleteWithdrawalTx(String withdrawalId) throws SQLException {
        try (Connection conn = getConn()) {
            conn.setAutoCommit(false);
            try {
                WithdrawalRequest wr = findWithdrawalByIdConn(conn, withdrawalId);
                if (!"approved".equals(wr.getStatus().getValue())) {
                    throw new SQLException("Only approved withdrawals can be marked completed");
                }
                markWithdrawalCompletedConn(conn, withdrawalId);
                completeWithdrawalBalanceConn(conn, wr.getWalletId(), wr.getAmount());
                InstructorWallet wallet = findWalletByInstructorIdConn(conn, wr.getInstructorId());
                addWithdrawalTransactionConn(conn, wallet, wr.getInstructorId(), wr.getAmount(), withdrawalId,
                        "completed", "Payout sent to instructor", -1);
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    private WithdrawalRequest findWithdrawalByIdConn(Connection conn, String id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM withdrawal_requests WHERE id=?::uuid")) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapWithdrawal(rs);
            }
        }
        throw new SQLException("Withdrawal not found");
    }

    private void updateWithdrawalStatusConn(Connection conn, String withdrawalId, String status,
                                            String adminId, String note) throws SQLException {
        String sql = """
            UPDATE withdrawal_requests
            SET status=?::withdrawal_status, reviewed_by_admin_id=?::uuid, review_note=?, reviewed_at=now(), updated_at=now()
            WHERE id=?::uuid
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setString(2, adminId);
            if (note != null) ps.setString(3, note);
            else ps.setNull(3, Types.VARCHAR);
            ps.setString(4, withdrawalId);
            ps.executeUpdate();
        }
    }

    private void markWithdrawalCompletedConn(Connection conn, String withdrawalId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE withdrawal_requests SET status='completed', completed_at=now(), updated_at=now() WHERE id=?::uuid")) {
            ps.setString(1, withdrawalId);
            ps.executeUpdate();
        }
    }

    private static String toJsonbDetails(String details) {
        if (details == null || details.isBlank()) return "{}";
        String escaped = details.replace("\\", "\\\\").replace("\"", "\\\"");
        return "{\"masked\":\"" + escaped + "\"}";
    }

    public void updateWithdrawalStatus(String withdrawalId, String status, String adminId, String note) {
        String sql = "UPDATE withdrawal_requests SET status=?::withdrawal_status, reviewed_by_admin_id=?::uuid, review_note=?, reviewed_at=now() WHERE id=?::uuid";
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, status); ps.setString(2, adminId);
            if (note != null) ps.setString(3, note); else ps.setNull(3, Types.VARCHAR);
            ps.setString(4, withdrawalId); ps.executeUpdate();
        } catch (SQLException e) { System.err.println("Update withdrawal error: " + e.getMessage()); }
    }

    public void markWithdrawalCompleted(String withdrawalId) {
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement("UPDATE withdrawal_requests SET status='completed', completed_at=now() WHERE id=?::uuid")) {
            ps.setString(1, withdrawalId); ps.executeUpdate();
        } catch (SQLException e) { System.err.println("Complete withdrawal error: " + e.getMessage()); }
    }

    public Optional<WithdrawalRequest> findWithdrawalById(String id) {
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement("SELECT * FROM withdrawal_requests WHERE id=?::uuid")) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return Optional.of(mapWithdrawal(rs)); }
        } catch (SQLException e) { System.err.println("Find withdrawal error: " + e.getMessage()); }
        return Optional.empty();
    }

    // ==================== CHART DATA ====================

    public Map<String, BigDecimal> getMonthlyEarnings(String instructorId) {
        String sql = """
            SELECT TO_CHAR(wt.created_at, 'Mon') AS month, SUM(wt.amount) AS total
            FROM wallet_transactions wt
            JOIN instructor_wallets iw ON iw.id = wt.wallet_id
            WHERE iw.instructor_id = ?::uuid AND wt.transaction_type::text = 'earning'
              AND wt.created_at > now() - interval '6 months'
            GROUP BY TO_CHAR(wt.created_at, 'Mon'), EXTRACT(MONTH FROM wt.created_at)
            ORDER BY EXTRACT(MONTH FROM wt.created_at)
            """;
        Map<String, BigDecimal> map = new LinkedHashMap<>();
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, instructorId);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) map.put(rs.getString("month"), rs.getBigDecimal("total")); }
        } catch (SQLException e) { System.err.println("Monthly earnings error: " + e.getMessage()); }
        return map;
    }

    public Map<String, BigDecimal> getCourseRevenue(String instructorId) {
        String sql = "SELECT c.title, COALESCE(SUM(p.amount),0) as revenue FROM courses c LEFT JOIN payments p ON p.course_id=c.id AND p.status='paid' WHERE c.instructor_id=?::uuid GROUP BY c.title ORDER BY revenue DESC LIMIT 6";
        Map<String, BigDecimal> map = new LinkedHashMap<>();
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, instructorId);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) map.put(rs.getString("title"), rs.getBigDecimal("revenue")); }
        } catch (SQLException e) { System.err.println("Course revenue error: " + e.getMessage()); }
        return map;
    }

    // ==================== MAPPERS ====================

    private InstructorWallet mapWallet(ResultSet rs) throws SQLException {
        InstructorWallet w = new InstructorWallet();
        w.setId(rs.getString("id")); w.setInstructorId(rs.getString("instructor_id"));
        w.setCurrency(rs.getString("currency"));
        w.setPendingBalance(rs.getBigDecimal("pending_balance"));
        w.setAvailableBalance(rs.getBigDecimal("available_balance"));
        w.setTotalEarned(rs.getBigDecimal("total_earned"));
        w.setTotalWithdrawn(rs.getBigDecimal("total_withdrawn"));
        w.setCreatedAt(rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null);
        return w;
    }

    private WalletTransaction mapTransaction(ResultSet rs) throws SQLException {
        WalletTransaction t = new WalletTransaction();
        t.setId(rs.getString("id"));
        t.setWalletId(rs.getString("wallet_id"));
        try { t.setType(rs.getString("transaction_type")); } catch (SQLException e) { t.setType(rs.getString("type")); }
        t.setAmount(rs.getBigDecimal("amount"));
        t.setBalanceBefore(rs.getBigDecimal("balance_before"));
        t.setBalanceAfter(rs.getBigDecimal("balance_after"));
        try { t.setDescription(rs.getString("note")); } catch (SQLException e) { t.setDescription(rs.getString("description")); }
        t.setStatus(rs.getString("status"));
        try { t.setRelatedCourseId(rs.getString("course_id")); } catch (SQLException ignored) {}
        try { t.setCourseName(rs.getString("course_name")); } catch (SQLException ignored) {}
        try { t.setInstructorName(rs.getString("instructor_name")); } catch (SQLException ignored) {}
        try { t.setInstructorEmail(rs.getString("instructor_email")); } catch (SQLException ignored) {}
        t.setCreatedAt(rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null);
        return t;
    }

    private AdminWallet mapAdminWallet(ResultSet rs) throws SQLException {
        AdminWallet w = new AdminWallet();
        w.setId(rs.getString("id"));
        w.setCurrency(rs.getString("currency"));
        w.setTotalRevenue(rs.getBigDecimal("total_revenue"));
        w.setTotalCommissions(rs.getBigDecimal("total_commissions"));
        w.setPendingInstructorPayouts(rs.getBigDecimal("pending_instructor_payouts"));
        w.setPaidInstructorPayouts(rs.getBigDecimal("paid_instructor_payouts"));
        w.setAvailableBalance(rs.getBigDecimal("available_balance"));
        if (rs.getTimestamp("updated_at") != null) {
            w.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        }
        return w;
    }

    private AdminWalletTransaction mapAdminTxn(ResultSet rs) throws SQLException {
        AdminWalletTransaction t = new AdminWalletTransaction();
        t.setId(rs.getString("id"));
        t.setAdminWalletId(rs.getString("admin_wallet_id"));
        t.setPaymentId(rs.getString("payment_id"));
        t.setTransactionType(rs.getString("transaction_type"));
        t.setAmount(rs.getBigDecimal("amount"));
        t.setBalanceBefore(rs.getBigDecimal("balance_before"));
        t.setBalanceAfter(rs.getBigDecimal("balance_after"));
        t.setNote(rs.getString("note"));
        if (rs.getTimestamp("created_at") != null) {
            t.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        }
        return t;
    }

    private InstructorPayoutSummary mapInstructorPayout(ResultSet rs) throws SQLException {
        InstructorPayoutSummary s = new InstructorPayoutSummary();
        s.setInstructorId(rs.getString("instructor_id"));
        s.setFullName(rs.getString("full_name"));
        s.setEmail(rs.getString("email"));
        s.setAvailableBalance(rs.getBigDecimal("available_balance"));
        s.setPendingBalance(rs.getBigDecimal("pending_balance"));
        s.setTotalEarned(rs.getBigDecimal("total_earned"));
        s.setTotalWithdrawn(rs.getBigDecimal("total_withdrawn"));
        s.setPendingWithdrawalCount(rs.getInt("pending_withdrawal_count"));
        return s;
    }

    private WithdrawalRequest mapWithdrawalAdmin(ResultSet rs) throws SQLException {
        WithdrawalRequest wr = new WithdrawalRequest();
        wr.setId(rs.getString("withdrawal_id"));
        wr.setInstructorId(rs.getString("instructor_id"));
        wr.setInstructorName(rs.getString("instructor_name"));
        wr.setInstructorEmail(rs.getString("instructor_email"));
        wr.setWalletAvailableBalance(rs.getBigDecimal("wallet_available_balance"));
        wr.setWalletPendingBalance(rs.getBigDecimal("wallet_pending_balance"));
        wr.setCompletedWithdrawalsCount(rs.getInt("completed_withdrawals_count"));
        wr.setAmount(rs.getBigDecimal("amount"));
        wr.setMethod(rs.getString("method"));
        wr.setMethodDetails(parseMethodDetails(rs.getString("method_details")));
        wr.setStatus(WithdrawalStatus.fromValue(rs.getString("status")));
        try { wr.setWalletId(rs.getString("wallet_id")); } catch (SQLException ignored) {}
        try { wr.setReviewedByAdminId(rs.getString("reviewed_by_admin_id")); } catch (SQLException ignored) {}
        try { wr.setReviewNote(rs.getString("review_note")); } catch (SQLException ignored) {}
        wr.setRequestedAt(rs.getTimestamp("requested_at") != null ? rs.getTimestamp("requested_at").toLocalDateTime() : null);
        try { wr.setReviewedAt(rs.getTimestamp("reviewed_at") != null ? rs.getTimestamp("reviewed_at").toLocalDateTime() : null); } catch (SQLException ignored) {}
        try { wr.setCompletedAt(rs.getTimestamp("completed_at") != null ? rs.getTimestamp("completed_at").toLocalDateTime() : null); } catch (SQLException ignored) {}
        return wr;
    }

    private WithdrawalRequest mapWithdrawal(ResultSet rs) throws SQLException {
        WithdrawalRequest wr = new WithdrawalRequest();
        try {
            wr.setId(rs.getString("withdrawal_id"));
        } catch (SQLException e) {
            wr.setId(rs.getString("id"));
        }
        wr.setInstructorId(rs.getString("instructor_id"));
        try { wr.setWalletId(rs.getString("wallet_id")); } catch (SQLException ignored) {}
        wr.setAmount(rs.getBigDecimal("amount")); wr.setMethod(rs.getString("method"));
        try { wr.setMethodDetails(parseMethodDetails(rs.getString("method_details"))); } catch (SQLException ignored) {}
        try { wr.setInstructorEmail(rs.getString("instructor_email")); } catch (SQLException ignored) {}
        try { wr.setWalletAvailableBalance(rs.getBigDecimal("wallet_available_balance")); } catch (SQLException ignored) {}
        try { wr.setWalletPendingBalance(rs.getBigDecimal("wallet_pending_balance")); } catch (SQLException ignored) {}
        try { wr.setCompletedWithdrawalsCount(rs.getInt("completed_withdrawals_count")); } catch (SQLException ignored) {}
        wr.setStatus(WithdrawalStatus.fromValue(rs.getString("status")));
        try { wr.setReviewedByAdminId(rs.getString("reviewed_by_admin_id")); } catch (SQLException ignored) {}
        try { wr.setReviewNote(rs.getString("review_note")); } catch (SQLException ignored) {}
        wr.setRequestedAt(rs.getTimestamp("requested_at") != null ? rs.getTimestamp("requested_at").toLocalDateTime() : null);
        try { wr.setReviewedAt(rs.getTimestamp("reviewed_at") != null ? rs.getTimestamp("reviewed_at").toLocalDateTime() : null); } catch (SQLException ignored) {}
        try { wr.setCompletedAt(rs.getTimestamp("completed_at") != null ? rs.getTimestamp("completed_at").toLocalDateTime() : null); } catch (SQLException ignored) {}
        try { wr.setInstructorName(rs.getString("instructor_name")); } catch (SQLException ignored) {}
        return wr;
    }

    private static String parseMethodDetails(String raw) {
        if (raw == null || raw.isBlank()) return null;
        if (raw.contains("\"masked\"")) {
            int start = raw.indexOf("\"masked\"");
            int colon = raw.indexOf(':', start);
            int q1 = raw.indexOf('"', colon + 1);
            int q2 = raw.indexOf('"', q1 + 1);
            if (q1 >= 0 && q2 > q1) return raw.substring(q1 + 1, q2);
        }
        return raw;
    }
}
