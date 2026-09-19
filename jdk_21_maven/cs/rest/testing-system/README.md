# Testing System

**_Note:_** 🚧 _This project is under development. Service interruptions may occur in the production environment._ 🚧

## Overview

The Testing System REST API is a backend application that manages test creation, participation, and result evaluation.
This project showcases backend development expertise using modern technologies and the **Spring Boot framework**.

## Key Features

- **User Management**: Admins control accounts and test assignments while test takers participate with automatic
  progress saving.
- **Test Management**: Admins create and assign tests with multiple question types (MCQ, Checkbox, True/False, Text).
- **Results Management**: Automatic evaluation with pass/fail status and summaries.
- **Secure and Scalable**: Role-based access control with little effort to scale, encrypted password storage, and
  modular design.

For a detailed breakdown of the software requirements, refer to [this document](Software%20Requirements%20Document.pdf).

## Tech Stack

- **Backend Framework**: Spring Boot 3
- **Database**: PostgreSQL
- **Persistence**: Spring Data JDBC and Spring JDBC Template
- **Caching**: Redis (for token blocklisting & autosave test progress)
- **Security**: Spring Security (Authentication & Authorization)
- **Containerization**: Docker
- **Documentation**: Swagger UI (OpenAPI)
- **Testing & Debugging**: Postman
- **Cloud Deployment**

## Diagrams

### Entity-Relationship Diagram (ERD)

The following ER diagram shows the core database structure of the Testing System:
![ER Diagram](ER_Diagram.jpg)

### API Examples

![Example responses](example_responses.png)

For exploring and testing complete API endpoints, visit
the [Swagger UI](https://testing.mirodil.dev/api/swagger-ui.html)
or [Postman collection](https://www.postman.com/mirodil-dev/workspace/testing-system) with saved
examples.

## Installation & Setup

If you'd like to simply access the API, you can use the
already [deployed application](https://testing.mirodil.dev/api/swagger-ui.html). However, if you prefer to set it
up locally, follow the steps below.

### Prerequisites

Before starting, ensure the following tools are installed on your system:

1. [**Docker**](https://docs.docker.com/get-started/get-docker/): Version 20.10 or higher

2. **Docker Compose**: Version 1.29 or higher (included with Docker Desktop)

### Steps

Clone the Testing System project from the GitHub repository:

```bash
git clone https://github.com/mirodilkamilov/testing-system.git
cd testing-system
```

Copy `.env.example` to create your `.env` file and make sure you set `SECRET_KEY` variable. It should be at least 32
character long:

```bash
cp .env.example .env
```

Use Maven to package the application as a JAR file. Skip tests during this step since containerized PostgreSQL and Redis
won't be running yet:

```bash
./mvnw clean package -DskipTests
```

Build and start the Docker containers (you can add the -d option to run in detached mode):

```docker
docker-compose up --build
```

Once the containers are running, the application will be available at http://localhost:8080. You can navigate
to http://localhost:8080/api/swagger-ui.html to view the API documentation.

To stop and remove the containers, run:

```docker
docker-compose down
```

### Frontend Integration

To allow frontend developers to integrate with the backend during local development, CORS has been configured to permit
requests from the following origins:

- http://localhost:3000
- http://127.0.0.1:3000

If you're working on a frontend application, you can make API calls to the backend without encountering CORS-related
issues. Ensure your frontend is running on one of the allowed origins above.

## Contribute

I developed this project to showcase my Java skills and web technologies. Developers interested in building a
user-friendly interface for this application are welcome to contribute. I also invite anyone who wants to enhance and
extend the existing functionality, helping this project evolve and improve.

