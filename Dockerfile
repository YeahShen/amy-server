FROM registry.cn-hangzhou.aliyuncs.com/zxhysite/java-ffmpeg:21-jre-alpine-3.21
LABEL authors="ashen"

USER root

COPY target/*.jar /app.jar

RUN mkdir /amy
RUN mkdir /amy/poster /amy/artist-avatar /amy/archive /amy/video_1 /amy/public /amy/user-avatar

ENV TZ=Asia/Shanghai

EXPOSE 8080

ENTRYPOINT ["java","-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5006","-jar","/app.jar", "--spring.profiles.active=prod"]