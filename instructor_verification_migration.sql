-- ==========================================
-- INSTRUCTOR VERIFICATION MIGRATION
-- Run this against your Supabase PostgreSQL database
-- ==========================================

ALTER TABLE instructors
  ADD COLUMN IF NOT EXISTS id_front_url text,
  ADD COLUMN IF NOT EXISTS id_back_url text,
  ADD COLUMN IF NOT EXISTS experience_certificate_url text,
  ADD COLUMN IF NOT EXISTS cv_url text,
  ADD COLUMN IF NOT EXISTS verification_status text NOT NULL DEFAULT 'PENDING',
  ADD COLUMN IF NOT EXISTS rejection_reason text,
  ADD COLUMN IF NOT EXISTS instructor_bio text;

-- Index for quickly finding pending verifications
CREATE INDEX IF NOT EXISTS idx_instructors_verification_status
  ON instructors(verification_status);
