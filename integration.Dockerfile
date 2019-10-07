FROM zenika/kotlin:1.3-jdk8-alpine as build

WORKDIR /kafka2s3

ENV GRADLE "/kafka2s3/gradlew --no-daemon"

# Copy and generate the gradle wrapper
COPY gradlew .
COPY gradle/ ./gradle
RUN $GRADLE wrapper

# Copy the gradle config and install dependencies
COPY build.gradle.kts .
COPY settings.gradle.kts .
COPY gradle.properties .
RUN $GRADLE build && $GRADLE test

RUN apk update && apk add --nocache --virtual gosu

COPY . .

ENTRYPOINT ["./entrypoint.sh"]
