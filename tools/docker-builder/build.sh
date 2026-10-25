#!/bin/bash

VERSION=$(date +%Y%m%d%H%M%S)

docker build --network host \
        -t openems-builder:latest \
        -t openems-builder:${VERSION} \
        .
