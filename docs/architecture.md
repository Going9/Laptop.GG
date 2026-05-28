# LaptopGG 운영형 구조

LaptopGG는 단일 repository 안에서 Gradle 멀티모듈로 런타임 역할을 분리한 모듈형 모놀리스로 운영한다. Oracle Cloud 저사양 앱 서버와 DB 서버, GitHub Actions 크롤러 구조를 전제로 하며 Docker/Kubernetes 전환은 현재 범위가 아니다.

## Runtime Roles

- `web-app`: 사용자 화면, 추천 API, 노트북 상세, 댓글 기능을 담당한다. 운영 프로필은 `postgres,deploy`다.
- `crawler-job`: GitHub Actions에서 jar를 실행해 Danawa 데이터를 수집하고 DB에 직접 저장한다. 운영 프로필은 `postgres,crawler`다.
- `web-app`은 crawler 프로필이나 crawler HTTP API를 갖지 않는다.
- `crawler-job`은 non-web batch jar이며 actuator나 web server 설정을 갖지 않는다.

## Code Boundaries

- `domain`: JPA 엔티티와 도메인 enum을 담는다.
- `application`: 화면/API use case, 추천 정책, 노트북 상세, 댓글, 크롤러 저장 use case, application service와 out port를 담는다.
- `infrastructure-jpa`: Spring Data repository, Flyway migration, application port adapter를 담는다.
- `web-app`: Controller, Thymeleaf template, static resource, actuator health를 담는다.
- `crawler-job`: 외부 배치 잡의 crawler 하위 컴포넌트와 startup runner를 담는다.

`application`은 공개 REST/Form DTO를 알지 않는다. Query, command, result 모델은 application 안에 두고, `web-app`이 공개 DTO와 application 모델 사이를 변환한다.
애플리케이션 서비스 구현은 `application.service`, crawler job 구현은 `job.crawler` 패키지에 둬 런타임 역할을 패키지명에서도 드러낸다.

## Crawler Components

- `CrawlerService`: 전체 목록 순회, 상세 재수집 판단, 실패/열화 집계를 조율한다.
- `DanawaClient`: HTTP 요청, 재시도, pacing, 전역 cooldown을 담당한다.
- `ListPageCrawler`: 목록 페이지 요청 컨텍스트와 상품 카드 배치를 만든다.
- `DetailCrawler`: 상세 페이지와 상세 스펙 테이블을 가져오고 파싱한다.
- `LaptopSnapshotMerger`: 파싱된 스펙을 `Laptop` 스냅샷으로 조립한다.
- `SaveCrawledLaptopUseCase`: 노트북 저장, 프로필 동기화, 가격 이력 기록, 추천 점수 갱신을 application 트랜잭션으로 묶는다.
- `TrackCrawlerRunUseCase`: `crawler_run` 실행 이력을 application port를 통해 기록한다.
- `CrawlerAdvisoryLockService`: PostgreSQL advisory lock으로 중복 실행을 차단한다.

## Operations Surface

- Public user routes and REST DTOs stay unchanged.
- `/actuator/health` is the only new operational endpoint and should be checked from localhost or internal server paths.
- Public crawler HTTP APIs are not implemented in `web-app`. GitHub Actions `crawler-job` is the official crawler execution path.
- Schema changes are managed by Flyway. Deploy profile keeps `baseline-on-migrate=false`; use `legacy-baseline` or `SPRING_FLYWAY_BASELINE_ON_MIGRATE=true` only for one-time legacy onboarding.
