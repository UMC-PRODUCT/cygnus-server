# Test Seed 테스트 케이스

- 테스트 파일: 11개
- 테스트 케이스: 58개
- 분류 기준: `Controller`, `UseCase`, `Repository`, `E2E`, `Scheduler`, `Domain`, `External Adapter`, `Support`

| 카테고리 | 케이스 수 |
|---|---:|
| Controller / Inbound Adapter | 1 |
| UseCase / Application Service | 56 |
| Repository / Outbound Persistence | 1 |

## Controller / Inbound Adapter

### SeedControllerBeanRegistrationTest
- 위치: `src/test/java/com/umc/product/test/adapter/in/web/SeedControllerBeanRegistrationTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [31](../../../src/test/java/com/umc/product/test/adapter/in/web/SeedControllerBeanRegistrationTest.java#L31) | 모든 시딩 빈은 @Profile(!prod) + @ConditionalOnProperty(app.seed.enabled=true) 가드를 가진다 | 파라미터 @ValueSource classes = { SeedController.class, MemberSeedService.class, ChallengerSeedService.class, ProjectSeedService.class, ProjectSeedDataCleanupService.class, ProjectApplicationSeedService.class, CurriculumSeedService.class, NoticeSeedService.class, DummyMemberFactory.class, DummyCurriculumFactory.class, DummyNoticeFactory.class, PartAssignmentPolicy.class, SeedProperties.class } | 성공: 검증 assertThat(profile); .isNotNull(); assertThat(profile.value()); .contains("!prod"); |

## UseCase / Application Service

### ChallengerSeedServiceTest
- 위치: `src/test/java/com/umc/product/test/application/service/ChallengerSeedServiceTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [63](../../../src/test/java/com/umc/product/test/application/service/ChallengerSeedServiceTest.java#L63) | parts 가 null 이면 ADMIN 제외 모든 파트가 대상이 된다 | 호출 seed(new SeedChallengersCommand(gisuId, 1, null, null)) | 성공: 검증 assertThat(result.perCellSummary()).hasSize((int) expectedParts); assertThat(result.perCellSummary()) |
| [85](../../../src/test/java/com/umc/product/test/application/service/ChallengerSeedServiceTest.java#L85) | chapterIds 필터가 있으면 해당 Chapter 만 시딩한다 | 조건 chapterIds 필터가 있으면 해당 Chapter 만 시딩한다 | 성공: 검증 assertThat(result.perCellSummary()); .hasSize(1); assertThat(s.chapterId()).isEqualTo(2L); assertThat(s.schoolId()).isEqualTo(201L); |
| [114](../../../src/test/java/com/umc/product/test/application/service/ChallengerSeedServiceTest.java#L114) | 한 셀에서 createChallengerBulk 가 실패해도 다른 셀 시딩은 진행된다 | 조건 한 셀에서 createChallengerBulk 가 실패해도 다른 셀 시딩은 진행된다 | 실패: 검증 assertThat(result.perCellSummary()).hasSize(2); assertThat(result.totalCreated()).isEqualTo(1); |
| [141](../../../src/test/java/com/umc/product/test/application/service/ChallengerSeedServiceTest.java#L141) | 멤버 batch 생성 실패는 memberFailed 로 보고되고 챌린저 생성은 호출하지 않는다 | 조건 멤버 batch 생성 실패는 memberFailed 로 보고되고 챌린저 생성은 호출하지 않는다 | 실패: 검증 assertThat(result.perCellSummary()).hasSize(1); assertThat(cell.created()).isZero(); assertThat(cell.memberFailed()).isEqualTo(2); assertThat(cell.challengerFailed()).isZero(); |
| [167](../../../src/test/java/com/umc/product/test/application/service/ChallengerSeedServiceTest.java#L167) | gisuId 가 null 이면 활성 기수를 사용한다 | 조건 gisuId 가 null 이면 활성 기수를 사용한다 | 성공: 검증 assertThat(result.gisuId()).isEqualTo(10L); |

### CurriculumSeedServiceTest
- 위치: `src/test/java/com/umc/product/test/application/service/CurriculumSeedServiceTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [67](../../../src/test/java/com/umc/product/test/application/service/CurriculumSeedServiceTest.java#L67) | 기본 파트 사용 시 ADMIN 제외 모든 파트에 대해 커리큘럼이 생성된다 | 호출 seed(new SeedCurriculumCommand(gisuId, 1, 0, null, null)) | 성공: 검증 assertThat(result.createdCurriculumIds()).hasSize((int) expectedPartCount); |
| [84](../../../src/test/java/com/umc/product/test/application/service/CurriculumSeedServiceTest.java#L84) | 주차 수만큼 WeeklyCurriculum 과 OriginalWorkbook 이 생성된다 | 조건 주차 수만큼 WeeklyCurriculum 과 OriginalWorkbook 이 생성된다 | 성공: 검증 assertThat(result.createdCurriculumIds()).hasSize(2); assertThat(result.createdWeeklyCurriculumIds()).hasSize(8); // 2 × 4; assertThat(result.createdOriginalWorkbookIds()).hasSize(8); |
| [102](../../../src/test/java/com/umc/product/test/application/service/CurriculumSeedServiceTest.java#L102) | missionsPerWorkbook 만큼 미션이 생성된다 | 조건 missionsPerWorkbook 만큼 미션이 생성된다 | 성공: 검증 assertThat(result.createdMissionIds()).hasSize(6); // 2 × 3 |
| [118](../../../src/test/java/com/umc/product/test/application/service/CurriculumSeedServiceTest.java#L118) | Curriculum 생성 실패는 다음 파트로 격리되어 다른 파트는 정상 시딩된다 | 조건 Curriculum 생성 실패는 다음 파트로 격리되어 다른 파트는 정상 시딩된다 | 실패: 검증 assertThat(result.curriculumFailed()).isEqualTo(1); assertThat(result.createdCurriculumIds()).hasSize(1); assertThat(result.createdWeeklyCurriculumIds()).hasSize(2); // 두 번째 파트의 2주차 |
| [145](../../../src/test/java/com/umc/product/test/application/service/CurriculumSeedServiceTest.java#L145) | releaseRequesterMemberId 가 지정되면 워크북을 RELEASED 로 전환한다 | 조건 releaseRequesterMemberId 가 지정되면 워크북을 RELEASED 로 전환한다 | 성공: 검증 assertThat(result.released()).isTrue(); |
| [162](../../../src/test/java/com/umc/product/test/application/service/CurriculumSeedServiceTest.java#L162) | WeeklyCurriculum 과 OriginalWorkbook 은 파트별 bulk UseCase 로 생성한다 | 조건 WeeklyCurriculum 과 OriginalWorkbook 은 파트별 bulk UseCase 로 생성한다 | 성공: 검증 assertThat(result.createdWeeklyCurriculumIds()).hasSize(6); assertThat(result.createdOriginalWorkbookIds()).hasSize(6); |
| [183](../../../src/test/java/com/umc/product/test/application/service/CurriculumSeedServiceTest.java#L183) | releaseRequesterMemberId 가 null 이면 워크북 상태 전환을 호출하지 않는다 | 조건 releaseRequesterMemberId 가 null 이면 워크북 상태 전환을 호출하지 않는다 | 성공: 검증 assertThat(result.released()).isFalse(); |
| [200](../../../src/test/java/com/umc/product/test/application/service/CurriculumSeedServiceTest.java#L200) | RELEASED 전환 실패 시 releaseFailed 에 카운트되고 released=false 로 반환한다 | 조건 RELEASED 전환 실패 시 releaseFailed 에 카운트되고 released=false 로 반환한다 | 실패: 검증 assertThat(result.released()).isFalse(); assertThat(result.releaseFailed()).isEqualTo(2); |
| [219](../../../src/test/java/com/umc/product/test/application/service/CurriculumSeedServiceTest.java#L219) | RELEASED 전환 시 모든 워크북 ID 와 요청자 멤버 ID 가 정확히 전달된다 | 호출 seed(new SeedCurriculumCommand(gisuId, 3, 0, List.of(ChallengerPart.WEB), 777L)) | 성공: 검증 assertThat(sent).hasSize(3); assertThat(sent).allSatisfy(cmd -> assertThat(cmd.requestedMemberId()).isEqualTo(777L)); |
| [238](../../../src/test/java/com/umc/product/test/application/service/CurriculumSeedServiceTest.java#L238) | gisuId 가 null 이면 활성 기수를 사용한다 | 조건 gisuId 가 null 이면 활성 기수를 사용한다 | 성공: 검증 assertThat(result.gisuId()).isEqualTo(10L); |
| [254](../../../src/test/java/com/umc/product/test/application/service/CurriculumSeedServiceTest.java#L254) | parts 에 ADMIN 이 포함되어도 ADMIN 은 시딩되지 않는다 | 조건 parts 에 ADMIN 이 포함되어도 ADMIN 은 시딩되지 않는다 | 성공: 검증 assertThat(result.createdCurriculumIds()).hasSize(1); |

### MemberSeedServiceTest
- 위치: `src/test/java/com/umc/product/test/application/service/MemberSeedServiceTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [57](../../../src/test/java/com/umc/product/test/application/service/MemberSeedServiceTest.java#L57) | force=false 이고 회원 수가 임계값 초과면 시딩을 스킵한다 | 호출 seed(new SeedMembersCommand(5, false)) | 실패: 검증 assertThat(result.skipped()).isTrue(); assertThat(result.reason()).contains("threshold"); |
| [72](../../../src/test/java/com/umc/product/test/application/service/MemberSeedServiceTest.java#L72) | force=true 면 임계값 체크를 무시하고 시딩한다 | 호출 seed(new SeedMembersCommand(2, true)) | 성공: 검증 assertThat(result.skipped()).isFalse(); assertThat(result.registered()).isEqualTo(2); |
| [88](../../../src/test/java/com/umc/product/test/application/service/MemberSeedServiceTest.java#L88) | 정상 시딩 시 email batchRegister 가 1번 호출된다 | 호출 seed(new SeedMembersCommand(5, false)) | 성공: 정상 시딩 시 email batchRegister 가 1번 호출된다 |
| [103](../../../src/test/java/com/umc/product/test/application/service/MemberSeedServiceTest.java#L103) | email batchRegister 실패 시 등록 수는 0으로 반환된다 | 호출 seed(new SeedMembersCommand(3, false)) | 실패: 검증 assertThat(result.registered()).isZero(); |
| [120](../../../src/test/java/com/umc/product/test/application/service/MemberSeedServiceTest.java#L120) | count=0 이면 register 가 호출되지 않는다 | 호출 seed(new SeedMembersCommand(0, false)) | 성공: 검증 assertThat(result.skipped()).isFalse(); assertThat(result.registered()).isZero(); |
| [135](../../../src/test/java/com/umc/product/test/application/service/MemberSeedServiceTest.java#L135) | email 시퀀스는 현재 회원 수 + 1 부터 시작한다 | 호출 seed(new SeedMembersCommand(2, true)) | 성공: email 시퀀스는 현재 회원 수 + 1 부터 시작한다 |

### NoticeSeedServiceTest
- 위치: `src/test/java/com/umc/product/test/application/service/NoticeSeedServiceTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [61](../../../src/test/java/com/umc/product/test/application/service/NoticeSeedServiceTest.java#L61) | 4 가지 scope 별로 요청 수량만큼 공지가 생성된다 | 조건 4 가지 scope 별로 요청 수량만큼 공지가 생성된다 | 성공: 검증 assertThat(result.totalCreated()).isEqualTo(15); assertThat(result.createdNoticeIds()).hasSize(15); assertThat(global.created()).isEqualTo(3); assertThat(chapter.created()).isEqualTo(4); |
| [90](../../../src/test/java/com/umc/product/test/application/service/NoticeSeedServiceTest.java#L90) | 권한 부족 등의 실패는 scope 단위 failed 카운트에 반영되고 다른 scope 는 진행된다 | 조건 권한 부족 등의 실패는 scope 단위 failed 카운트에 반영되고 다른 scope 는 진행된다 | 실패: 검증 assertThat(chapter.failed()).isEqualTo(2); // 2 chapters × 1; assertThat(chapter.created()).isZero(); assertThat(findScope(result, "GLOBAL").created()).isEqualTo(1); assertThat(findScope(result, "SCHOOL").created()).is... |
| [122](../../../src/test/java/com/umc/product/test/application/service/NoticeSeedServiceTest.java#L122) | 모든 count 가 0 이면 createNotice 가 호출되지 않는다 | 조건 모든 count 가 0 이면 createNotice 가 호출되지 않는다 | 성공: 검증 assertThat(result.totalCreated()).isZero(); |
| [139](../../../src/test/java/com/umc/product/test/application/service/NoticeSeedServiceTest.java#L139) | gisuId 가 null 이면 활성 기수를 사용한다 | 조건 gisuId 가 null 이면 활성 기수를 사용한다 | 성공: 검증 assertThat(result.gisuId()).isEqualTo(10L); |
| [156](../../../src/test/java/com/umc/product/test/application/service/NoticeSeedServiceTest.java#L156) | parts 가 null 이면 ADMIN 제외 모든 파트가 사용된다 | 조건 parts 가 null 이면 ADMIN 제외 모든 파트가 사용된다 | 성공: 검증 assertThat(partCaptor.getAllValues()).noneMatch(p -> p == ChallengerPart.ADMIN); |
| [177](../../../src/test/java/com/umc/product/test/application/service/NoticeSeedServiceTest.java#L177) | 제목·내용에 대상 범위 정보가 포함된 채로 생성된다 (DummyNoticeFactory 위임 확인) | 호출 seed(new SeedNoticeCommand(gisuId, 1L, 1, 1, 1, 1, List.of(ChallengerPart.WEB))) | 성공: 검증 assertThat(sent).anyMatch(c -> c.title().contains("[전체]")); assertThat(sent).anyMatch(c -> c.title().contains("[지부]")); assertThat(sent).anyMatch(c -> c.title().contains("[학교]")); assertThat(sent).anyMatch(c -> c.titl... |

### PartAssignmentPolicyTest
- 위치: `src/test/java/com/umc/product/test/application/service/PartAssignmentPolicyTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [21](../../../src/test/java/com/umc/product/test/application/service/PartAssignmentPolicyTest.java#L21) | 풀이 충분히 클 때 첫 슬롯은 항상 PLAN | 호출 nextProjectSlots(20).get(0)).isEqualTo(ChallengerPart.PLAN) | 성공: 검증 assertThat(sut.nextProjectSlots(20).get(0)).isEqualTo(ChallengerPart.PLAN); |
| [45](../../../src/test/java/com/umc/product/test/application/service/PartAssignmentPolicyTest.java#L45) | 풀이 MIN_TOTAL(11) 미만이면 빈 슬롯 리스트를 반환한다 | 호출 nextProjectSlots(pool)) | 성공: 검증 assertThat(sut.nextProjectSlots(pool)); .isEmpty(); |
| [79](../../../src/test/java/com/umc/product/test/application/service/PartAssignmentPolicyTest.java#L79) | 프론트엔드/백엔드 파트 외의 값은 PLAN 외에 등장하지 않는다 | 호출 nextProjectSlots(20) | 성공: 검증 assertThat(FRONTEND.contains(part) \|\| BACKEND.contains(part)); .isTrue(); |

### ProjectScenarioSeedServiceTest
- 위치: `src/test/java/com/umc/product/test/application/service/ProjectScenarioSeedServiceTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [161](../../../src/test/java/com/umc/product/test/application/service/ProjectScenarioSeedServiceTest.java#L161) | createDraft와 updateProject만 호출되고 form/submit/publish는 호출되지 않는다 | 조건 createDraft와 updateProject만 호출되고 form/submit/publish는 호출되지 않는다 | 성공: 검증 assertThat(result.createdProjects()).hasSize(1); assertThat(created.projectId()).isEqualTo(500L); assertThat(created.finalStatus()).isEqualTo(TargetProjectStatus.DRAFT); assertThat(created.productOwnerMemberId()).isEq... |
| [197](../../../src/test/java/com/umc/product/test/application/service/ProjectScenarioSeedServiceTest.java#L197) | DRAFT 시나리오 / DRAFT 단계 호출에 더해 upsertForm + submit이 호출되고 publish는 호출되지 않는다 | 조건 DRAFT 시나리오 / DRAFT 단계 호출에 더해 upsertForm + submit이 호출되고 publish는 호출되지 않는다 | 성공: 검증 assertThat(result.createdProjects()).hasSize(1); assertThat(created.finalStatus()).isEqualTo(TargetProjectStatus.PENDING_REVIEW); assertThat(created.applicationFormId()).isEqualTo(9001L); assertThat(created.partFills(... |
| [228](../../../src/test/java/com/umc/product/test/application/service/ProjectScenarioSeedServiceTest.java#L228) | PENDING_REVIEW 시나리오 / 전체 단계가 호출되고 PartFill이 quota 범위 내에서 채워진다 | 조건 PENDING_REVIEW 시나리오 / 전체 단계가 호출되고 PartFill이 quota 범위 내에서 채워진다 | 성공: 검증 assertThat(result.createdProjects()).hasSize(1); assertThat(created.finalStatus()).isEqualTo(TargetProjectStatus.IN_PROGRESS); assertThat(created.partFills()).hasSize(3); assertThat(created.partFills().get(0).part()).... |
| [287](../../../src/test/java/com/umc/product/test/application/service/ProjectScenarioSeedServiceTest.java#L287) | PO 본인은 멤버 충원에서 제외된다 | 조건 PO 본인은 멤버 충원에서 제외된다 | 성공: PO 본인은 멤버 충원에서 제외된다 |
| [312](../../../src/test/java/com/umc/product/test/application/service/ProjectScenarioSeedServiceTest.java#L312) | 멤버 풀이 비어있어도 partFills는 quota entry 수만큼 0으로 채워서 반환한다 | 조건 멤버 풀이 비어있어도 partFills는 quota entry 수만큼 0으로 채워서 반환한다 | 성공: 검증 assertThat(created.partFills()).hasSize(3); assertThat(created.partFills()).allMatch(f -> f.filled() == 0L); assertThat(created.partFills().get(0).quota()).isEqualTo(2L); assertThat(created.partFills().get(1).quota())... |
| [343](../../../src/test/java/com/umc/product/test/application/service/ProjectScenarioSeedServiceTest.java#L343) | productOwnerMemberIds size가 projectCount와 다르면 IllegalArgumentException | 조건 productOwnerMemberIds size가 projectCount와 다르면 IllegalArgumentException | 실패: 예외 IllegalArgumentException |
| [356](../../../src/test/java/com/umc/product/test/application/service/ProjectScenarioSeedServiceTest.java#L356) | PO 입력 검증 / 입력된 PO가 PLAN 챌린저가 아니면 IllegalArgumentException | 조건 PO 입력 검증 / 입력된 PO가 PLAN 챌린저가 아니면 IllegalArgumentException | 실패: 예외 IllegalArgumentException |
| [371](../../../src/test/java/com/umc/product/test/application/service/ProjectScenarioSeedServiceTest.java#L371) | PO 입력 검증 / 입력된 PO가 챌린저가 아니면 IllegalArgumentException | 조건 PO 입력 검증 / 입력된 PO가 챌린저가 아니면 IllegalArgumentException | 실패: 예외 IllegalArgumentException |
| [387](../../../src/test/java/com/umc/product/test/application/service/ProjectScenarioSeedServiceTest.java#L387) | PO 입력 검증 / PO 입력이 null이고 PLAN 풀이 부족하면 IllegalArgumentException | 조건 PO 입력 검증 / PO 입력이 null이고 PLAN 풀이 부족하면 IllegalArgumentException | 실패: 예외 IllegalArgumentException |
| [402](../../../src/test/java/com/umc/product/test/application/service/ProjectScenarioSeedServiceTest.java#L402) | 랜덤 PO 픽 / PO 입력이 null이면 PLAN ACTIVE 챌린저 풀에서 N명 픽한다 | 조건 랜덤 PO 픽 / PO 입력이 null이면 PLAN ACTIVE 챌린저 풀에서 N명 픽한다 | 성공: 검증 assertThat(result.createdProjects()).hasSize(2); assertThat(result.createdProjects()) |
| [431](../../../src/test/java/com/umc/product/test/application/service/ProjectScenarioSeedServiceTest.java#L431) | 랜덤 PO 픽 / submit 실패 시 reachedStatus=DRAFT, failedStep=SUBMIT으로 failedProjects에 기록된다 | 조건 랜덤 PO 픽 / submit 실패 시 reachedStatus=DRAFT, failedStep=SUBMIT으로 failedProjects에 기록된다 | 실패: 검증 assertThat(result.createdProjects()).isEmpty(); assertThat(result.failedProjects()).hasSize(1); assertThat(failed.projectId()).isEqualTo(700L); assertThat(failed.reachedStatus()).isEqualTo(TargetProjectStatus.DRAFT); |
| [459](../../../src/test/java/com/umc/product/test/application/service/ProjectScenarioSeedServiceTest.java#L459) | 단계별 실패 격리 / publish 실패 시 reachedStatus=PENDING_REVIEW, failedStep=PUBLISH로 기록된다 | 조건 단계별 실패 격리 / publish 실패 시 reachedStatus=PENDING_REVIEW, failedStep=PUBLISH로 기록된다 | 실패: 검증 assertThat(result.createdProjects()).isEmpty(); assertThat(result.failedProjects()).hasSize(1); assertThat(failed.projectId()).isEqualTo(701L); assertThat(failed.reachedStatus()).isEqualTo(TargetProjectStatus.PENDING_... |
| [488](../../../src/test/java/com/umc/product/test/application/service/ProjectScenarioSeedServiceTest.java#L488) | 한 프로젝트가 실패해도 다른 프로젝트의 시딩은 계속 진행된다 | 조건 한 프로젝트가 실패해도 다른 프로젝트의 시딩은 계속 진행된다 | 실패: 검증 assertThat(result.createdProjects()).hasSize(1); assertThat(result.createdProjects().get(0).productOwnerMemberId()).isEqualTo(poB); assertThat(result.failedProjects()).hasSize(1); assertThat(result.failedProjects().ge... |

### ProjectSeedDataCleanupServiceTest
- 위치: `src/test/java/com/umc/product/test/application/service/ProjectSeedDataCleanupServiceTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [34](../../../src/test/java/com/umc/product/test/application/service/ProjectSeedDataCleanupServiceTest.java#L34) | gisuId가 지정되면 해당 기수 프로젝트 데이터를 삭제한다 | 호출 delete(DeleteSeedProjectDataCommand.of(9L)) | 성공: 검증 assertThat(result.gisuId()).isEqualTo(9L); assertThat(result.deletedProjects()).isEqualTo(2); assertThat(result.deletedProjectMembers()).isEqualTo(20); |
| [56](../../../src/test/java/com/umc/product/test/application/service/ProjectSeedDataCleanupServiceTest.java#L56) | gisuId가 null이면 활성 기수 프로젝트 데이터를 삭제한다 | 호출 delete(DeleteSeedProjectDataCommand.of(null)) | 성공: 검증 assertThat(result.gisuId()).isEqualTo(10L); assertThat(result.deletedProjects()).isEqualTo(1); |

### ProjectSeedServiceTest
- 위치: `src/test/java/com/umc/product/test/application/service/ProjectSeedServiceTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [70](../../../src/test/java/com/umc/product/test/application/service/ProjectSeedServiceTest.java#L70) | 풀이 MIN_TOTAL(11) 미만인 school 은 시딩을 스킵한다 | 호출 seed(new SeedProjectsCommand(5, 9L)) | 성공: 검증 assertThat(result.createdProjectIds()).isEmpty(); assertThat(result.partialProjects()).isEmpty(); assertThat(result.skippedChapters()).isNotEmpty(); assertThat(result.skippedChapters().get(0).reason()).contains("INSUF... |
| [92](../../../src/test/java/com/umc/product/test/application/service/ProjectSeedServiceTest.java#L92) | 정상 시딩 시 PO 가 PLAN 챌린저로 등록되지 않은 경우 자동 등록한다 | 호출 seed(new SeedProjectsCommand(1, gisuId)) | 성공: 검증 assertThat(result.createdProjectIds()).hasSize(1); assertThat(result.partialProjects()).isEmpty(); |
| [118](../../../src/test/java/com/umc/product/test/application/service/ProjectSeedServiceTest.java#L118) | 같은 호출 안에서 한 멤버가 두 프로젝트에 중복 배정되지 않는다 | 호출 seed(new SeedProjectsCommand(2, gisuId)) | 실패: 검증 assertThat(result.createdProjectIds()).hasSize(2); assertThat(usedMemberIds.add(cmd.memberId())); .isTrue(); |
| [149](../../../src/test/java/com/umc/product/test/application/service/ProjectSeedServiceTest.java#L149) | addProjectMember 가 중간에 실패하면 partialProjects 로 보고한다 | 호출 seed(new SeedProjectsCommand(1, gisuId)) | 실패: 검증 assertThat(result.createdProjectIds()).isEmpty(); assertThat(result.partialProjects()).hasSize(1); assertThat(partial.projectId()).isEqualTo(2000L); assertThat(partial.addedMemberCount()).isEqualTo(1); |
| [180](../../../src/test/java/com/umc/product/test/application/service/ProjectSeedServiceTest.java#L180) | createDraft 자체 실패는 failedCount 로 분류된다 | 호출 seed(new SeedProjectsCommand(1, gisuId)) | 실패: 검증 assertThat(result.createdProjectIds()).isEmpty(); assertThat(result.partialProjects()).isEmpty(); assertThat(result.failedCount()).isGreaterThanOrEqualTo(1); |
| [206](../../../src/test/java/com/umc/product/test/application/service/ProjectSeedServiceTest.java#L206) | school 풀과 후보 챌린저는 batch 로 한 번만 조회하고 캐시를 재사용한다 | 호출 seed(new SeedProjectsCommand(2, gisuId)) | 성공: 검증 assertThat(result.createdProjectIds()).hasSize(2); |
| [240](../../../src/test/java/com/umc/product/test/application/service/ProjectSeedServiceTest.java#L240) | gisuId 가 null 이면 활성 기수를 사용한다 | 호출 seed(new SeedProjectsCommand(1, null)) | 성공: 검증 assertThat(result.createdProjectIds()).isEmpty(); |

### ScenarioPartQuotaPolicyTest
- 위치: `src/test/java/com/umc/product/test/application/service/ScenarioPartQuotaPolicyTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [21](../../../src/test/java/com/umc/product/test/application/service/ScenarioPartQuotaPolicyTest.java#L21) | pickQuotas는 DESIGN 1, FE 1, BE 1로 항상 3개 entry를 반환한다 | 호출 pickQuotas() | 성공: 검증 assertThat(quotas).hasSize(3); assertThat(quotas.get(0).part()).isEqualTo(ChallengerPart.DESIGN); assertThat(FRONTEND_PARTS).contains(quotas.get(1).part()); assertThat(BACKEND_PARTS).contains(quotas.get(2).part()); |
| [34](../../../src/test/java/com/umc/product/test/application/service/ScenarioPartQuotaPolicyTest.java#L34) | DESIGN quota는 항상 1~2 사이 | 호출 pickQuotas() | 성공: 검증 assertThat(quotas.get(0).quota()).isBetween(1L, 2L); |
| [43](../../../src/test/java/com/umc/product/test/application/service/ScenarioPartQuotaPolicyTest.java#L43) | FE quota와 BE quota는 항상 3~4 사이 | 호출 pickQuotas() | 성공: 검증 assertThat(quotas.get(1).quota()).isBetween(3L, 4L); assertThat(quotas.get(2).quota()).isBetween(3L, 4L); |

## Repository / Outbound Persistence

### ProjectSeedDataCleanupPersistenceAdapterTest
- 위치: `src/test/java/com/umc/product/test/adapter/out/persistence/ProjectSeedDataCleanupPersistenceAdapterTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [41](../../../src/test/java/com/umc/product/test/adapter/out/persistence/ProjectSeedDataCleanupPersistenceAdapterTest.java#L41) | 대상 기수의 프로젝트 관련 데이터만 모두 삭제한다 | 호출 deleteByGisuId(targetGisu.getId()) | 성공: 검증 assertThat(result.deletedProjects()).isEqualTo(1); assertThat(result.deletedProjectMembers()).isEqualTo(1); assertThat(result.deletedProjectApplications()).isEqualTo(1); assertThat(result.deletedProjectApplicationForm... |
