# 시각 기준 검토 기록

2026-10-14, `feat/modern-ui-validation`. 기준 환경: Ubuntu 24.04 x86_64,
Playwright 1.64.0, Chromium 156.0.8078.4, WebKit 27.2, JDK 17, 저장소 로컬 폰트.
원래 체크아웃 기준 커밋: `262d7f6b77324e20f081a826773ee1e359f05551`.

서버 측 H2 fixture를 대상으로 배치/상호작용 Full 108개가 통과했다.
모바일 예산 빠른 선택 줄 정렬을 검토 후 보정하고 입력/stress 24개,
최종 입력 12개를 추가 통과시켰다. 폰트 실제 로드 검사, 이미지 준비,
가로 넘침, 형제 컨트롤/카드 겹침, 텍스트 잘림, hit-test를 유지했다.

각 상태의 Chromium/WebKit 6개 너비 화면을 모아 직접 검토했다:
`form`, `form-validation`, `results`, `detail`, `missing`, `empty`,
`not-found`, `invalid`, `comment-edit`, `forbidden`.
검토 대상은 각 화면의 전체 페이지 이미지였으며, 모바일/데스크톱
대표 이미지는 원본 크기로도 열어 제품명·가격·선택·댓글 배치를 확인했다.
검토용 후보와 모음은 `output/ui/candidates/`, `output/ui/candidates/review/`.

원본 폰트의 빈 VTT 테이블 때문에 Chromium이 폰트를 거부하는 문제를
실제 로드 assertion과 trace로 발견하고 수정했다. 누락 이미지 SVG의
한글도 같은 폰트의 윤곽선으로 만들어 OS 폰트에 의존하지 않는다.
네이티브 필수 입력 안내는 먼저 validity/초점을 확인하고 Escape/Tab으로
다음 컨트롤에 이동한 뒤 다른 컨트롤의 가림을 검사한다.

검토 후에만 명시적 `--update-snapshots=all`로 초기 기준을 생성한다.
일반 실행/CI는 `updateSnapshots:none`이다. 이후 수정은 필요한 화면과
프로젝트만 선택해 PNG diff를 검토하고 일반 Full을 다시 통과시킨다.
초기 기준 120개를 생성했고, 후보와 픽셀 차이가 있던 4개 이미지도 별도로 검토했다.
차이는 네이티브 선택 표시의 색상과 우연한 hover였다. 캡처 전 포인터를
좌측 위 빈 지점으로 옮겨 hover를 통제하고 후보에도 animations:disabled를
동일하게 적용한다. 초점·선택·오류 영역은 그대로 비교한다.
이번 생성 결과와 최종 시각 비교 결과는 `docs/ui-verification.md` 참조.

댓글 저장 한도 확인 후 생성/수정 입력 레이블·maxlength를 기존 DB와 같은
255자로 맞췄다. 255자 저장/수정, 256자 요청 거부와 데이터 보존을 포함한
영향 상태 36개가 통과했고 상세/누락/댓글 펼침 후보를 두 브라우저의 모든
너비에서 다시 검토했다. 해당 상태만 선택하여 기준을 갱신한다.

중립 포인터로 비교했을 때 Chromium 375/390의 form-validation에서만
이전 hover 스핀 버튼과 40픽셀 차이가 남았다. expected/actual/diff를
직접 확인하고 --update-snapshots=changed로 이 두 PNG만 갱신했다.
일반 Full 재실행 전후 해시를 확인하여 자동 기준 갱신이 없음을 검사한다.

최종 일반 Full: 108 성공 / 실패 0 / skip 0. 실행 전후 기준 120개의 SHA-256이
모두 같았고 테스트 서버와 임시 DB 종료도 확인했다.
