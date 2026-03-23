# L1 Control Plane - Webapp Service

This is the web application module for operators to manage tenants, subscriptions, sites, and devices in the L1 Control Plane system.

## Setup Instructions

1. Ensure you have the `admin-service` running locally or accessible.
2. From the root directory, rebuild the project:
   ```bash
   mvn clean install -pl webapp-service -am
   ```
3. Run the web application:
   ```bash
   mvn spring-boot:run -pl webapp-service
   ```

## Configuration

The default port is `8081` so it doesn't conflict with `admin-service` (which typically runs on `8080`). You can change this in `application.yml` or via the `PORT` environment variable.
The default URL for `admin-service` is `http://localhost:8080/v1/admin`. Override it using the `ADMIN_SERVICE_URL` environment variable if needed.

## Access

Once running, access the web app at: [http://localhost:8081](http://localhost:8081)

**Default Login:**
- Username: `admin`
- Password: `password123`

## Features
- **Tenants**: Create and list tenants.
- **Subscriptions**: Create subscriptions for tenants.
- **Sites**: Create and manage sites for tenants.
- **Devices**: Create, manage, and provision devices. Contains a download button to generate the JSON bootstrap token.

## DB Schema for Users
A reference database schema for a future migration to database-backed authentication is provided in `src/main/resources/db/users_schema.sql`. Currently, the app uses Spring Security's in-memory authentication configured in `application.yml`.
