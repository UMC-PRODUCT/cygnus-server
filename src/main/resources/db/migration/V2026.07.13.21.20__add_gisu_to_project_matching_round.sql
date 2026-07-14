DO
$$
DECLARE
    invalid_round_id BIGINT;
BEGIN
    SELECT round.id
    INTO invalid_round_id
    FROM public.project_matching_round round
             LEFT JOIN public.chapter ON chapter.id = round.chapter_id
    WHERE chapter.id IS NULL
    ORDER BY round.id
    LIMIT 1;

    IF invalid_round_id IS NOT NULL THEN
        RAISE EXCEPTION 'PMR_GISU_AUDIT_ORPHAN_CHAPTER round_id=%', invalid_round_id
            USING ERRCODE = 'P0001';
    END IF;
END
$$;

DO
$$
DECLARE
    invalid_round_id BIGINT;
BEGIN
    SELECT round.id
    INTO invalid_round_id
    FROM public.project_matching_round round
             JOIN public.chapter ON chapter.id = round.chapter_id
    WHERE chapter.gisu_id IS NULL
    ORDER BY round.id
    LIMIT 1;

    IF invalid_round_id IS NOT NULL THEN
        RAISE EXCEPTION 'PMR_GISU_AUDIT_NULL_CHAPTER_GISU round_id=%', invalid_round_id
            USING ERRCODE = 'P0001';
    END IF;
END
$$;

ALTER TABLE public.project_matching_round
    ADD COLUMN gisu_id BIGINT;

UPDATE public.project_matching_round round
SET gisu_id = chapter.gisu_id
FROM public.chapter
WHERE chapter.id = round.chapter_id;

DO
$$
DECLARE
    invalid_round_id BIGINT;
BEGIN
    SELECT id
    INTO invalid_round_id
    FROM public.project_matching_round
    WHERE gisu_id IS NULL
    ORDER BY id
    LIMIT 1;

    IF invalid_round_id IS NOT NULL THEN
        RAISE EXCEPTION 'PMR_GISU_AUDIT_REMAINING_NULL_GISU round_id=%', invalid_round_id
            USING ERRCODE = 'P0001';
    END IF;
END
$$;

DO
$$
DECLARE
    invalid_round_id BIGINT;
BEGIN
    SELECT round.id
    INTO invalid_round_id
    FROM public.project_matching_round round
             JOIN public.gisu ON gisu.id = round.gisu_id
    WHERE (gisu.start_at <= round.starts_at
        AND round.starts_at < round.ends_at
        AND round.ends_at < round.decision_deadline
        AND round.decision_deadline < gisu.end_at) IS NOT TRUE
    ORDER BY round.id
    LIMIT 1;

    IF invalid_round_id IS NOT NULL THEN
        RAISE EXCEPTION 'PMR_GISU_AUDIT_ROUND_PERIOD round_id=%', invalid_round_id
            USING ERRCODE = 'P0001';
    END IF;
END
$$;

DO
$$
DECLARE
    invalid_application_id BIGINT;
BEGIN
    SELECT application.id
    INTO invalid_application_id
    FROM public.project_application application
             JOIN public.project_application_form form ON form.id = application.project_application_form_id
             JOIN public.project ON project.id = form.project_id
             JOIN public.project_matching_round round ON round.id = application.applied_matching_round_id
    WHERE project.gisu_id IS DISTINCT FROM round.gisu_id
       OR project.chapter_id IS DISTINCT FROM round.chapter_id
    ORDER BY application.id
    LIMIT 1;

    IF invalid_application_id IS NOT NULL THEN
        RAISE EXCEPTION 'PMR_GISU_AUDIT_APPLICATION_SCOPE application_id=%', invalid_application_id
            USING ERRCODE = 'P0001';
    END IF;
END
$$;

ALTER TABLE public.project_matching_round
    ALTER COLUMN gisu_id SET NOT NULL;

ALTER TABLE public.project_matching_round
    ADD CONSTRAINT fk_project_matching_round_on_gisu
        FOREIGN KEY (gisu_id) REFERENCES public.gisu (id) NOT VALID;

ALTER TABLE public.project_matching_round
    VALIDATE CONSTRAINT fk_project_matching_round_on_gisu;

