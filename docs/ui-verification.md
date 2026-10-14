# UI 개편 실행 기록

2026-10-14, 브랜치 `feat/modern-ui-validation`.
원래 체크아웃 main: `262d7f6b77324e20f081a826773ee1e359f05551`.
원격 push·병합·운영 배포를 실행하지 않았다. 기존 미추적 `logs_83298507218/`는 건드리지 않았다.

## 조사와 변경 범위

루트/상위/하위 AGENTS.md를 검색했으나 기존 파일이 없어 루트 규칙을 새로 작성했다.
Kotlin 1.9.25, Spring Boot 3.5.14, JDK 17, Gradle, Thymeleaf, 일반 CSS/JavaScript.
기존 Playwright/npm 설정은 없었다. 운영 로컬 실행은 README의 PostgreSQL compose와
`:web-app:bootRun --args='--spring.profiles.active=postgres'` 경로다.
이번 브라우저 검사는 운영 설정 대신 테스트 전용 `:web-app:uiServer`를 실행했다.

조사한 화면: GET `/`/`/recommends`, POST `/recommends`, GET `/laptops/{id}`,
POST `/comments`, `/comments/{id}/edit`, `/comments/{id}/delete`, 공통 application-error.
추천 조건, 정렬과 페이지 이동은 POST+hidden fields이며 댓글 CRUD는 실제 서버 form이다.
공통 fragment와 CSS가 입력/결과/상세/오류에 영향을 준다. 기존 테스트, 모듈 경계,
PostgreSQL/Flyway 검사, CI, 웹 배포, 독립 크롤러 workflow도 확인했다.

## 디자인과 파일

흰색/중립 회색과 절제된 청색, 로컬 한글 폰트, 44px 컨트롤, 작은 모서리,
공통 간격/타입 토큰을 사용한다. 큰 소개·블러·그라데이션·장식 애니메이션·그림자를
정리했다. CSS는 1,925줄에서 약 165줄의 단일 규칙 집합으로 줄였다.

입력은 직접 입력과 빠른 선택을 노출하고 선택 상태/fieldset/실제 radio와 checkbox를
유지했다. 결과는 제품명·가격·추천 이유·프로세서/무게/화면 순서와 같은 이미지 비율을
공유한다. 상세는 주요 정보와 행동을 먼저 보이고 사양을 분류한다. 헤더·빈 상태·오류도
같은 컴포넌트를 사용한다. 긴 제목/URL이 줄바꿈되고 누락/실패 이미지는 대체된다.

주요 파일:

- `web-app/src/main/resources/static/styles.css`, `recommendation-form.js`, `ui.js`, `image-placeholder.svg`, `fonts/`
- `web-app/src/main/resources/templates/fragments/layout.html`, `recommendation-form.html`, `recommendation-list.html`, `laptop-detail.html`, `error/application-error.html`
- `web-app/src/main/kotlin/going9/laptopgg/web/controller/RecommendationPageController.kt`, `RecommendationController.kt`, `web/view/RecommendationPageModelFactory.kt`
- `application/.../comment/CommentCommandValidator.kt` 및 기존 `CommentUseCaseTest.kt`
- `web-app/build.gradle.kts`, `web-app/src/test/kotlin/going9/laptopgg/ui/UiTestServer.kt`, `web-app/src/test/resources/static/ui/laptop.svg`, `RecommendationFormRenderingTest.kt`와 기존 controller tests
- `package.json`, `package-lock.json`, `tests/ui/playwright.config.js`, `layout.js`, `pages.spec.js`, `comments.spec.js`, `baselines/`, `BASELINES.md`, `README.md`
- `AGENTS.md`, `docs/ui-design.md`, `.github/workflows/ui.yml`, `ci.yml`, `deploy-web.yml`

발견하여 수정한 연동 문제:

