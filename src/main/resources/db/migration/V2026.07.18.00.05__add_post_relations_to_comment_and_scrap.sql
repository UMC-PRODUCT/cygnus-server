DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM public.comment c
        LEFT JOIN public.post p ON p.id = c.post_id
        WHERE p.id IS NULL
    ) THEN
        RAISE EXCEPTION 'comment.post_id에 존재하지 않는 post 참조가 있습니다.';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM public.scrap s
        LEFT JOIN public.post p ON p.id = s.post_id
        WHERE p.id IS NULL
    ) THEN
        RAISE EXCEPTION 'scrap.post_id에 존재하지 않는 post 참조가 있습니다.';
    END IF;
END
$$;

CREATE INDEX idx_comment_post_id ON public.comment (post_id);

ALTER TABLE ONLY public.comment
    ADD CONSTRAINT fk_comment_post_id
        FOREIGN KEY (post_id) REFERENCES public.post (id) ON DELETE RESTRICT;

ALTER TABLE ONLY public.scrap
    ADD CONSTRAINT fk_scrap_post_id
        FOREIGN KEY (post_id) REFERENCES public.post (id) ON DELETE RESTRICT;
