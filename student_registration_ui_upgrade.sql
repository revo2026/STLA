-- ==========================================
-- STLA Student Registration UI Upgrade Migration
-- ==========================================
-- Adds skill_level, preferred_language, daily_goal_minutes to students table.
-- These fields are collected during the enhanced student registration wizard.
-- Safe to run on existing DB — uses IF NOT EXISTS / ADD COLUMN IF NOT EXISTS.

-- Add new student registration fields
ALTER TABLE public.students ADD COLUMN IF NOT EXISTS skill_level text DEFAULT 'beginner';
ALTER TABLE public.students ADD COLUMN IF NOT EXISTS preferred_language text DEFAULT 'en';
ALTER TABLE public.students ADD COLUMN IF NOT EXISTS daily_goal_minutes int DEFAULT 30;

-- Create storage buckets for avatars
SELECT storage.create_bucket('student-avatars', public := true);
SELECT storage.create_bucket('instructor-avatars', public := true);
SELECT storage.create_bucket('course-thumbnails', public := true);
SELECT storage.create_bucket('lesson-videos', public := false);
SELECT storage.create_bucket('lesson-resources', public := false);
SELECT storage.create_bucket('certificates', public := false);
