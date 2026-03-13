SHELL := /bin/bash
.ONESHELL:
.SHELLFLAGS := -eu -o pipefail -c

BACKEND_DIR ?= backend/java
FRONTEND_DIR ?= frontend
DB_MAKE_DIR ?= db/make
TAG_MAJOR ?= 1
TAG_DATE ?= $(shell date +%Y%m%d)
TAG_PREFIX ?= v$(TAG_MAJOR).$(TAG_DATE)
FRONTEND_HOST ?= 0.0.0.0
FRONTEND_API_BASE_URL ?= https://housedb.v1.rafex.cloud

.PHONY: help build test clean verify image run-image db-up db-down db-migrate db-info db-validate frontend-install frontend-dev frontend-build frontend-preview frontend-dev-cloud print-next-tag release-tag

help:
	@echo "Targets:"
	@echo "  make build        -> compila backend Java"
	@echo "  make test         -> ejecuta tests backend"
	@echo "  make verify       -> ejecuta verificación Maven"
	@echo "  make image        -> construye imagen de backend"
	@echo "  make run-image    -> ejecuta imagen de backend"
	@echo "  make frontend-install -> instala dependencias del frontend"
	@echo "  make frontend-dev     -> levanta frontend local"
	@echo "  make frontend-dev-cloud -> levanta frontend local usando backend $(FRONTEND_API_BASE_URL)"
	@echo "  make frontend-build   -> compila frontend"
	@echo "  make frontend-preview -> previsualiza frontend compilado"
	@echo "  make db-up        -> levanta PostgreSQL local"
	@echo "  make db-migrate   -> ejecuta migraciones Flyway"
	@echo "  make db-info      -> muestra estado Flyway"
	@echo "  make db-validate  -> valida migraciones Flyway"
	@echo "  make print-next-tag -> calcula el siguiente tag release"
	@echo "  make release-tag  -> crea y sube el siguiente tag release"

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

frontend-install:
	npm --prefix $(FRONTEND_DIR) install

frontend-dev:
	npm --prefix $(FRONTEND_DIR) run dev -- --host $(FRONTEND_HOST)

frontend-dev-cloud:
	VITE_API_BASE_URL=$(FRONTEND_API_BASE_URL) npm --prefix $(FRONTEND_DIR) run dev -- --host $(FRONTEND_HOST)

frontend-build:
	npm --prefix $(FRONTEND_DIR) run build

frontend-preview:
	npm --prefix $(FRONTEND_DIR) run preview -- --host $(FRONTEND_HOST)

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

print-next-tag:
	@prefix="$(TAG_PREFIX)"; \
	last_tag="$$(git tag --list "$$prefix*" --sort=-v:refname | head -n 1)"; \
	if [ -z "$$last_tag" ]; then \
		echo "$$prefix"; \
	elif [[ "$$last_tag" =~ ^$${prefix}-([0-9]+)$$ ]]; then \
		next="$$(($${BASH_REMATCH[1]} + 1))"; \
		echo "$$prefix-$$next"; \
	elif [ "$$last_tag" = "$$prefix" ]; then \
		echo "$$prefix-1"; \
	else \
		echo "$$prefix"; \
	fi

release-tag:
	@next_tag="$$( $(MAKE) --no-print-directory print-next-tag TAG_MAJOR=$(TAG_MAJOR) TAG_DATE=$(TAG_DATE) )"; \
	echo "Creating tag $$next_tag"; \
	git tag -a "$$next_tag" -m "Release $$next_tag"; \
	git push origin "$$next_tag"
