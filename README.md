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
| TTS              | Kokoro TTS (Docker)                            |
| API Externa      | Google Gemini API (raciocinio + geracao de midias) |

## Status

- US010 -- Infraestrutura (Docker, health check, estrutura base): concluida
- US009A -- API de raciocinio (integracao com Gemini, chat com raciocinio logico): concluida
- US008 -- Autenticacao (registro, login JWT, refresh token): concluida
- US001 -- Upload de imagem (upload, thumbnail, validacao JPG/PNG <=5MB): concluida

## Pre-requisitos

- Java 21 (Temurin ou OpenJDK)
- Docker + Docker Compose (para ambiente completo)
- Maven 3.9+ (opcional, se usar wrapper)
- NVIDIA GPU (para Kokoro TTS via Docker)

## Como Executar

### Docker (ambiente completo)

```bash
docker compose up
```

Sobe o app (`http://localhost:8080`), PostgreSQL 16, Redis 7 e Kokoro TTS (`http://localhost:5050`). O profile ativo e `prod`.

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
    RestTemplateConfig.java      # Configuracao do RestTemplate (timeouts)
    AiApiProperties.java         # Properties de configuracao da API de raciocinio
  controller/
    AuthController.java          # Endpoints /api/auth/*
    HealthController.java        # Endpoint /actuator/health
    MediaController.java         # Endpoints /api/media/*
    ChatController.java          # Endpoint /api/chat/reason
    UploadController.java        # Endpoints /api/uploads/*
    UploadExceptionHandler.java  # Tratamento de erros de upload
  integration/
    MediaApiClient.java          # Cliente HTTP resiliente para API externa de midias
    ReasoningApiClient.java      # Cliente HTTP resiliente para API de raciocinio
  service/
    AuthService.java             # Interface de autenticacao
    AuthServiceImpl.java         # Implementacao (registro, login, refresh)
    JwtService.java              # Geracao e validacao de tokens JWT
    MediaService.java            # Interface de geracao de midias
    MediaServiceImpl.java        # Implementacao (geracao, cache Redis, fallback BD)
    ReasoningService.java        # Interface de raciocinio
    ReasoningServiceImpl.java    # Implementacao (orquestracao, parsing)
    UploadService.java           # Interface de upload
    UploadServiceImpl.java       # Implementacao (upload, thumbnail)
  repository/
    GeneratedMediaRepository.java # Repositorio de midias geradas
    UploadedImageRepository.java
    UserRepository.java
  model/
    GeneratedMedia.java          # Entidade generated_media
    UploadedImage.java           # Entidade uploaded_images
    User.java                    # Entidade users
    dto/
      ErrorResponse.java
      LoginRequest.java
      LoginResponse.java
      MediaGenerationRequest.java  # DTO de requisicao de geracao
      MediaGenerationResponse.java # DTO de resposta de geracao
      ReasoningApiResponse.java    # DTO de resposta bruta da API externa
      ReasoningRequest.java        # DTO de requisicao de raciocinio
      ReasoningResponse.java       # DTO de resposta do raciocinio
      RegisterRequest.java
      UploadResponse.java
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

### Geracao de Midias

| Metodo | Rota                  | Descricao                            | Autenticacao |
|--------|-----------------------|--------------------------------------|--------------|
| POST   | `/api/media/generate` | Gera imagem a partir de prompt textual | Sim          |
| GET    | `/api/media/{id}`     | Serve imagem gerada                  | Sim          |

**Generate Request:**
```json
{
  "prompt": "um diagrama de classes UML",
  "style": "colorido",
  "diagramType": "flowchart",
  "options": {
    "theme": "dark",
    "resolution": "1920x1080"
  }
}
```

**Generate Response:**
```json
{
  "id": "uuid",
  "url": "/api/media/{id}",
  "thumbnailUrl": null,
  "originalName": "um diagrama de classes UML",
  "size": 12345,
  "mimeType": "image/png"
}
```

**Comportamento:**
- O prompt e hasheado (SHA-256) para servir como chave de cache no Redis
- Em caso de erro na API externa, busca no banco de dados (fallback)
- A imagem gerada e armazenada em `uploads/diagrams/{uuid}.png`
- Midias nao referenciadas sao removidas apos 1 hora (limpeza agendada)

### Raciocinio

| Metodo | Rota                 | Descricao                            | Autenticacao |
|--------|----------------------|--------------------------------------|--------------|
| POST   | `/api/chat/reason`   | Envia pergunta e recebe resposta com raciocinio | Sim |

**Reason Request:**
```json
{
  "question": "Como resolver uma equacao de segundo grau?",
  "context": "ax^2 + bx + c = 0 (opcional)"
}
```

**Reason Response:**
```json
{
  "answer": "Para resolver uma equacao de segundo grau, use a formula de Bhaskara...",
  "steps": [
    "Identifique os coeficientes a, b e c",
    "Calcule o discriminante: Delta = b^2 - 4ac",
    "Aplique a formula: x = (-b +- sqrt(Delta)) / 2a"
  ],
  "confidence": 0.95
}
```

**Comportamento:**
- Cliente HTTP resiliente com retry (3 tentativas, backoff exponencial 1s/2s/4s)
- Timeout configurado em 30s para leitura
- Erros 4xx sao lancados sem retry; 5xx e timeout disparam retry

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
| `TTS_API_URL`         | `http://localhost:5050/v1/audio/speech` | URL do Kokoro TTS |

## APIs Externas

| API            | Finalidade                          | Provedor    | Autenticacao             |
|----------------|-------------------------------------|-------------|--------------------------|
| Gemini (flash) | Raciocinio logico (raciocinio)      | Google      | API Key (Bearer token)   |
| Gemini (flash) | Geracao de midias (imagens/diagramas)| Google      | API Key (Bearer token)   |
| Kokoro TTS     | Sintese de voz (texto para audio)   | Kokoro      | Nao requerida (Docker local) |
