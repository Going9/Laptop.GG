# LaptopGG 운영형 구조

LaptopGG는 단일 repository 안에서 Gradle 멀티모듈로 런타임 역할을 분리한 모듈형 모놀리스로 운영한다. Oracle Cloud 저사양 앱 서버와 DB 서버, GitHub Actions 크롤러 구조를 전제로 하며 Docker/Kubernetes 전환은 현재 범위가 아니다.

## Runtime Roles

- `web-app`: 사용자 화면, 추천 API, 노트북 상세, 댓글 기능을 담당한다. 운영 프로필은 `postgres,deploy`다.
- `web-app` uses graceful shutdown so deploy restarts can drain in-flight requests before systemd forces termination.
- `crawler-job`: GitHub Actions에서 jar를 실행해 Danawa 데이터를 수집하고 DB에 직접 저장한다. 운영 프로필은 `postgres,crawler`다.
- `web-app`은 crawler 프로필이나 crawler HTTP API를 갖지 않는다.
- `crawler-job`은 non-web batch jar이며 actuator나 web server 설정을 갖지 않는다.
- crawler 저장, 프로필 동기화, 가격 이력, 추천 점수 갱신 use case는 `application-crawler` 모듈에만 존재하며 web runtime classpath에 들어가지 않는다.
- `web-app`과 `crawler-job`은 application use case를 component scan으로 찾지 않고 runtime config에서 필요한 bean만 명시적으로 조립한다.
- `web-app`과 `crawler-job`은 infrastructure adapter package를 직접 scan하지 않고, 역할별 adapter config facade만 import한다.
- `crawler-job`은 component scan 범위를 배치 실행에 필요한 `job` 하위 패키지로 제한한다.

## Code Boundaries

