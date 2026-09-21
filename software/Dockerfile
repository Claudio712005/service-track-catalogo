# syntax=docker/dockerfile:1.7

ARG JAVA_VERSION=25
ARG OTEL_AGENT_VERSION=2.31.1

FROM eclipse-temurin:${JAVA_VERSION}-jdk-alpine AS build

WORKDIR /workspace

COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle ./gradle
RUN chmod +x gradlew

RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew --no-daemon dependencies > /dev/null 2>&1 || true

COPY src ./src

RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew --no-daemon clean bootJar -x test

RUN mv build/libs/*-SNAPSHOT.jar /workspace/aplicacao.jar


FROM eclipse-temurin:${JAVA_VERSION}-jre-alpine AS preparo

ARG OTEL_AGENT_VERSION

WORKDIR /preparo

COPY --from=build /workspace/aplicacao.jar ./aplicacao.jar

RUN java -Djarmode=tools -jar aplicacao.jar extract --layers --launcher --destination extraido

RUN apk add --no-cache curl && \
    curl -fsSL --retry 5 --retry-all-errors --retry-delay 3 \
      -o /preparo/opentelemetry-javaagent.jar \
      "https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v${OTEL_AGENT_VERSION}/opentelemetry-javaagent.jar" && \
    test -s /preparo/opentelemetry-javaagent.jar

WORKDIR /preparo/extraido

RUN java -XX:ArchiveClassesAtExit=/preparo/aplicacao.jsa \
         -Dspring.context.exit=onRefresh \
         -Dservicetrack.security.jwt.habilitado=false \
         -Dspring.main.banner-mode=off \
         -jar aplicacao.jar > /preparo/treino.log 2>&1 \
    && test -s /preparo/aplicacao.jsa \
    || (echo "AVISO: arquivo CDS nao gerado; a imagem sobe sem ele" && cat /preparo/treino.log | tail -20 && rm -f /preparo/aplicacao.jsa && touch /preparo/aplicacao.jsa)


FROM eclipse-temurin:${JAVA_VERSION}-jre-alpine AS runtime

RUN addgroup -S servicetrack && adduser -S servicetrack -G servicetrack

WORKDIR /app

COPY --from=preparo --chown=servicetrack:servicetrack /preparo/extraido/dependencies/ ./
COPY --from=preparo --chown=servicetrack:servicetrack /preparo/extraido/spring-boot-loader/ ./
COPY --from=preparo --chown=servicetrack:servicetrack /preparo/extraido/snapshot-dependencies/ ./
COPY --from=preparo --chown=servicetrack:servicetrack /preparo/extraido/application/ ./
COPY --from=preparo --chown=servicetrack:servicetrack --chmod=444 /preparo/aplicacao.jsa ./aplicacao.jsa
COPY --from=preparo --chown=servicetrack:servicetrack --chmod=444 /preparo/opentelemetry-javaagent.jar ./opentelemetry-javaagent.jar

USER servicetrack

ENV OTEL_JAVAAGENT_ENABLED=false
ENV OTEL_SERVICE_NAME=service-track-catalogo
ENV SPRING_PROFILES_ACTIVE=prod

ENV JDK_JAVA_OPTIONS="\
-XX:MaxRAMPercentage=75 \
-XX:InitialRAMPercentage=50 \
-XX:+UseSerialGC \
-Xshare:auto \
-XX:SharedArchiveFile=/app/aplicacao.jsa \
-XX:+ExitOnOutOfMemoryError \
-Dfile.encoding=UTF-8"

EXPOSE 8080

ENTRYPOINT ["java", "-javaagent:/app/opentelemetry-javaagent.jar", "-jar", "aplicacao.jar"]
