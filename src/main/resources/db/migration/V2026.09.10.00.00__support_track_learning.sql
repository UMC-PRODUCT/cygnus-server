-- 기존 기수와 학습 이력은 Part로 유지하고, 신규 기수의 Track 학습을 별도로 연결한다.
ALTER TABLE public.gisu
    ADD COLUMN learning_type VARCHAR(255) NOT NULL DEFAULT 'PART',
    ADD CONSTRAINT gisu_learning_type_check CHECK (learning_type IN ('PART', 'TRACK'));

ALTER TABLE public.curriculum
    ALTER COLUMN part DROP NOT NULL,
    ADD COLUMN track VARCHAR(255),
    ADD CONSTRAINT curriculum_learning_selection_check
        CHECK ((part IS NOT NULL) <> (track IS NOT NULL)),
    ADD CONSTRAINT curriculum_track_check
        CHECK (track IN ('PLAN', 'DESIGN', 'WEB_PRODUCT_ENGINEER', 'MOBILE_PRODUCT_ENGINEER')),
    ADD CONSTRAINT uk_curriculum_gisu_id_track UNIQUE (gisu_id, track);

ALTER TABLE public.study_group
    ALTER COLUMN part DROP NOT NULL,
    ADD COLUMN track VARCHAR(255),
    ADD CONSTRAINT study_group_learning_selection_check
        CHECK ((part IS NOT NULL) <> (track IS NOT NULL)),
    ADD CONSTRAINT study_group_track_check
        CHECK (track IN ('PLAN', 'DESIGN', 'WEB_PRODUCT_ENGINEER', 'MOBILE_PRODUCT_ENGINEER'));

CREATE INDEX idx_study_group_gisu_id_track ON public.study_group (gisu_id, track);

-- 운영진 코드는 기존 nullable part를 담당 파트로 사용하므로 과거 행의 형태를 바꾸지 않는다.
ALTER TABLE public.challenger_record
    ADD COLUMN track VARCHAR(255),
    ADD CONSTRAINT challenger_record_track_check
        CHECK (track IN ('PLAN', 'DESIGN', 'WEB_PRODUCT_ENGINEER', 'MOBILE_PRODUCT_ENGINEER')),
    ADD CONSTRAINT challenger_record_track_registration_check
        CHECK (track IS NULL OR (
            part IS NULL AND challenger_role_type IS NULL AND organization_id IS NULL
        ));
