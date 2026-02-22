SHELL := /bin/bash
.ONESHELL:
.SHELLFLAGS := -eu -o pipefail -c

FLYWAY := ../flywayw
ARGS ?=
NONINTERACTIVE ?= 0

CONTAINER_TOOL ?= podman
DB_CONTAINER ?= housedb-postgres
DB_IMAGE ?= docker.io/library/postgres:16
DB_NAME ?= housedb
DB_USER ?= housedb
DB_PASSWORD ?= housedb
DB_PORT ?= 5432

.PHONY: help up down logs psql migrate info validate repair

define run_flyway
	@FLYWAY_URL_VAL="$${FLYWAY_URL:-}"; \
	if [[ -z "$$FLYWAY_URL_VAL" ]]; then \
		if [[ "$(NONINTERACTIVE)" == "1" ]]; then echo "ERROR: FLYWAY_URL is required"; exit 2; fi; \
		read -rp "Enter FLYWAY_URL: " FLYWAY_URL_VAL; \
	fi; \
	FLYWAY_USER_VAL="$${FLYWAY_USER:-}"; \
	if [[ -z "$$FLYWAY_USER_VAL" ]]; then \
		if [[ "$(NONINTERACTIVE)" == "1" ]]; then echo "ERROR: FLYWAY_USER is required"; exit 2; fi; \
		read -rp "Enter FLYWAY_USER: " FLYWAY_USER_VAL; \
	fi; \
	FLYWAY_PASSWORD_VAL="$${FLYWAY_PASSWORD:-}"; \
	if [[ -z "$$FLYWAY_PASSWORD_VAL" ]]; then \
		if [[ "$(NONINTERACTIVE)" == "1" ]]; then echo "ERROR: FLYWAY_PASSWORD is required"; exit 2; fi; \
		read -srp "Enter FLYWAY_PASSWORD: " FLYWAY_PASSWORD_VAL; echo; \
	fi; \
	FLYWAY_URL="$$FLYWAY_URL_VAL" FLYWAY_USER="$$FLYWAY_USER_VAL" FLYWAY_PASSWORD="$$FLYWAY_PASSWORD_VAL" $(FLYWAY) $(ARGS) $(1)
endef

help:
	@echo "Targets:"
	@echo "  up        Levanta PostgreSQL local"
	@echo "  down      Detiene y elimina contenedor PostgreSQL"
	@echo "  migrate   Flyway migrate"
	@echo "  info      Flyway info"
	@echo "  validate  Flyway validate"
	@echo "  repair    Flyway repair"

up:
	-$(CONTAINER_TOOL) rm -f $(DB_CONTAINER) >/dev/null 2>&1 || true
	$(CONTAINER_TOOL) run -d --name $(DB_CONTAINER) \
		-e POSTGRES_DB=$(DB_NAME) \
		-e POSTGRES_USER=$(DB_USER) \
		-e POSTGRES_PASSWORD=$(DB_PASSWORD) \
		-p $(DB_PORT):5432 \
		$(DB_IMAGE)

down:
	-$(CONTAINER_TOOL) rm -f $(DB_CONTAINER)

logs:
	$(CONTAINER_TOOL) logs -f $(DB_CONTAINER)

psql:
	$(CONTAINER_TOOL) exec -it $(DB_CONTAINER) psql -U $(DB_USER) -d $(DB_NAME)

migrate:
	$(call run_flyway,migrate)

info:
	$(call run_flyway,info)

validate:
	$(call run_flyway,validate)

repair:
	$(call run_flyway,repair)
