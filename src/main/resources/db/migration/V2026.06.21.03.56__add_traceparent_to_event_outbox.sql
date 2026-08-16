-- 원 요청 trace와 아웃박스 relay를 span link로 연결하기 위해 발행 시점의 W3C traceparent를 보관한다.
-- 시스템/스케줄 발행 등 trace 컨텍스트가 없는 경우를 위해 nullable 로 둔다.
ALTER TABLE public.event_outbox
    ADD COLUMN traceparent varchar(64);
