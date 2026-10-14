# Image chỉ có backend (API) — dùng để deploy lên Fly.io: chạy `fly deploy` trong thư mục be/
# (Dockerfile ở thư mục gốc dự án là bản gộp FE + BE cho docker compose chạy local.)

# 1) Build jar. Tải dependency trước để cache layer khi chỉ sửa code
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /be
COPY pom.xml ./
RUN mvn -q dependency:go-offline
COPY src ./src
RUN mvn -q -DskipTests package

# 2) Image chạy: chỉ JRE + jar, không chạy bằng root
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S app && adduser -S app -G app
COPY --from=build /be/target/be-0.0.1-SNAPSHOT.jar app.jar
USER app
# Máy nhỏ (512MB): heap tối đa 75% RAM, SerialGC tốn ít bộ nhớ nhất
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC -XX:TieredStopAtLevel=1"
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
