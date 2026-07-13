-- 동기화 객체를 제거하기 전에 남아 있는 legacy SUPER_ADMIN을 member 소유 시스템 역할로 최종 이관한다.
INSERT INTO public.member_system_role (created_at, updated_at, member_id, role_type)
SELECT DISTINCT
    now(),
    now(),
    c.member_id,
    'SUPER_ADMIN'
FROM public.challenger_role cr
JOIN public.challenger c ON c.id = cr.challenger_id
JOIN public.member m ON m.id = c.member_id
WHERE cr.role_type = 'SUPER_ADMIN'
ON CONFLICT (member_id, role_type) DO NOTHING;

-- challenger_role 변경을 member_system_role에 반영하던 rolling-deployment trigger를 제거한다.
DROP TRIGGER IF EXISTS sync_super_admin_system_role ON public.challenger_role;

-- challenger 삭제를 member_system_role에 반영하던 rolling-deployment trigger를 제거한다.
DROP TRIGGER IF EXISTS sync_super_admin_system_role_on_challenger_delete ON public.challenger;

-- challenger_role 변경 trigger가 사용하던 동기화 함수를 제거한다.
DROP FUNCTION IF EXISTS public.sync_super_admin_system_role_from_challenger_role();

-- challenger 삭제 trigger가 사용하던 동기화 함수를 제거한다.
DROP FUNCTION IF EXISTS public.sync_super_admin_system_role_before_challenger_delete();

-- legacy 역할 존재 여부로 member_system_role을 조정하던 공통 함수를 제거한다.
DROP FUNCTION IF EXISTS public.reconcile_super_admin_system_role(bigint);

-- 최종 이관이 끝난 SUPER_ADMIN을 기수별 ChallengerRole 저장소에서 제거한다.
DELETE FROM public.challenger_role
WHERE role_type = 'SUPER_ADMIN';

-- SUPER_ADMIN을 허용하던 기존 challenger_role 역할 타입 제약을 제거한다.
ALTER TABLE public.challenger_role
    DROP CONSTRAINT IF EXISTS challenger_role_role_type_check;

-- challenger_role에는 기수별 운영진 역할만 저장할 수 있도록 제약을 다시 생성한다.
ALTER TABLE public.challenger_role
    ADD CONSTRAINT challenger_role_role_type_check CHECK (
        (role_type)::text = ANY (ARRAY[
            ('CENTRAL_PRESIDENT'::character varying)::text,
            ('CENTRAL_VICE_PRESIDENT'::character varying)::text,
            ('CENTRAL_OPERATING_TEAM_MEMBER'::character varying)::text,
            ('CENTRAL_EDUCATION_TEAM_MEMBER'::character varying)::text,
            ('CHAPTER_PRESIDENT'::character varying)::text,
            ('SCHOOL_PRESIDENT'::character varying)::text,
            ('SCHOOL_VICE_PRESIDENT'::character varying)::text,
            ('SCHOOL_PART_LEADER'::character varying)::text,
            ('SCHOOL_ETC_ADMIN'::character varying)::text
        ])
    );

-- 이력 데이터에서도 member 소유 시스템 역할과 중복되는 legacy SUPER_ADMIN을 제거한다.
DELETE FROM public.challenger_record
WHERE challenger_role_type = 'SUPER_ADMIN';

-- SUPER_ADMIN을 허용하던 기존 challenger_record 역할 타입 제약을 제거한다.
ALTER TABLE public.challenger_record
    DROP CONSTRAINT IF EXISTS challenger_record_challenger_role_type_check;

-- challenger_record에는 기수별 운영진 역할만 기록할 수 있도록 제약을 다시 생성한다.
ALTER TABLE public.challenger_record
    ADD CONSTRAINT challenger_record_challenger_role_type_check CHECK (
        (challenger_role_type)::text = ANY (ARRAY[
            ('CENTRAL_PRESIDENT'::character varying)::text,
            ('CENTRAL_VICE_PRESIDENT'::character varying)::text,
            ('CENTRAL_OPERATING_TEAM_MEMBER'::character varying)::text,
            ('CENTRAL_EDUCATION_TEAM_MEMBER'::character varying)::text,
            ('CHAPTER_PRESIDENT'::character varying)::text,
            ('SCHOOL_PRESIDENT'::character varying)::text,
            ('SCHOOL_VICE_PRESIDENT'::character varying)::text,
            ('SCHOOL_PART_LEADER'::character varying)::text,
            ('SCHOOL_ETC_ADMIN'::character varying)::text
        ])
    );
