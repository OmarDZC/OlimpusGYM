.PHONY: help up down infra logs backend-test ai-test e2e front-dev clean

help:
	@echo "make infra         - levanta solo postgres, redis y rabbitmq"
	@echo "make up            - levanta todo con docker compose (build incluido)"
	@echo "make down          - para todo"
	@echo "make logs          - logs de todos los servicios"
	@echo "make backend-test  - tests del backend (Spring)"
	@echo "make ai-test       - tests del servicio de IA (FastAPI)"
	@echo "make e2e           - tests end-to-end con Playwright (necesita backend+frontend arriba)"
	@echo "make front-dev     - frontend en modo desarrollo"
	@echo "make clean         - para todo y borra volúmenes (borra la BD!)"

infra:
	docker compose up -d postgres redis rabbitmq

up:
	docker compose up --build -d

down:
	docker compose down

logs:
	docker compose logs -f

backend-test:
	cd backend && mvn -q test

ai-test:
	cd ai-service && python -m pytest -q

e2e:
	cd e2e && npm install && npx playwright test

front-dev:
	cd frontend && npm install && npm run dev

clean:
	docker compose down -v
