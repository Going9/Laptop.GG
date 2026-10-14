# LaptopGG UI 검사

실제 Spring MVC/Thymeleaf 서버를 Playwright Test로 검사한다. React나 프런트 개발 서버는 사용하지 않는다. 루트에서 명령을 실행한다.

## 설치와 서버

JDK 17, Node 24/npm, Ubuntu 24.04 기준. 다른 OS에서도 배치/상호작용은 실행할 수 있으나 Linux 시각 기준을 갱신하지 않는다.

```bash
npm ci
npx playwright install --with-deps chromium webkit
npm run test:ui:server
```

`npm install --save-dev --save-exact @playwright/test@1.64.0`으로 구성했고 lockfile을 고정했다. 설치와 실제 브라우저 검사는 로컬 Ubuntu 24.04에서 수행했다.

수동 서버는 `http://127.0.0.1:18080`에서 동작한다. 준비 확인은 `curl -fsS http://127.0.0.1:18080/actuator/health/readiness`. 종료는 실행 터미널의 Ctrl+C. 종료 후 같은 curl이 연결 실패해야 한다. 서버 래퍼가 Gradle/JVM 프로세스 그룹에 종료 신호를 전달한다.

Fast/Full 명령은 서버 실행·readiness 확인·종료를 자동 처리한다. 수동 서버를 종료한 뒤 실행한다. 다른 서버를 재사용하지 않으며 18080이 이미 사용 중이면 실패한다. DB는 기동 시 생성되고 프로세스 종료 시 사라진다. 같은 작업 디렉터리에서 UI 서버 시작과 별도 Gradle 빌드를 동시에 실행하지 않는다. 서로 생성 중인 Kotlin 클래스/JAR를 읽어 컴파일 실패가 발생할 수 있다.

## 검사 선택: Fast / 대표 / Full

```bash
npm run test:ui:fast
npm run test:ui:check
npm run test:ui:full
npm run test:ui:fast -- --grep 'form:'
npm run test:ui:full -- --grep 'results:' --project=webkit-390
```

Fast는 로컬 개발용 배치·상호작용 검사다. 기본 범위는 모든 대상 화면과 상태, 주요 상호작용. Chromium 360×800, 390×844, 1440×900. 한 화면만 바꿨을 때 `--grep`으로 선택 가능하다. 공통 CSS/fragment/JS 변경은 영향 화면을 확인하고 필요하면 전체 Fast를 실행한다. 문서·UI와 무관한 변경에는 로컬 UI 검사를 강제하지 않는다.

대표 검사(`test:ui:check`)는 Chromium/WebKit의 360×800·1440×900에서 36개 배치·상호작용·시각 비교를 실행한다. 공통 UI 변경과 CI의 기본이다. `-- --grep 'form:'` 등으로 영향 시나리오를 선택할 수 있다.

Full은 대규모 개편, 브라우저 지원·폰트·레이아웃 체계 변경, 광범위한 기준 갱신 시 이유를 밝히고 선택하는 확장 검사다. 매 변경·배포마다 의무가 아니다. Chromium/WebKit 각각 360×800, 375×812, 390×844, 430×932, 768×1024, 1440×900. 1440 프로젝트의 stress 테스트에서 599/600, 899/900 경계를 입력·결과·상세에 추가 검사한다. 전체 배치·상호작용과 검토된 기준 이미지 비교를 수행한다.

기본 실행은 Fast 27개, 대표 36개, Full 108개다. 기능·배치·보안 관련 assertion은 유지하며 범위만 선택한다. `--grep 'detail:|missing:'`, `'error:'`, `'empty:'`, `'comments:'`, `'stress:'`도 지원한다. DOM 준비·폰트 준비·이미지 로드·assertion과 Playwright 자동 대기를 사용하며 고정 sleep이 없다.

## 페이지·상태와 준비 조건

