DOCKER_COMPOSE=docker-compose

# interactively run commands inside container.
bash:
	@$(DOCKER_COMPOSE) run --rm --entrypoint bash --service-ports service

login:
	@az acr login --name firepandalabs

build:
	@$(DOCKER_COMPOSE) build app

push:
	@$(DOCKER_COMPOSE) push app

up:
	@$(DOCKER_COMPOSE) up app
