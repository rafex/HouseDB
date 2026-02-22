SHELL := /bin/bash
.ONESHELL:
.SHELLFLAGS := -eu -o pipefail -c

BACKEND_DIR ?= backend/java
DB_MAKE_DIR ?= db/make

.PHONY: help build test clean verify image run-image db-up db-down db-migrate db-info db-validate

help:
	@echo "Targets:"
	@echo "  make build        -> compila backend Java"
	@echo "  make test         -> ejecuta tests backend"
	@echo "  make verify       -> ejecuta verificación Maven"
	@echo "  make image        -> construye imagen de backend"
	@echo "  make run-image    -> ejecuta imagen de backend"
	@echo "  make db-up        -> levanta PostgreSQL local"
	@echo "  make db-migrate   -> ejecuta migraciones Flyway"
	@echo "  make db-info      -> muestra estado Flyway"
	@echo "  make db-validate  -> valida migraciones Flyway"

build:
	$(MAKE) -C $(BACKEND_DIR) build

test:
	$(MAKE) -C $(BACKEND_DIR) test

clean:
	$(MAKE) -C $(BACKEND_DIR) clean

verify:
	$(MAKE) -C $(BACKEND_DIR) verify

image:
	$(MAKE) -C $(BACKEND_DIR) image

run-image:
	$(MAKE) -C $(BACKEND_DIR) run-image

db-up:
	$(MAKE) -C $(DB_MAKE_DIR) up

db-down:
	$(MAKE) -C $(DB_MAKE_DIR) down

db-migrate:
	$(MAKE) -C $(DB_MAKE_DIR) migrate

db-info:
	$(MAKE) -C $(DB_MAKE_DIR) info

db-validate:
	$(MAKE) -C $(DB_MAKE_DIR) validate