CREATE INDEX idx_project_matching_round_gisu_chapter_starts
    ON public.project_matching_round (gisu_id, chapter_id, starts_at);

CREATE FUNCTION public.fill_project_matching_round_gisu_for_legacy_writer()
    RETURNS TRIGGER
    LANGUAGE plpgsql
    SET search_path = pg_catalog
AS
$$
DECLARE
    expected_gisu_id BIGINT;
BEGIN
    SELECT chapter.gisu_id
    INTO expected_gisu_id
    FROM public.chapter
    WHERE chapter.id = NEW.chapter_id
    FOR SHARE;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'PMR_GISU_TRIGGER_CHAPTER_NOT_FOUND chapter_id=%', NEW.chapter_id
            USING ERRCODE = 'P0001';
    END IF;

    IF expected_gisu_id IS NULL THEN
        RAISE EXCEPTION 'PMR_GISU_TRIGGER_NULL_CHAPTER_GISU chapter_id=%', NEW.chapter_id
            USING ERRCODE = 'P0001';
    END IF;

    IF NEW.gisu_id IS NULL THEN
        NEW.gisu_id := expected_gisu_id;
    ELSIF NEW.gisu_id <> expected_gisu_id THEN
        RAISE EXCEPTION
            'PMR_GISU_TRIGGER_GISU_CHAPTER_MISMATCH chapter_id=% expected_gisu_id=% actual_gisu_id=%',
            NEW.chapter_id, expected_gisu_id, NEW.gisu_id
            USING ERRCODE = 'P0001';
    END IF;

    RETURN NEW;
END
$$;

CREATE TRIGGER trg_project_matching_round_legacy_gisu
    BEFORE INSERT OR UPDATE OF chapter_id, gisu_id
    ON public.project_matching_round
    FOR EACH ROW
EXECUTE FUNCTION public.fill_project_matching_round_gisu_for_legacy_writer();

CREATE FUNCTION public.enforce_project_matching_round_period()
    RETURNS TRIGGER
    LANGUAGE plpgsql
    SET search_path = pg_catalog
AS
$$
DECLARE
    gisu_start_at TIMESTAMP WITH TIME ZONE;
    gisu_end_at TIMESTAMP WITH TIME ZONE;
BEGIN
    SELECT gisu.start_at, gisu.end_at
    INTO gisu_start_at, gisu_end_at
    FROM public.gisu
    WHERE gisu.id = NEW.gisu_id
    FOR SHARE;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'PMR_GISU_TRIGGER_GISU_NOT_FOUND gisu_id=%', NEW.gisu_id
            USING ERRCODE = 'P0001';
    END IF;

    IF (gisu_start_at <= NEW.starts_at
        AND NEW.starts_at < NEW.ends_at
        AND NEW.ends_at < NEW.decision_deadline
        AND NEW.decision_deadline < gisu_end_at) IS NOT TRUE THEN
        RAISE EXCEPTION 'PMR_GISU_TRIGGER_ROUND_PERIOD round_id=%', NEW.id
            USING ERRCODE = 'P0001';
    END IF;

    RETURN NEW;
END
$$;

CREATE TRIGGER trg_project_matching_round_period
    BEFORE INSERT OR UPDATE OF gisu_id, starts_at, ends_at, decision_deadline
    ON public.project_matching_round
    FOR EACH ROW
EXECUTE FUNCTION public.enforce_project_matching_round_period();

CREATE FUNCTION public.enforce_project_application_matching_scope()
    RETURNS TRIGGER
    LANGUAGE plpgsql
    SET search_path = pg_catalog
AS
$$
DECLARE
    project_gisu_id BIGINT;
    project_chapter_id BIGINT;
    round_gisu_id BIGINT;
    round_chapter_id BIGINT;
