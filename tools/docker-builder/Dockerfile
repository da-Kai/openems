FROM docker:29.9

RUN apk update && apk add --no-cache git curl jq bash dos2unix python3 py3-docker-py py3-gitpython;

COPY --from=docker/buildx-bin /buildx /usr/libexec/docker/cli-plugins/docker-buildx

RUN docker buildx version

WORKDIR /home/openems

COPY ./start start

RUN git config --global credential.helper cache

RUN git init && \
    git remote add origin https://git.intranet.fenecon.de/FENECON/fems.git && \
    git remote -v

RUN ln -sf /bin/bash /bin/sh

ENV COMMIT=HEAD
ENV DEV=false
ENV TYPE=EDGE
ENV BRANCH=develop
ENV IMAGE_PREFIX="openems"

VOLUME /var/run/docker.sock
VOLUME /home/openems/git_secret

ENV DOCKER_SERVER=""
ENV GIT_ORIGIN="https://github.com/OpenEMS/openems.git"

ENTRYPOINT ["bash", "/home/openems/start"]