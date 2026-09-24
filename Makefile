# Set JAVA_HOME to a JDK 21 installation before invoking Maven.
MVN ?= ./mvnw

.PHONY: up down logs test verify build clean smoke

up:
	docker compose up -d --build --wait --wait-timeout 240

down:
	docker compose down

logs:
	docker compose logs -f payflow-app audit-service

test:
	$(MVN) -B test
	$(MVN) -B -f audit-service/pom.xml test

verify:
	$(MVN) -B verify
	$(MVN) -B -f audit-service/pom.xml verify

build:
	$(MVN) -B -DskipTests package
	$(MVN) -B -f audit-service/pom.xml -DskipTests package

clean:
	$(MVN) -B clean
	$(MVN) -B -f audit-service/pom.xml clean

smoke:
	./scripts/smoke-test.sh
