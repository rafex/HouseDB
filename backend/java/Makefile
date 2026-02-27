SHELL := /bin/bash
.ONESHELL:
.SHELLFLAGS := -euo pipefail -c

MVN ?= ./mvnw
PARENT_DIR ?= housedb-parent
IMAGE ?= housedb-backend:latest
PORT ?= 8080
CONTAINER_NAME ?= housedb-backend-run
CONTAINER_TOOL ?= podman
DB_USER ?=
DB_PASSWORD ?=
DB_URL ?=

.PHONY: help build test clean verify build-without-tests image run-image run-image-from-current glowroot-jetty glowroot-jetty-build

help:
	@echo "Targets:"
	@echo "  make build               -> mvn clean package"
	@echo "  make build-without-tests -> mvn -DskipTests clean package"
	@echo "  make test                -> mvn test"
	@echo "  make verify              -> mvn verify"
	@echo "  make image               -> build container image ($(IMAGE))"
	@echo "  make run-image           -> build + run image"
	@echo "  make glowroot-jetty      -> run java app with Glowroot agent"
	@echo "  make glowroot-jetty-build-> build and run java app with Glowroot agent"

build:
	$(MAKE) -C $(PARENT_DIR) build

build-without-tests:
	$(MAKE) -C $(PARENT_DIR) build-without-tests

test:
	$(MAKE) -C $(PARENT_DIR) test

clean:
	$(MAKE) -C $(PARENT_DIR) clean

verify:
	$(MAKE) -C $(PARENT_DIR) verify

image: build-without-tests
	@echo "Building image $(IMAGE)"
	$(CONTAINER_TOOL) build -t "$(IMAGE)" -f Dockerfile .

run-image: image run-image-from-current

glowroot-jetty:
	$(MAKE) -C $(PARENT_DIR) glowroot-jetty

glowroot-jetty-build:
	$(MAKE) -C $(PARENT_DIR) glowroot-jetty-build

run-image-from-current:
	@echo "Running container $(CONTAINER_NAME) on port $(PORT)"
	-@$(CONTAINER_TOOL) rm -f "$(CONTAINER_NAME)" 2>/dev/null || true
	@env_args=""; \
	if [ -n "$(DB_USER)" ]; then env_args="$$env_args -e DB_USER=$(DB_USER)"; elif [ -n "$$DB_USER" ]; then env_args="$$env_args -e DB_USER=$$DB_USER"; fi; \
	if [ -n "$(DB_PASSWORD)" ]; then env_args="$$env_args -e DB_PASSWORD=$(DB_PASSWORD)"; elif [ -n "$$DB_PASSWORD" ]; then env_args="$$env_args -e DB_PASSWORD=$$DB_PASSWORD"; fi; \
	if [ -n "$(DB_URL)" ]; then env_args="$$env_args -e DB_URL=$(DB_URL)"; elif [ -n "$$DB_URL" ]; then env_args="$$env_args -e DB_URL=$$DB_URL"; fi; \
	$(CONTAINER_TOOL) run -d --name "$(CONTAINER_NAME)" $$env_args -p "$(PORT):8080" "$(IMAGE)"
