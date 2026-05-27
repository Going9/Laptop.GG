# LaptopGG 운영형 구조

LaptopGG는 단일 Gradle/Spring Boot 프로젝트를 유지하되 런타임 역할을 분리한 모듈형 모놀리스로 운영한다. Oracle Cloud 저사양 앱 서버와 DB 서버, GitHub Actions 크롤러 구조를 전제로 하며 Docker/Kubernetes 전환은 현재 범위가 아니다.

## Runtime Roles

- `web`: 사용자 화면, 추천 API, 노트북 상세, 댓글 기능을 담당한다. 운영 프로필은 `postgres,deploy`다.
- `crawler-job`: GitHub Actions에서 jar를 실행해 Danawa 데이터를 수집하고 DB에 직접 저장한다. 운영 프로필은 `postgres,crawler`다.
- `local-dev`: 로컬 개발에서만 `/api/crawl/laptops` HTTP 엔드포인트를 활성화한다.

## Code Boundaries

- `controller`: URL, 요청 바인딩, Thymeleaf 모델 구성만 담당한다.
- `application`: 화면/API use case 진입점이다. 컨트롤러는 이 계층만 호출한다.
- `service`: 추천 정책, 노트북 상세, 댓글, 프로필 동기화 같은 애플리케이션 로직을 담는다.
- `domain`: JPA 엔티티와 repository 계약을 담는다.
- `service.crawler`: 외부 배치 잡의 crawler 하위 컴포넌트다.

## Crawler Components

- `CrawlerService`: 전체 목록 순회와 저장 흐름을 조율한다.
- `DanawaClient`: HTTP 요청, 재시도, pacing, 전역 cooldown을 담당한다.
- `ListPageCrawler`: 목록 페이지 요청 컨텍스트와 상품 카드 배치를 만든다.
- `DetailCrawler`: 상세 페이지와 상세 스펙 테이블을 가져오고 파싱한다.
- `LaptopSnapshotMerger`: 파싱된 스펙을 `Laptop` 스냅샷으로 조립한다.
- `CrawlerPersistenceService`: 노트북 저장, 프로필 동기화, 가격 이력 기록을 트랜잭션으로 묶는다.
- `CrawlerRunService`: `crawler_run` 실행 이력을 기록한다.
- `CrawlerAdvisoryLockService`: PostgreSQL advisory lock으로 중복 실행을 차단한다.

## Operations Surface

- Public user routes and REST DTOs stay unchanged.
- `/actuator/health` is the only new operational endpoint and should be checked from localhost or internal server paths.
- Public crawler HTTP APIs are not part of production. `/api/crawl/laptops` is active only with `local-dev`.
- Schema changes are managed by Flyway. Deploy profile keeps `baseline-on-migrate=false`; use `legacy-baseline` or `SPRING_FLYWAY_BASELINE_ON_MIGRATE=true` only for one-time legacy onboarding.
