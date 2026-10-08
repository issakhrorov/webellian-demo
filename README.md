# Authors API

Simple Java Spring Boot application that searches authors and their works. Data is read from MySQL first and falls back to the Open Library Authors API when the local database has no matching rows.

## Tech Stack

- Java 21
- Spring Boot
- Spring Web / WebClient
- Spring Data JPA / Hibernate
- Liquibase
- MySQL
- Docker / Docker Compose

## API

### Search Authors

```http
GET /api/authors?name=j%20k%20rowling
```

Response:

```json
[
  {
    "id": "OL23919A",
    "name": "J. K. Rowling"
  }
]
```

The service first searches cached authors by name. If there is no local match, it calls:

```text
https://openlibrary.org/search/authors.json?q={name}
```

Returned authors are saved to MySQL and then returned as JSON.

### Search Author Works

```http
GET /api/authors/OL23919A/works
```

Response:

```json
[
  {
    "id": "OL82563W",
    "title": "Harry Potter and the Philosopher's Stone"
  }
]
```

The service first searches cached works by author id. If there are no local works, it calls:

```text
https://openlibrary.org/authors/{authorId}/works.json?limit=100
```

Returned works are saved to MySQL and then returned as JSON.

## Run With Docker Compose

```bash
docker compose up --build
```

The API will be available at:

```text
http://localhost:8080
```

MySQL will be available at:

```text
localhost:3306
```

Default database settings:

```text
Database: authors_db
Username: authors_user
Password: authors_password
```

## Run Locally

Start MySQL first:

```bash
docker compose up -d mysql
```

Then run the application:

```bash
mvn spring-boot:run
```

Environment variables can override defaults:

```bash
DB_URL=jdbc:mysql://localhost:3306/authors_db \
DB_USERNAME=authors_user \
DB_PASSWORD=authors_password \
OPEN_LIBRARY_BASE_URL=https://openlibrary.org \
mvn spring-boot:run
```

## Build And Test

```bash
mvn clean test
mvn clean package
```

## Useful Curl Examples

```bash
curl "http://localhost:8080/api/authors?name=j%20k%20rowling"
curl "http://localhost:8080/api/authors/OL23919A/works"
curl "http://localhost:8080/actuator/health"
```

## Publish To Git

Create an empty GitHub/Bitbucket repository, then push this project:

```bash
git remote add origin <repository-url>
git add .
git commit -m "Create authors API service"
git push -u origin main
```

Repository link:

```text
https://github.com/issakhrorov/webellian-demo
```
