# Debian 기반의 OpenJDK 이미지 사용
FROM openjdk:21-jdk-slim

# 작업 디렉토리 설정
WORKDIR /app

# 필요한 툴 설치
RUN apt-get -y update && apt-get install -y \
    wget \
    unzip \
    curl \
    tzdata && \
    ln -snf /usr/share/zoneinfo/Asia/Seoul /etc/localtime && echo "Asia/Seoul" > /etc/timezone

# Chrome 다운로드 및 설치
RUN wget https://dl.google.com/linux/direct/google-chrome-stable_current_amd64.deb && \
    apt -y install ./google-chrome-stable_current_amd64.deb && \
    rm ./google-chrome-stable_current_amd64.deb

# 인자 설정 - JAR_File
ARG JAR_FILE=build/libs/*.jar

# JAR 파일 복제
COPY ${JAR_FILE} app.jar

# 실행 프로필과 추가 JVM 옵션은 환경 변수로 주입
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS:-} -Duser.timezone=Asia/Seoul -jar app.jar --spring.profiles.active=${SPRING_PROFILES_ACTIVE:-postgres,deploy}"]