- `laptop-taxonomy`: CPU/GPU/배터리/휴대성 분류 enum을 담는 Spring-free shared enum module이다.
- `persistence-model`: web/crawler가 공유하는 노트북/추천 JPA 엔티티를 담는다.
- `persistence-model-web`: web 런타임 전용 댓글 JPA 엔티티를 담는다.
- `persistence-model-crawler`: crawler 런타임 전용 실행 이력/가격 이력 JPA 엔티티를 담는다.
- persistence model 계층의 JPA 엔티티는 Kotlin `data class`를 쓰지 않고, to-one 연관관계는 `fetch = LAZY`를 명시해 영속성 객체 정체성과 저사양 운영 조회 비용을 안정적으로 유지한다.
- 노트북 표시/식별 필드인 `name`, `image_url`, `detail_page`는 application command와 PostgreSQL check constraint가 함께 보호한다. 레거시 데이터가 남아 있을 수 있어 신규/변경 데이터부터 막는 `NOT VALID` constraint로 편입한다.
- 노트북 usage 값은 application 저장 use case에서 trim/filter/distinct 정규화하고, DB check constraint로 신규 blank usage 저장을 차단한다.
- 댓글은 application 계약과 DB 제약을 맞춰 laptop, author, content, password hash를 필수로 둔다. 레거시 nullable 댓글은 Flyway에서 `comment_invalid_legacy`에 보관한 뒤 public 댓글 테이블에서 제외한다.
- `recommendation-contract`: 추천 use-case enum을 담는 Spring-free public recommendation vocabulary module이다.
- `recommendation-core`: 추천 점수 가중치와 gate 정책을 담는 Spring-free scoring policy module이다.
- `application`: 화면/API use case, 노트북 상세, 댓글, web-facing out port를 담는다.
- `application-crawler`: crawler 저장/동기화 use case, feature별 crawler 전용 out port, profile/score 정책, crawler command 계약을 담는다.
- `application-crawler`의 public surface는 command/result, use case interface, out port, Danawa 정규화 resolver로 제한한다. CPU/GPU 분류기, profile score policy, 저장/실행이력 service 구현은 assembler 뒤의 internal 구현으로 둔다.
- `infrastructure-jpa-core`: 공통 PostgreSQL/JPA profile 설정을 담고 Flyway migration 리소스는 싣지 않는다.
- `infrastructure-flyway`: Flyway migration 리소스와 Flyway 런타임 의존성을 담아 web deploy와 migration 통합 테스트에서만 사용한다.
- `infrastructure-jpa`: web-facing JPA application port adapter를 담는다.
- `infrastructure-jpa-crawler`: crawler 저장, 프로필 갱신, 가격 이력, 추천 점수 JPA adapter와 crawler repository를 담아 web runtime classpath에서 crawler persistence 구현을 제외한다.
- `infrastructure-jpa.adapter.web`, `infrastructure-jpa-crawler.adapter.crawler`: 런타임 역할별 JPA adapter를 나눠 web context와 crawler context가 서로의 adapter를 스캔하지 않게 한다.
- `infrastructure-jpa.repository.web`, `infrastructure-jpa-crawler.repository.crawler`: 런타임 역할별 Spring Data repository를 나눠 web/crawler가 필요한 repository만 등록한다.
- `infrastructure-jpa.config.WebJpaRepositoryConfig`, `infrastructure-jpa-crawler.config.CrawlerJpaRepositoryConfig`: 런타임 역할별 entity scan package를 소유한다.
- `infrastructure-jpa.config.WebJpaAdapterConfig`, `infrastructure-jpa-crawler.config.CrawlerJpaAdapterConfig`: 런타임이 import하는 공개 facade이며, 내부 JPA adapter scan을 각 인프라 모듈 안에 숨긴다.
- `infrastructure-jpa-core/src/main/resources/laptopgg-persistence.yml`: web/crawler가 공통으로 import하는 PostgreSQL, Flyway property, JPA 운영 설정의 단일 출처다.
- `infrastructure-security`: 비밀번호 해시 같은 보안 application port adapter를 담고, `PasswordHashAdapterConfig`만 런타임 공개 facade로 노출한다.
- `integration-tests`: web/crawler persistence를 함께 띄우는 cross-module 통합 테스트를 담아 production adapter 모듈의 테스트 의존이 서로 섞이지 않게 한다.
- `web-app`: `web.controller`, `web.dto`, Thymeleaf template, static resource, actuator health를 담는다.
- `web-app`의 Thymeleaf 화면은 추천/상세 화면별 typed page model을 루트 모델로 받아, controller와 template 사이의 문자열 attribute key 결합을 줄인다.
- `crawler-job`: 외부 배치 잡의 crawler 하위 컴포넌트와 startup runner를 담는다.
- `ops/postgres/laptopgg-postgresql.conf`: 1GB DB 서버용 PostgreSQL 관측성/커넥션 baseline 설정이다.
- `gradle/structure-check.gradle.kts`: 모듈 의존 방향, runtime scan 범위, port/adapter 패키지 규칙을 검증하는 빌드 구조 규칙을 담는다.

`application`은 공개 REST/Form DTO를 알지 않는다. Query, command, result 모델은 application 안에 두고, `web-app`이 공개 DTO와 application 모델 사이를 변환한다. 이 application 계약 모델은 JPA entity를 파라미터나 반환 타입으로 노출하지 않는다.
비밀번호 해시처럼 외부 라이브러리 구현이 필요한 세부사항은 application port로 정의하고 infrastructure adapter에서 구현한다.
추천/상세/댓글의 사용자 흐름은 `application` use case가 직접 소유한다. 크롤러 저장, 프로필 동기화, 가격 이력, 추천 점수 갱신 흐름과 그 out port는 `application-crawler`에 둔다. 추천 use-case enum은 `recommendation-contract`, 점수 가중치/gate 정책은 `recommendation-core`에 두어 web은 공개 선택지 계약만 알고 crawler 점수 projection과 web 추천 계산은 같은 정책을 공유한다. crawler job 구현은 `job.crawler` 패키지에 둬 런타임 역할을 패키지명에서도 드러낸다.
`application-crawler`의 out port는 generic `port.out`이 아니라 `persistence.port`, `profile.port`, `price.port`, `recommendation.port`, `run.port`, `common.port`처럼 실제 use case 소속 아래에 둔다.
`crawler-job`은 persistence model, `application.port.out`, 사용자 흐름 application 구현에 직접 의존하지 않고, `application-crawler`의 command/use case 계약만 호출한다. 추천/상세/댓글 같은 사용자 흐름 use case와 웹 추천 계산기는 crawler runtime에 직접 등록하지 않는다.
`crawler-job`은 프로필 점수 계산 정책을 Spring bean으로 직접 조립하지 않고, Danawa 파싱에 필요한 CPU/GPU 정규화 resolver와 저장 use case만 등록한다.
`crawler-job`은 `infrastructure-jpa-crawler`의 crawler 전용 adapter config facade만 직접 알고, 공통 persistence 설정은 adapter 모듈 내부 의존으로 가져온다. Flyway migration 리소스와 web-facing `infrastructure-jpa` 모듈은 런타임 classpath에 올리지 않는다.
`application` 테스트도 `infrastructure-jpa`를 의존하지 않는다. JPA repository나 Spring Data가 필요한 통합 테스트는 `integration-tests` 모듈로 둔다.
`crawler-job` 테스트는 수집 orchestration, parser, runner 경계만 검증한다. 저장 use case와 JPA adapter가 함께 필요한 persistence 통합 테스트는 `integration-tests` 모듈에 둔다.

