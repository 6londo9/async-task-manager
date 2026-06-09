DB_USER=user
DB_NAME=tasksdb
DOCKER_PROFILE ?= dev
SPRING_PROFILE ?= default
APP_PORT=8081

.PHONY: compile start-app start-docker run-dev run-prod stop stop-app

compile:
	mvn clean compile

start-app:
	mvn spring-boot:run -Dspring-boot.run.profiles=$(SPRING_PROFILE)

start-docker:
	docker compose --profile $(DOCKER_PROFILE) up -d

run-dev:
	$(MAKE) compile
	$(MAKE) start-docker
	$(MAKE) start-app

run-prod:
	$(MAKE) compile
	$(MAKE) start-docker DOCKER_PROFILE=docker
	$(MAKE) start-app SPRING_PROFILE=docker

stop:
	$(MAKE) stop-app
	docker compose --profile "*" down

stop-app:
	@PID=$$(lsof -t -i:$(APP_PORT)); if [ -n "$$PID" ]; then kill -9 $$PID || true; fi