| 화면 / 상태 | 진입 / 준비 | 검증 |
| --- | --- | --- |
| 추천 입력 | GET `/`, `/recommends` | 직접 입력, 빠른 선택, radio/checkbox, 비활성/선택, 레이블, 본문 건너뛰기, 키보드 초점, 필수/범위 검증, 제출 |
| 추천 결과 / 많은 후보 | 실제 POST `/recommends`, 기본 조건 | 25개 후보, 10개 카드, 5개 정렬, 가격/무게 순서, 이전/다음 페이지, 입력 조건 유지, 상세 링크 |
| 조건 수정 | 결과의 GET form | 예산·무게·화면·용도 재표시, 다시 제출 |
| 상세 / 긴 제품명 | GET `/laptops/1` | 이미지 비율, 원화 가격, 긴 한글/영문 제목, 사양 분류, 주요 행동, 댓글 빈 상태 |
| 누락 | GET `/laptops/26` | 이미지 없음, 가격/CPU/화면/무게 등 미확인, 로컬 이미지 대체 |
| 이미지 요청 실패 | 상세 2의 image src를 없는 로컬 주소로 변경 | error 이벤트 처리와 비율 유지 |
| 빈 결과 | 예산 100000원·무게 0.1kg 실제 제출 | 카드 없음, 안내·재조정, 조건 유지 |
| 404 | GET `/laptops/999999` | 상태 코드, 오류 안내, 복귀 |
| 400 | GET `/laptops/invalid-id` | 바인딩 검증 오류·복귀 |
| 폼 검증 | 빈 예산 제출·상한 초과 입력 | 제출 차단, validity, 입력 초점, 오류 테두리; 의도한 브라우저 안내 말풍선은 Escape/Tab으로 초점을 옮긴 뒤 다른 컨트롤 hit-test |
| 댓글 / 403 | 프로젝트별 상세 2~13 | 255자 실제 DB 생성, 256자 생성/수정 요청 400 거부와 데이터 보존, 잘못된 비밀번호 거부, 수정·새로고침 후 유지, 삭제·새로고침 후 없음, 수정 details의 Enter 열기/닫기 |
| 긴 이유·URL / 경계 | 실제 결과 후 reason DOM 스트레스 + 599/600, 899/900 | 줄바꿈·배치·가림·가로 넘침 |

`layout.js`는 문서 가로 넘침, 의미 있는 형제 카드/컨트롤 겹침, 주요 텍스트·버튼 잘림, 주요 행동 hit-test/44px 터치 영역, 이미지 비율을 검사한다. label/input 같은 의도한 중첩이나 모든 DOM 쌍을 비교하지 않는다.

## 격리와 통합 범위

`web-app/src/test/kotlin/going9/laptopgg/ui/UiTestServer.kt`는 테스트 클래스패스에서만 실행된다. 기존 `application-test.yml`을 재사용하며 JVM 인자로 loopback/port/H2 driver/url/계정/create-drop/Flyway off를 강제한다. DataSource URL도 fixture 삽입 전에 검사한다. 앱의 web 전용 클래스패스에 크롤러가 없다. 운영 환경변수와 JVM 주입 변수를 래퍼에서 제거한다.

26개 합성 노트북·profile·recommendation_score를 서버 측 JDBC로 생성한다. 실제 추천 서비스·JPA query·Thymeleaf를 사용한다. DB 스키마를 UI 작업에 변경하지 않는다. 이미지와 구매 링크는 loopback fixture, 폰트는 저장소의 로컬 OFL 폰트다. 브라우저의 loopback 외 요청은 차단한다. 메일/유료 API/외부 쓰기/크롤러 호출이 없다.

댓글은 프로젝트별 다른 노트북 ID로 충돌을 피하고 기존 실제 컨트롤러·Bcrypt·DB 저장 로직을 사용한다. 기존 저장 한도 255자를 입력창과 공통 생성/수정 validator에 맞추고 경계값을 실제 저장하여 확인한다. 매번 생성한 비밀번호를 코드/로그/Git에 저장하지 않는다. HTTP 본문이 남는 trace는 댓글 파일에서만 끈다. 시각 캡처는 비밀번호 입력 전 또는 오류/삭제 후 화면만 사용한다.

긴 추천 이유·오류 안내·URL은 별도로 DOM을 늘리는 배치 스트레스 검사다. 서버의 초기 HTML 데이터를 browser API mocking으로 바꿨다고 주장하지 않는다. 결과·상세·오류의 시각 기준은 고정 서버 fixture이고, 추천 제출·정렬·이동·댓글은 실제 서버/H2 통합 검사다. H2 검사는 운영 PostgreSQL 데이터나 실제 휴대폰 검사를 대신하지 않는다.

## 실패 분석과 증거

```bash
npx playwright show-report output/ui/report
npx playwright show-trace output/ui/results/<실패한-테스트>/trace.zip
```

`output/ui/results.json`, HTML report, 실패 screenshot, error-context, trace가 생성된다. 별도 실행 전에 보존하려면 해당 디렉터리를 `output/ui/<실행명>` 아래 복사한다. trace가 없는 댓글 실패는 screenshot/DOM 상태/서버 로그로 분석한다. 실행 기록과 이전 실패 증거의 경로는 `docs/ui-verification.md`에 있다.

