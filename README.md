# LaptopGG

## 1. 프로젝트 목표
저는 IT 및 전자기기에 관심이 많고 특히 노트북과 PC하드웨어 관심이 있습니다.
그래서 종종 IT 커뮤니티나 대학 커뮤니티에서 노트북 구입을 고민하는 분들께 추천드리곤 했습니다.
하지만 모든 분들에게 답변해드리기는 어려웠고 제가 노트북을 추천할 때 고려하는 요소들을 반영하는 추천 사이트를 만들기로 했습니다.
LaptopGG는 리그오브레전드의 통계 사이트 OP.GG를 오마주했습니다.

## 2. 프로젝트 환경
- 구형 노트북에 **Ubuntu Server**를 설치하여 서버로 사용
  - CPU: Intel i3-6100 (저전력, TDP 15W)
  - 사양: 2코어 4스레드, 2GB RAM, 256GB SSD
- **iptime 공유기**를 통해 유선 연결 및 공유기 자체적인 **DDNS** 서비스 사용
- **GitHub Actions**를 활용한 CI/CD 환경 구축
- [www.laptopgg.com](https://www.laptopgg.com) 도메인 구입 및 DNS 설정 완료  

## 3. 서버 아키텍처
- **Spring Boot (Kotlin)** + **Thymeleaf** + **PostgreSQL**
  - 화면은 Spring MVC + Thymeleaf로 빠르게 제공하고, 추천/상세 API는 REST 형태로 유지합니다.
  - 웹 서버와 크롤러를 같은 코드베이스 안에서 분리 실행합니다.
  - 운영 기준 권장 구조는 `Oracle micro(웹)` + `GitHub Actions(크롤러)` + `PostgreSQL` 입니다.
- **Spring Boot JPA** + **Kotlin JDSL**
  - ORM으로 **JPA**와 Line사의 오픈소스인 **Kotlin JDSL**을 사용하고 있습니다.
  - **QueryDSL**과 **JOOQ**도 검토했으나, QueryDSL의 유지보수 문제와 JOOQ의 초기 설정 복잡성으로 인해 **Kotlin JDSL**을 최종적으로 선택했습니다.

![laptopgg 아키텍처](https://github.com/user-attachments/assets/e05fe73e-b4ae-43ff-8a24-3603f58d09a4)

## 4. 노트북 추천 방식
**필터링**과 **가중치** 방식을 사용하여 노트북을 추천합니다.

1. **카테고리 설정**  
   노트북 등록 시, 카테고리를 설정합니다. 카테고리는 다음과 같습니다:
   - 사무용
   - 경량 사무용
   - 사무용 + 가끔 롤
   - 크리에이터용
   - 경량형 게이밍
   - 메인스트림 게이밍
   - 헤비급 게이밍

2. **사용자 선택 요소**  
   사용자는 카테고리를 선택하고, 예산, 최대 허용 무게 등의 조건을 설정합니다.

3. **필터링 및 가중치 적용**  
   선택한 카테고리와 조건에 맞추어 노트북 목록을 필터링한 후, 각 특성에 따라 점수를 부여하고 가중치를 적용하여 최적의 노트북을 추천합니다.  
   예시:
   - **4K 해상도**를 기준으로 해당 노트북의 화면 선명도를 평가합니다.
   - **예산 대비 가격 점수**, **무게 점수**, **디스플레이 색 정확도 점수** 등 다양한 요소를 점수화합니다.
   - 예를 들어, **영상 편집용** 노트북을 선택한 경우, **디스플레이의 색 정확도**와 **RAM 용량**이 중요한 요소로 평가되어 가중치가 높게 적용됩니다.

## 5. 현재 상황
- 기본적인 노트북 등록 및 추천 로직은 완성된 상태입니다.
- 아직 상세 페이지는 다듬어야 할 필요가 있습니다.
- 추천 로직의 정교함을 높일 필요가 있으며, 노트북 등록 작업이 예상보다 많은 시간이 소요될 것으로 보입니다.

## 6. 로컬 실행
PostgreSQL 기준으로 로컬에서 바로 테스트하려면 아래 순서로 실행하면 됩니다.

1. 로컬 PostgreSQL 실행
```bash
docker compose -f docker-compose.postgres.yml up -d
```

2. Spring Boot 웹 실행
```bash
cd LaptopGG
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/laptopgg
export SPRING_DATASOURCE_USERNAME=laptopgg
export SPRING_DATASOURCE_PASSWORD=laptopgg
./gradlew bootRun --args='--spring.profiles.active=postgres'
```

3. crawler 모드 단독 실행
```bash
cd LaptopGG
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/laptopgg
export SPRING_DATASOURCE_USERNAME=laptopgg
export SPRING_DATASOURCE_PASSWORD=laptopgg
./gradlew bootRun --args='--spring.profiles.active=postgres,crawler --app.crawler.limit=3'
```

4. 확인할 주소
- 앱: [http://localhost:8080](http://localhost:8080)
- 추천 API: `POST /api/recommends`
- 크롤링 API: `GET /api/crawl/laptops?limit=3`

기본 로컬 DB 연결값은 아래와 같습니다.
- host: `localhost`
- port: `5432`
- database: `laptopgg`
- username: `laptopgg`
- password: `laptopgg`

필요하면 아래 환경 변수로 연결값을 덮어쓸 수 있습니다.
- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`

## 7. 운영 구조
- 웹 서버: `Oracle micro` 에서 `postgres,deploy` 프로필로 실행
- 크롤러: 퍼블릭 레포의 GitHub Actions 에서 `postgres,crawler` 프로필로 실행
- PostgreSQL: 외부 managed DB 또는 별도 Oracle 인스턴스에 배치

GitHub Actions crawler workflow 는 SSH 터널을 통해 PostgreSQL 에 접속하도록 설계되어 있습니다. 필요한 시크릿은 아래와 같습니다.
- `CRAWLER_SSH_HOST`
- `CRAWLER_SSH_PORT`
- `CRAWLER_SSH_USER`
- `CRAWLER_SSH_PRIVATE_KEY`
- `CRAWLER_DB_NAME`
- `CRAWLER_DB_USERNAME`
- `CRAWLER_DB_PASSWORD`
- `CRAWLER_TUNNEL_TARGET_HOST`
- `CRAWLER_TUNNEL_TARGET_PORT`
