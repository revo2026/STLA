-- ==========================================
-- ADVANCED QUIZ BUILDER MIGRATION
-- Extends quiz system with 9 question types
-- ==========================================

-- 1. Add new values to quiz_question_type enum
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_enum WHERE enumlabel = 'short_answer' AND enumtypid = 'public.quiz_question_type'::regtype) THEN
        ALTER TYPE public.quiz_question_type ADD VALUE 'short_answer';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_enum WHERE enumlabel = 'essay' AND enumtypid = 'public.quiz_question_type'::regtype) THEN
        ALTER TYPE public.quiz_question_type ADD VALUE 'essay';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_enum WHERE enumlabel = 'fill_blank' AND enumtypid = 'public.quiz_question_type'::regtype) THEN
        ALTER TYPE public.quiz_question_type ADD VALUE 'fill_blank';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_enum WHERE enumlabel = 'matching' AND enumtypid = 'public.quiz_question_type'::regtype) THEN
        ALTER TYPE public.quiz_question_type ADD VALUE 'matching';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_enum WHERE enumlabel = 'ordering' AND enumtypid = 'public.quiz_question_type'::regtype) THEN
        ALTER TYPE public.quiz_question_type ADD VALUE 'ordering';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_enum WHERE enumlabel = 'image_question' AND enumtypid = 'public.quiz_question_type'::regtype) THEN
        ALTER TYPE public.quiz_question_type ADD VALUE 'image_question';
    END IF;
END $$;

-- 2. Extend quiz_questions table with new columns
ALTER TABLE public.quiz_questions
    ADD COLUMN IF NOT EXISTS description TEXT,
    ADD COLUMN IF NOT EXISTS question_image_url TEXT,
    ADD COLUMN IF NOT EXISTS difficulty TEXT NOT NULL DEFAULT 'medium',
    ADD COLUMN IF NOT EXISTS is_required BOOLEAN NOT NULL DEFAULT true,
    ADD COLUMN IF NOT EXISTS max_word_count INT,
    ADD COLUMN IF NOT EXISTS model_answer TEXT,
    ADD COLUMN IF NOT EXISTS manual_grading BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS blank_template TEXT;

-- 3. Drop unique constraint on (quiz_id, question_order) to allow easier reordering
ALTER TABLE public.quiz_questions DROP CONSTRAINT IF EXISTS quiz_questions_quiz_id_question_order_key;

-- 4. Create question_accepted_answers table (for short_answer and fill_blank)
CREATE TABLE IF NOT EXISTS public.question_accepted_answers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    question_id UUID NOT NULL REFERENCES public.quiz_questions(id) ON DELETE CASCADE,
    answer_text TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 5. Create question_match_pairs table (for matching questions)
CREATE TABLE IF NOT EXISTS public.question_match_pairs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    question_id UUID NOT NULL REFERENCES public.quiz_questions(id) ON DELETE CASCADE,
    left_item TEXT NOT NULL,
    right_item TEXT NOT NULL,
    pair_order INT NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 6. Create question_sequence_items table (for ordering questions)
CREATE TABLE IF NOT EXISTS public.question_sequence_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    question_id UUID NOT NULL REFERENCES public.quiz_questions(id) ON DELETE CASCADE,
    item_text TEXT NOT NULL,
    correct_position INT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 7. Extend quiz_answers for text-based answers
ALTER TABLE public.quiz_answers
    ADD COLUMN IF NOT EXISTS answer_text TEXT,
    ADD COLUMN IF NOT EXISTS matching_answers JSONB,
    ADD COLUMN IF NOT EXISTS ordering_answer UUID[];

-- 8. Indexes
CREATE INDEX IF NOT EXISTS idx_accepted_answers_question ON public.question_accepted_answers(question_id);
CREATE INDEX IF NOT EXISTS idx_match_pairs_question ON public.question_match_pairs(question_id, pair_order);
CREATE INDEX IF NOT EXISTS idx_sequence_items_question ON public.question_sequence_items(question_id, correct_position);

-- 9. Updated_at triggers for new tables
CREATE TRIGGER trg_accepted_answers_updated_at BEFORE UPDATE ON public.question_accepted_answers
    FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();
CREATE TRIGGER trg_match_pairs_updated_at BEFORE UPDATE ON public.question_match_pairs
    FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();
CREATE TRIGGER trg_sequence_items_updated_at BEFORE UPDATE ON public.question_sequence_items
    FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

-- 10. RLS for new tables
ALTER TABLE public.question_accepted_answers ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.question_match_pairs ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.question_sequence_items ENABLE ROW LEVEL SECURITY;

-- Allow all for authenticated users (same pattern as quiz_questions)
CREATE POLICY accepted_answers_all ON public.question_accepted_answers FOR ALL USING (true) WITH CHECK (true);
CREATE POLICY match_pairs_all ON public.question_match_pairs FOR ALL USING (true) WITH CHECK (true);
CREATE POLICY sequence_items_all ON public.question_sequence_items FOR ALL USING (true) WITH CHECK (true);
