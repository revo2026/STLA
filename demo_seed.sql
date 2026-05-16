-- ============================================================
-- STLA Desktop — Demo Seed Data
-- Run this AFTER the main stla.sql schema has been applied.
-- Passwords are BCrypt hashed: all demo passwords = "password123"
-- ============================================================

-- BCrypt hash for "password123"
-- $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy

-- ============================================================
-- 1. PROFILES (5 users: 2 students, 2 instructors, 1 admin)
-- ============================================================
INSERT INTO profiles (id, full_name, email, password_hash, role, is_active, bio, country, avatar_url, created_at) VALUES
('11111111-aaaa-aaaa-aaaa-111111111111', 'Mohammed Al-Rashid', 'student1@stla.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'student', true, 'Passionate about programming and AI.', 'Saudi Arabia', NULL, NOW() - INTERVAL '30 days'),
('22222222-aaaa-aaaa-aaaa-222222222222', 'Sara Ahmed', 'student2@stla.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'student', true, 'Frontend developer learning new skills.', 'UAE', NULL, NOW() - INTERVAL '25 days'),
('33333333-aaaa-aaaa-aaaa-333333333333', 'Dr. Ahmed Khalil', 'instructor1@stla.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'instructor', true, 'Senior software engineer with 15+ years of experience.', 'Jordan', NULL, NOW() - INTERVAL '60 days'),
('44444444-aaaa-aaaa-aaaa-444444444444', 'Fatima Hassan', 'instructor2@stla.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'instructor', true, 'Data scientist and ML instructor.', 'Egypt', NULL, NOW() - INTERVAL '45 days'),
('55555555-aaaa-aaaa-aaaa-555555555555', 'Admin User', 'admin@stla.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'admin', true, 'Platform administrator.', 'Saudi Arabia', NULL, NOW() - INTERVAL '90 days');

-- ============================================================
-- 2. STUDENTS
-- ============================================================
INSERT INTO students (id, profile_id, headline, learning_goals, preferred_language, created_at) VALUES
('s1-student-1111-aaaa-111111111111', '11111111-aaaa-aaaa-aaaa-111111111111', 'Computer Science Student', 'Master Java and AI', 'en', NOW() - INTERVAL '30 days'),
('s2-student-2222-aaaa-222222222222', '22222222-aaaa-aaaa-aaaa-222222222222', 'Web Developer', 'Learn React and Node.js', 'en', NOW() - INTERVAL '25 days');

-- ============================================================
-- 3. INSTRUCTORS
-- ============================================================
INSERT INTO instructors (id, profile_id, expertise, qualification, years_of_experience, is_verified, created_at) VALUES
('i1-instructor-3333-aaaa-333333333333', '33333333-aaaa-aaaa-aaaa-333333333333', 'Java, Spring Boot, Microservices', 'PhD Computer Science', 15, true, NOW() - INTERVAL '60 days'),
('i2-instructor-4444-aaaa-444444444444', '44444444-aaaa-aaaa-aaaa-444444444444', 'Python, Machine Learning, Data Science', 'MSc Data Science', 10, true, NOW() - INTERVAL '45 days');

-- ============================================================
-- 4. ADMINS
-- ============================================================
INSERT INTO admins (id, profile_id, admin_level, permissions, created_at) VALUES
('a1-admin-5555-aaaa-555555555555', '55555555-aaaa-aaaa-aaaa-555555555555', 1, '{"all": true}', NOW() - INTERVAL '90 days');

-- ============================================================
-- 5. CATEGORIES (5)
-- ============================================================
INSERT INTO categories (id, name, slug, description, icon_name, is_active, created_at) VALUES
('cat-0001-aaaa-aaaa-aaaaaaaaaaaa', 'Programming', 'programming', 'Software development and coding courses', '💻', true, NOW() - INTERVAL '90 days'),
('cat-0002-aaaa-aaaa-aaaaaaaaaaaa', 'Data Science', 'data-science', 'Data analysis, ML, and AI courses', '📊', true, NOW() - INTERVAL '90 days'),
('cat-0003-aaaa-aaaa-aaaaaaaaaaaa', 'Web Development', 'web-development', 'Frontend and backend web technologies', '🌐', true, NOW() - INTERVAL '90 days'),
('cat-0004-aaaa-aaaa-aaaaaaaaaaaa', 'Mobile Development', 'mobile-development', 'iOS, Android, and cross-platform apps', '📱', true, NOW() - INTERVAL '90 days'),
('cat-0005-aaaa-aaaa-aaaaaaaaaaaa', 'Design', 'design', 'UI/UX and graphic design courses', '🎨', true, NOW() - INTERVAL '90 days');

-- ============================================================
-- 6. COURSES (8)
-- ============================================================
INSERT INTO courses (id, instructor_id, category_id, title, description, price, status, level, language, duration_hours, enrollment_count, rating_avg, is_featured, created_at) VALUES
('c001-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'i1-instructor-3333-aaaa-333333333333', 'cat-0001-aaaa-aaaa-aaaaaaaaaaaa', 'Java Programming Masterclass', 'Complete Java course from zero to hero. Covers OOP, collections, streams, and advanced topics.', 49.99, 'approved', 'beginner', 'en', 40, 25, 4.7, true, NOW() - INTERVAL '50 days'),
('c002-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'i1-instructor-3333-aaaa-333333333333', 'cat-0001-aaaa-aaaa-aaaaaaaaaaaa', 'Spring Boot REST API Development', 'Build production-ready APIs with Spring Boot, JPA, and PostgreSQL.', 79.99, 'approved', 'intermediate', 'en', 30, 18, 4.5, true, NOW() - INTERVAL '40 days'),
('c003-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'i2-instructor-4444-aaaa-444444444444', 'cat-0002-aaaa-aaaa-aaaaaaaaaaaa', 'Python for Data Science', 'Learn Python, NumPy, Pandas, and Matplotlib for data analysis.', 59.99, 'approved', 'beginner', 'en', 35, 32, 4.8, true, NOW() - INTERVAL '45 days'),
('c004-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'i2-instructor-4444-aaaa-444444444444', 'cat-0002-aaaa-aaaa-aaaaaaaaaaaa', 'Machine Learning A-Z', 'Deep dive into ML algorithms, neural networks, and real-world projects.', 99.99, 'approved', 'advanced', 'en', 60, 15, 4.6, true, NOW() - INTERVAL '35 days'),
('c005-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'i1-instructor-3333-aaaa-333333333333', 'cat-0003-aaaa-aaaa-aaaaaaaaaaaa', 'React.js Complete Guide', 'Modern React with hooks, context, Redux, and Next.js.', 69.99, 'approved', 'intermediate', 'en', 25, 22, 4.4, false, NOW() - INTERVAL '30 days'),
('c006-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'i2-instructor-4444-aaaa-444444444444', 'cat-0004-aaaa-aaaa-aaaaaaaaaaaa', 'Flutter Mobile Development', 'Build beautiful cross-platform apps with Flutter and Dart.', 89.99, 'approved', 'intermediate', 'en', 45, 12, 4.3, false, NOW() - INTERVAL '20 days'),
('c007-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'i1-instructor-3333-aaaa-333333333333', 'cat-0005-aaaa-aaaa-aaaaaaaaaaaa', 'UI/UX Design Fundamentals', 'Design thinking, wireframing, prototyping with Figma.', 39.99, 'pending', 'beginner', 'en', 20, 0, NULL, false, NOW() - INTERVAL '5 days'),
('c008-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'i2-instructor-4444-aaaa-444444444444', 'cat-0003-aaaa-aaaa-aaaaaaaaaaaa', 'Node.js Backend Development', 'Express, MongoDB, JWT auth, and deployment.', 54.99, 'draft', 'beginner', 'en', 22, 0, NULL, false, NOW() - INTERVAL '2 days');

-- ============================================================
-- 7. COURSE LESSONS
-- ============================================================
INSERT INTO course_lessons (id, course_id, title, content_type, content_url, duration_minutes, sort_order, is_free_preview, created_at) VALUES
-- Java Masterclass lessons
('l001-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'c001-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'Introduction to Java', 'video', 'https://example.com/java-intro', 45, 1, true, NOW()),
('l002-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'c001-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'Variables and Data Types', 'video', 'https://example.com/java-vars', 30, 2, false, NOW()),
('l003-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'c001-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'OOP Concepts', 'video', 'https://example.com/java-oop', 60, 3, false, NOW()),
('l004-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'c001-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'Collections Framework', 'video', 'https://example.com/java-coll', 50, 4, false, NOW()),
-- Python Data Science lessons
('l005-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'c003-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'Python Basics', 'video', 'https://example.com/python-basics', 40, 1, true, NOW()),
('l006-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'c003-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'NumPy Arrays', 'video', 'https://example.com/numpy', 35, 2, false, NOW()),
('l007-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'c003-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'Pandas DataFrames', 'video', 'https://example.com/pandas', 50, 3, false, NOW()),
-- React lessons
('l008-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'c005-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'React Fundamentals', 'video', 'https://example.com/react-fund', 45, 1, true, NOW()),
('l009-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'c005-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'Hooks Deep Dive', 'video', 'https://example.com/react-hooks', 55, 2, false, NOW());

-- ============================================================
-- 8. ENROLLMENTS
-- ============================================================
INSERT INTO enrollments (id, student_id, course_id, status, progress_percent, enrolled_at) VALUES
('e001-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 's1-student-1111-aaaa-111111111111', 'c001-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'active', 65, NOW() - INTERVAL '20 days'),
('e002-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 's1-student-1111-aaaa-111111111111', 'c003-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'active', 30, NOW() - INTERVAL '15 days'),
('e003-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 's1-student-1111-aaaa-111111111111', 'c005-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'completed', 100, NOW() - INTERVAL '10 days'),
('e004-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 's2-student-2222-aaaa-222222222222', 'c001-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'active', 40, NOW() - INTERVAL '18 days'),
('e005-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 's2-student-2222-aaaa-222222222222', 'c004-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'active', 10, NOW() - INTERVAL '5 days');

-- ============================================================
-- 9. PAYMENTS
-- ============================================================
INSERT INTO payments (id, student_id, course_id, amount, status, gateway_provider, transaction_id, paid_at) VALUES
('p001-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 's1-student-1111-aaaa-111111111111', 'c001-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 49.99, 'paid', 'Visa', 'txn_visa_001', NOW() - INTERVAL '20 days'),
('p002-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 's1-student-1111-aaaa-111111111111', 'c003-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 59.99, 'paid', 'PayPal', 'txn_pp_002', NOW() - INTERVAL '15 days'),
('p003-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 's1-student-1111-aaaa-111111111111', 'c005-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 69.99, 'paid', 'Visa', 'txn_visa_003', NOW() - INTERVAL '10 days'),
('p004-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 's2-student-2222-aaaa-222222222222', 'c001-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 49.99, 'paid', 'Wallet', 'txn_wallet_004', NOW() - INTERVAL '18 days'),
('p005-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 's2-student-2222-aaaa-222222222222', 'c004-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 99.99, 'paid', 'Visa', 'txn_visa_005', NOW() - INTERVAL '5 days');

-- ============================================================
-- 10. NOTIFICATIONS
-- ============================================================
INSERT INTO notifications (id, recipient_id, title, body, is_read, created_at) VALUES
('n001-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '11111111-aaaa-aaaa-aaaa-111111111111', 'Enrollment Successful', 'You have been enrolled in Java Programming Masterclass.', true, NOW() - INTERVAL '20 days'),
('n002-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '11111111-aaaa-aaaa-aaaa-111111111111', 'New Lesson Available', 'A new lesson has been added to Python for Data Science.', false, NOW() - INTERVAL '3 days'),
('n003-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '22222222-aaaa-aaaa-aaaa-222222222222', 'Welcome!', 'Welcome to STLA Learning Platform.', true, NOW() - INTERVAL '25 days'),
('n004-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '33333333-aaaa-aaaa-aaaa-333333333333', 'Course Approved', 'Your course "Java Programming Masterclass" has been approved.', true, NOW() - INTERVAL '48 days'),
('n005-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '33333333-aaaa-aaaa-aaaa-333333333333', 'New Enrollment', 'Sara Ahmed enrolled in Spring Boot REST API Development.', false, NOW() - INTERVAL '1 day');

-- ============================================================
-- 11. REVIEWS
-- ============================================================
INSERT INTO reviews (id, student_id, course_id, rating, comment, created_at) VALUES
('r001-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 's1-student-1111-aaaa-111111111111', 'c001-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 5, 'Excellent course! Very well structured and comprehensive.', NOW() - INTERVAL '8 days'),
('r002-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 's1-student-1111-aaaa-111111111111', 'c005-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 4, 'Great content but could use more practice exercises.', NOW() - INTERVAL '5 days'),
('r003-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 's2-student-2222-aaaa-222222222222', 'c001-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 5, 'Best Java course I have ever taken!', NOW() - INTERVAL '12 days');

-- ============================================================
-- 12. ISSUED CERTIFICATES
-- ============================================================
INSERT INTO issued_certificates (id, student_id, course_id, certificate_no, issued_at) VALUES
('cert-001-aaaa-aaaa-aaaaaaaaaaaa', 's1-student-1111-aaaa-111111111111', 'c005-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'STLA-CERT-2024-0001', NOW() - INTERVAL '10 days');

-- ============================================================
-- 13. INSTRUCTOR WALLETS
-- ============================================================
INSERT INTO instructor_wallets (id, instructor_id, available_balance, pending_balance, total_earned, total_withdrawn, updated_at) VALUES
('w001-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'i1-instructor-3333-aaaa-333333333333', 350.00, 69.99, 419.99, 0.00, NOW()),
('w002-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'i2-instructor-4444-aaaa-444444444444', 280.00, 99.99, 379.99, 0.00, NOW());

-- ============================================================
-- 14. WITHDRAWAL REQUESTS
-- ============================================================
INSERT INTO withdrawal_requests (id, instructor_id, amount, method, status, created_at) VALUES
('wd01-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'i1-instructor-3333-aaaa-333333333333', 200.00, 'Bank Transfer', 'pending', NOW() - INTERVAL '2 days');

-- ============================================================
-- 15. ACTIVITY LOGS
-- ============================================================
INSERT INTO activity_logs (id, actor_profile_id, action, target_table, target_id, details, created_at) VALUES
('al01-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '55555555-aaaa-aaaa-aaaa-555555555555', 'Approved course', 'courses', 'c001-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'Approved Java Programming Masterclass', NOW() - INTERVAL '48 days'),
('al02-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '55555555-aaaa-aaaa-aaaa-555555555555', 'Approved course', 'courses', 'c003-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'Approved Python for Data Science', NOW() - INTERVAL '43 days'),
('al03-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '11111111-aaaa-aaaa-aaaa-111111111111', 'Enrolled in course', 'enrollments', 'e001-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'Enrolled in Java Programming Masterclass', NOW() - INTERVAL '20 days'),
('al04-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '55555555-aaaa-aaaa-aaaa-555555555555', 'User registered', 'profiles', '22222222-aaaa-aaaa-aaaa-222222222222', 'New student Sara Ahmed registered', NOW() - INTERVAL '25 days');

-- ============================================================
-- DEMO DATA SUMMARY
-- ============================================================
-- 5 Profiles: 2 students, 2 instructors, 1 admin
-- 5 Categories: Programming, Data Science, Web Dev, Mobile Dev, Design
-- 8 Courses: 6 approved, 1 pending, 1 draft
-- 9 Lessons across 3 courses
-- 5 Enrollments
-- 5 Payments (all completed)
-- 5 Notifications (mix of read/unread)
-- 3 Reviews
-- 1 Certificate issued
-- 2 Instructor wallets
-- 1 Withdrawal request (pending)
-- 4 Activity logs
--
-- Login credentials:
--   student1@stla.com / password123
--   student2@stla.com / password123
--   instructor1@stla.com / password123
--   instructor2@stla.com / password123
--   admin@stla.com / password123
