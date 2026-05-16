package com.stla.services;

import com.stla.app.AppConfig;
import com.stla.core.database.DatabaseConnection;
import com.stla.data.repositories.*;
import com.stla.domain.models.Course;
import com.stla.domain.models.Payment;
import com.stla.patterns.facade.EnrollmentFacade;
import com.stla.patterns.observer.AppEvent;
import com.stla.patterns.observer.EventBus;
import com.stla.patterns.strategy.PaymentStrategy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class PaymentService {

    private final PaymentRepository paymentRepo = new PaymentRepository();
    private final EnrollmentRepository enrollmentRepo = new EnrollmentRepository();
    private final WalletRepository walletRepo = new WalletRepository();
    private final CourseRepositoryImpl courseRepo = new CourseRepositoryImpl();
    private final NotificationService notificationService = new NotificationService();

    public List<Payment> getAllPayments() { return paymentRepo.findAll(); }
    public List<Payment> getStudentPayments(String studentId) { return paymentRepo.findByStudentId(studentId); }
    public Optional<Payment> getPayment(String paymentId) { return paymentRepo.findById(paymentId); }

    public EnrollmentFacade.PurchaseResult processCoursePurchase(
            String studentId, String courseId, BigDecimal amount,
            PaymentStrategy strategy, String paymentMethod, String walletProvider) {

        if (!strategy.validatePaymentDetails()) {
            notifyPaymentFailure(studentId, courseId, "Invalid payment details.");
            return new EnrollmentFacade.PurchaseResult(false, "Invalid payment details.", null, null, null);
        }
        if (!strategy.processPayment(amount, studentId, courseId)) {
            notifyPaymentFailure(studentId, courseId, "Payment processing failed.");
            return new EnrollmentFacade.PurchaseResult(false, "Payment processing failed.", null, null, null);
        }

        Optional<Course> courseOpt = courseRepo.findById(courseId);
        if (courseOpt.isEmpty()) {
            return new EnrollmentFacade.PurchaseResult(false, "Course not found.", null, null, null);
        }
        Course course = courseOpt.get();

        if (enrollmentRepo.isEnrolled(studentId, courseId)) {
            return new EnrollmentFacade.PurchaseResult(false, "Already enrolled in this course.", null, null, null);
        }

        Connection conn = null;
        try {
            conn = DatabaseConnection.getInstance().getConnection();
            conn.setAutoCommit(false);

            // Must not query missing tables on this connection — failed SQL aborts the whole transaction.
            int commissionPercent = walletRepo.getCommissionPercent();
            BigDecimal commission = amount.multiply(BigDecimal.valueOf(commissionPercent))
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            BigDecimal instructorShare = amount.subtract(commission);

            Payment payment = new Payment();
            payment.setStudentId(studentId);
            payment.setCourseId(courseId);
            payment.setAmount(amount);
            payment.setCurrency("USD");
            payment.setGatewayProvider(strategy.getMethodName());
            payment.setGatewayTransactionId("TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            payment.setPaymentReference(payment.getGatewayTransactionId());
            payment.setMethodType(paymentMethod);
            payment.setWalletProvider(walletProvider);
            payment.setCommissionAmount(commission);
            payment.setInstructorShare(instructorShare);
            payment.setAdminShare(commission);

            String paymentId = paymentRepo.createPaidPayment(conn, payment);
            String enrollmentId = enrollmentRepo.createEnrollment(conn, studentId, courseId);
            paymentRepo.linkEnrollment(conn, paymentId, enrollmentId);

            try {
                walletRepo.recordCoursePurchaseRevenue(conn, paymentId, amount, commission, instructorShare);
            } catch (SQLException adminWalletEx) {
                if (!isMissingSchema(adminWalletEx)) throw adminWalletEx;
                System.err.println("Admin wallet skipped (run payment_enrollment_wallet_migration.sql): "
                        + adminWalletEx.getMessage());
            }
            walletRepo.creditInstructorRevenue(conn, course.getInstructorId(), instructorShare, paymentId, courseId,
                    "Revenue from course: " + course.getTitle());

            conn.commit();

            payment.setId(paymentId);
            payment.setEnrollmentId(enrollmentId);
            payment.setCourseName(course.getTitle());

            publishEvents(studentId, course, payment, instructorShare);

            return new EnrollmentFacade.PurchaseResult(true, "Enrollment successful!",
                    paymentId, enrollmentId, payment);
        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { System.err.println("Rollback error: " + ex.getMessage()); }
            }
            logSqlError("Purchase transaction error", e);
            String msg = rootSqlMessage(e);
            notifyPaymentFailure(studentId, courseId, msg);
            return new EnrollmentFacade.PurchaseResult(false, "Transaction failed: " + msg, null, null, null);
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ignored) {}
            }
        }
    }

    private void notifyPaymentFailure(String studentId, String courseId, String reason) {
        String title = courseRepo.findById(courseId).map(Course::getTitle).orElse("course");
        notificationService.notifyPaymentFailed(studentId, title, reason);
    }

    private void publishEvents(String studentId, Course course, Payment payment, BigDecimal instructorShare) {
        EventBus.getInstance().publish(new AppEvent(
                AppEvent.EventType.PAYMENT_COMPLETED, studentId, payment.getId(),
                "Your payment of $" + payment.getAmount() + " for \"" + course.getTitle() + "\" was successful."));
        EventBus.getInstance().publish(new AppEvent(
                AppEvent.EventType.ENROLLMENT_CREATED, studentId, course.getId(),
                "You are now enrolled in \"" + course.getTitle() + "\". Start learning!"));
        EventBus.getInstance().publish(new AppEvent(
                AppEvent.EventType.INSTRUCTOR_REVENUE_ADDED, course.getInstructorId(), course.getId(),
                "$" + instructorShare + " was added to your wallet from \"" + course.getTitle() + "\"."));
        EventBus.getInstance().publish(new AppEvent(
                AppEvent.EventType.ADMIN_COMMISSION_ADDED, null, payment.getId(),
                "Platform commission of $" + payment.getCommissionAmount() + " from \"" + course.getTitle() + "\"."));
    }

    public int getDefaultCommissionPercent() {
        return AppConfig.getInstance().getPlatformCommissionPercent();
    }

    private static boolean isMissingSchema(SQLException e) {
        String state = e.getSQLState();
        String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
        return "42P01".equals(state) || "42703".equals(state)
                || msg.contains("does not exist") || msg.contains("undefined_table")
                || msg.contains("undefined_column");
    }

    private static String rootSqlMessage(SQLException e) {
        SQLException cur = e;
        while (cur.getNextException() != null) cur = cur.getNextException();
        return cur.getMessage() != null ? cur.getMessage() : e.getMessage();
    }

    private static void logSqlError(String prefix, SQLException e) {
        System.err.println(prefix + ": " + e.getMessage());
        SQLException next = e.getNextException();
        while (next != null) {
            System.err.println("  caused by: " + next.getMessage());
            next = next.getNextException();
        }
    }
}
