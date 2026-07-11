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

DROP TRIGGER IF EXISTS sync_super_admin_system_role ON public.challenger_role;
DROP TRIGGER IF EXISTS sync_super_admin_system_role_on_challenger_delete ON public.challenger;

DROP FUNCTION IF EXISTS public.sync_super_admin_system_role_from_challenger_role();
DROP FUNCTION IF EXISTS public.sync_super_admin_system_role_before_challenger_delete();
DROP FUNCTION IF EXISTS public.reconcile_super_admin_system_role(bigint);

DELETE FROM public.challenger_role
WHERE role_type = 'SUPER_ADMIN';

ALTER TABLE public.challenger_role
    DROP CONSTRAINT IF EXISTS challenger_role_role_type_check;

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

DELETE FROM public.challenger_record
WHERE challenger_role_type = 'SUPER_ADMIN';

ALTER TABLE public.challenger_record
    DROP CONSTRAINT IF EXISTS challenger_record_challenger_role_type_check;

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
