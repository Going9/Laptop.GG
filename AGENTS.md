# LaptopGG 개발 규칙

## 실제 구성과 공통 UI

Kotlin 1.9.25, Spring Boot 3.5.14, JDK 17, Gradle, Thymeleaf, 일반 CSS/JavaScript.
React나 UI 라이브러리로 전환하지 않는다. 공통 스타일은 `web-app/src/main/resources/static/styles.css`, 공통 head/header/hidden fields/product image는 `templates/fragments/layout.html`에 있다. 페이지는 `recommendation-form.html`, `recommendation-list.html`, `laptop-detail.html`, `error/application-error.html`. 입력 동작은 `recommendation-form.js`, 이미지 대체는 `ui.js`.

## UI 변경 전 확인

- 루트와 해당 하위 AGENTS.md를 읽고 실제 체크아웃을 기준으로 조사한다.
- 공통 파일의 모든 호출부, 화면, 테스트, fixture, 배포 workflow까지 영향을 확인한다.
- 요청 파라미터, `th:object`/`th:field`, JavaScript의 ID/data 속성을 함께 확인한다. 폼은 최상위 `${laptopRecommendationRequest}`에 바인딩한다. 중첩 모델은 field 이름에 접두사를 만들어 서버 바인딩을 깨뜨릴 수 있다.
- 추천 로직, DB 스키마, 크롤러를 UI 변경에 섞지 않는다. 사용자 동작·검증·오류 처리를 보존한다. 댓글 내용은 기존 DB 한도 255자와 입력창/공통 생성·수정 validator가 일치해야 한다. 경계값 저장과 초과 요청 거부를 검사한다.
- 별도 브랜치에서 작업한다. 승인 없이 main push/병합/배포하지 않는다.

## 디자인·반응형·접근성

`docs/ui-design.md` 기준과 기존 CSS 변수를 사용한다. 흰색·중립색과 절제된 강조색, 정상 한글 자간, 일관된 간격·글자 크기·44px 컨트롤·작은 모서리를 유지한다. 중복/미사용 CSS는 삭제하며 덮어쓰기, 불필요한 `!important`, 고정 높이, absolute 배치로 문제를 가리지 않는다.

모바일 우선 360~1440px. 페이지 `overflow-x:hidden` 금지. 유동 열, `min-width:0`, 긴 제목·URL 줄바꿈으로 해결한다. 필요한 표 등의 내부 스크롤만 허용한다. 이미지 비율과 `object-fit:contain`을 유지하고 누락/실패 이미지를 처리한다.

레이블, 실제 radio/checkbox, 선택/비활성/오류 상태, 명확한 `:focus-visible`, 키보드 조작, 본문 건너뛰기를 유지한다. hover만으로 필수 정보를 전달하지 않는다. 모달/팝오버 추가 시 Escape, 초점 이동·복귀, 열기·닫기 검사를 함께 만든다. 장식 애니메이션은 피하고 필요한 모션은 `prefers-reduced-motion`을 지원한다.

## 실행 기준

변경 영향과 위험에 맞는 최소한의 검사를 선택한다. 공통 파일 수정이나 배포 자체를 이유로 Full을 자동 의무화하지 않는다.

- 문서·주석·UI에 영향 없는 변경: 관련 정적 검사 또는 서버 테스트만 수행한다.
- 한 화면의 문구·스타일·동작: 영향 화면의 Fast(`-- --grep 'detail:'` 등)를 수행한다. 시각 변경은 해당 기준만 비교하고 모바일·데스크톱을 직접 확인한다.
- 공통 CSS/fragment/입력 동작이나 여러 화면 변경: 전체 Fast와 영향 화면의 WebKit·시각 비교·관련 breakpoint를 검사한다. `npm run test:ui:check`는 Chromium/WebKit의 360×800·1440×900 대표 36개 검사이며 시각 비교를 포함한다. 필요한 부분은 `--grep`으로 선택한다.
- 대규모 UI 개편, 브라우저 지원·폰트·레이아웃 체계 변경, 광범위한 기준 이미지 갱신: 이유를 짧게 밝히고 `npm run test:ui:full`의 확장 108개 검사를 선택한다. Chromium/WebKit 6개 너비와 599/600, 899/900 경계를 검사한다.
- 서버 변경은 관련 Gradle 테스트/빌드로 검증한다. 데이터 저장·인증·입력 검증·접근성·핵심 사용자 동작은 비용 때문에 생략하지 않는다. 기능 검증과 화면 크기별 배치 검증을 구분한다.
- 코드 변경·새 실패·미해결 우려 없이 성공한 검사를 반복하지 않는다. 동일 결과물을 CI에서 검증했다면 같은 전체 검사를 로컬에서 다시 할 필요는 없다. 실패 수정 후에는 영향 검사부터 재실행하고 넓은 영향이 확인될 때만 확대한다.
- assertion과 자동 대기로 성공 조건을 증명한다. 고정 sleep과 모든 DOM 쌍의 겹침 비교는 금지하고 의미 있는 형제 카드·컨트롤·hit-test로 제한한다.

