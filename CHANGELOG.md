# Changelog

## [0.1.11] - 2026-06-09

### Changed
- Spring Boot `3.5.14` → `3.5.15` — Spring Framework 6.2.19 자연 포함 (BOM 오버라이드 제거)

---

## [0.1.10] - 2026-06-09

### Changed
- Spring Framework `6.2.18` → `6.2.19` (보안 취약점 패치)
  - `spring-framework-bom` 명시적 오버라이드로 적용

---

## [0.1.8] - 2026-06-09

### Changed
- Spring Boot `3.2.0` → `3.5.14` (보안 취약점 패치)
- Spring Framework `6.1.1` → `6.2.18` (Spring Boot BOM 관리)

---

## [0.1.6] - 2026-06-09

### Added
- `aileron.datahub.enabled` 설정 추가 (기본: true)
  - `false` 설정 시 모든 emit skip — 비즈니스 로직에 영향 없음
  - DataHub 연동이 필요 없는 환경에서 완전히 디펜던시 제거 가능

---

## [0.1.5] - 2026-06-09

### Fixed
- DataHub 복구 시 emit이 자동으로 재개되지 않던 문제 수정
  - 실패 후 `aileron.datahub.cooldown-sec`(기본 60초) 경과 시 자동 재시도
  - 재시도 성공 시 정상 emit 재개, 실패 시 쿨다운 재시작

---

## [0.1.4] - 2026-06-09

### Fixed
- DataHub down 시 emit 즉시 skip 처리로 큐 누적 방지
  - 첫 실패 감지 시 `datahubReachable=false` 설정 → 이후 요청 즉시 skip
  - emit 성공 시 자동 복구 (`datahubReachable=true`)
  - 요청 폭주 시 큐 누적 및 스레드 블로킹 문제 해결

---

## [0.1.3] - 2026-06-09

### Fixed
- DataHub 연결 불가 시 스레드 블로킹 및 소켓 고갈 방지
  - `RestEmitter` 싱글턴으로 관리 (double-checked locking) — 요청마다 새 커넥션풀 생성 방지
  - `connectTimeoutSec=3` 기본값 설정 (기존: 무제한)
  - `maxRetries=0` 기본값 설정 (기존: 3회 재시도 × 30초 = 최대 90초 블로킹)
  - 환경변수 `aileron.datahub.connect-timeout-sec`, `aileron.datahub.max-retries` 로 조정 가능

---

## [0.1.2] - 2026-06-09

### Fixed
- datahub-client 버전 `1.4.0.3.3-patched`에 맞게 타입 수정
  - `UrnArray` → `DatasetUrnArray` 타입 수정 (`DataJobInputOutput.setInputDatasets/setOutputDatasets`)
  - `DataJobPatchBuilder` import 추가

---

## [0.1.1] - 2026-06-09

### Fixed
- datahub-client groupId를 `io.acryl` → `io.github.baccas300` 로 변경
- Maven Central groupId를 `io.github.wansik92` 로 변경 후 namespace 인증

---

## [0.1.0] - 2026-06-09

### Added
- Spring AOP 기반 DataHub lineage 자동 수집 라이브러리 최초 릴리즈
- `@DatahubJob` 어노테이션 — DataFlow / DataJob / DataProcessInstance 자동 emit
- `DatahubLineage.addInput/addOutput` — 수동 lineage 주입 API
- `upstreamJobs` 파라미터 — DataJob 간 리니지 체이닝 지원
- `patch` 파라미터 — 기존 lineage에 추가 (덮어쓰지 않음)
- `aileron.datahub.gms-url`, `env`, `silent-fail` 설정 지원
- Spring Boot AutoConfiguration 지원
