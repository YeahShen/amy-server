FROM registry.cn-hangzhou.aliyuncs.com/ashen_station/ffmpeg-openjdk21:latest
LABEL authors="ashen"

USER root

# 多模块工程：可执行 jar 只在 boot 模块产出（其余模块为普通库 jar）
COPY boot/target/*.jar /app.jar

RUN mkdir /amy
RUN mkdir /amy/poster /amy/artist-avatar /amy/archive /amy/video_1 /amy/user-avatar /amy/temp /amy/video_temp

ENV TZ=Asia/Shanghai

# 9999=应用端口，5006=jdwp 调试端口
EXPOSE 9999 5006

ENTRYPOINT ["java","-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5006","-jar","/app.jar", "--spring.profiles.active=prod"]