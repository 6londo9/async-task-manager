DB_USER=user
DB_NAME=tasksdb
DOCKER_PROFILE ?= dev
APP_PORT=8080

.PHONY: package start-docker run-dev run-prod stop restart-dev restart-prod

package:
	mvn clean package -DskipTests

start-docker:
	docker compose --profile $(DOCKER_PROFILE) up -d --build

run-dev:
	$(MAKE) package
	$(MAKE) start-docker

run-prod:
	$(MAKE) start-docker DOCKER_PROFILE=docker

stop:
	docker compose --profile "*" down

restart-dev:
	$(MAKE) stop
	$(MAKE) run-dev

restart-prod:
	$(MAKE) stop
	$(MAKE) run-prod