1. 중첩 `th:object`에서 field 이름에 `laptopRecommendationRequest.` 접두사가 붙어 실제 입력값이 무시되거나 SELECT 제출이 400으로 실패했다. 최상위 요청 모델에 바인딩한다.
2. 단일 `sort=price,desc`를 Spring List 바인딩이 쉼표로 나눠 내림차순을 잃었다. page/API 모두 MultiValueMap에서 원본 정렬 값을 읽는다. URL 계약과 공통 page parser는 유지한다.
3. 조건 수정이 초기값으로 돌아가던 경로를 GET form 바인딩으로 바꾸고 화면/용도/예산/무게를 보존한다. 빈 결과의 재조정도 같은 조건을 유지한다.
4. 댓글 입력 허용 500자와 실제 varchar(255) 저장 한도가 달랐다. 입력/수정 레이블·maxlength와 공통 add/update validator를 255자로 맞춘다. 256자 요청은 DB 쓰기 전 400으로 거부한다. DB 스키마는 바꾸지 않는다.
5. 원본 폰트의 빈 TSI3 테이블을 Chromium이 거부했다. 선택적 VTT 메타데이터를 제거하고 두 브라우저의 실제 폰트 로드를 assertion으로 확인한다.

## 실제 실행 명령

```bash
npm install --save-dev --save-exact @playwright/test@1.64.0
npm ci
npx playwright install --with-deps chromium webkit
node tests/ui/server.mjs
npm run test:ui:fast
UI_LAYOUT_ONLY=1 UI_CAPTURE=1 npm run test:ui:full
UI_LAYOUT_ONLY=1 UI_CAPTURE=1 npm run test:ui:full -- --grep 'form:|stress:'
UI_LAYOUT_ONLY=1 UI_CAPTURE=1 npm run test:ui:full -- --grep 'form:'
npm run test:ui:full -- --update-snapshots=all
UI_LAYOUT_ONLY=1 UI_CAPTURE=1 npm run test:ui:full -- --grep 'detail:|missing:|comments:'
npm run test:ui:full -- --grep 'detail:|missing:|comments:' --update-snapshots=all
npm run test:ui:full -- --grep 'form:' --project=chromium-375 --project=chromium-390 --update-snapshots=changed
UI_CAPTURE=1 npm run test:ui:full
```

실행 시 JDK 17을 선택하는 명령 앞에 `JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64`를
지정했다. 로컬 첫 빌드는 기본 JDK 21에서도 수행했지만 최종 서버/기준 검사는 JDK 17이다.
최종 Fast 27/0/0, Full 108/0/0이며 의도한 기준 갱신과 이전 실패 결과는 아래 실행 표에 기록한다. 일반 Full에서 120개 기준 PNG의 SHA-256이 모두 유지됨을 확인했다. 테스트 서버 readiness 연결 종료와 임시 PostgreSQL 컨테이너 삭제도 확인했다.

격리된 PostgreSQL 16 컨테이너는 다음과 같이 임시 loopback 포트에서 시작했다.
운영 DB나 기존 로컬 DB를 재사용하지 않았다. 종료 시 `--rm`으로 삭제했다.

```bash
docker run --rm -d --name laptopgg-ui-postgres \
  -e POSTGRES_DB=laptopgg_ui_test -e POSTGRES_HOST_AUTH_METHOD=trust \
  -p 127.0.0.1::5432 postgres:16
UI_PG_PORT=$(docker port laptopgg-ui-postgres 5432 | cut -d: -f2)
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 \
POSTGRES_INTEGRATION_TESTS=true \
POSTGRES_INTEGRATION_JDBC_URL="jdbc:postgresql://127.0.0.1:${UI_PG_PORT}/laptopgg_ui_test" \
POSTGRES_INTEGRATION_USERNAME=postgres POSTGRES_INTEGRATION_PASSWORD= \
./gradlew --no-daemon verifyStructure test :web-app:bootJar :crawler-job:bootJar
docker stop laptopgg-ui-postgres
```

같은 임시 구성으로 `clean verifyStructure test :web-app:bootJar :crawler-job:bootJar`도 실행했다.
마지막 전체 서버 검사는 285개 성공, 실패 0, skip 0이며 모듈 경계와 두 JAR 빌드가 통과했다.
XML 합계는 `output/ui/server-test-counts.json`, 전체 로그는 `output/ui/gradle-postgres-final.log`.

