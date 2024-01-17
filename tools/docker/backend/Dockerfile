ARG JAVA_VERSION=openjdk17

### Build jar builder
FROM --platform=$BUILDPLATFORM alpine:latest AS jar_builder

ARG JAVA_VERSION

RUN apk update && apk upgrade

RUN apk add --no-cache \
    ${JAVA_VERSION}-jdk


### Build backend binary
FROM --platform=$BUILDPLATFORM jar_builder AS build_backend

WORKDIR /src

RUN --mount=type=bind,target=.,readwrite \
    --mount=type=cache,target=/root/.gradle \
    ./gradlew --no-daemon clean build buildBackend && \
    cp build/openems-backend.jar /opt


### Build jar container base
FROM ghcr.io/linuxserver/baseimage-alpine:edge AS base_container

ARG JAVA_VERSION

RUN apk update && apk upgrade

RUN apk add --no-cache \
    ${JAVA_VERSION}-jre-headless


### Build backend container
FROM base_container

RUN mkdir -p /opt/openems /var/opt/openems/config /var/opt/openems/data

COPY --from=build_backend /opt /opt/openems
COPY tools/docker/backend/root/ /

VOLUME /var/opt/openems/config
VOLUME /var/opt/openems/data

EXPOSE 8079 8081 8082