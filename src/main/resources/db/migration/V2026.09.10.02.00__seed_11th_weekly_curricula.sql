-- 11기 목차를 등록한다. 실제 주차 일정 확정 전까지 모든 항목에 기수 전체 기간을 사용한다.
DO $$
DECLARE
    target_gisu_id BIGINT;
BEGIN
    -- 앞선 마이그레이션이 만든 11기와 Track 커리큘럼을 재사용한다.
    SELECT id INTO STRICT target_gisu_id
    FROM public.gisu
    WHERE generation = 11 AND learning_type = 'TRACK';

    IF (
        SELECT COUNT(*) FROM public.curriculum
        WHERE gisu_id = target_gisu_id
            AND track IN ('PLAN', 'DESIGN', 'WEB_PRODUCT_ENGINEER', 'MOBILE_PRODUCT_ENGINEER')
    ) <> 4 THEN
        RAISE EXCEPTION '11기의 기본 Track 커리큘럼 4개가 필요합니다.';
    END IF;

    -- weekly_curriculum의 날짜는 TIMESTAMP WITHOUT TIME ZONE이므로 UTC 시각을 명시한다.
    -- Chapter 0은 0주차, Design Appendix 1·2는 같은 번호의 본문과 is_extra로 구분한다.
    INSERT INTO public.weekly_curriculum (
        curriculum_id, week_no, is_extra, title, starts_at, ends_at, created_at, updated_at
    )
    SELECT curriculum.id, seed.week_no, seed.is_extra, seed.title,
        gisu.start_at AT TIME ZONE 'UTC', gisu.end_at AT TIME ZONE 'UTC',
        CURRENT_TIMESTAMP AT TIME ZONE 'UTC', CURRENT_TIMESTAMP AT TIME ZONE 'UTC'
    FROM (VALUES
        ('PLAN', 0, FALSE, '서비스 기획 입문'),
        ('PLAN', 1, FALSE, '문제 정의와 리서치 (1)'),
        ('PLAN', 2, FALSE, '문제 정의와 리서치 (2)'),
        ('PLAN', 3, FALSE, '서비스 정의와 비즈니스 모델링'),
        ('PLAN', 4, FALSE, '기획 산출물 (1) UX 설계'),
        ('PLAN', 5, FALSE, '기획 산출물 (2) 상세 기능 정의'),
        ('PLAN', 6, FALSE, '기획 산출물 (3) 기획 문서 작성'),
        ('PLAN', 7, FALSE, '기획 산출물 (4) 화면 설계'),
        ('PLAN', 8, FALSE, '프로젝트 관리 및 협업'),
        ('PLAN', 9, FALSE, '서비스 품질 검증'),
        ('PLAN', 10, FALSE, '그로스 전략 설계'),
        ('DESIGN', 0, FALSE, '피그마 기초 학습'),
        ('DESIGN', 1, FALSE, 'UI 디자인 입문: 클론 디자인 App & Web'),
        ('DESIGN', 2, FALSE, '리디자인: Pain Point 분석'),
        ('DESIGN', 3, FALSE, '리디자인: Solution 탐구'),
        ('DESIGN', 4, FALSE, '와이어프레임 & 디자인 시스템 구축'),
        ('DESIGN', 5, FALSE, 'UI 디자인 진행'),
        ('DESIGN', 6, FALSE, 'UI 디자인 확장 & 포트폴리오 제작'),
        ('DESIGN', 7, FALSE, '프로토타입 제작 & 복습 가이드'),
        ('DESIGN', 8, FALSE, '매칭 프로젝트 디자인 (1)'),
        ('DESIGN', 9, FALSE, '매칭 프로젝트 디자인 (2)'),
        ('DESIGN', 10, FALSE, '매칭 프로젝트 디자인 (3)'),
        ('DESIGN', 1, TRUE, '협업 가이드'),
        ('DESIGN', 2, TRUE, '디자인 인사이트'),
        ('MOBILE_PRODUCT_ENGINEER', 1, FALSE, '데이터 모델링과 앱 UI 기초'),
        ('MOBILE_PRODUCT_ENGINEER', 2, FALSE, 'SQL 데이터 조작과 사용자 입력 폼'),
        ('MOBILE_PRODUCT_ENGINEER', 3, FALSE, '서버 환경 세팅과 앱 화면 내비게이션'),
        ('MOBILE_PRODUCT_ENGINEER', 4, FALSE, 'ORM 기반 CRUD와 비동기 UI 처리'),
        ('MOBILE_PRODUCT_ENGINEER', 5, FALSE, 'Public API 설계와 앱 아키텍처 정립'),
        ('MOBILE_PRODUCT_ENGINEER', 6, FALSE, 'CRUD API 구축과 네트워크 통신'),
        ('MOBILE_PRODUCT_ENGINEER', 7, FALSE, 'JWT 인증/인가와 사용자 토큰 관리'),
        ('MOBILE_PRODUCT_ENGINEER', 8, FALSE, '핵심 비즈니스 로직과 데이터 상태 관리'),
        ('MOBILE_PRODUCT_ENGINEER', 9, FALSE, 'API 명세 확정과 심화 기능 연동'),
        ('MOBILE_PRODUCT_ENGINEER', 10, FALSE, '클라우드 환경 분리 배포와 앱 출시'),
        ('WEB_PRODUCT_ENGINEER', 1, FALSE, '데이터 모델링과 타입 시스템 기초'),
        ('WEB_PRODUCT_ENGINEER', 2, FALSE, 'SQL 데이터 조작과 React UI 기초'),
        ('WEB_PRODUCT_ENGINEER', 3, FALSE, '서버 환경 세팅과 웹 화면 라우팅'),
        ('WEB_PRODUCT_ENGINEER', 4, FALSE, 'ORM 기반 CRUD와 클라이언트 상태 관리'),
        ('WEB_PRODUCT_ENGINEER', 5, FALSE, 'Public API 구축과 웹 API 연동'),
        ('WEB_PRODUCT_ENGINEER', 6, FALSE, 'CRUD API와 서버 상태 관리'),
        ('WEB_PRODUCT_ENGINEER', 7, FALSE, 'JWT 인증/인가와 사용자 인증 연동'),
        ('WEB_PRODUCT_ENGINEER', 8, FALSE, '핵심 비즈니스 로직과 사용자 기능 연동'),
        ('WEB_PRODUCT_ENGINEER', 9, FALSE, 'API 안정화와 Next.js 웹 개발'),
        ('WEB_PRODUCT_ENGINEER', 10, FALSE, '운영 환경 분리와 웹 서비스 배포')
    ) AS seed(track, week_no, is_extra, title)
    JOIN public.curriculum curriculum ON curriculum.gisu_id = target_gisu_id AND curriculum.track = seed.track
    JOIN public.gisu gisu ON gisu.id = target_gisu_id
    -- 이미 등록한 주차의 ID·제목·일정과 연결된 워크북은 보존한다.
    ON CONFLICT (curriculum_id, week_no, is_extra) DO NOTHING;
END $$;