## Crawler Components

- `CrawlerService`: 전체 목록 순회, 상세 재수집 판단, 실패/열화 집계를 조율한다.
- `DanawaClient`: HTTP 요청, 재시도, pacing, 전역 cooldown을 담당한다.
- `ListPageCrawler`: 목록 페이지 요청 컨텍스트와 상품 카드 배치를 만든다.
- `DetailCrawler`: 상세 페이지와 상세 스펙 테이블을 가져오고 파싱한다.
- `LaptopSnapshotMerger`: 파싱된 스펙을 application crawler command로 조립한다.
- `LoadExistingCrawledLaptopLookupUseCase`: 목록 페이지 단위의 기존 상품 batch lookup을 읽기 트랜잭션으로 처리한다.
- `SaveCrawledLaptopUseCase`: 노트북 저장, 프로필 동기화, 가격 이력 기록, 추천 점수 갱신을 application-crawler 트랜잭션으로 묶는다.
- `TrackCrawlerRunUseCase`: `crawler_run` 실행 이력을 application-crawler port를 통해 기록한다.
- `CrawlerRunLockPort`: 중복 실행 차단 계약을 application-crawler에 두고, `infrastructure-jpa-crawler`의 PostgreSQL advisory lock adapter가 구현한다.

## Operations Surface

- Public user routes and REST DTOs stay unchanged.
- Recommendation pagination uses the application `PageQuery` contract as the single source of the default and maximum page size. Web adapters clamp public requests to that bound, and the use case rejects oversized direct calls before persistence.
- Recommendation screen-size selection keeps explicit semantics: `SELECT` requires at least one selected size, while omitted mode with no selected size resolves to `ANY`.
- `/actuator/health` is the only new operational endpoint and should be checked from localhost or internal server paths.
- Public crawler HTTP APIs are not implemented in `web-app`. GitHub Actions `crawler-job` is the official crawler execution path.
- Web deploy workflow runs are serialized instead of cancelled in progress, so release symlink switching and failed health rollback can finish deterministically.
- Successful web deploys prune old release directories while preserving the active release, the previous rollback target, and the newest 5 releases.
- Schema changes are managed by Flyway through the web deploy path. `crawler-job` does not carry Flyway migration resources or run Flyway migrations against production; it validates the mapped schema through JPA and fails before crawling when the schema is not ready.
- Crawler workflow validates the persistence path against a local PostgreSQL service before the production crawl, so DB write regressions are caught before the SSH tunnel is used for production writes.
- Crawler workflow runs `ops/sql/crawler-identity-preflight.sql` against the production DB tunnel before executing the crawler jar. Duplicate normalized `laptop.product_code` or `laptop.detail_page` groups must be diagnosed with `ops/sql/crawler-identity-diagnostics.sql` and cleaned before crawling.
- Production crawler datasource environment variables are scoped to the DB tunnel verification and crawler execution steps; test and build steps must not inherit the production tunnel datasource.
- Crawler numeric inputs must be positive integers when provided. The startup runner validates crawler request and tuning configuration before advisory lock acquisition, crawling, or `crawler_run` creation.
- Deploy profile keeps `baseline-on-migrate=false`; use `legacy-baseline` or `SPRING_FLYWAY_BASELINE_ON_MIGRATE=true` only for one-time legacy onboarding.
