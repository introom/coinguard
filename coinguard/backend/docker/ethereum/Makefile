DOCKER_COMPOSE=docker-compose

# interactively run commands inside container.
bash:
	@$(DOCKER_COMPOSE) run --rm --service-ports bash

login:
	@az acr login --name firepandalabs

build:
	@$(DOCKER_COMPOSE) build app

push:
	@$(DOCKER_COMPOSE) push app

up:
	@$(DOCKER_COMPOSE) up app

up-d:
	@$(DOCKER_COMPOSE) up -d app

down:
	@$(DOCKER_COMPOSE) down app
