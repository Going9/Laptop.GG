# 베이스 이미지 설정
FROM ubuntu:22.04 as builder

# 필수 패키지 설치
RUN apt-get update && apt-get install -y \
    openjdk-17-jdk \
    curl \
    unzip

# Gradle 설치
RUN curl -s "https://get.sdkman.io" | bash \
    && source "$HOME/.sdkman/bin/sdkman-init.sh" \
    && sdk install gradle 7.5

# 작업 디렉토리 설정
WORKDIR /app

# Gradle 캐시를 활용하기 위해 Gradle 설정 파일과 프로젝트 파일을 분리하여 복사
COPY build.gradle.kts settings.gradle.kts ./
COPY gradle /app/gradle

# Gradle 의존성 다운로드
RUN gradle build --no-daemon || return 0

# 프로젝트 소스 복사
COPY src /app/src

# 프로젝트 빌드
RUN gradle build --no-daemon

# 실행 단계
FROM ubuntu:22.04

# 필수 패키지 설치
RUN apt-get update && apt-get install -y openjdk-17-jre

# 작업 디렉토리 설정
WORKDIR /app

# 빌드 단계에서 생성된 jar 파일 복사
COPY --from=builder /app/build/libs/*.jar app.jar

# 컨테이너 시작 시 실행할 명령어
ENTRYPOINT ["java","-jar","/app/app.jar"]
