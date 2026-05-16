-- ==========================================
-- STLA FULL SCHEMA (CREATE) SCRIPT
-- ==========================================

create extension if not exists pgcrypto;

-- --------------------------
-- ENUMS
-- --------------------------
create type public.app_role as enum ('student', 'instructor', 'admin');
create type public.course_status as enum ('draft', 'pending', 'approved', 'rejected');
create type public.course_level as enum ('beginner', 'intermediate', 'advanced');
create type public.payment_status as enum ('pending', 'paid', 'failed', 'refunded');
create type public.payment_method_type as enum ('visa', 'digital_wallet', 'paypal');
create type public.wallet_transaction_type as enum ('earning', 'withdrawal', 'adjustment');
create type public.withdrawal_status as enum ('pending', 'approved', 'rejected', 'completed');
create type public.notification_type as enum ('enrollment', 'payment', 'course_update', 'new_lesson', 'withdrawal', 'admin_alert');
create type public.quiz_question_type as enum ('single_choice', 'multiple_choice', 'true_false');
create type public.message_status as enum ('sent', 'delivered', 'read');
create type public.resource_type as enum ('pdf', 'doc', 'zip', 'link', 'image', 'other');
create type public.enrollment_status as enum ('active', 'cancelled', 'completed');
create type public.txn_status as enum ('pending', 'completed', 'failed', 'reversed');
create type public.withdraw_method_type as enum ('visa', 'digital_wallet');

-- --------------------------
-- SHARED HELPERS
-- --------------------------
create or replace function public.set_updated_at()
returns trigger
language plpgsql
as $$
begin
  new.updated_at = now();
  return new;
end;
$$;

create or replace function public.current_user_role()
returns public.app_role
language sql
stable
security definer
set search_path = public
as $$
  select p.role
  from public.profiles p
  where p.id = auth.uid()
  limit 1;
$$;

create or replace function public.is_admin()
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select exists (
    select 1
    from public.profiles p
    join public.admins a on a.profile_id = p.id
    where p.id = auth.uid() and p.role = 'admin'
  );
$$;

create or replace function public.is_course_instructor(p_course_id uuid)
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select exists (
    select 1
    from public.courses c
    join public.instructors i on i.id = c.instructor_id
    where c.id = p_course_id and i.profile_id = auth.uid()
  );
$$;

create or replace function public.is_enrolled_in_course(p_course_id uuid)
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select exists (
    select 1
    from public.enrollments e
    join public.students s on s.id = e.student_id
    where s.profile_id = auth.uid()
      and e.course_id = p_course_id
      and e.status = 'active'
  );
$$;

