DB_USER=user
DB_NAME=tasksdb
DOCKER_PROFILE ?= dev
SPRING_PROFILE ?= default
APP_PORT=8080

.PHONY: package start-docker run-dev run-prod stop

package:
	mvn clean package

start-docker:
	docker compose --profile $(DOCKER_PROFILE) up -d

run-dev:
	$(MAKE) package
	$(MAKE) start-docker

run-prod:
	$(MAKE) package
	$(MAKE) start-docker DOCKER_PROFILE=docker

stop:
	docker compose --profile "*" down
