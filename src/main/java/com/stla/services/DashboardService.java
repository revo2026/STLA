package com.stla.services;

import com.stla.data.repositories.*;
import com.stla.domain.models.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * Service for dashboard statistics and user management operations.
 */
public class DashboardService {

    private final ProfileRepositoryImpl profileRepo = new ProfileRepositoryImpl();
    private final CourseRepositoryImpl courseRepo = new CourseRepositoryImpl();
    private final CategoryRepositoryImpl categoryRepo = new CategoryRepositoryImpl();

    public int getTotalUsers() { return profileRepo.countAll(); }
    public int getTotalStudents() { return profileRepo.countByRole("student"); }
    public int getTotalInstructors() { return profileRepo.countByRole("instructor"); }
    public int getTotalAdmins() { return profileRepo.countByRole("admin"); }
    public int getTotalCourses() { return courseRepo.countAll(); }
    public int getPendingCourses() { return courseRepo.countByStatus("pending"); }

    public List<Course> getFeaturedCourses() { return courseRepo.findFeatured(); }
    public List<Course> getApprovedCourses() { return courseRepo.findApproved(); }
    public List<Course> getInstructorCourses(String instructorId) { return courseRepo.findByInstructorId(instructorId); }
    public List<Course> searchCourses(String keyword) { return courseRepo.searchByTitle(keyword); }

    public List<Category> getActiveCategories() { return categoryRepo.findActive(); }
    public List<Category> getAllCategories() { return categoryRepo.findAll(); }

    public List<Profile> getAllUsers() { return profileRepo.findAll(); }

    public List<Profile> getRecentUsers(int limit) {
        List<Profile> all = profileRepo.findAll();
        return all.subList(0, Math.min(limit, all.size()));
    }

    public List<Profile> getUsersByRole(String role) { return profileRepo.findByRole(role); }

    /** Gross paid payment total. */
    public BigDecimal getPlatformRevenue() {
        return queryDecimal("SELECT COALESCE(SUM(amount), 0) FROM payments WHERE status = 'paid'");
    }

    public int getPaidPaymentsCount() {
        return queryInt("SELECT COUNT(*) FROM payments WHERE status = 'paid'");
    }

    /** Enrollments grouped by calendar month for the last N months. */
    public ChartSeries getEnrollmentsByMonth(int months) {
        String sql = """
            WITH months AS (
              SELECT generate_series(
                date_trunc('month', CURRENT_DATE) - (? - 1) * INTERVAL '1 month',
                date_trunc('month', CURRENT_DATE),
                INTERVAL '1 month'
              ) AS month_start
            )
            SELECT TO_CHAR(m.month_start, 'Mon') AS label,
                   COALESCE(COUNT(e.id), 0) AS val
            FROM months m
            LEFT JOIN enrollments e ON date_trunc('month', e.enrolled_at) = m.month_start
            GROUP BY m.month_start
            ORDER BY m.month_start
            """;
        return queryChartSeries(sql, months, "val");
    }

    /** Paid revenue grouped by calendar month for the last N months. */
    public ChartSeries getMonthlyRevenue(int months) {
        String sql = """
            WITH months AS (
              SELECT generate_series(
                date_trunc('month', CURRENT_DATE) - (? - 1) * INTERVAL '1 month',
                date_trunc('month', CURRENT_DATE),
                INTERVAL '1 month'
              ) AS month_start
            )
            SELECT TO_CHAR(m.month_start, 'Mon') AS label,
                   COALESCE(SUM(p.amount), 0) AS val
            FROM months m
            LEFT JOIN payments p ON date_trunc('month', p.paid_at) = m.month_start
              AND p.status = 'paid'
            GROUP BY m.month_start
            ORDER BY m.month_start
            """;
        return queryChartSeries(sql, months, "val");
    }

    private ChartSeries queryChartSeries(String sql, int months, String valueColumn) {
        java.util.List<String> labels = new java.util.ArrayList<>();
        java.util.List<Double> values = new java.util.ArrayList<>();
        try (var conn = com.stla.core.database.DatabaseConnection.getInstance().getConnection();
             var ps = conn.prepareStatement(sql)) {
            ps.setInt(1, Math.max(1, months));
            try (var rs = ps.executeQuery()) {
                while (rs.next()) {
                    labels.add(rs.getString("label"));
                    values.add(rs.getBigDecimal(valueColumn).doubleValue());
                }
            }
        } catch (Exception e) {
            System.err.println("Chart series error: " + e.getMessage());
        }
        return new ChartSeries(
                labels.toArray(new String[0]),
                values.stream().mapToDouble(Double::doubleValue).toArray());
    }

    private int queryInt(String sql) {
        try (var conn = com.stla.core.database.DatabaseConnection.getInstance().getConnection();
             var ps = conn.prepareStatement(sql);
             var rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (Exception e) {
            System.err.println("Query error: " + e.getMessage());
        }
        return 0;
    }

    private BigDecimal queryDecimal(String sql) {
        try (var conn = com.stla.core.database.DatabaseConnection.getInstance().getConnection();
             var ps = conn.prepareStatement(sql);
             var rs = ps.executeQuery()) {
            if (rs.next()) return rs.getBigDecimal(1);
        } catch (Exception e) {
            System.err.println("Query error: " + e.getMessage());
        }
        return BigDecimal.ZERO;
    }
}
