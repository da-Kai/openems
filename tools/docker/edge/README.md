# How to use openems edge docker images:

## Start openems docker containers:

### With docker compose:
1. Copy docker-compose.yml to a directory of your choice.

2. Typ the following command in the directory where the docker-compose.yml file is located.
`docker compose up -d`

3. Access openems in your browser.
IP = IP address of the docker host ("localhost" for local running docker)  
    |       |                                         |
    | ----- | --------------------------------------- |
    | Edge: | http://IP:8080/system/console/configMgr |
    | UI:   | http://IP                               |

## Build your own docker images:

1. Go into the root directory of the openems project.

2. Type the following build command.
    |       |                                                                        |
    | ----- | ---------------------------------------------------------------------- |
    | Edge: | `docker build -t openems_edge -f tools/docker/edge/openems/Dockerfile` |
    | UI:   | `docker build -t openems_edge_ui -f tools/docker/edge/ui Dockerfile`   |