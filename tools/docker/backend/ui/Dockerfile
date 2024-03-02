ARG NODE_VERSION=21
ARG UI_VERSION=openems,openems-backend-prod,prod

### Build ui files
FROM --platform=$BUILDPLATFORM node:${NODE_VERSION}-alpine AS build_ui

ARG UI_VERSION

RUN apk update && apk upgrade && apk add --no-cache patch

COPY ./ui /src
COPY ./tools/docker/backend/ui/backend.patch /src

WORKDIR /src

RUN patch -p1 < backend.patch
RUN npm install
RUN node_modules/.bin/ng build -c "${UI_VERSION}"

### Build ui base
FROM ghcr.io/linuxserver/baseimage-alpine:edge AS ui_base

RUN apk update && apk upgrade

RUN apk add --no-cache \
    nginx openssl
	
## Build ui container
FROM ui_base

RUN mkdir -p /etc/nginx /var/log/nginx /var/www/html/openems

COPY --from=build_ui /src/target /var/www/html/openems
COPY tools/docker/backend/ui/root/ /

RUN find /etc/s6-overlay/s6-rc.d -type f -exec chmod +x {} \;

VOLUME /etc/nginx
VOLUME /var/log/nginx

ENV BACKEND_HOST 127.0.0.1
ENV BACKEND_WEBSOCKET_PORT 8075
ENV BACKEND_REST_API_PORT 8084

EXPOSE 80 443