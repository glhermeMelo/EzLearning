# EzLearning

Plataforma de educacao inteligente -- tutor digital personalizado para estudantes de Ciencias Exatas. Suporte 24/7 com explicacoes multimodais (texto, audio, imagens).

## Stack

| Camada           | Tecnologia                                     |
|------------------|------------------------------------------------|
| Linguagem        | Java 21                                        |
| Framework        | Spring Boot 3.4.4                              |
| Build            | Maven                                          |
| Autenticacao     | Spring Security + JWT (jjwt 0.12.6)            |
| ORM              | Spring Data JPA + Hibernate                    |
| Database         | PostgreSQL 16 (prod) / H2 (dev)                |
| Cache            | Redis 7                                        |
| Migrations       | Flyway                                         |
| Documentacao     | Springdoc OpenAPI (Swagger UI)                 |
| Container        | Docker + Docker Compose                        |
| PDF              | Apache PDFBox 3.0.3                            |

## Status

- US010 -- Infraestrutura (Docker, health check, estrutura base): concluida
- US008 -- Autenticacao (registro, login JWT, refresh token): concluida
- US001 -- Upload de imagem (upload, thumbnail, validacao JPG/PNG <=5MB): concluida

## Pre-requisitos

- Java 21 (Temurin ou OpenJDK)
- Docker + Docker Compose (para ambiente completo)
- Maven 3.9+ (opcional, se usar wrapper)

## Como Executar

### Docker (ambiente completo)

```bash
docker compose up
```

Sobe o app (`http://localhost:8080`), PostgreSQL 16 e Redis 7. O profile ativo e `prod`.

### Desenvolvimento local (H2 + Redis)

```bash
mvn spring-boot:run -Dspring.profiles.active=dev
```

Usa H2 em memoria e Redis externo (`localhost:6379`). Flyway desabilitado, JPA cria as tabelas automaticamente.

### Compilar

```bash
mvn clean compile
```

### Testar

```bash
mvn test
```

## Estrutura do Projeto

```
src/main/java/com/ezlearning/
  config/
    CorsConfig.java              # Configuracao CORS
    SecurityConfig.java          # Seguranca, JWT filter, BCrypt
    JwtAuthenticationFilter.java # Filtro de autenticacao JWT
  controller/
    AuthController.java          # Endpoints /api/auth/*
    HealthController.java        # Endpoint /actuator/health
    UploadController.java        # Endpoints /api/uploads/*
    UploadExceptionHandler.java  # Tratamento de erros de upload
  service/
    AuthService.java             # Interface de autenticacao
    AuthServiceImpl.java         # Implementacao (registro, login, refresh)
    JwtService.java              # Geracao e validacao de tokens JWT
    UploadService.java           # Interface de upload
    UploadServiceImpl.java       # Implementacao (upload, thumbnail)
  repository/
    UserRepository.java
    UploadedImageRepository.java
  model/
    User.java                    # Entidade users
    UploadedImage.java           # Entidade uploaded_images
    dto/
      RegisterRequest.java
      LoginRequest.java
      LoginResponse.java
      UploadResponse.java
      ErrorResponse.java
  EzLearningApplication.java

src/main/resources/
  application.yml               # Configuracao principal
  application-dev.yml           # Profile dev (H2)
  application-prod.yml          # Profile prod (tuning)
  db/migration/
    V1__create_users_table.sql
    V2__create_uploaded_images_table.sql
```

## API Endpoints

### Saude

| Metodo | Rota               | Descricao          | Autenticacao |
|--------|--------------------|--------------------|--------------|
| GET    | `/actuator/health` | Health check       | Nao          |

### Autenticacao

| Metodo | Rota                | Descricao                          | Autenticacao |
|--------|---------------------|------------------------------------|--------------|
| POST   | `/api/auth/register`| Cadastro de usuario                | Nao          |
| POST   | `/api/auth/login`   | Login (retorna access + refresh)   | Nao          |
| POST   | `/api/auth/refresh` | Renova access token via refresh    | Nao          |

**Register Request:**
```json
{
  "name": "string",
  "email": "string",
  "password": "string"
}
```

**Login Request:**
```json
{
  "email": "string",
  "password": "string"
}
```

**Login Response (register, login, refresh):**
```json
{
  "token": "string (access token)",
  "refreshToken": "string (refresh token)"
}
```

**Refresh Request:**
```json
{
  "refreshToken": "string"
}
```

### Upload de Imagens

| Metodo | Rota                        | Descricao                    | Autenticacao |
|--------|-----------------------------|------------------------------|--------------|
| POST   | `/api/uploads`              | Upload de imagem (<=5MB)     | Nao          |
| GET    | `/api/uploads/{id}`         | Servir imagem original       | Nao          |
| GET    | `/api/uploads/{id}/thumbnail`| Servir thumbnail            | Nao          |

**Upload Response:**
```json
{
  "id": "uuid",
  "url": "/api/uploads/{id}",
  "thumbnailUrl": "/api/uploads/{id}/thumbnail",
  "originalName": "string",
  "size": 0
}
```

**Restricoes:**
- Formatos aceitos: `image/jpeg`, `image/png`
- Tamanho maximo: 5MB

### Documentacao Interativa

- Swagger UI: `/swagger-ui.html`
- OpenAPI JSON: `/api-docs`

## Variaveis de Ambiente

| Variavel              | Default            | Descricao                     |
|-----------------------|--------------------|-------------------------------|
| `DATABASE_URL`        | `jdbc:postgresql://localhost:5432/ezlearning` | URL do PostgreSQL |
| `DATABASE_USER`       | `ezlearning`       | Usuario do banco              |
| `DATABASE_PASSWORD`   | `ezlearning`       | Senha do banco                |
| `REDIS_HOST`          | `localhost`        | Host do Redis                 |
| `JWT_SECRET`          | -- (obrigatorio)   | Chave secreta para assinar JWT |
| `CORS_ALLOWED_ORIGINS`| `http://localhost:5173,http://localhost:3000` | Origens permitidas CORS |
| `REASONING_API_URL`   | --                 | URL da API de raciocinio      |
| `REASONING_API_KEY`   | --                 | Chave da API de raciocinio    |
| `MEDIA_API_URL`       | --                 | URL da API de midia           |
| `MEDIA_API_KEY`       | --                 | Chave da API de midia         |
