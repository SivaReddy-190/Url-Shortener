# URL Shortener

A simple Spring Boot-based URL Shortener that converts long URLs into short, shareable links using H2 as an in-memory database.

---

## Technologies Used

- Java
- Spring Boot
- Maven
- Lombok
- Guava
- Postgresql db

---

## Features

- Takes a long URL and generates a short, unique URL.
- Redirects short URLs to the original long URLs.
- REST API based implementation.

---

## API Endpoints

- `POST /generate`  
  Generate a short URL from a long URL.

- `GET /{shortLink}`  
  Redirect to the original long URL using the short link.
- `GET /stats/{shortLink}`
  Shows click_count
- `DELETE/{shortLink}`
  Deletes shortLink and original url
---

## How to Run the Project

1. **Clone the repository:**

```bash
git clone https://github.com/SivaReddy-190/Url-Shortner.git
cd Url-Shortner
mvn clean install
mvn spring-boot:run
