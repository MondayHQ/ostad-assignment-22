# Custom Authentication API

This is a simple custom authentication API built with Spring Boot. It provides endpoints for user registration, email verification, and login.

## Tech Stack

- Java 21
- Spring Boot 3
- Spring Security
- Maven
- PostgreSQL
- JWT for token-based authentication

## How to Run

1.  **Clone the repository:**
    ```bash
    git clone https://github.com/MondayHQ/ostad-assignment-22.git
    ```
2.  **Navigate to the project directory:**
    ```bash
    cd custom-authentication
    ```
3.  **Configure the database:**
    - Open `src/main/resources/application.properties`.
    - Update the `spring.datasource.url`, `spring.datasource.username`, and `spring.datasource.password` properties with your PostgreSQL database details.
4.  **Run the application:**
    ```bash
    mvn spring-boot:run
    ```
    The application will start on port 8080.

## API Endpoints

### 1. Create New Account

- **URL:** `/auth`
- **Method:** `POST`
- **Description:** Creates a new user account and sends a verification email.
- **Request Body:**
  ```json
  {
    "email": "user@example.com",
    "password": "password123"
  }
  ```
- **Success Response (201 CREATED):**
  ```json
  {
    "email": "user@example.com",
    "message": "Account created successfully. Please check your email for verification link."
  }
  ```

### 2. Verify Email

- **URL:** `/auth/verify`
- **Method:** `GET`
- **Description:** Verifies a user's email address using the provided token.
- **Query Parameters:**
  - `email`: The user's email address.
  - `token`: The verification token sent to the user's email.
- **Example URL:** `http://localhost:8080/auth/verify?email=user@example.com&token=your-verification-token`
- **Success Response (200 OK):**
  ```json
  {
    "email": "user@example.com",
    "message": "Email verified successfully."
  }
  ```

### 3. Login

- **URL:** `/auth/login`
- **Method:** `POST`
- **Description:** Authenticates a user and returns a JWT token. The user can login only if the email is verified.
- **Request Body:**
  ```json
  {
    "email": "user@example.com",
    "password": "password123"
  }
  ```
- **Success Response (200 OK):**
  ```json
  {
    "email": "user@example.com",
    "token": "your-jwt-token"
  }
  ```
