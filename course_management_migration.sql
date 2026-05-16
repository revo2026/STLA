-- ============================================
-- STLA Course Management Migration
-- Run this AFTER instructor_verification_migration.sql
-- ============================================

-- 1. Extend courses table
ALTER TABLE courses
  ADD COLUMN IF NOT EXISTS rejection_reason text,
  ADD COLUMN IF NOT EXISTS what_you_will_learn text,
  ADD COLUMN IF NOT EXISTS requirements text,
  ADD COLUMN IF NOT EXISTS target_audience text,
  ADD COLUMN IF NOT EXISTS has_certificate boolean DEFAULT false,
  ADD COLUMN IF NOT EXISTS has_quiz boolean DEFAULT false,
  ADD COLUMN IF NOT EXISTS has_mentor_support boolean DEFAULT false,
  ADD COLUMN IF NOT EXISTS has_resources boolean DEFAULT false;

-- 2. Course sections
CREATE TABLE IF NOT EXISTS course_sections (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  course_id uuid NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
  title text NOT NULL,
  section_order int NOT NULL DEFAULT 0,
  created_at timestamptz DEFAULT now(),
  updated_at timestamptz DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_sections_course ON course_sections(course_id);

-- 3. Link lessons to sections
ALTER TABLE course_lessons
  ADD COLUMN IF NOT EXISTS section_id uuid REFERENCES course_sections(id) ON DELETE SET NULL;

-- 4. Lesson resources
CREATE TABLE IF NOT EXISTS lesson_resources (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  lesson_id uuid NOT NULL REFERENCES course_lessons(id) ON DELETE CASCADE,
  title text NOT NULL,
  resource_url text NOT NULL,
  resource_type text DEFAULT 'pdf',
  file_size bigint DEFAULT 0,
  created_at timestamptz DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_resources_lesson ON lesson_resources(lesson_id);

-- 5. Quiz attempts
CREATE TABLE IF NOT EXISTS quiz_attempts (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  quiz_id uuid NOT NULL REFERENCES quizzes(id) ON DELETE CASCADE,
  student_id uuid NOT NULL REFERENCES students(id),
  score int DEFAULT 0,
  total_points int DEFAULT 0,
  passed boolean DEFAULT false,
  started_at timestamptz DEFAULT now(),
  completed_at timestamptz
);
CREATE INDEX IF NOT EXISTS idx_attempts_quiz ON quiz_attempts(quiz_id);
CREATE INDEX IF NOT EXISTS idx_attempts_student ON quiz_attempts(student_id);

-- 6. Quiz responses
CREATE TABLE IF NOT EXISTS quiz_responses (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  attempt_id uuid NOT NULL REFERENCES quiz_attempts(id) ON DELETE CASCADE,
  question_id uuid NOT NULL REFERENCES quiz_questions(id),
  selected_option_ids uuid[] DEFAULT '{}',
  is_correct boolean DEFAULT false
);
CREATE INDEX IF NOT EXISTS idx_responses_attempt ON quiz_responses(attempt_id);
