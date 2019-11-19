# Multi stage docker build - stage 1 builds jar file
FROM zenika/kotlin:1.3-jdk8-slim as build

ARG http_proxy_host=""
ARG http_proxy_port=""

WORKDIR /kafka2s3

# Set gradle proxy
ENV GRADLE_OPTS="${GRADLE_OPTS} -Dhttp.proxyHost=$http_proxy_host -Dhttp.proxyPort=$http_proxy_port"
ENV GRADLE_OPTS="${GRADLE_OPTS} -Dhttps.proxyHost=$http_proxy_host -Dhttps.proxyPort=$http_proxy_port"

RUN echo "ENV gradle: ${GRADLE_OPTS}" \
    && echo "ARG host: ${http_proxy_host}" \
    && echo "ARG port: ${http_proxy_port}"

ENV GRADLE "/kafka2s3/gradlew"

# Copy and generate the gradle wrapper
COPY gradlew .
COPY gradle/ ./gradle

# Copy the gradle config and install dependencies
COPY build.gradle.kts .
COPY settings.gradle.kts .
COPY gradle.properties .

# Copy the source
COPY src/ ./src

RUN $GRADLE wrapper \
    && $GRADLE build \
    && $GRADLE distTar

# Second build stage starts here
FROM openjdk:8-alpine

ARG http_proxy_full=""

# Define User and Groups
ENV USER_NAME=k2s3
ENV GROUP_NAME=k2s3

RUN addgroup ${GROUP_NAME}
RUN adduser --system --ingroup ${GROUP_NAME} ${USER_NAME}


# Set environment variables for apt-get
ENV http_proxy=${http_proxy_full}
ENV https_proxy=${http_proxy_full}
ENV HTTP_PROXY=${http_proxy_full}
ENV HTTPS_PROXY=${http_proxy_full}

ARG VERSION=1.0-SNAPSHOT
ARG DIST=kafka2s3-$VERSION
ARG DIST_FILE=$DIST.tar

RUN echo "ENV http: ${http_proxy}" \
    && echo "ENV https: ${https_proxy}" \
    && echo "ENV HTTP: ${HTTP_PROXY}" \
    && echo "ENV HTTPS: ${HTTPS_PROXY}" \
    && echo "ARG full: ${http_proxy_full}"

ENV acm_cert_helper_version 0.8.0
RUN echo "===> Installing Dependencies ..." \
    && echo "===> Updating base packages ..." \
    && apk update \
    && apk upgrade \
    && echo "==Update done==" \
    && apk add --no-cache g++ python3-dev libffi-dev openssl-dev gcc util-linux \
    && pip3 install --upgrade pip setuptools \
    && pip3 install https://github.com/dwp/acm-pca-cert-generator/releases/download/${acm_cert_helper_version}/acm_cert_helper-${acm_cert_helper_version}.tar.gz \
    && echo "==Dependencies done=="

WORKDIR /kafka2s3

COPY ./entrypoint.sh .

COPY --from=build /kafka2s3/build/distributions/$DIST_FILE .

RUN tar -xf $DIST_FILE --strip-components=1

RUN chown ${USER_NAME}:${GROUP_NAME} . -R

USER $USER_NAME
ENTRYPOINT ["./entrypoint.sh"]
CMD ["./bin/kafka2s3"]
