SHELL := /bin/bash
.ONESHELL:
.SHELLFLAGS := -euo pipefail -c

MVN ?= ./mvnw
JAR_JETTY ?= housedb-transport-jetty/target/housedb-transport-jetty-0.1.0-SNAPSHOT-jar-with-dependencies.jar
GLOWROOT_AGENT_JAR ?= ../observability/glowroot/glowroot.jar

.PHONY: help build build-without-tests test clean verify glowroot-jetty glowroot-jetty-build

help:
	@echo "Targets:"
	@echo "  make build               -> mvn clean package"
	@echo "  make build-without-tests -> mvn -DskipTests clean package"
	@echo "  make test                -> mvn test"
	@echo "  make clean               -> mvn clean"
	@echo "  make verify              -> mvn verify"
	@echo "  make glowroot-jetty      -> run fat jar with Glowroot agent"
	@echo "  make glowroot-jetty-build-> build and run fat jar with Glowroot agent"

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

glowroot-jetty:
	@echo "Running HouseDB with Glowroot agent:"
	java \
	  -javaagent:$(GLOWROOT_AGENT_JAR) \
	  -jar $(JAR_JETTY)

glowroot-jetty-build: build
	@echo "Building and running HouseDB with Glowroot agent:"
	java \
	  -javaagent:$(GLOWROOT_AGENT_JAR) \
	  -jar $(JAR_JETTY)
