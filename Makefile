.PHONY: help
help:
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-30s\033[0m %s\n", $$1, $$2}'

.PHONY: bootstrap
bootstrap: ## Bootstrap local environment for first use
	make git-hooks

.PHONY: git-hooks
git-hooks: ## Set up hooks in .git/hooks
	@{ \
		HOOK_DIR=.git/hooks; \
		for hook in $(shell ls .githooks); do \
			if [ ! -h $${HOOK_DIR}/$${hook} -a -x $${HOOK_DIR}/$${hook} ]; then \
				mv $${HOOK_DIR}/$${hook} $${HOOK_DIR}/$${hook}.local; \
				echo "moved existing $${hook} to $${hook}.local"; \
			fi; \
			ln -s -f ../../.githooks/$${hook} $${HOOK_DIR}/$${hook}; \
		done \
	}

.PHONY: build-base
build-base: ## build the base images which certain images extend.
	docker build --tag dwp-python-preinstall:latest --file ./python.preinstall.Dockerfile .

.PHONY: build
build: ## Build Kafka2S3
	./gradlew build

.PHONY: dist
dist: ## Assemble distribution files in build/dist
	./gradlew assembleDist

.PHONY: services
services:
	docker-compose up -d zookeeper kafka aws-s3 s3-provision

.PHONY: hosts
hosts:
	./hosts.sh

.PHONY: up
up: build-base
	docker-compose up --build -d

.PHONY: restart
restart: ## Restart Kafka2S3 and all supporting services
	docker-compose restart

.PHONY: down
down: ## Bring down the Kafka2S3 Docker container and support services
	docker-compose down

.PHONY: destroy
destroy: down ## Bring down the Kafka2S3 Docker container and services then delete all volumes
	docker network prune -f
	docker volume prune -f

.PHONY: integration
integration: up ## Run the integration tests in a Docker container
	docker-compose run --rm integration-test \
		./gradlew --no-daemon --rerun-tasks \
		integration
