SHELL := /bin/bash
.ONESHELL:
.SHELLFLAGS := -euo pipefail -c

MVN ?= ./mvnw

.PHONY: help build build-without-tests test clean verify

help:
	@echo "Targets:"
	@echo "  make build               -> mvn clean package"
	@echo "  make build-without-tests -> mvn -DskipTests clean package"
	@echo "  make test                -> mvn test"
	@echo "  make clean               -> mvn clean"
	@echo "  make verify              -> mvn verify"

build:
	$(MVN) -q clean package

build-without-tests:
	$(MVN) -q -DskipTests clean package

test:
	$(MVN) -q test

clean:
	$(MVN) -q clean

verify:
	$(MVN) -q verify
