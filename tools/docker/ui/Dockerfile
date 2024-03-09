ARG NODE_VERSION=21
ARG OEM=openems
ARG ENV=edge
ARG UI_VERSION=${OEM}-${ENV}-docker

### Build ui files
FROM --platform=$BUILDPLATFORM node:${NODE_VERSION}-alpine AS build_ui

ARG UI_VERSION

RUN apk update && apk upgrade && apk add --no-cache patch

COPY ./ui /src
COPY ./tools/docker/ui/assets/*.patch /src

WORKDIR /src

RUN npm install
RUN node_modules/.bin/ng build -c "${UI_VERSION}"

### Build ui base
FROM ghcr.io/linuxserver/baseimage-alpine:edge AS ui_base

RUN apk update && apk upgrade

RUN apk add --no-cache \
    nginx openssl
	
## Build ui container
FROM ui_base

RUN mkdir -p /etc/nginx/site-templates /var/log/nginx /var/www/html/openems

COPY --from=build_ui /src/target /var/www/html/openems
COPY tools/docker/ui/assets/env.template.js /var/www/html/openems/assets/
COPY tools/docker/ui/root/ /

RUN find /etc/s6-overlay/s6-rc.d -type f -exec chmod +x {} \;

VOLUME /etc/nginx
VOLUME /var/log/nginx

ENV UI_WEBSOCKET 'ws://localhost:8080'

EXPOSE 80 443