-- --------------------------
-- CORE USER TABLES
-- --------------------------
create table public.profiles (
  id uuid primary key default gen_random_uuid(),
  role public.app_role not null,
  full_name text not null,
  email text not null unique,
  password_hash text not null,
  phone text,
  avatar_url text,
  bio text,
  country text,
  timezone text,
  is_active boolean not null default true,
  last_login_at timestamptz,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table public.students (
  id uuid primary key default gen_random_uuid(),
  profile_id uuid not null unique references public.profiles(id) on delete cascade,
  headline text,
  interests text[],
  learning_goals text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table public.instructors (
  id uuid primary key default gen_random_uuid(),
  profile_id uuid not null unique references public.profiles(id) on delete cascade,
  title text,
  expertise_tags text[] default '{}',
  years_experience int check (years_experience is null or years_experience >= 0),
  rating_avg numeric(3,2) not null default 0.00 check (rating_avg between 0 and 5),
  rating_count int not null default 0 check (rating_count >= 0),
  total_students int not null default 0 check (total_students >= 0),
  total_courses int not null default 0 check (total_courses >= 0),
  is_public boolean not null default true,
  is_verified boolean not null default false,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table public.admins (
  id uuid primary key default gen_random_uuid(),
  profile_id uuid not null unique references public.profiles(id) on delete cascade,
  admin_level smallint not null default 1 check (admin_level between 1 and 5),
  permissions jsonb not null default '{}'::jsonb,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

-- --------------------------
-- COURSE DOMAIN
-- --------------------------
create table public.categories (
  id uuid primary key default gen_random_uuid(),
  name text not null unique,
  slug text not null unique,
  description text,
  icon_name text,
  is_active boolean not null default true,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table public.courses (
  id uuid primary key default gen_random_uuid(),
  instructor_id uuid not null references public.instructors(id) on delete cascade,
  category_id uuid not null references public.categories(id) on delete restrict,
  title text not null,
  slug text not null unique,
  subtitle text,
  description text,
  thumbnail_url text,
  intro_video_url text,
  language text default 'en',
  level public.course_level not null default 'beginner',
  price numeric(10,2) not null default 0 check (price >= 0),
  estimated_hours int check (estimated_hours is null or estimated_hours > 0),
  status public.course_status not null default 'draft',
  approval_note text,
  approved_by_admin_id uuid references public.admins(id) on delete set null,
  approved_at timestamptz,
  published_at timestamptz,
  is_featured boolean not null default false,
  is_archived boolean not null default false,
  rating_avg numeric(3,2) not null default 0.00 check (rating_avg between 0 and 5),
  rating_count int not null default 0 check (rating_count >= 0),
  enrollment_count int not null default 0 check (enrollment_count >= 0),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table public.course_lessons (
  id uuid primary key default gen_random_uuid(),
  course_id uuid not null references public.courses(id) on delete cascade,
  title text not null,
  description text,
  lesson_order int not null check (lesson_order > 0),
  video_url text,
  duration_seconds int default 0 check (duration_seconds >= 0),
  is_preview boolean not null default false,
  is_published boolean not null default true,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  unique (course_id, lesson_order)
);

create table public.lesson_resources (
  id uuid primary key default gen_random_uuid(),
  lesson_id uuid not null references public.course_lessons(id) on delete cascade,
  title text not null,
  resource_type public.resource_type not null default 'other',
  file_url text,
  external_url text,
  file_size_bytes bigint check (file_size_bytes is null or file_size_bytes >= 0),
  is_downloadable boolean not null default true,
  resource_order int not null default 1 check (resource_order > 0),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  check (file_url is not null or external_url is not null)
);

create table public.course_addons (
  id uuid primary key default gen_random_uuid(),
  code text not null unique, -- certificate, quiz, mentor_support
  name text not null unique,
  description text,
  default_price numeric(10,2) not null default 0 check (default_price >= 0),
  is_active boolean not null default true,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table public.course_addon_map (
  id uuid primary key default gen_random_uuid(),
  course_id uuid not null references public.courses(id) on delete cascade,
  addon_id uuid not null references public.course_addons(id) on delete restrict,
  is_enabled boolean not null default true,
  custom_price numeric(10,2) check (custom_price is null or custom_price >= 0),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  unique (course_id, addon_id)
);

-- --------------------------
-- ENROLLMENT + PAYMENT
-- --------------------------
create table public.enrollments (
  id uuid primary key default gen_random_uuid(),
  student_id uuid not null references public.students(id) on delete cascade,
  course_id uuid not null references public.courses(id) on delete cascade,
  status public.enrollment_status not null default 'active',
  enrolled_at timestamptz not null default now(),
  completed_at timestamptz,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  unique (student_id, course_id)
);

create table public.payment_methods (
  id uuid primary key default gen_random_uuid(),
  student_id uuid not null references public.students(id) on delete cascade,
  method_type public.payment_method_type not null,
  provider text,                -- e.g. Visa, PayPal, wallet provider name
  masked_details text,          -- ****4242 / email
  token_ref text,               -- gateway token (never raw PAN)
  is_default boolean not null default false,
  is_active boolean not null default true,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table public.payments (
  id uuid primary key default gen_random_uuid(),
  student_id uuid not null references public.students(id) on delete restrict,
  course_id uuid not null references public.courses(id) on delete restrict,
  enrollment_id uuid references public.enrollments(id) on delete set null,
  payment_method_id uuid references public.payment_methods(id) on delete set null,
  amount numeric(10,2) not null check (amount >= 0),
  currency char(3) not null default 'USD',
  status public.payment_status not null default 'pending',
  gateway_provider text,
  gateway_transaction_id text,
  paid_at timestamptz,
  refunded_at timestamptz,
  failure_reason text,
  metadata jsonb not null default '{}'::jsonb,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

-- --------------------------
-- PROGRESS
-- --------------------------
create table public.student_course_progress (
  id uuid primary key default gen_random_uuid(),
  enrollment_id uuid not null unique references public.enrollments(id) on delete cascade,
  student_id uuid not null references public.students(id) on delete cascade,
  course_id uuid not null references public.courses(id) on delete cascade,
  progress_percent numeric(5,2) not null default 0 check (progress_percent between 0 and 100),
  lessons_completed int not null default 0 check (lessons_completed >= 0),
  total_lessons int not null default 0 check (total_lessons >= 0),
  last_lesson_id uuid references public.course_lessons(id) on delete set null,
  last_accessed_at timestamptz,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  unique (student_id, course_id)
);

create table public.lesson_progress (
  id uuid primary key default gen_random_uuid(),
  student_id uuid not null references public.students(id) on delete cascade,
  lesson_id uuid not null references public.course_lessons(id) on delete cascade,
  enrollment_id uuid references public.enrollments(id) on delete cascade,
  watched_seconds int not null default 0 check (watched_seconds >= 0),
  completed boolean not null default false,
  completed_at timestamptz,
  last_position_seconds int not null default 0 check (last_position_seconds >= 0),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  unique (student_id, lesson_id)
);

-- --------------------------
-- QUIZZES
-- --------------------------
create table public.quizzes (
  id uuid primary key default gen_random_uuid(),
  course_id uuid not null references public.courses(id) on delete cascade,
  lesson_id uuid references public.course_lessons(id) on delete set null,
  title text not null,
  description text,
  time_limit_minutes int check (time_limit_minutes is null or time_limit_minutes > 0),
  passing_score int not null default 70 check (passing_score between 0 and 100),
  attempts_allowed int not null default 3 check (attempts_allowed > 0),
  shuffle_questions boolean not null default false,
  show_answers_after_submit boolean not null default true,
  is_published boolean not null default true,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table public.quiz_questions (
  id uuid primary key default gen_random_uuid(),
  quiz_id uuid not null references public.quizzes(id) on delete cascade,
  question_text text not null,
  explanation text,
  question_type public.quiz_question_type not null default 'single_choice',
  points int not null default 1 check (points > 0),
  question_order int not null check (question_order > 0),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  unique (quiz_id, question_order)
);

create table public.quiz_options (
  id uuid primary key default gen_random_uuid(),
  question_id uuid not null references public.quiz_questions(id) on delete cascade,
  option_text text not null,
  is_correct boolean not null default false,
  option_order int not null check (option_order > 0),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  unique (question_id, option_order)
);

create table public.quiz_attempts (
  id uuid primary key default gen_random_uuid(),
  quiz_id uuid not null references public.quizzes(id) on delete cascade,
  student_id uuid not null references public.students(id) on delete cascade,
  enrollment_id uuid references public.enrollments(id) on delete set null,
  attempt_number int not null check (attempt_number > 0),
  score numeric(6,2) not null default 0,
  is_passed boolean not null default false,
  started_at timestamptz not null default now(),
  submitted_at timestamptz,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  unique (quiz_id, student_id, attempt_number)
);

create table public.quiz_answers (
  id uuid primary key default gen_random_uuid(),
  attempt_id uuid not null references public.quiz_attempts(id) on delete cascade,
  question_id uuid not null references public.quiz_questions(id) on delete cascade,
  selected_option_ids uuid[] not null default '{}',
  answer_text text,
  is_correct boolean,
  points_awarded numeric(6,2) not null default 0,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  unique (attempt_id, question_id)
);

-- --------------------------
-- CERTIFICATES
-- --------------------------
create table public.certificates (
  id uuid primary key default gen_random_uuid(),
  course_id uuid not null unique references public.courses(id) on delete cascade,
  template_name text not null,
  template_file_url text,
  issue_criteria text,
  is_active boolean not null default true,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table public.issued_certificates (
  id uuid primary key default gen_random_uuid(),
  certificate_id uuid not null references public.certificates(id) on delete restrict,
  student_id uuid not null references public.students(id) on delete cascade,
  course_id uuid not null references public.courses(id) on delete cascade,
  enrollment_id uuid references public.enrollments(id) on delete set null,
  certificate_no text not null unique,
  file_url text,
  issued_at timestamptz not null default now(),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  unique (student_id, course_id)
);

-- --------------------------
-- SOCIAL + MESSAGING + NOTIFS
-- --------------------------
create table public.instructor_followers (
  id uuid primary key default gen_random_uuid(),
  instructor_id uuid not null references public.instructors(id) on delete cascade,
  student_id uuid not null references public.students(id) on delete cascade,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  unique (instructor_id, student_id)
);

create table public.messages (
  id uuid primary key default gen_random_uuid(),
  sender_profile_id uuid not null references public.profiles(id) on delete cascade,
  receiver_profile_id uuid not null references public.profiles(id) on delete cascade,
  enrollment_id uuid references public.enrollments(id) on delete set null,
  message_body text not null,
  status public.message_status not null default 'sent',
  sent_at timestamptz not null default now(),
  read_at timestamptz,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  check (sender_profile_id <> receiver_profile_id)
);

create table public.notifications (
  id uuid primary key default gen_random_uuid(),
  recipient_profile_id uuid not null references public.profiles(id) on delete cascade,
  actor_profile_id uuid references public.profiles(id) on delete set null,
  type public.notification_type not null,
  title text not null,
  body text not null,
  reference_table text,
  reference_id uuid,
  is_read boolean not null default false,
  read_at timestamptz,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

-- --------------------------
-- INSTRUCTOR WALLET + PAYOUTS
-- --------------------------
create table public.instructor_wallets (
  id uuid primary key default gen_random_uuid(),
  instructor_id uuid not null unique references public.instructors(id) on delete cascade,
  currency char(3) not null default 'USD',
  pending_balance numeric(12,2) not null default 0 check (pending_balance >= 0),
  available_balance numeric(12,2) not null default 0 check (available_balance >= 0),
  total_earned numeric(12,2) not null default 0 check (total_earned >= 0),
  total_withdrawn numeric(12,2) not null default 0 check (total_withdrawn >= 0),
  last_settlement_at timestamptz,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table public.wallet_transactions (
  id uuid primary key default gen_random_uuid(),
  wallet_id uuid not null references public.instructor_wallets(id) on delete cascade,
  instructor_id uuid not null references public.instructors(id) on delete cascade,
  payment_id uuid references public.payments(id) on delete set null,
  withdrawal_request_id uuid,
  transaction_type public.wallet_transaction_type not null,
  status public.txn_status not null default 'completed',
  amount numeric(12,2) not null check (amount >= 0),
  direction smallint not null check (direction in (-1, 1)), -- +1 credit, -1 debit
  balance_before numeric(12,2) not null default 0,
  balance_after numeric(12,2) not null default 0,
  note text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table public.withdrawal_requests (
  id uuid primary key default gen_random_uuid(),
  instructor_id uuid not null references public.instructors(id) on delete cascade,
  wallet_id uuid not null references public.instructor_wallets(id) on delete cascade,
  amount numeric(12,2) not null check (amount > 0),
  method public.withdraw_method_type not null,
  method_details jsonb not null default '{}'::jsonb, -- masked fields only
  status public.withdrawal_status not null default 'pending',
  reviewed_by_admin_id uuid references public.admins(id) on delete set null,
  review_note text,
  requested_at timestamptz not null default now(),
  reviewed_at timestamptz,
  completed_at timestamptz,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

alter table public.wallet_transactions
  add constraint wallet_transactions_withdrawal_fk
  foreign key (withdrawal_request_id)
  references public.withdrawal_requests(id)
  on delete set null;

-- --------------------------
-- REVIEWS + ANALYTICS + LOGS
-- --------------------------
create table public.reviews (
  id uuid primary key default gen_random_uuid(),
  course_id uuid not null references public.courses(id) on delete cascade,
  student_id uuid not null references public.students(id) on delete cascade,
  enrollment_id uuid references public.enrollments(id) on delete set null,
  rating int not null check (rating between 1 and 5),
  title text,
  comment text,
  is_published boolean not null default true,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  unique (course_id, student_id)
);

create table public.course_analytics (
  id uuid primary key default gen_random_uuid(),
  course_id uuid not null references public.courses(id) on delete cascade,
  metric_date date not null default current_date,
  views_count int not null default 0 check (views_count >= 0),
  enrollments_count int not null default 0 check (enrollments_count >= 0),
  revenue_amount numeric(12,2) not null default 0 check (revenue_amount >= 0),
  avg_watch_percent numeric(5,2) not null default 0 check (avg_watch_percent between 0 and 100),
  quiz_attempts_count int not null default 0 check (quiz_attempts_count >= 0),
  completions_count int not null default 0 check (completions_count >= 0),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  unique (course_id, metric_date)
);

create table public.activity_logs (
  id uuid primary key default gen_random_uuid(),
  admin_id uuid references public.admins(id) on delete set null,
  actor_profile_id uuid references public.profiles(id) on delete set null,
  action text not null,
  target_table text,
  target_id uuid,
  details jsonb not null default '{}'::jsonb,
  ip_address inet,
  user_agent text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

-- --------------------------
-- PUBLIC INSTRUCTOR VIEW
-- --------------------------
create view public.instructor_public_profiles as
select
  i.id as instructor_id,
  i.profile_id,
  p.full_name,
  p.avatar_url,
  p.bio,
  i.title,
  i.expertise_tags,
  i.rating_avg,
  i.rating_count,
  i.total_students,
  i.total_courses,
  i.is_verified
from public.instructors i
join public.profiles p on p.id = i.profile_id
where i.is_public = true and p.is_active = true;

-- --------------------------
-- INDEXES (PERFORMANCE)
-- --------------------------
create index idx_profiles_role on public.profiles(role);
create index idx_profiles_active on public.profiles(is_active);

create index idx_students_profile_id on public.students(profile_id);
create index idx_instructors_profile_id on public.instructors(profile_id);
create index idx_instructors_public_rank on public.instructors(is_public, rating_avg desc, total_students desc, total_courses desc);

create index idx_courses_instructor on public.courses(instructor_id);
create index idx_courses_category on public.courses(category_id);
create index idx_courses_status on public.courses(status);
create index idx_courses_status_created on public.courses(status, created_at desc);
create index idx_courses_featured on public.courses(is_featured) where is_featured = true;
create index idx_courses_search_title on public.courses using gin (to_tsvector('simple', coalesce(title,'') || ' ' || coalesce(subtitle,'') || ' ' || coalesce(description,'')));

create index idx_lessons_course_order on public.course_lessons(course_id, lesson_order);
create index idx_resources_lesson on public.lesson_resources(lesson_id);

create index idx_course_addon_map_course on public.course_addon_map(course_id);
create index idx_course_addon_map_addon on public.course_addon_map(addon_id);

create index idx_enrollments_student on public.enrollments(student_id);
create index idx_enrollments_course on public.enrollments(course_id);
create index idx_enrollments_student_status on public.enrollments(student_id, status);

create index idx_payment_methods_student on public.payment_methods(student_id);
create index idx_payments_student on public.payments(student_id);
create index idx_payments_course on public.payments(course_id);
create index idx_payments_status on public.payments(status);
create index idx_payments_created_at on public.payments(created_at desc);

create index idx_scp_student_course on public.student_course_progress(student_id, course_id);
create index idx_lesson_progress_student on public.lesson_progress(student_id);
create index idx_lesson_progress_lesson on public.lesson_progress(lesson_id);

create index idx_quizzes_course on public.quizzes(course_id);
create index idx_quiz_questions_quiz_order on public.quiz_questions(quiz_id, question_order);
create index idx_quiz_options_question_order on public.quiz_options(question_id, option_order);
create index idx_quiz_attempts_student_quiz on public.quiz_attempts(student_id, quiz_id);

create index idx_issued_cert_student on public.issued_certificates(student_id);
create index idx_issued_cert_course on public.issued_certificates(course_id);

create index idx_followers_instructor on public.instructor_followers(instructor_id);
create index idx_followers_student on public.instructor_followers(student_id);

create index idx_messages_sender on public.messages(sender_profile_id, created_at desc);
create index idx_messages_receiver on public.messages(receiver_profile_id, created_at desc);

create index idx_notifications_recipient_read on public.notifications(recipient_profile_id, is_read, created_at desc);
create index idx_notifications_type on public.notifications(type);

create index idx_wallets_instructor on public.instructor_wallets(instructor_id);
create index idx_wallet_tx_wallet_created on public.wallet_transactions(wallet_id, created_at desc);
create index idx_wallet_tx_instructor_created on public.wallet_transactions(instructor_id, created_at desc);
create index idx_withdrawals_instructor_status on public.withdrawal_requests(instructor_id, status, requested_at desc);

create index idx_reviews_course on public.reviews(course_id);
create index idx_reviews_student on public.reviews(student_id);

create index idx_analytics_course_date on public.course_analytics(course_id, metric_date desc);

create index idx_activity_logs_admin_created on public.activity_logs(admin_id, created_at desc);
create index idx_activity_logs_target on public.activity_logs(target_table, target_id);

-- --------------------------
-- UPDATED_AT TRIGGERS
-- --------------------------
create trigger trg_profiles_updated_at before update on public.profiles for each row execute function public.set_updated_at();
create trigger trg_students_updated_at before update on public.students for each row execute function public.set_updated_at();
create trigger trg_instructors_updated_at before update on public.instructors for each row execute function public.set_updated_at();
create trigger trg_admins_updated_at before update on public.admins for each row execute function public.set_updated_at();
create trigger trg_categories_updated_at before update on public.categories for each row execute function public.set_updated_at();
create trigger trg_courses_updated_at before update on public.courses for each row execute function public.set_updated_at();
create trigger trg_course_lessons_updated_at before update on public.course_lessons for each row execute function public.set_updated_at();
create trigger trg_lesson_resources_updated_at before update on public.lesson_resources for each row execute function public.set_updated_at();
create trigger trg_course_addons_updated_at before update on public.course_addons for each row execute function public.set_updated_at();
create trigger trg_course_addon_map_updated_at before update on public.course_addon_map for each row execute function public.set_updated_at();
create trigger trg_enrollments_updated_at before update on public.enrollments for each row execute function public.set_updated_at();
create trigger trg_payment_methods_updated_at before update on public.payment_methods for each row execute function public.set_updated_at();
create trigger trg_payments_updated_at before update on public.payments for each row execute function public.set_updated_at();
create trigger trg_scp_updated_at before update on public.student_course_progress for each row execute function public.set_updated_at();
create trigger trg_lesson_progress_updated_at before update on public.lesson_progress for each row execute function public.set_updated_at();
create trigger trg_quizzes_updated_at before update on public.quizzes for each row execute function public.set_updated_at();
create trigger trg_quiz_questions_updated_at before update on public.quiz_questions for each row execute function public.set_updated_at();
create trigger trg_quiz_options_updated_at before update on public.quiz_options for each row execute function public.set_updated_at();
create trigger trg_quiz_attempts_updated_at before update on public.quiz_attempts for each row execute function public.set_updated_at();
create trigger trg_quiz_answers_updated_at before update on public.quiz_answers for each row execute function public.set_updated_at();
create trigger trg_certificates_updated_at before update on public.certificates for each row execute function public.set_updated_at();
create trigger trg_issued_certificates_updated_at before update on public.issued_certificates for each row execute function public.set_updated_at();
create trigger trg_instructor_followers_updated_at before update on public.instructor_followers for each row execute function public.set_updated_at();
create trigger trg_messages_updated_at before update on public.messages for each row execute function public.set_updated_at();
create trigger trg_notifications_updated_at before update on public.notifications for each row execute function public.set_updated_at();
create trigger trg_instructor_wallets_updated_at before update on public.instructor_wallets for each row execute function public.set_updated_at();
create trigger trg_wallet_transactions_updated_at before update on public.wallet_transactions for each row execute function public.set_updated_at();
create trigger trg_withdrawal_requests_updated_at before update on public.withdrawal_requests for each row execute function public.set_updated_at();
create trigger trg_reviews_updated_at before update on public.reviews for each row execute function public.set_updated_at();
create trigger trg_course_analytics_updated_at before update on public.course_analytics for each row execute function public.set_updated_at();
create trigger trg_activity_logs_updated_at before update on public.activity_logs for each row execute function public.set_updated_at();

-- --------------------------
-- REVIEW RATING REFRESH HELPER
-- --------------------------
create or replace function public.refresh_course_rating(p_course_id uuid)
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
  v_avg numeric(3,2);
  v_count int;
begin
  select coalesce(avg(r.rating)::numeric(3,2), 0), count(*)
  into v_avg, v_count
  from public.reviews r
  where r.course_id = p_course_id and r.is_published = true;

  update public.courses
  set rating_avg = v_avg,
      rating_count = v_count
  where id = p_course_id;
end;
$$;

-- --------------------------
-- RLS ENABLE + POLICIES
-- --------------------------
alter table public.profiles enable row level security;
alter table public.students enable row level security;
alter table public.instructors enable row level security;
alter table public.admins enable row level security;
alter table public.categories enable row level security;
alter table public.courses enable row level security;
alter table public.course_lessons enable row level security;
alter table public.lesson_resources enable row level security;
alter table public.course_addons enable row level security;
alter table public.course_addon_map enable row level security;
alter table public.enrollments enable row level security;
alter table public.payment_methods enable row level security;
alter table public.payments enable row level security;
alter table public.student_course_progress enable row level security;
alter table public.lesson_progress enable row level security;
alter table public.quizzes enable row level security;
alter table public.quiz_questions enable row level security;
alter table public.quiz_options enable row level security;
alter table public.quiz_attempts enable row level security;
alter table public.quiz_answers enable row level security;
alter table public.certificates enable row level security;
alter table public.issued_certificates enable row level security;
alter table public.instructor_followers enable row level security;
alter table public.messages enable row level security;
alter table public.notifications enable row level security;
alter table public.instructor_wallets enable row level security;
alter table public.wallet_transactions enable row level security;
alter table public.withdrawal_requests enable row level security;
alter table public.reviews enable row level security;
alter table public.course_analytics enable row level security;
alter table public.activity_logs enable row level security;

-- Profiles
create policy profiles_select_policy on public.profiles
for select using (
  id = auth.uid()
  or exists (
    select 1 from public.instructors i
    where i.profile_id = profiles.id and i.is_public = true
  )
  or public.is_admin()
);
create policy profiles_update_self on public.profiles
for update using (id = auth.uid() or public.is_admin())
with check (id = auth.uid() or public.is_admin());
create policy profiles_insert_self on public.profiles
for insert with check (id = auth.uid() or public.is_admin());

-- Role tables
create policy students_owner_admin_all on public.students
for all using (
  profile_id = auth.uid() or public.is_admin()
) with check (
  profile_id = auth.uid() or public.is_admin()
);

create policy instructors_read_public_owner_admin on public.instructors
for select using (
  is_public = true or profile_id = auth.uid() or public.is_admin()
);
create policy instructors_owner_admin_write on public.instructors
for all using (
  profile_id = auth.uid() or public.is_admin()
) with check (
  profile_id = auth.uid() or public.is_admin()
);

create policy admins_admin_only on public.admins
for all using (public.is_admin()) with check (public.is_admin());

-- categories/addons visible to authenticated, writable by admin
create policy categories_read_auth on public.categories for select using (auth.uid() is not null);
create policy categories_admin_write on public.categories for all using (public.is_admin()) with check (public.is_admin());

create policy course_addons_read_auth on public.course_addons for select using (auth.uid() is not null);
create policy course_addons_admin_write on public.course_addons for all using (public.is_admin()) with check (public.is_admin());

-- Courses
create policy courses_select_policy on public.courses
for select using (
  status = 'approved'
  or public.is_course_instructor(id)
  or public.is_admin()
);

create policy courses_insert_instructor on public.courses
for insert with check (
  public.is_admin()
  or exists (
    select 1 from public.instructors i
    where i.id = instructor_id and i.profile_id = auth.uid()
  )
);

create policy courses_update_instructor_admin on public.courses
for update using (
  public.is_course_instructor(id) or public.is_admin()
) with check (
  public.is_course_instructor(id) or public.is_admin()
);

-- Lessons/resources locked if not enrolled (except preview or instructor/admin)
create policy lessons_select_policy on public.course_lessons
for select using (
  is_preview = true
  or public.is_enrolled_in_course(course_id)
  or public.is_course_instructor(course_id)
  or public.is_admin()
);

create policy lessons_write_instructor_admin on public.course_lessons
for all using (
  public.is_course_instructor(course_id) or public.is_admin()
) with check (
  public.is_course_instructor(course_id) or public.is_admin()
);

create policy resources_select_policy on public.lesson_resources
for select using (
  exists (
    select 1
    from public.course_lessons l
    where l.id = lesson_resources.lesson_id
      and (
        l.is_preview = true
        or public.is_enrolled_in_course(l.course_id)
        or public.is_course_instructor(l.course_id)
        or public.is_admin()
      )
  )
);

create policy resources_write_instructor_admin on public.lesson_resources
for all using (
  exists (
    select 1 from public.course_lessons l
    where l.id = lesson_resources.lesson_id
      and (public.is_course_instructor(l.course_id) or public.is_admin())
  )
) with check (
  exists (
    select 1 from public.course_lessons l
    where l.id = lesson_resources.lesson_id
      and (public.is_course_instructor(l.course_id) or public.is_admin())
  )
);

-- course_addon_map
create policy course_addon_map_select_auth on public.course_addon_map
for select using (auth.uid() is not null);

create policy course_addon_map_write_owner_admin on public.course_addon_map
for all using (
  public.is_admin() or public.is_course_instructor(course_id)
) with check (
  public.is_admin() or public.is_course_instructor(course_id)
);

-- Enrollments
create policy enrollments_select_owner_instructor_admin on public.enrollments
for select using (
  public.is_admin()
  or exists (select 1 from public.students s where s.id = student_id and s.profile_id = auth.uid())
  or public.is_course_instructor(course_id)
);

create policy enrollments_insert_student_admin on public.enrollments
for insert with check (
  public.is_admin()
  or exists (select 1 from public.students s where s.id = student_id and s.profile_id = auth.uid())
);

create policy enrollments_update_owner_admin on public.enrollments
for update using (
  public.is_admin()
  or exists (select 1 from public.students s where s.id = student_id and s.profile_id = auth.uid())
) with check (
  public.is_admin()
  or exists (select 1 from public.students s where s.id = student_id and s.profile_id = auth.uid())
);

-- Payments + methods
create policy payment_methods_owner_admin on public.payment_methods
for all using (
  public.is_admin()
  or exists (select 1 from public.students s where s.id = student_id and s.profile_id = auth.uid())
) with check (
  public.is_admin()
  or exists (select 1 from public.students s where s.id = student_id and s.profile_id = auth.uid())
);

create policy payments_select_policy on public.payments
for select using (
  public.is_admin()
  or exists (select 1 from public.students s where s.id = student_id and s.profile_id = auth.uid())
  or public.is_course_instructor(course_id)
);

create policy payments_insert_student_admin on public.payments
for insert with check (
  public.is_admin()
  or exists (select 1 from public.students s where s.id = student_id and s.profile_id = auth.uid())
);

create policy payments_update_admin_only on public.payments
for update using (public.is_admin()) with check (public.is_admin());

-- Progress
create policy scp_owner_instructor_admin on public.student_course_progress
for all using (
  public.is_admin()
  or exists (select 1 from public.students s where s.id = student_id and s.profile_id = auth.uid())
  or public.is_course_instructor(course_id)
) with check (
  public.is_admin()
  or exists (select 1 from public.students s where s.id = student_id and s.profile_id = auth.uid())
  or public.is_course_instructor(course_id)
);

create policy lesson_progress_owner_instructor_admin on public.lesson_progress
for all using (
  public.is_admin()
  or exists (select 1 from public.students s where s.id = student_id and s.profile_id = auth.uid())
  or exists (
      select 1
      from public.course_lessons l
      where l.id = lesson_progress.lesson_id
        and public.is_course_instructor(l.course_id)
  )
) with check (
  public.is_admin()
  or exists (select 1 from public.students s where s.id = student_id and s.profile_id = auth.uid())
  or exists (
      select 1
      from public.course_lessons l
      where l.id = lesson_progress.lesson_id
        and public.is_course_instructor(l.course_id)
  )
);

-- Quizzes
create policy quizzes_select_policy on public.quizzes
for select using (
  exists (
    select 1 from public.courses c
    where c.id = quizzes.course_id
      and (c.status = 'approved' or public.is_course_instructor(c.id) or public.is_admin() or public.is_enrolled_in_course(c.id))
  )
);

create policy quizzes_write_owner_admin on public.quizzes
for all using (
  public.is_course_instructor(course_id) or public.is_admin()
) with check (
  public.is_course_instructor(course_id) or public.is_admin()
);

create policy quiz_questions_select_auth on public.quiz_questions for select using (auth.uid() is not null);
create policy quiz_questions_write_owner_admin on public.quiz_questions
for all using (
  exists (select 1 from public.quizzes q where q.id = quiz_id and (public.is_course_instructor(q.course_id) or public.is_admin()))
) with check (
  exists (select 1 from public.quizzes q where q.id = quiz_id and (public.is_course_instructor(q.course_id) or public.is_admin()))
);

create policy quiz_options_select_auth on public.quiz_options for select using (auth.uid() is not null);
create policy quiz_options_write_owner_admin on public.quiz_options
for all using (
  exists (
    select 1 from public.quiz_questions qq
    join public.quizzes q on q.id = qq.quiz_id
    where qq.id = question_id and (public.is_course_instructor(q.course_id) or public.is_admin())
  )
) with check (
  exists (
    select 1 from public.quiz_questions qq
    join public.quizzes q on q.id = qq.quiz_id
    where qq.id = question_id and (public.is_course_instructor(q.course_id) or public.is_admin())
  )
);

create policy quiz_attempts_owner_instructor_admin on public.quiz_attempts
for all using (
  public.is_admin()
  or exists (select 1 from public.students s where s.id = student_id and s.profile_id = auth.uid())
  or exists (select 1 from public.quizzes q where q.id = quiz_id and public.is_course_instructor(q.course_id))
) with check (
  public.is_admin()
  or exists (select 1 from public.students s where s.id = student_id and s.profile_id = auth.uid())
  or exists (select 1 from public.quizzes q where q.id = quiz_id and public.is_course_instructor(q.course_id))
);

create policy quiz_answers_owner_instructor_admin on public.quiz_answers
for all using (
  public.is_admin()
  or exists (
    select 1 from public.quiz_attempts qa
    join public.students s on s.id = qa.student_id
    where qa.id = attempt_id and s.profile_id = auth.uid()
  )
  or exists (
    select 1
    from public.quiz_attempts qa
    join public.quizzes q on q.id = qa.quiz_id
    where qa.id = attempt_id and public.is_course_instructor(q.course_id)
  )
) with check (
  public.is_admin()
  or exists (
    select 1 from public.quiz_attempts qa
    join public.students s on s.id = qa.student_id
    where qa.id = attempt_id and s.profile_id = auth.uid()
  )
  or exists (
    select 1
    from public.quiz_attempts qa
    join public.quizzes q on q.id = qa.quiz_id
    where qa.id = attempt_id and public.is_course_instructor(q.course_id)
  )
);

-- Certificates
create policy certificates_select_policy on public.certificates
for select using (
  exists (
    select 1 from public.courses c
    where c.id = certificates.course_id
      and (c.status = 'approved' or public.is_course_instructor(c.id) or public.is_admin())
  )
);
create policy certificates_write_owner_admin on public.certificates
for all using (
  public.is_admin() or public.is_course_instructor(course_id)
) with check (
  public.is_admin() or public.is_course_instructor(course_id)
);

create policy issued_cert_owner_instructor_admin on public.issued_certificates
for all using (
  public.is_admin()
  or exists (select 1 from public.students s where s.id = student_id and s.profile_id = auth.uid())
  or public.is_course_instructor(course_id)
) with check (
  public.is_admin()
  or exists (select 1 from public.students s where s.id = student_id and s.profile_id = auth.uid())
  or public.is_course_instructor(course_id)
);

-- Followers/messages/notifications
create policy followers_select_auth on public.instructor_followers for select using (auth.uid() is not null);
create policy followers_insert_student on public.instructor_followers
for insert with check (
  public.is_admin()
  or exists (select 1 from public.students s where s.id = student_id and s.profile_id = auth.uid())
);
create policy followers_delete_student_admin on public.instructor_followers
for delete using (
  public.is_admin()
  or exists (select 1 from public.students s where s.id = student_id and s.profile_id = auth.uid())
);

create policy messages_select_participants_admin on public.messages
for select using (
  sender_profile_id = auth.uid() or receiver_profile_id = auth.uid() or public.is_admin()
);
create policy messages_insert_sender_admin on public.messages
for insert with check (
  sender_profile_id = auth.uid() or public.is_admin()
);
create policy messages_update_participants_admin on public.messages
for update using (
  sender_profile_id = auth.uid() or receiver_profile_id = auth.uid() or public.is_admin()
) with check (
  sender_profile_id = auth.uid() or receiver_profile_id = auth.uid() or public.is_admin()
);

create policy notifications_owner_admin on public.notifications
for all using (
  recipient_profile_id = auth.uid() or public.is_admin()
) with check (
  recipient_profile_id = auth.uid() or public.is_admin()
);

-- Wallets + withdrawals
create policy wallets_owner_admin on public.instructor_wallets
for all using (
  public.is_admin()
  or exists (select 1 from public.instructors i where i.id = instructor_id and i.profile_id = auth.uid())
) with check (
  public.is_admin()
  or exists (select 1 from public.instructors i where i.id = instructor_id and i.profile_id = auth.uid())
);

create policy wallet_tx_owner_admin on public.wallet_transactions
for select using (
  public.is_admin()
  or exists (select 1 from public.instructors i where i.id = instructor_id and i.profile_id = auth.uid())
);
create policy wallet_tx_admin_write on public.wallet_transactions
for insert with check (public.is_admin());
create policy wallet_tx_admin_update on public.wallet_transactions
for update using (public.is_admin()) with check (public.is_admin());

create policy withdrawals_owner_admin on public.withdrawal_requests
for all using (
  public.is_admin()
  or exists (select 1 from public.instructors i where i.id = instructor_id and i.profile_id = auth.uid())
) with check (
  public.is_admin()
  or exists (select 1 from public.instructors i where i.id = instructor_id and i.profile_id = auth.uid())
);

-- Reviews / analytics / activity logs
create policy reviews_select_auth on public.reviews for select using (auth.uid() is not null);
create policy reviews_insert_student on public.reviews
for insert with check (
  public.is_admin()
  or exists (select 1 from public.students s where s.id = student_id and s.profile_id = auth.uid())
);
create policy reviews_update_owner_admin on public.reviews
for update using (
  public.is_admin()
  or exists (select 1 from public.students s where s.id = student_id and s.profile_id = auth.uid())
) with check (
  public.is_admin()
  or exists (select 1 from public.students s where s.id = student_id and s.profile_id = auth.uid())
);

create policy analytics_select_owner_admin on public.course_analytics
for select using (
  public.is_admin() or public.is_course_instructor(course_id)
);
create policy analytics_write_admin on public.course_analytics
for all using (public.is_admin()) with check (public.is_admin());

create policy activity_logs_admin_only on public.activity_logs
for all using (public.is_admin()) with check (public.is_admin());

-- --------------------------
-- SEED DATA (SAFE BASICS)
-- --------------------------
insert into public.categories (name, slug, description, icon_name) values
('Design', 'design', 'UI/UX and visual design courses', 'palette'),
('Development', 'development', 'Web and software development', 'code'),
('Mobile', 'mobile', 'Mobile app development', 'phone_android'),
('Data Science', 'data-science', 'Data analysis and ML courses', 'bar_chart')
on conflict (slug) do nothing;

insert into public.course_addons (code, name, description, default_price) values
('certificate', 'Certificate', 'Official completion certificate', 10),
('quiz', 'Quiz', 'Course quizzes and assessments', 0),
('mentor_support', 'Mentor Support', 'Direct mentor Q&A support', 30)
on conflict (code) do nothing;

-- Optional profile seeds require real auth.users IDs.
-- Create users in Supabase Auth first, then insert profiles/students/instructors/admins.

select storage.create_bucket('course-thumbnails', public := true);
select storage.create_bucket('lesson-videos', public := false);
select storage.create_bucket('lesson-resources', public := false);
select storage.create_bucket('instructor-avatars', public := true);
select storage.create_bucket('student-avatars', public := true);
select storage.create_bucket('certificates', public := false);

insert into storage.buckets (id, name, public)
values 
  ('course-thumbnails', 'course-thumbnails', true),
  ('lesson-videos', 'lesson-videos', false),
  ('lesson-resources', 'lesson-resources', false),
  ('instructor-avatars', 'instructor-avatars', true),
  ('student-avatars', 'student-avatars', true),
  ('certificates', 'certificates', false);