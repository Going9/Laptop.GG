# jdk17 Image Start
FROM openjdk:17

# 작업 디렉토리 설정
#WORKDIR /LaptopGG

# 인자 설정 - JAR_File
ARG JAR_FILE=build/libs/*.jar

# jar 파일 복제
COPY ${JAR_FILE} app.jar

# 환경 변수 설정
ENV PROFILE=deploy

# 실행 명령어에 전달된 프로파일 추가
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=${PROFILE}"]
