# Service and database configuration
APP_SERVICE=app
DB_SERVICE=mysql
DB_CONTAINER=security-mysql
DB_USER=root
DB_PASSWORD=password
DB_NAME=security_db
INIT_SCRIPT=./src/main/resources/schema.sql

# 🚀 Build all Docker images
build:
	docker-compose build

# 🔼 Start all containers in detached mode
up:
	docker-compose up -d

# 🧹 Stop and remove all containers, volumes, and orphans
down:
	docker-compose down --volumes --remove-orphans

# 🔁 Rebuild and restart only the application container
rebuild-app:
	docker-compose build $(APP_SERVICE)
	docker-compose up -d $(APP_SERVICE)

# 🛑 Stop only the application container
stop-app:
	docker-compose stop $(APP_SERVICE)

# ▶️ Start only the application container
start-app:
	docker-compose start $(APP_SERVICE)

# 🔄 Restart only the application container
restart-app:
	docker-compose restart $(APP_SERVICE)

# ❌ Remove the application container
rm-app:
	docker-compose rm -f $(APP_SERVICE)

# 📊 Show status of running containers
status:
	docker ps

# 📜 Tail application logs
logs:
	docker-compose logs -f $(APP_SERVICE)

# ✅ Check health status of the MySQL container
health-check:
	@echo "Checking health status of container $(DB_SERVICE):"
	@docker inspect --format='{{.State.Health.Status}}' $(DB_CONTAINER)

# 🗄️ Execute database initialization SQL script
init-db:
	@echo "Running SQL init script..."
	docker exec -i $(DB_CONTAINER) mysql -u$(DB_USER) -p$(DB_PASSWORD) $(DB_NAME) < $(INIT_SCRIPT)