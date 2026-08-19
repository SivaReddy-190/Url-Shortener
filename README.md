# URL Shortener

A modern Spring Boot-based URL Shortener that converts long URLs into short, shareable links, backed by PostgreSQL and secured with JWT Authentication.

---

## Technologies Used

- Java 21
- Spring Boot 3.5.x
- Spring Security + JJWT (JSON Web Tokens)
- Spring Data JPA
- PostgreSQL
- Maven
- Lombok

---

## Features

- **Authentication & Authorization**: Secure API endpoints with JWT.
- **Ownership tracking**: Users manage their own short links.
- **Public redirection**: Anyone can use the short link to redirect to the original URL without authentication.
- **Link Expiration**: Short links expire after a configurable duration.
- **Click Tracking**: Tracks the number of times a short link is visited.
- **Clean Architecture**: Organized into feature-based packages (`auth`, `url`), with global exception handling.
- **Environment Profiles**: Configuration segregated into `dev` and `prod` profiles.

---

## API Endpoints

### Authentication (`/api/v1/auth`)
- `POST /register`: Create a new user account. Returns a JWT token.
- `POST /login`: Authenticate existing user. Returns a JWT token.

### URLs (`/api/v1/urls`) — *Requires Bearer Token*
- `POST /generate`: Generate a short URL from a long URL.
- `GET /stats/{shortLink}`: View click statistics for a short link (owner only).
- `DELETE /{shortLink}`: Delete a short link (owner only).
- `GET /my-links`: Retrieve all short links created by the authenticated user.

### Public
- `GET /{shortLink}`: Redirect to the original long URL.

---

## How to Run the Project

### Prerequisites
- Java 21
- PostgreSQL database
- JWT Secret configured via environment variable

### Setup

1. **Clone the repository:**
```bash
git clone https://github.com/SivaReddy-190/Url-Shortener.git
cd Url-Shortener
```

2. **Configure Database & Secrets:**
Ensure PostgreSQL is running and set the following environment variables:
```bash
# Windows PowerShell
$env:DB_USER = "your_db_username"
$env:DB_PASS = "your_db_password"
$env:JWT_SECRET = "your_base64_encoded_256_bit_secret"

# Linux / macOS
export DB_USER="your_db_username"
export DB_PASS="your_db_password"
export JWT_SECRET="your_base64_encoded_256_bit_secret"
```

3. **Run the Application:**
We use the `dev` profile for local development which enables auto schema generation and verbose SQL logging:
```bash
mvn clean install
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```