`output/tools/actionlint`(1.7.12)로 실제 workflow 전체를 검사하여 오류 0.
`node --check`로 server/config/spec 파일, `git diff --check`로 공백 오류도 검사했다.
운영 JAR zip을 확인하여 UI 서버·fixture 리소스·test 프로필이 포함되지 않음을 확인했다.

## 실패 분석과 재검사

| 실행 | 성공 | 실패 | skip | 원인 / 증거 |
| --- | ---: | ---: | ---: | --- |
| 초기 Fast | 18 | 9 | 0 | 중첩 field 이름 / `output/ui/failed-binding/` |
| 바인딩 수정 후 Fast | 24 | 3 | 0 | 내림차순 파라미터 / `output/ui/failed-sort/` |
| 정렬 수정 후 Fast | 27 | 0 | 0 | 생성/정렬/이동/댓글 통합 통과 |
| 최초 Full 배치 | 102 | 6 | 0 | WebKit 네이티브 필수 입력 안내가 빠른 선택을 가림 / `failed-native-validation/` |
| Escape만 적용한 WebKit 단일 입력 | 0 | 1 | 0 | Tab으로 초점을 옮겨 안내 닫음 / `failed-native-escape/` |
| WebKit 단일 재검사 | 1 | 0 | 0 | 필수 상태·입력 초점 확인 뒤 Escape/Tab |
| 폰트 로드 assertion 추가 후 Full 배치 | 54 | 54 | 0 | Chromium OTS TSI3 decode / `failed-font/` |
| 폰트 수정 후 두 브라우저 입력 | 2 | 0 | 0 | 로컬 한글 폰트 실제 로드 |
| 수정 후 Full 배치 | 108 | 0 | 0 | `full-layout/`, `full-layout-final.log` |
| 입력/긴 오류/경계 추가 재검사 | 24 | 0 | 0 | `form-stress/`, `form-stress-final.log` |
| 최종 빠른 선택 정렬 재검사 | 12 | 0 | 0 | `form-grid/`, `form-grid-final.log` |
| 최초 시각 기준 생성 Full | 108 | 0 | 0 | 120개 기준 생성 / `baseline-generation/` |
| 최초 일반 시각 Full | 108 | 0 | 0 | 갱신 없이 비교 / `full-before-neutral/` |
| 댓글 한도 수정 후 최종 Fast | 27 | 0 | 0 | `fast/`, `fast-final.log` |
| 댓글 한도 영향 상태 후보 | 36 | 0 | 0 | `comment-limit-candidates/`, `comment-limit-candidates.log` |
| 검토 후 영향 상태만 기준 갱신 | 36 | 0 | 0 | `comment-limit-baselines/`, `comment-limit-baselines.log` |
| 중립 포인터 적용 후 일반 Full | 106 | 2 | 0 | 기존 hover 스핀 버튼 차이 / `failed-pointer-neutral/` |
| 의도한 두 검증 기준만 갱신 | 2 | 0 | 0 | 실제 변경 PNG 두 장 확인 / `neutral-pointer-baselines/` |
| 최종 일반 Full (시각 비교 포함) | 108 | 0 | 0 | `full/`, `full-final.log`; 120개 기준 해시 변경 없음 |

초기 설정 단계에서 webServer cwd와 describe 내부 trace 설정이 지원되지 않아 실행 전에
실패한 것도 수정했다. 이런 실행은 테스트 0개이며 성공으로 계산하지 않았다.
Gradle 두 실행을 서로 다른 JDK로 동시에 시작하여 생성 중인 클래스/JAR를 읽는 컴파일
실패도 발생했다. 서버 종료 후 순차 `clean` 빌드로 재검증했다.

실패 스크린샷, DOM 상태/좌표, trace의 실제 POST URL/파라미터/상태를 확인했다.
겹침 assertion을 삭제하거나 허용치를 늘리지 않았다. 네이티브 안내의 의도한 중첩은
먼저 validity와 초점을 증명한 뒤 키보드로 닫고 다른 컨트롤을 검사한다.

## 실제 브라우저·시각 검토

