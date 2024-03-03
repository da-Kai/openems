ARG NODE_VERSION=21
ARG UI_VERSION=openems,openems-edge-prod,prod

### Build ui files
FROM --platform=$BUILDPLATFORM node:${NODE_VERSION}-alpine AS build_ui

ARG UI_VERSION

RUN apk update && apk upgrade && apk add --no-cache patch

COPY ./ui /src
COPY ./tools/docker/edge/ui/assets/*.patch /src

WORKDIR /src

RUN patch -p1 < edge.patch
RUN patch -p1 < index.patch
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
COPY tools/docker/edge/ui/assets/env.template.js /var/www/html/openems/assets
COPY tools/docker/edge/ui/root/ /

RUN find /etc/s6-overlay/s6-rc.d -type f -exec chmod +x {} \;

VOLUME /etc/nginx
VOLUME /var/log/nginx

ENV EDGE_HOST 127.0.0.1
ENV EDGE_WEBSOCKET_PORT 8075
ENV EDGE_REST_API_PORT 8084

EXPOSE 80 443