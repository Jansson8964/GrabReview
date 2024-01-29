# 使用Java 8的基础镜像
FROM openjdk:8-jre

# 设定时区
ENV TZ=Europe/London
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 拷贝jar包
COPY MyYelp.jar /app.jar

# 设置容器启动时执行的命令
ENTRYPOINT ["java", "-jar", "/app.jar"]
