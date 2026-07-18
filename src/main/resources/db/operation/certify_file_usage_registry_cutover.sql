BEGIN;

SET LOCAL lock_timeout = '5s';

LOCK TABLE public.member, public.school, public.notice_image, public.project,
    public.answer, public.chat_message, public.umc_product_member, public.certificate,
    public.file_metadata, public.file_usage_owner, public.file_usage,
    public.file_usage_registry_cutover IN SHARE MODE;

DO $$
DECLARE
    has_mismatch BOOLEAN;
BEGIN
    WITH legacy_raw AS (
        SELECT 'member'::text AS usage_namespace,
               source.id::text AS resource_key,
               'profile-image'::text AS slot,
               source.profile_image_id::text AS file_id
        FROM public.member source
        WHERE source.profile_image_id IS NOT NULL

        UNION ALL

        SELECT 'organization.school', source.id::text, 'logo', source.logo_image_id::text
        FROM public.school source
        WHERE source.logo_image_id IS NOT NULL

        UNION ALL

        SELECT 'notice', source.notice_id::text, 'images', source.image_id::text
        FROM public.notice_image source

        UNION ALL

        SELECT 'project', source.id::text, 'logo', source.logo_file_id::text
        FROM public.project source
        WHERE source.logo_file_id IS NOT NULL

        UNION ALL

        SELECT 'project', source.id::text, 'thumbnail', source.thumbnail_file_id::text
        FROM public.project source
        WHERE source.thumbnail_file_id IS NOT NULL

        UNION ALL

        SELECT 'form.answer', source.id::text, 'attachments', attachment.file_id::text
        FROM public.answer source
        CROSS JOIN LATERAL unnest(source.file_ids) AS attachment(file_id)

        UNION ALL

        SELECT 'chat.message', source.id::text, 'attachments', attachment.file_id::text
        FROM public.chat_message source
        CROSS JOIN LATERAL unnest(source.file_metadata_ids) AS attachment(file_id)

        UNION ALL

        SELECT 'organization.umc-product-member', source.id::text,
               'profile-image', source.profile_image_id::text
        FROM public.umc_product_member source
        WHERE source.profile_image_id IS NOT NULL

        UNION ALL

        SELECT 'certificate', source.id::text, 'file', source.file_id::text
        FROM public.certificate source
    ),
    legacy_valid AS (
        SELECT usage_namespace, resource_key, slot, file_id
        FROM legacy_raw
        WHERE file_id IS NOT NULL
          AND btrim(file_id) <> ''
    ),
    expected AS (
        SELECT DISTINCT usage_namespace, resource_key, slot, file_id
        FROM legacy_valid
    ),
    actual AS (
        SELECT owner.usage_namespace, owner.resource_key, owner.slot, usage.file_id
        FROM public.file_usage usage
        JOIN public.file_usage_owner owner ON owner.id = usage.owner_id
    )
    SELECT
        EXISTS (
            SELECT 1
            FROM legacy_raw
            WHERE file_id IS NULL OR btrim(file_id) = ''
        )
        OR EXISTS (
            SELECT 1
            FROM legacy_valid
            GROUP BY usage_namespace, resource_key, slot, file_id
            HAVING COUNT(*) > 1
        )
        OR EXISTS (
            SELECT 1
            FROM legacy_valid legacy
            LEFT JOIN public.file_metadata metadata ON metadata.id = legacy.file_id
            WHERE metadata.id IS NULL OR metadata.is_uploaded = FALSE
        )
        OR EXISTS (
            SELECT usage_namespace, resource_key, slot, file_id FROM expected
            EXCEPT
            SELECT usage_namespace, resource_key, slot, file_id FROM actual
        )
        OR EXISTS (
            SELECT usage_namespace, resource_key, slot, file_id FROM actual
            EXCEPT
            SELECT usage_namespace, resource_key, slot, file_id FROM expected
        )
        OR EXISTS (
            SELECT 1
            FROM public.file_metadata metadata
            WHERE (
                EXISTS (
                    SELECT 1 FROM public.file_usage usage
                    WHERE usage.file_id = metadata.id
                )
                AND metadata.unreferenced_at IS NOT NULL
            ) OR (
                metadata.is_uploaded = TRUE
                AND NOT EXISTS (
                    SELECT 1 FROM public.file_usage usage
                    WHERE usage.file_id = metadata.id
                )
                AND metadata.unreferenced_at IS NULL
            )
        )
    INTO has_mismatch;

    IF has_mismatch THEN
        RAISE EXCEPTION
            'file usage registry cutover certification failed: legacy and registry differ';
    END IF;

    UPDATE public.file_usage_registry_cutover
    SET status = 'READY',
        verified_at = CURRENT_TIMESTAMP,
        updated_at = CURRENT_TIMESTAMP
    WHERE singleton = TRUE
      AND status = 'PENDING';

    IF NOT FOUND THEN
        RAISE EXCEPTION
            'file usage registry cutover certification failed: PENDING row not found';
    END IF;
END
$$;

COMMIT;
