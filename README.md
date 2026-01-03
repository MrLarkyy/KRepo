# 📦 KRepo

[![CodeFactor](https://www.codefactor.io/repository/github/mrlarkyy/krepo/badge)](https://www.codefactor.io/repository/github/mrlarkyy/krepo)
![Kotlin](https://img.shields.io/badge/kotlin-2.3.0-purple.svg?logo=kotlin)
[![Discord](https://img.shields.io/discord/884159187565826179?color=5865F2&label=Discord&logo=discord&logoColor=white)](https://discord.com/invite/ffKAAQwNdC)

**KRepo** is a lightweight, high-performance artifact repository manager built with Kotlin and Spring Boot. Inspired by Reposilite, it provides a simple way to host your Maven artifacts using either local file storage or Amazon S3.

## ✨ Features

- **Multi-Storage Support**: Switch between Local File System and S3-compatible storage.
- **Stateless Auth**: Secure your deployments with JWT-based authentication.
- **Deploy Tokens**: Manage granular access for CI/CD pipelines (supports `tk_` prefixes).
- **Console Interface**: Manage users, tokens, and repositories directly from the terminal.
- **Modern Stack**: Built with Kotlin 2.3.0, Java 25, and Spring Data JPA.

---

## 🚀 Quick Start

### 1. Requirements
* **Java 25** or higher.
* **PostgreSQL** (for production) or H2 (for testing).

### 2. Running the Application
Clone the repository and build the project using Gradle:

```bash
./gradlew bootRun
```

The application will start on `http://localhost:8080` by default.

### 3. Initial Setup (CLI)
KRepo includes an interactive console that starts with the application. Use it to set up your first user and tokens:

Admin user is created automatically with a random password.

* **Generate a Deploy Token:** `token-generate admin my-token READ WRITE`
* **List Repositories:** `repo-list`

---

## 🖥️ Console Commands Reference

Once the application is running, you can manage everything via the terminal:

### Repository Management
- `repo-list`: List all managed repositories.
- `repo-set-visibility <name> <PUBLIC|PRIVATE|HIDDEN>`: Change who can see/access the repository.

### User Management
- `user-list`: List all registered users and their roles.
- `user-create <username> <password> <ROLES...>`: Create a new account (e.g., `ROLE_USER,ROLE_ADMIN`).
- `user-delete <username>`: Remove a user account.
- `user-promote <username>`: Grant a user the `ROLE_ADMIN` role.
- `user-demote <username>`: Remove the `ROLE_ADMIN` role from a user.

### Token Management
- `token-generate <username> <token_name> <PERMS...>`: Generate a deploy token (e.g., `READ,WRITE`).
- `token-reset <username> <token_name>`: Revoke the old token and generate a new raw value.

### System
- `help`: Show all available commands.
- `exit`: Safely shutdown the application.

---

## 🛠 Configuration

KRepo can be configured via `src/main/resources/application.yml`.

### Storage Setup
You can choose your preferred storage provider:

#### File System (Default)
```yaml
krepo:
  storage:
    provider: fs
    fs:
      root-path: ./data/repositories
```

#### Amazon S3
```yaml
krepo:
  storage:
    provider: s3
    s3:
      bucket-name: my-krepo-bucket
      region: us-east-1
      access-key: ${S3_ACCESS_KEY}
      secret-key: ${S3_SECRET_KEY}
```

---

## 🤖 API
[Read the API Documentation](https://petstore.swagger.io/?url=https://raw.githubusercontent.com/MrLarkyy/KRepo/master/openapi.json)

---

## 📦 Using KRepo in your Build Script

To use KRepo in your Gradle project, add the following to your `build.gradle.kts`:

### For consuming artifacts:
```kotlin
repositories {
    maven {
        url = uri("http://your-domain.com/releases")
    }
}
```

### For deploying artifacts:
```kotlin
publishing {
    repositories {
        maven {
            url = uri("http://your-domain.com/releases")
            credentials {
                username = "admin" 
                password = "tk_your-deploy-token" // Tokens must start with tk_
            }
        }
    }
}
```

---

## 🛠 Development

### Project Structure
- `gg.aquatic.krepo.repository`: Core logic for managing repository entities and resources.
- `gg.aquatic.krepo.storage`: Implementation of FS and S3 storage providers.
- `gg.aquatic.krepo.security`: JWT and Token-based authentication logic.
- `gg.aquatic.krepo.ConsoleCommands`: Interactive CLI management tool.

### Running Tests
```bash
./gradlew test
```

## 💬 Community & Support

Got questions, need help, or want to showcase what you've built with **KEvent**? Join our community!

[![Discord Banner](https://img.shields.io/badge/Discord-Join%20our%20Server-5865F2?style=for-the-badge&logo=discord&logoColor=white)](https://discord.com/invite/ffKAAQwNdC)

*   **Discord**: [Join the Aquatic Development Discord](https://discord.com/invite/ffKAAQwNdC)
*   **Issues**: Open a ticket on GitHub for bugs or feature requests.

---