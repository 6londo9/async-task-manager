DB_USER=user
DB_NAME=tasksdb
DOCKER_PROFILE ?= dev
APP_PORT=8080

.PHONY: package start-docker run-dev run-prod stop stop-app restart-dev restart-prod restart-app-dev restart-app-prod cleanup

package:
	mvn clean package -DskipTests

start-docker:
	docker compose --profile $(DOCKER_PROFILE) up -d --build

run-dev:
	$(MAKE) package
	$(MAKE) start-docker

run-prod:
	$(MAKE) start-docker DOCKER_PROFILE=prod

stop:
	docker compose --profile "*" down

stop-app:
	docker compose --profile "*" stop async-task-manager-$(DOCKER_PROFILE)

restart-dev:
	$(MAKE) stop
	$(MAKE) run-dev

restart-prod:
	$(MAKE) stop
	$(MAKE) run-prod

restart-app-dev:
	$(MAKE) stop-app
	$(MAKE) run-dev

restart-app-prod:
	$(MAKE) stop-app DOCKER_PROFILE=prod
	$(MAKE) run-prod

cleanup:
	docker compose --profile "*" down -v