## 브라우저와 실패 처리

UI를 바꾼 화면은 Playwright MCP 또는 실제 Playwright 브라우저로 대표 모바일·데스크톱에서 확인한다. 긴/누락/빈/오류 상태는 변경과 관련된 상태를 선택한다. 이미 확인한 화면과 기준 이미지를 새 이유 없이 전부 재검토하지 않는다. 에뮬레이션을 실제 휴대폰 검사라고 보고하지 않는다.

실패하면 보고서 요약부터 읽고 실패한 항목의 스크린샷·DOM 좌표·trace로 원인을 확인한다. 필요한 assertion을 삭제하거나 비교 허용치를 늘려 실패를 숨기지 않는다. 오탐은 의도한 중첩·내부 스크롤과 근거를 문서화한 뒤 대상을 조정한다.

## 격리와 기준 이미지

UI 테스트는 `:web-app:uiServer` 테스트 클래스패스, loopback:18080, 생성/종료 시 사라지는 H2와 서버 측 합성 데이터만 사용한다. 운영 DB/API/계정/크롤러/메일/유료 API는 사용하지 않는다. 외부 상품 이미지·폰트는 로컬 fixture/폰트로 통제하고 운영 환경변수를 상속하지 않는다. 브라우저 API mock으로 서버 초기 HTML을 대체했다고 가정하지 않는다.

비밀번호/세션/토큰을 코드·로그·Git·스크린샷에 남기지 않는다. 댓글 테스트 자격정보는 매번 생성하고 요청 본문이 기록되는 trace를 끈다. 테스트 시작 시 새 DB와 fixture가 생성되고 종료 시 프로세스 그룹과 DB가 정리된다. 기존 서버 재사용을 금지한다.

시각 기준은 배치·상호작용이 통과한 합성 화면을 직접 검토한 뒤 `tests/ui/baselines`에 보관한다. 일반 실행과 CI는 갱신하지 않는다(`updateSnapshots:none`). 갱신은 의도한 화면·프로젝트만 선택하고 diff를 검토한다. 갱신 후 변경한 기준의 시각 비교를 다시 실행한다. 전체 UI에 영향이 있을 때만 Full로 확대한다. Ubuntu 24.04, package-lock의 Playwright 버전, 저장소 로컬 폰트 환경을 일치시킨다. macOS/Windows 기준을 Linux 기준 위에 덮어쓰지 않는다.

## 완료 보고

변경 화면/동작·디자인 결정·주요 파일, 실제 명령·성공/실패/skip 수, 브라우저/뷰포트, 직접 시각 검토한 화면, MCP 결과/미사용 이유, 보고서/스크린샷/trace 위치, CI와 실제 배포 의존성, 미검증 사항을 보고한다. 고정 fixture 화면과 실제 DB에 제출/저장한 통합 동작을 구분한다. 실행하지 못한 검사를 성공이라고 하지 않는다. 외부 branch protection은 확인 없이 보장하지 않는다.

정적 CSS·JavaScript·폰트는 Spring content resource chain과 Thymeleaf `@{...}`로
내용 해시 URL을 생성한다. 공통 템플릿에 원본 정적 파일 경로를 직접 쓰지 말고,
CSS 안의 폰트 URL과 preload URL이 같은 버전인지 검사한다. 배포 후 CDN을 거친
공개 브라우저에서 리소스 반영도 확인한다.

CI 기본 UI 검사는 대표 36개이며 Full은 수동 실행의 `full=true`로 선택한다. 동일 커밋의 선택한 UI 검사·서버 검사·빌드를 한 번 실행한다. 배포 job은 두 검사에 의존하고 같은 run의 검증된 JAR artifact를 사용한다. 배포에 동일 검사를 다시 추가하지 않는다. 수동 배포도 CI workflow의 main 실행을 사용한다.
