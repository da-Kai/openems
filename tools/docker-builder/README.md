# OpenEMS Docker Builder

This image checks out an OpenEMS branch and builds one of its Docker images
using the Docker daemon on the host.

## Build the builder image

Run the build script from this directory:

```sh
./build.sh
```

It creates `openems-builder:latest` and a timestamped tag.

## Run a build

```sh
docker run --rm \
	-v /var/run/docker.sock:/var/run/docker.sock \
	-e TYPE=EDGE \
	-e BRANCH=develop \
	openems-builder:latest
```

The container fetches the selected branch from `GIT_ORIGIN`, checks it out,
and builds the requested image. The default origin is
`https://github.com/OpenEMS/openems.git`.

## Configuration

| Variable | Default | Purpose |
| --- | --- | --- |
| `TYPE` | `EDGE` | Image to build: `EDGE`, `BACKEND`, `BACKEND-EDGE`, `UI` (or `BACKEND-UI`), or `EDGE-UI`. |
| `BRANCH` | `develop` | Branch to fetch and build. |
| `GIT_ORIGIN` | `https://github.com/OpenEMS/openems.git` | Git remote URL. |
| `IMAGE_PREFIX` | `openems` | Prefix for the generated image tag. |
| `DOCKER_SERVER` | empty | When set, the built image is pushed to `localhost:5000` and to this registry host. |
| `BUILDARG_*` | none | Environment variables with this prefix are passed as Docker build arguments for `BACKEND`, `BACKEND-EDGE`, and UI builds. The prefix is removed from the argument name. |

The generated image is tagged with its type, branch, and build timestamp, for
example `openems/edge/develop:261025120000`.

## Security

The container needs access to `/var/run/docker.sock` to build images. A
container with this socket mounted can control the host Docker daemon, so only
run this builder with trusted configuration and source.
