# FCM 모바일 감사 실행 순서

1. FCM 알림 요청, 대상 해석, 배치 이벤트, Firebase adapter, token 관리 코드를 확인해 검토 범위를 정했다.
2. 공통 notification/data payload와 deep link 전달 구조를 확인하고 Android/iOS별 옵션 지원 여부를 점검했다.
3. 관리자 발송 권한과 audit, 요청 validation, token 등록·비활성화, Firebase credential 처리 경계를 점검했다.
4. audience 확장 방식, 500개 단위 batching, outbound port, 공용 event outbox의 발행·재시도 구조를 점검했다.
5. Firebase 공식 문서와 구현을 대조해 모바일 처리 방식과 오류 코드 해석을 확인했다.
6. 확인한 내용에서 모바일 친화성, 보안, 운영성, 확장성의 강점과 개선 항목을 분류해 최종 보고서를 작성했다.
7. 보고서의 결론을 구현과 다시 대조하고, 실행 중 별도 서버·브라우저·컨테이너를 생성하지 않았음을 확인했다.
