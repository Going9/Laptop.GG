# 가격 오류와 CPU 추천 점수 개선

## 재현과 원인

2026-10-14 운영 `POST /recommends`를 예산 150만원, 무게 1.5kg, 화면 ANY,
용도 NOT_SURE로 제출하면 첫 결과는 ASUS 젠북 S 14 UX5406SA-PZ429W
(내부 ID 3107, 다나와 상품코드 78236909), 가격은 328,990원이었다.
동일 시점 [다나와 상세](https://prod.danawa.com/info/?pcode=78236909&cate=112758)의
일반 최저가와 `min_price_78236909`는 2,629,000원이었다.

`DanawaProductCardParser`는 `.prod_pricelist .text__number`를 먼저 읽었다.
실제 목록에서 이 요소는 일반 최저가 앞에 있는 빠른 배송 판매 제안에 속한다.
조사 시점에도 이 제안은 2,099,000원으로 일반 최저가와 달랐다.
저장된 328,990원의 과거 제안 HTML은 보관되어 있지 않아 당시 판매처 오류까지
단정할 수는 없다. 같은 가격이 ASUS 7개 상품에 저장되어 있었다.
화면과 API는 DB의 가격을 그대로 표시했고, 낮은 오가격은 예산 적합 점수까지 높였다.

수집 가격은 상품명 링크의 상품코드와 일치하는 `min_price_<상품코드>`로 제한한다.
이 값이 없을 때만 같은 상품코드의 일반 가격 링크를 읽는다.
빠른 배송, 다른 SSD 옵션, 카드 할인 등의 숫자를 대신 사용하지 않는다.
가격이 없는 응답은 기존의 미수집 처리 규칙을 유지한다. 기존 DB 값은 코드 배포만으로
바뀌지 않으므로 수정된 크롤러를 실행해야 한다.

## CPU 추천 계획과 적용

1. 가격 파서와 실제 구조를 반영한 fixture/회귀 테스트를 수정한다.
2. 기존 CPU 성능·전력 효율 점수와 용도별 가중치를 재사용하되 CPU 분류를 개선한다.
3. 저장된 스펙으로 전체 프로필과 용도별 점수 projection을 재계산한다.
4. CI 검증 후 main의 검증된 JAR를 배포하고 가격을 재수집한다.
5. 운영 기본 추천 결과와 다나와 동일 상품 가격, 모바일·데스크톱 브라우저를 확인한다.

기존에는 대부분의 Intel/AMD 모델이 U/H/HS/HX 접미사만으로 같은 점수를 받았다.
Intel Core는 세대와 i3/i5/i7/i9 등급, 3자리 Core/Ultra는 시리즈와 모델 구간을 구분한다.
AMD는 7000 이후 모바일 모델의 아키텍처 자리와 등급, 4000~6000 계열,
접미사 없는 Ryzen/AI/AI Max 모델을 구분한다.
Ryzen AI HX 370은 일반 게이밍 HX보다 앞에서 판별한다.
V/U의 효율과 H/HS/HX의 성능 특성은 별도로 유지한다.
Apple과 Snapdragon의 기존 처리는 유지한다.

CPU 성능은 사무·게임·제작 점수에, 전력 효율은 배터리 점수에 반영된다.
GPU, RAM, 화면, 무게, 배터리 용량, 예산의 기존 가중치는 유지한다.
DB 스키마와 공개 API는 바꾸지 않는다.

점수는 벤치마크 실측치나 배터리 사용 시간이 아닌, 스펙을 바탕으로 정한 상대적 휴리스틱이다.
동일 CPU라도 노트북의 냉각과 전력 제한에 따른 차이는 아직 반영하지 않는다.
모르는 모델에는 기존 fallback을 적용한다. 실측 데이터셋 도입 시 보정이 필요하다.

근거:

- [Intel 노트북 CPU 비교표](https://cdrdv2-public.intel.com/841783/Intel-Core-Comparsion.pdf): 258V와 155H의 코어 구성·기본 전력 차이.
- [AMD 모바일 7000 비교표](https://www.amd.com/content/dam/amd/en/documents/partner-hub/ryzen/amd-ryzen-7000-series-mobile-processors-quick-reference-guide.pdf): 7520U/7530U/7535U/7840U 계열의 아키텍처 차이.
- [AMD AI 300/Max/200 발표](https://ir.amd.com/news-events/press-releases/detail/1232/amd-announces-expanded-consumer-and-commercial-ai-pc-portfolio-at-ces).
- [AMD AI 400 발표](https://ir.amd.com/news-events/press-releases/detail/1270/amd-expands-ai-leadership-across-client-graphics-and-software-with-new-ryzen-ryzen-ai-and-amd-rocm-announcements-at-ces-2026).

## 기존 점수 반영

`Crawl Laptops` 수동 실행에서 `rebuild_profiles=true`를 선택한다.
이 모드는 외부 수집 없이 기존 스펙을 읽고 전체 CPU/용도 점수를 갱신한다.
동일 크롤러 advisory lock을 사용하므로 수집과 동시에 실행하지 않는다.
100개씩 ID를 조회하고 상품별 트랜잭션으로 프로필과 8개 용도 점수를 함께 저장한다.
가격·상품 스펙·댓글·가격 이력은 변경하지 않는다. 중간 실패 후 전체 재실행할 수 있다.

별도 실행이 필요한 경우 검증된 crawler JAR에 다음 인수를 사용한다.

```text
--spring.profiles.active=postgres,crawler
--app.crawler.run-on-startup=false
--app.crawler.rebuild-profiles=true
```

가격 정정은 같은 버전의 크롤러를 일반 모드로 실행한다. UI/시각 기준 변경은 없다.
CI의 대표 UI 검사와 서버 테스트·빌드가 통과한 동일 run의 web JAR를 배포한다.
