# Debian 기반의 OpenJDK 이미지 사용
FROM openjdk:17

# 작업 디렉토리 설정
WORKDIR /LaptopGG

# Chrome 설치 (Debian 기반)
RUN apt-get update && apt-get install -y \
    wget \
    gnupg \
    udev \
    chromium

# 인자 설정 - JAR_File
ARG JAR_FILE=build/libs/*.jar

# jar 파일 복제
COPY ${JAR_FILE} app.jar

# 실행 명령어에 전달된 프로파일 추가
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=${PROFILE}"]
