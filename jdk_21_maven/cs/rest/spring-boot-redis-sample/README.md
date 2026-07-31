# Spring Boot Redis Sample

A comprehensive Spring Boot 3.5.6 application demonstrating Redis integration with caching, data persistence, and
RESTful APIs.

## Features

- Spring Boot 3.5.6 with Java 21
- Redis Stack integration for data storage and caching
- Spring Data Redis for repository management
- Spring Security with BCrypt password encoding
- RESTful API endpoints for Books and Users management
- Redis caching with configurable TTL
- Docker Compose integration for easy Redis setup
- Sample data initialization on startup
- Testcontainers for integration testing

## Technologies

- **Spring Boot**: 3.5.6
- **Java**: 21
- **Redis**: Redis Stack (latest)
- **Spring Data Redis**: Repository pattern implementation
- **Spring Security**: User authentication and authorization
- **Spring Validation**: Input validation
- **Lombok**: Reduce boilerplate code
- **Docker Compose**: Container orchestration
- **Testcontainers**: Integration testing

## Prerequisites

- Java 21 or higher
- Docker and Docker Compose
- Maven 3.x

## Project Structure

```
spring-boot-redis-sample/
├── src/main/java/
│   └── id/my/hendisantika/springbootredissample/
│       ├── SpringBootRedisSampleApplication.java  # Main application class
│       ├── boot/                                  # Data initialization
│       │   ├── CreateBookRatings.java
│       │   ├── CreateBooks.java
│       │   ├── CreateRoles.java
│       │   └── CreateUsers.java
│       ├── controller/                            # REST Controllers
│       │   ├── BookController.java
│       │   └── UserController.java
│       ├── model/                                 # Domain models
│       │   ├── Book.java
│       │   ├── BookRating.java
│       │   ├── Category.java
│       │   ├── Role.java
│       │   └── User.java
│       └── repository/                            # Redis repositories
│           ├── BookRatingRepository.java
│           ├── BookRepository.java
│           ├── CategoryRepository.java
│           ├── RoleRepository.java
│           └── UserRepository.java
├── src/main/resources/
│   ├── application.properties                     # Application configuration
│   └── data/books/                                # Sample book data
├── compose.yaml                                   # Docker Compose configuration
└── pom.xml                                        # Maven dependencies
```

## Configuration

### Application Properties (application.properties:1)

```properties
spring.application.name=bookstore
spring.data.redis.host=localhost
spring.data.redis.port=6379
spring.data.redis.password=${REDIS_PASSWORD:53cret}
spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
app.numberOfRatings=5000
app.ratingStars=5
```

### Redis Configuration (compose.yaml:1)

The application uses Redis Stack with the following configuration:

- Port: 6379
- Password: 53cret
- Persistence: AOF (Append-Only File)
- Data directory: ./data

## Getting Started

### 1. Clone the repository

```bash
git clone <repository-url>
cd spring-boot-redis-sample2
```

### 2. Start Redis with Docker Compose

```bash
docker compose up -d
```

### 3. Build the application

```bash
./mvnw clean install
```

### 4. Run the application

```bash
./mvnw spring-boot:run
```

The application will start on http://localhost:8080

## API Endpoints

### Books API

#### Get all books (with pagination)

```bash
GET /api/books?page=0&size=10
```

Response:

```json
{
  "total": 2,
  "page": 0,
  "pages": 1,
  "books": [
    ...
  ]
}
```

#### Get book by ISBN

```bash
GET /api/books/{isbn}
```

#### Get all categories

```bash
GET /api/books/categories
```

### Users API

#### Get users by email

```bash
GET /api/users?email=yuji@yopmail.com
```

## Caching

The application uses Redis caching with the following configuration (SpringBootRedisSampleApplication.java:36):

- Cache TTL: 1 hour
- Cache name prefix: Package name
- Null values caching: Disabled

### Cached Endpoints

- `/api/books` - Cached with key: `page-size`
- `/api/books/categories` - Cached

## Data Models

### Book (Book.java:1)

- ISBN (ID)
- Title, Subtitle, Description
- Language, Page Count
- Price, Currency
- Authors (Set)
- Categories (Set)
- Thumbnail, Info Link

### User (User.java:1)

- ID
- Name, Email
- Password (BCrypt encrypted)
- Roles (Set)

### Category (Category.java:1)

- ID
- Name

### BookRating (BookRating.java:1)

- ID
- User reference
- Book reference
- Rating (1-5 stars)

## Testing

Run tests with:

```bash
./mvnw test
```

The project includes Testcontainers for integration testing with Redis.

## Docker Compose Services

The `compose.yaml` defines a Redis Stack service:

- Image: redis/redis-stack:latest
- Container name: redis
- Ports: 6379:6379
- Volume: ./data:/data
- Password protected: 53cret

## Development

### Spring Boot DevTools

The application includes Spring Boot DevTools for:

- Automatic application restart on code changes
- LiveReload server on port 35729

### Sample Data

On startup, the application automatically creates:

- 2 Roles (admin, customer)
- 5 Users
- 2 Books
- 5000 Book Ratings

## Building for Production

```bash
./mvnw clean package -DskipTests
java -jar target/spring-boot-redis-sample-0.0.1-SNAPSHOT.jar
```

## License

This project is for educational purposes.

## Author

- Name: Hendi Santika
- Link: s.id/hendisantika
- Email: hendisantika@yahoo.co.id
- Telegram: @hendisantika34

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.
