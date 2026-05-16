-- ============================================
-- STLA Storage Policies for Supabase
-- Run this in the Supabase SQL Editor
-- ============================================

-- Allow anyone to upload to student-avatars
CREATE POLICY "Allow public upload to student-avatars"
ON storage.objects FOR INSERT
TO anon, authenticated
WITH CHECK (bucket_id = 'student-avatars');

-- Allow anyone to update in student-avatars
CREATE POLICY "Allow public update in student-avatars"
ON storage.objects FOR UPDATE
TO anon, authenticated
USING (bucket_id = 'student-avatars');

-- Allow anyone to delete from student-avatars
CREATE POLICY "Allow public delete from student-avatars"
ON storage.objects FOR DELETE
TO anon, authenticated
USING (bucket_id = 'student-avatars');

-- Allow anyone to upload to instructor-avatars
CREATE POLICY "Allow public upload to instructor-avatars"
ON storage.objects FOR INSERT
TO anon, authenticated
WITH CHECK (bucket_id = 'instructor-avatars');

-- Allow anyone to update in instructor-avatars
CREATE POLICY "Allow public update in instructor-avatars"
ON storage.objects FOR UPDATE
TO anon, authenticated
USING (bucket_id = 'instructor-avatars');

-- Allow anyone to delete from instructor-avatars
CREATE POLICY "Allow public delete from instructor-avatars"
ON storage.objects FOR DELETE
TO anon, authenticated
USING (bucket_id = 'instructor-avatars');

-- Allow anyone to upload to course-thumbnails
CREATE POLICY "Allow public upload to course-thumbnails"
ON storage.objects FOR INSERT
TO anon, authenticated
WITH CHECK (bucket_id = 'course-thumbnails');

-- Allow public read on public buckets (should already exist)
CREATE POLICY "Allow public read on student-avatars"
ON storage.objects FOR SELECT
TO anon, authenticated
USING (bucket_id = 'student-avatars');

CREATE POLICY "Allow public read on instructor-avatars"
ON storage.objects FOR SELECT
TO anon, authenticated
USING (bucket_id = 'instructor-avatars');

CREATE POLICY "Allow public read on course-thumbnails"
ON storage.objects FOR SELECT
TO anon, authenticated
USING (bucket_id = 'course-thumbnails');