BEGIN
    SELECT project.gisu_id, project.chapter_id
    INTO project_gisu_id, project_chapter_id
    FROM public.project_application_form form
             JOIN public.project ON project.id = form.project_id
    WHERE form.id = NEW.project_application_form_id
    FOR SHARE OF form, project;

    IF NOT FOUND THEN
        RAISE EXCEPTION
            'PMR_GISU_TRIGGER_APPLICATION_FORM_NOT_FOUND application_form_id=%',
            NEW.project_application_form_id
            USING ERRCODE = 'P0001';
    END IF;

    SELECT round.gisu_id, round.chapter_id
    INTO round_gisu_id, round_chapter_id
    FROM public.project_matching_round round
    WHERE round.id = NEW.applied_matching_round_id
    FOR SHARE;

    IF NOT FOUND THEN
        RAISE EXCEPTION
            'PMR_GISU_TRIGGER_MATCHING_ROUND_NOT_FOUND matching_round_id=%',
            NEW.applied_matching_round_id
            USING ERRCODE = 'P0001';
    END IF;

    IF project_gisu_id IS DISTINCT FROM round_gisu_id
        OR project_chapter_id IS DISTINCT FROM round_chapter_id THEN
        RAISE EXCEPTION
            'PMR_GISU_TRIGGER_APPLICATION_SCOPE application_id=%', NEW.id
            USING ERRCODE = 'P0001';
    END IF;

    RETURN NEW;
END
$$;

CREATE TRIGGER trg_project_application_matching_scope
    BEFORE INSERT OR UPDATE OF project_application_form_id, applied_matching_round_id
    ON public.project_application
    FOR EACH ROW
EXECUTE FUNCTION public.enforce_project_application_matching_scope();

CREATE FUNCTION public.enforce_project_parent_application_scope()
    RETURNS TRIGGER
    LANGUAGE plpgsql
    SET search_path = pg_catalog
AS
$$
DECLARE
    invalid_application_id BIGINT;
BEGIN
    SELECT application.id
    INTO invalid_application_id
    FROM public.project_application_form form
             JOIN public.project_application application
                  ON application.project_application_form_id = form.id
             JOIN public.project_matching_round round
                  ON round.id = application.applied_matching_round_id
    WHERE form.project_id = NEW.id
      AND (NEW.gisu_id IS DISTINCT FROM round.gisu_id
        OR NEW.chapter_id IS DISTINCT FROM round.chapter_id)
    ORDER BY application.id
    LIMIT 1
    FOR SHARE OF round;

    IF invalid_application_id IS NOT NULL THEN
        RAISE EXCEPTION
            'PMR_GISU_TRIGGER_PROJECT_SCOPE project_id=% application_id=%',
            NEW.id, invalid_application_id
            USING ERRCODE = 'P0001';
    END IF;

    RETURN NEW;
END
$$;

CREATE TRIGGER trg_project_parent_application_scope
    BEFORE UPDATE OF gisu_id, chapter_id
    ON public.project
    FOR EACH ROW
EXECUTE FUNCTION public.enforce_project_parent_application_scope();

CREATE FUNCTION public.enforce_project_application_form_parent_scope()
    RETURNS TRIGGER
    LANGUAGE plpgsql
    SET search_path = pg_catalog
AS
$$
DECLARE
    project_gisu_id BIGINT;
    project_chapter_id BIGINT;
    invalid_application_id BIGINT;
BEGIN
    SELECT project.gisu_id, project.chapter_id
    INTO project_gisu_id, project_chapter_id
    FROM public.project
    WHERE project.id = NEW.project_id
    FOR SHARE;

    IF NOT FOUND THEN
        RAISE EXCEPTION
            'PMR_GISU_TRIGGER_PROJECT_NOT_FOUND project_id=%', NEW.project_id
            USING ERRCODE = 'P0001';
    END IF;

    SELECT application.id
    INTO invalid_application_id
    FROM public.project_application application
             JOIN public.project_matching_round round
                  ON round.id = application.applied_matching_round_id
    WHERE application.project_application_form_id = NEW.id
      AND (project_gisu_id IS DISTINCT FROM round.gisu_id
        OR project_chapter_id IS DISTINCT FROM round.chapter_id)
    ORDER BY application.id
    LIMIT 1
    FOR SHARE OF round;

    IF invalid_application_id IS NOT NULL THEN
        RAISE EXCEPTION
            'PMR_GISU_TRIGGER_APPLICATION_FORM_SCOPE application_form_id=% application_id=%',
            NEW.id, invalid_application_id
            USING ERRCODE = 'P0001';
    END IF;

    RETURN NEW;
END
$$;

CREATE TRIGGER trg_project_application_form_parent_scope
    BEFORE UPDATE OF project_id
    ON public.project_application_form
    FOR EACH ROW
EXECUTE FUNCTION public.enforce_project_application_form_parent_scope();
