# LaptopGG 운영형 구조

LaptopGG는 단일 repository 안에서 Gradle 멀티모듈로 런타임 역할을 분리한 모듈형 모놀리스로 운영한다. Oracle Cloud 저사양 앱 서버와 DB 서버, GitHub Actions 크롤러 구조를 전제로 하며 Docker/Kubernetes 전환은 현재 범위가 아니다.

## Runtime Roles

- `web-app`: 사용자 화면, 추천 API, 노트북 상세, 댓글 기능을 담당한다. 운영 프로필은 `postgres,deploy`다.
- `crawler-job`: GitHub Actions에서 jar를 실행해 Danawa 데이터를 수집하고 DB에 직접 저장한다. 운영 프로필은 `postgres,crawler`다.
- `web-app`은 crawler 프로필이나 crawler HTTP API를 갖지 않는다.
- `crawler-job`은 non-web batch jar이며 actuator나 web server 설정을 갖지 않는다.
- crawler 저장, 프로필 동기화, 가격 이력, 추천 점수 갱신 use case는 `application-crawler` 모듈에만 존재하며 web runtime classpath에 들어가지 않는다.
- `web-app`과 `crawler-job`은 application use case를 component scan으로 찾지 않고 runtime config에서 필요한 bean만 명시적으로 조립한다.
- `crawler-job`은 component scan 범위를 배치 실행에 필요한 `job`, `infrastructure.jpa.adapter.crawler`로 제한한다.

## Code Boundaries

- `domain`: JPA 엔티티와 도메인 enum을 담는다.
- `recommendation-core`: 추천 use-case enum, 점수 가중치, gate 정책을 담는 Spring-free/domain-free shared policy module이다.
- `application`: 화면/API use case, 노트북 상세, 댓글, web-facing out port를 담는다.
- `application-crawler`: crawler 저장/동기화 use case, crawler 전용 out port, profile/score 정책, crawler command 계약을 담는다.
- `infrastructure-jpa-core`: Flyway migration, entity scan, 공통 PostgreSQL/Flyway/JPA 설정을 담는다.
- `infrastructure-jpa`: web-facing JPA application port adapter를 담는다.
- `infrastructure-jpa-crawler`: crawler 저장, 프로필 갱신, 가격 이력, 추천 점수 JPA adapter와 crawler repository를 담아 web runtime classpath에서 crawler persistence 구현을 제외한다.
- `infrastructure-jpa.adapter.web`, `infrastructure-jpa-crawler.adapter.crawler`: 런타임 역할별 JPA adapter를 나눠 web context와 crawler context가 서로의 adapter를 스캔하지 않게 한다.
- `infrastructure-jpa.repository.web`, `infrastructure-jpa-crawler.repository.crawler`: 런타임 역할별 Spring Data repository를 나눠 web/crawler가 필요한 repository만 등록한다.
- `infrastructure-jpa-core/src/main/resources/laptopgg-persistence.yml`: web/crawler가 공통으로 import하는 PostgreSQL, Flyway, JPA 운영 설정의 단일 출처다.
- `infrastructure-security`: 비밀번호 해시 같은 보안 application port adapter를 담는다.
- `integration-tests`: web/crawler persistence를 함께 띄우는 cross-module 통합 테스트를 담아 production adapter 모듈의 테스트 의존이 서로 섞이지 않게 한다.
- `web-app`: `web.controller`, `web.dto`, Thymeleaf template, static resource, actuator health를 담는다.
- `crawler-job`: 외부 배치 잡의 crawler 하위 컴포넌트와 startup runner를 담는다.
- `ops/postgres/laptopgg-postgresql.conf`: 1GB DB 서버용 PostgreSQL 관측성/커넥션 baseline 설정이다.

`application`은 공개 REST/Form DTO를 알지 않는다. Query, command, result 모델은 application 안에 두고, `web-app`이 공개 DTO와 application 모델 사이를 변환한다. 이 application 계약 모델은 domain entity를 파라미터나 반환 타입으로 노출하지 않는다.
비밀번호 해시처럼 외부 라이브러리 구현이 필요한 세부사항은 application port로 정의하고 infrastructure adapter에서 구현한다.
추천/상세/댓글의 사용자 흐름은 `application` use case가 직접 소유한다. 크롤러 저장, 프로필 동기화, 가격 이력, 추천 점수 갱신 흐름과 그 out port는 `application-crawler`에 둔다. 추천 use-case enum과 점수 가중치/gate 정책은 `recommendation-core`에 두어 web 추천 계산과 crawler 점수 projection이 같은 정책을 공유한다. crawler job 구현은 `job.crawler` 패키지에 둬 런타임 역할을 패키지명에서도 드러낸다.
`crawler-job`은 domain entity, `application.port.out`, 사용자 흐름 application 구현에 직접 의존하지 않고, `application-crawler`의 command/use case 계약만 호출한다. 추천/상세/댓글 같은 사용자 흐름 use case와 웹 추천 계산기는 crawler runtime에 직접 등록하지 않는다.
`crawler-job`은 Flyway와 공통 persistence 설정을 위해 `infrastructure-jpa-core`를 사용하고, Spring Data repository는 `infrastructure-jpa-crawler`의 crawler 전용 repository만 등록한다. web-facing `infrastructure-jpa` 모듈은 런타임 classpath에 올리지 않는다.
`application` 테스트도 `infrastructure-jpa`를 의존하지 않는다. JPA repository나 Spring Data가 필요한 통합 테스트는 `integration-tests` 모듈로 둔다.
`crawler-job` 테스트는 수집 orchestration, parser, runner 경계만 검증한다. 저장 use case와 JPA adapter가 함께 필요한 persistence 통합 테스트는 `integration-tests` 모듈에 둔다.

## Crawler Components

- `CrawlerService`: 전체 목록 순회, 상세 재수집 판단, 실패/열화 집계를 조율한다.
- `DanawaClient`: HTTP 요청, 재시도, pacing, 전역 cooldown을 담당한다.
- `ListPageCrawler`: 목록 페이지 요청 컨텍스트와 상품 카드 배치를 만든다.
- `DetailCrawler`: 상세 페이지와 상세 스펙 테이블을 가져오고 파싱한다.
- `LaptopSnapshotMerger`: 파싱된 스펙을 application crawler command로 조립한다.
- `SaveCrawledLaptopUseCase`: 노트북 저장, 프로필 동기화, 가격 이력 기록, 추천 점수 갱신을 application-crawler 트랜잭션으로 묶는다.
- `TrackCrawlerRunUseCase`: `crawler_run` 실행 이력을 application-crawler port를 통해 기록한다.
- `CrawlerRunLockPort`: 중복 실행 차단 계약을 application-crawler에 두고, `infrastructure-jpa-crawler`의 PostgreSQL advisory lock adapter가 구현한다.

## Operations Surface

- Public user routes and REST DTOs stay unchanged.
- `/actuator/health` is the only new operational endpoint and should be checked from localhost or internal server paths.
- Public crawler HTTP APIs are not implemented in `web-app`. GitHub Actions `crawler-job` is the official crawler execution path.
- Schema changes are managed by Flyway. Deploy profile keeps `baseline-on-migrate=false`; use `legacy-baseline` or `SPRING_FLYWAY_BASELINE_ON_MIGRATE=true` only for one-time legacy onboarding.