Fast: Chromium 360×800, 390×844, 1440×900.
Full: Chromium 156.0.8078.4와 WebKit 27.2 각각 360×800, 375×812, 390×844,
430×932, 768×1024, 1440×900. 입력/결과/상세는 599/600, 899/900 경계도 검사한다.

Playwright MCP가 사용 가능하여 Chromium 152.0.7977.75로 실제 로컬 테스트 서버를 열었다. 변경 전 모바일/데스크톱
입력·결과·상세·누락·404를 기록하고, 변경 후 360×800 / 1440×900에서 입력·결과·상세·누락·빈 결과·
400·404를 다시 열고 전체 페이지 PNG를 검토했다. 변경 전 입력 360과 긴 상세 제목에서
가로 넘침을 확인했다. 최종 폰트 수정 후 검사한 14개 MCP 화면 모두 가로 넘침이 없고
로컬 한글 폰트가 실제 로드되었다. 최종 결과 JSON은 `output/ui/mcp-final.json`. 재빌드한 동일 URL의 오래된 브라우저 캐시가 남은
중간 확인은 캐시를 비활성화한 재탐색으로 바로잡았다.

자동 캡처한 두 브라우저/6개 너비의 10개 상태 전체 이미지 모음도 직접 검토했다.
`tests/ui/BASELINES.md`가 기준 검토 기록이다. 캡처 전 포인터를 (0,0)으로 옮기고
동일 animations 설정을 사용하여 우연한 hover를 통제한다. 중요한 영역은 마스킹하지 않는다.
이 검사는 브라우저 에뮬레이션이며 실제 휴대폰에서 검사했다고 보고하지 않는다.

고정 fixture의 시각 화면과 실제 추천 POST/정렬/페이지 이동/조건 수정/상세 이동/
댓글 DB 쓰기를 구분한다. 댓글은 실제 255자 생성·수정과 새로고침 확인, 256자 생성/수정
거부와 원본 보존, 비밀번호 오류, 삭제와 재조회까지 확인한다. 긴 추천 이유/오류/URL은
별도의 브라우저 DOM 배치 스트레스이며 서버 초기 HTML을 mocking으로 대체하지 않는다.

## 증거와 CI

- 변경 전/후 MCP PNG: `output/ui/before/`, `output/ui/after/`
- 검토 후보/모음: `output/ui/candidates/`, `output/ui/candidates/review/`
- Git 기준 PNG: `tests/ui/baselines/` (10개 상태 × 두 브라우저 × 6개 너비 = 120개)
- 최종 HTML/JSON/실패 파일: `output/ui/report/`, `output/ui/results.json`, `output/ui/results/`
- 이전 실패 trace/screenshot/error-context: 위 표의 `output/ui/failed-*/results/`
- 댓글 요청의 비밀번호가 trace에 남지 않도록 해당 spec만 trace off. 최종 성공 실행에는 실패 trace가 없다.

재사용 `ui.yml`을 CI와 Deploy Web 모두 호출하고, 배포 job의 `needs: ui`로 Full 실패 시
배포 단계가 실행되지 않도록 연결했다. actionlint 통과. 일반 CI는 기준 갱신 옵션이 없고
항상 report/screenshot/trace/JSON을 artifact에 보관한다.

2026-10-14 GitHub API에서 main 보호 설정은 404 `Branch not protected`, rulesets는 `[]`.
현재 병합 차단을 보장하지 않는다. 관리자가 실제 실행에 표시되는 `UI Full / UI Full`과
기존 `test` 상태를 required로 지정하고 우회 정책을 설정해야 한다. 원격 workflow 실행과
운영 배포는 수행하지 않았으므로 그 실행 결과를 성공으로 보고하지 않는다.

## 남은 검증 범위

실제 휴대폰, 운영 데이터/계정/DB/API, 원격 Actions 실행, 네트워크 성능 측정은 수행하지
않았다. CI의 같은 OS/브라우저/로컬 폰트 구성은 검토했으나 원격 실행으로 확인한 것은 아니다.
운영 크롤러, 추천 알고리즘과 DB 스키마는 변경하지 않았다.
