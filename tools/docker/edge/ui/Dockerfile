ARG NODE_VERSION=21
ARG UI_VERSION=openems,openems-edge-prod,prod

### Build ui files
FROM --platform=$BUILDPLATFORM node:${NODE_VERSION}-alpine AS build_ui

ARG UI_VERSION

WORKDIR /src/ui

RUN npm ci
RUN node_modules/.bin/ng build -c "${UI_VERSION}"

### Build ui base
FROM ghcr.io/linuxserver/baseimage-alpine:edge AS ui_base

RUN apk update && apk upgrade

RUN apk add --no-cache \
    nginx openssl
	
## Build ui container
FROM ui_base

RUN mkdir -p /etc/nginx/site-templates /var/log/nginx /var/www/html/openems

COPY --from=build_ui /src/ui/target /var/www/html/openems
COPY tools/docker/edge/ui/root/ /

VOLUME /etc/nginx
VOLUME /var/log/nginx

ENV EDGE_HOST 127.0.0.1
ENV EDGE_WEBSOCKET_PORT 8075
ENV EDGE_REST_API_PORT 8084

EXPOSE 80 443