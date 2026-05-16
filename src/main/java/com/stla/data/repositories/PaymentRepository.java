package com.stla.data.repositories;

import com.stla.core.database.DatabaseConnection;
import com.stla.domain.enums.PaymentStatus;
import com.stla.domain.models.Payment;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class PaymentRepository {

    private Connection getConn() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    public String createPaidPayment(Connection conn, Payment payment) throws SQLException {
        if (paymentsTableHasColumn("payment_reference")) {
            return insertExtended(conn, payment);
        }
        return insertBase(conn, payment);
    }

    /** Reliable check against live schema (no cached guess). */
    private boolean paymentsTableHasColumn(String columnName) {
        String sql = """
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = 'public' AND table_name = 'payments' AND column_name = ?
            LIMIT 1
            """;
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, columnName);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            return false;
        }
    }

    private String insertExtended(Connection conn, Payment payment) throws SQLException {
        String sql = """
            INSERT INTO payments (student_id, course_id, amount, currency, status, gateway_provider,
                gateway_transaction_id, paid_at, payment_reference, payment_method, wallet_provider,
                commission_amount, instructor_share, admin_share, metadata)
            VALUES (?::uuid, ?::uuid, ?, COALESCE(?, 'USD'), 'paid'::payment_status, ?, ?, now(), ?, ?, ?, ?, ?, ?, ?::jsonb)
            RETURNING id
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            bindExtended(ps, payment);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("id");
            }
        }
        throw new SQLException("Failed to create payment (extended)");
    }

    private String insertBase(Connection conn, Payment payment) throws SQLException {
        String metadata = buildMetadataJson(payment);
        String sql = """
            INSERT INTO payments (student_id, course_id, amount, currency, status, gateway_provider,
                gateway_transaction_id, paid_at, metadata)
            VALUES (?::uuid, ?::uuid, ?, COALESCE(?, 'USD'), 'paid'::payment_status, ?, ?, now(), ?::jsonb)
            RETURNING id
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, payment.getStudentId());
            ps.setString(2, payment.getCourseId());
            ps.setBigDecimal(3, payment.getAmount());
            ps.setString(4, payment.getCurrency());
            ps.setString(5, payment.getGatewayProvider());
            ps.setString(6, payment.getGatewayTransactionId() != null
                    ? payment.getGatewayTransactionId() : UUID.randomUUID().toString());
            ps.setString(7, metadata);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("id");
            }
        }
        throw new SQLException("Failed to create payment (base)");
    }

    private void bindExtended(PreparedStatement ps, Payment payment) throws SQLException {
        ps.setString(1, payment.getStudentId());
        ps.setString(2, payment.getCourseId());
        ps.setBigDecimal(3, payment.getAmount());
        ps.setString(4, payment.getCurrency());
        ps.setString(5, payment.getGatewayProvider());
        ps.setString(6, payment.getGatewayTransactionId() != null
                ? payment.getGatewayTransactionId() : UUID.randomUUID().toString());
        ps.setString(7, payment.getPaymentReference());
        ps.setString(8, payment.getMethodType());
        ps.setString(9, payment.getWalletProvider());
        ps.setBigDecimal(10, payment.getCommissionAmount());
        ps.setBigDecimal(11, payment.getInstructorShare());
        ps.setBigDecimal(12, payment.getAdminShare());
        ps.setString(13, buildMetadataJson(payment));
    }

    private String buildMetadataJson(Payment payment) {
        BigDecimal commission = payment.getCommissionAmount() != null ? payment.getCommissionAmount() : BigDecimal.ZERO;
        BigDecimal instructor = payment.getInstructorShare() != null ? payment.getInstructorShare() : BigDecimal.ZERO;
        String method = payment.getMethodType() != null ? payment.getMethodType().replace("\"", "") : "";
        String provider = payment.getWalletProvider() != null ? payment.getWalletProvider().replace("\"", "") : "";
        return String.format(
                "{\"commission_amount\":%s,\"instructor_share\":%s,\"payment_method\":\"%s\",\"wallet_provider\":\"%s\"}",
                commission, instructor, method, provider);
    }

    public void linkEnrollment(Connection conn, String paymentId, String enrollmentId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE payments SET enrollment_id=?::uuid, updated_at=now() WHERE id=?::uuid")) {
            ps.setString(1, enrollmentId);
            ps.setString(2, paymentId);
            ps.executeUpdate();
        }
    }

    public Optional<Payment> findById(String id) {
        String sql = """
            SELECT p.*, pr.full_name as student_name, c.title as course_name
            FROM payments p
            JOIN students s ON s.id = p.student_id
            JOIN profiles pr ON pr.id = s.profile_id
            JOIN courses c ON c.id = p.course_id
            WHERE p.id = ?::uuid
            """;
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapPayment(rs));
            }
        } catch (SQLException e) {
            System.err.println("Payment find error: " + e.getMessage());
        }
        return Optional.empty();
    }

    public List<Payment> findAll() {
        String sql = """
            SELECT p.*, pr.full_name as student_name, c.title as course_name
            FROM payments p
            JOIN students s ON s.id = p.student_id
            JOIN profiles pr ON pr.id = s.profile_id
            JOIN courses c ON c.id = p.course_id
            ORDER BY p.created_at DESC
            """;
        return queryList(sql);
    }

    public List<Payment> findByStudentId(String studentId) {
        String sql = """
            SELECT p.*, pr.full_name as student_name, c.title as course_name
            FROM payments p
            JOIN students s ON s.id = p.student_id
            JOIN profiles pr ON pr.id = s.profile_id
            JOIN courses c ON c.id = p.course_id
            WHERE p.student_id = ?::uuid
            ORDER BY p.created_at DESC
            """;
        List<Payment> list = new ArrayList<>();
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapPayment(rs));
            }
        } catch (SQLException e) {
            System.err.println("Student payments error: " + e.getMessage());
        }
        return list;
    }

    private List<Payment> queryList(String sql) {
        List<Payment> list = new ArrayList<>();
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapPayment(rs));
        } catch (SQLException e) {
            System.err.println("Payments query error: " + e.getMessage());
        }
        return list;
    }

    private Payment mapPayment(ResultSet rs) throws SQLException {
        Payment p = new Payment();
        p.setId(rs.getString("id"));
        p.setStudentId(rs.getString("student_id"));
        p.setCourseId(rs.getString("course_id"));
        try { p.setEnrollmentId(rs.getString("enrollment_id")); } catch (SQLException ignored) {}
        p.setAmount(rs.getBigDecimal("amount"));
        p.setCurrency(rs.getString("currency"));
        p.setStatus(PaymentStatus.fromValue(rs.getString("status")));
        p.setGatewayProvider(rs.getString("gateway_provider"));
        p.setGatewayTransactionId(rs.getString("gateway_transaction_id"));
        if (rs.getTimestamp("paid_at") != null) {
            p.setPaidAt(rs.getTimestamp("paid_at").toLocalDateTime());
        }
        if (rs.getTimestamp("created_at") != null) {
            p.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        }
        try { p.setPaymentReference(rs.getString("payment_reference")); } catch (SQLException ignored) {}
        try { p.setMethodType(rs.getString("payment_method")); } catch (SQLException ignored) {}
        try { p.setWalletProvider(rs.getString("wallet_provider")); } catch (SQLException ignored) {}
        try { p.setCommissionAmount(rs.getBigDecimal("commission_amount")); } catch (SQLException ignored) {}
        try { p.setInstructorShare(rs.getBigDecimal("instructor_share")); } catch (SQLException ignored) {}
        try { p.setAdminShare(rs.getBigDecimal("admin_share")); } catch (SQLException ignored) {}
        try { p.setStudentName(rs.getString("student_name")); } catch (SQLException ignored) {}
        try { p.setCourseName(rs.getString("course_name")); } catch (SQLException ignored) {}
        return p;
    }
}