스크린샷과 DOM 좌표로 실제 가림/넘침 원인을 찾고, trace에서 URL·제출 파라미터·응답 상태를 확인한다. 원인을 수정한 후 영향 검사를 먼저 다시 실행한다. 새 변경·실패·미해결 우려 없이 성공한 전체 검사를 반복하지 않는다. assertions 삭제·skip·비교 허용치 증가로 실패를 숨기지 않는다. 네이티브 검증 말풍선처럼 의도된 중첩은 먼저 해당 상태를 확인한 뒤 Escape 및 Tab으로 다음 컨트롤에 초점을 옮겨 닫는다.

## 기준 생성·갱신·검토

일반 실행은 `updateSnapshots: 'none'`이므로 기준을 만들거나 덮어쓰지 않는다. 기준은 `tests/ui/baselines/<browser-width>/`에 Git으로 보관한다. 모든 기준은 fullPage이고 마스킹하지 않는다. 비교 허용치를 임의로 추가하지 않는다.

```bash
# 배치·상호작용 검사를 먼저 통과시킨 후보 캡처 (시각 비교만 일시 분리)
UI_LAYOUT_ONLY=1 UI_CAPTURE=1 npm run test:ui:full
# output/ui/candidates의 화면 직접 검토 후 초기 기준 생성
npm run test:ui:full -- --update-snapshots=all
# 의도한 단일 화면/브라우저만 갱신할 때
npm run test:ui:full -- --grep 'detail:' --project=webkit-390 --update-snapshots=all
# 변경된 PNG diff 검토 후 해당 시각 비교를 갱신 없이 재실행
git diff --stat -- tests/ui/baselines
npm run test:ui:full -- --grep 'detail:' --project=webkit-390
# 광범위한 변경이면 전체 Full로 확대
```

이번 초기 기준 생성에서 사용한 절차다. `UI_LAYOUT_ONLY`는 후보 검토를 위한 로컬 절차이며 완료 검사나 일반 CI 명령에 사용하지 않는다. 폰트는 `static/fonts/laptopgg-sans.woff2`로 고정하고 모든 glyph를 유지했다. 라이선스/변환 출처도 해당 디렉터리에 보관한다.

Playwright 1.64.0, Chromium 156, WebKit 27.2, Linux Ubuntu 24.04에서 기준을 생성했다. CI도 ubuntu-24.04와 동일 lockfile/브라우저/로컬 폰트를 사용한다. OS/브라우저/폰트 차이는 [Playwright 공식 시각 비교 문서](https://playwright.dev/docs/test-snapshots)에 설명되어 있다. 다른 환경의 차이를 허용치로 숨기지 말고 동일 환경에서 재현한다.

## CI와 로컬 차이

`.github/workflows/ui.yml`은 재사용 workflow이며 `ci.yml`이 한 번 호출한다. CI 기본은 대표 36개이며 수동 실행의 `full=true`로 108개 Full을 선택한다. CI의 `deploy` job은 `needs: [ui, test]`로 선택한 UI 검사와 서버 검사가 모두 성공한 뒤에만 `deploy-web.yml`을 호출한다. 배포는 같은 workflow run의 `web-release-<sha>` artifact를 내려받아 사용하며 검사·DB 준비·JAR 빌드를 반복하지 않는다. main push와 CI workflow의 수동 실행이 같은 경로를 따른다. 수동 배포는 `gh workflow run ci.yml --ref main`으로 실행한다. 넓은 위험이 있을 때는 `gh workflow run ci.yml --ref main -f full=true`를 사용한다. feature 브랜치를 `--ref`로 지정하면 검사만 수행하고 배포하지 않는다. PR에서는 배포·운영용 artifact 업로드가 실행되지 않는다. UI는 H2라 별도 DB service가 필요 없고, 서버 단위/PostgreSQL 통합 테스트는 기존 job의 PostgreSQL 16 service DB를 사용한다. 일반 CI는 기준 갱신 인자가 없다.

실패 시 HTML·스크린샷·trace·JSON을 `ui-checks-<run_id>` artifact (기존 실행은 `ui-full-<run_id>`)에 14일 보관한다. 로컬 `output/`은 Git에 넣지 않는다. 로컬은 증거를 수동 보관하고 CI는 자동 artifact 업로드한다.

2026-10-14 GitHub API 조회에서 main branch protection은 `Branch not protected`(404), rulesets는 `[]`였다. PR 병합 차단을 보장하지 않는다. 관리자가 이 변경으로 실행되는 `UI Checks / UI Checks` 체크(새 설정의 예상 이름, 원격 실행으로 확인)와 기존 `test` 체크를 required로 지정하고, 우회/관리자 정책을 검토해야 한다. 실제 Actions 실행의 체크 이름을 확인한 뒤 지정한다. 후속 요청으로 원격 push·병합·배포를 실행했다. 보호 설정은 변경하지 않았다. 실행 결과는 `docs/ui-verification.md` 및 `output/deploy/record.md`에 기록한다.
