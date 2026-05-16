-- Payment, Enrollment, Admin Wallet migration for STLA Desktop
-- Run after stla.sql

-- Platform commission settings
CREATE TABLE IF NOT EXISTS public.platform_settings (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  setting_key text NOT NULL UNIQUE,
  setting_value text NOT NULL,
  description text,
  updated_at timestamptz NOT NULL DEFAULT now()
);

INSERT INTO public.platform_settings (setting_key, setting_value, description)
VALUES ('commission_percent', '20', 'Platform commission percentage on course sales')
ON CONFLICT (setting_key) DO NOTHING;

-- Admin wallet (platform treasury)
CREATE TABLE IF NOT EXISTS public.admin_wallet (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  currency char(3) NOT NULL DEFAULT 'USD',
  total_revenue numeric(14,2) NOT NULL DEFAULT 0 CHECK (total_revenue >= 0),
  total_commissions numeric(14,2) NOT NULL DEFAULT 0 CHECK (total_commissions >= 0),
  pending_instructor_payouts numeric(14,2) NOT NULL DEFAULT 0 CHECK (pending_instructor_payouts >= 0),
  paid_instructor_payouts numeric(14,2) NOT NULL DEFAULT 0 CHECK (paid_instructor_payouts >= 0),
  available_balance numeric(14,2) NOT NULL DEFAULT 0 CHECK (available_balance >= 0),
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);

INSERT INTO public.admin_wallet (currency) SELECT 'USD' WHERE NOT EXISTS (SELECT 1 FROM public.admin_wallet LIMIT 1);

-- Admin wallet transactions
CREATE TABLE IF NOT EXISTS public.admin_wallet_transactions (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  admin_wallet_id uuid NOT NULL REFERENCES public.admin_wallet(id) ON DELETE CASCADE,
  payment_id uuid REFERENCES public.payments(id) ON DELETE SET NULL,
  transaction_type text NOT NULL,
  amount numeric(12,2) NOT NULL CHECK (amount >= 0),
  balance_before numeric(14,2) NOT NULL DEFAULT 0,
  balance_after numeric(14,2) NOT NULL DEFAULT 0,
  note text,
  created_at timestamptz NOT NULL DEFAULT now()
);

-- Extend payments with revenue split columns
ALTER TABLE public.payments ADD COLUMN IF NOT EXISTS payment_reference text;
ALTER TABLE public.payments ADD COLUMN IF NOT EXISTS payment_method text;
ALTER TABLE public.payments ADD COLUMN IF NOT EXISTS wallet_provider text;
ALTER TABLE public.payments ADD COLUMN IF NOT EXISTS commission_amount numeric(12,2);
ALTER TABLE public.payments ADD COLUMN IF NOT EXISTS instructor_share numeric(12,2);
ALTER TABLE public.payments ADD COLUMN IF NOT EXISTS admin_share numeric(12,2);

-- Extend wallet transaction enum values (safe add)
DO $$ BEGIN
  ALTER TYPE public.wallet_transaction_type ADD VALUE IF NOT EXISTS 'course_purchase';
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;
DO $$ BEGIN
  ALTER TYPE public.wallet_transaction_type ADD VALUE IF NOT EXISTS 'platform_commission';
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;
DO $$ BEGIN
  ALTER TYPE public.wallet_transaction_type ADD VALUE IF NOT EXISTS 'instructor_revenue';
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;
DO $$ BEGIN
  ALTER TYPE public.wallet_transaction_type ADD VALUE IF NOT EXISTS 'refund';
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;
