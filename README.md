# EzLearning

Plataforma de educacao inteligente -- tutor digital personalizado para estudantes de Ciencias Exatas. Suporte 24/7 com explicacoes multimodais (texto, audio, imagens, PDF).

## 1. Stack

| Camada         | Tecnologia                              |
|----------------|-----------------------------------------|
| Linguagem      | Java 21                                 |
| Framework      | Spring Boot 3.4.4                       |
| Build          | Maven                                   |
| Autenticacao   | Spring Security + JWT (jjwt 0.12.6)     |
| ORM            | Spring Data JPA + Hibernate             |
| Database       | PostgreSQL 16 (prod) / H2 (dev)         |
| Cache          | Redis 7                                 |
| Migrations     | Flyway                                  |
| Documentacao   | Springdoc OpenAPI (Swagger UI)           |
| Container      | Docker + Docker Compose                 |
| IA / Chat      | Ollama (gemma2:2b)                      |
| Diagramas      | Template engine + Kroki (Mermaid)        |
| TTS            | gTTS (Google Text-to-Speech) — gratuito |
| PDF            | Apache PDFBox 3.0.3                     |

## 2. APIs Externas

| API                        | Proposito                    | Provedor | Autenticacao            |
|----------------------------|------------------------------|----------|-------------------------|
| Ollama (gemma2:2b)         | Raciocinio logico (chat)     | Local    | Nao requerida           |
| gTTS (Google TTS)          | Sintese de voz pt-BR         | Google   | Nao requerida (gratuito)|
| Kroki + Mermaid            | Renderizacao de diagramas    | Local    | Nao requerida           |

## 3. Pre-requisitos

- Debian 13 (ou similar) com Docker Engine + Docker Compose
- Java 21 JDK + Maven 3.9+ (apenas para dev local)

## 4. Setup

### 1. Instalar Docker

```bash
sudo apt update
sudo apt install -y ca-certificates curl gnupg
sudo install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/debian/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/debian $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
sudo apt update
sudo apt install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin
sudo systemctl enable docker
sudo usermod -aG docker $USER
```

> Reinicie a sessao apos adicionar o grupo `docker`.

### 2. Clonar e configurar

```bash
git clone https://github.com/glhermeMelo/EzLearning
cd EzLearning
cp .env.example .env
```

Edite o `.env`:

| Variavel             | Descricao                             |
|----------------------|---------------------------------------|
| `JWT_SECRET`         | Chave para assinar tokens JWT         |
| `REASONING_API_URL`  | URL do Ollama (padrao: `http://ollama:11434/v1/chat/completions`) |
| `REASONING_API_KEY`  | Chave da API (nao usada pelo Ollama)  |
| `MEDIA_API_URL`      | URL da API de midia (nao usado atualmente) |
| `MEDIA_API_KEY`      | Chave da API de midia                 |
| `TTS_API_URL`        | URL do servico TTS (padrao: `http://tts:8880/v1/audio/speech`) |

> **Nota:** O chat/reason usa Ollama local. O campo `REASONING_API_KEY` pode ser qualquer valor.

### 3. Subir tudo

```bash
docker compose up -d
```

Apos inicio, os servicos estarao disponiveis em:

| Servico                | URL                                       |
|------------------------|-------------------------------------------|
| App (API REST)         | `http://localhost:8080`                    |
| Swagger UI             | `http://localhost:8080/swagger-ui/index.html` |
| OpenAPI JSON           | `http://localhost:8080/v3/api-docs`        |
| gTTS (audio)           | `http://localhost:8880`                    |
| Ollama                 | `http://localhost:11434`                   |
| Kroki (diagramas)      | `http://localhost:8000`                    |

### 4. Puxar modelos Ollama (se ainda nao fez)

```bash
docker exec ezlearning-ollama ollama pull gemma2:2b
```

---

## 5. Documentacao da API

A documentacao interativa completa esta no Swagger UI. Abaixo o resumo de todos os endpoints.

### 5.1. Autenticacao (visao geral)

Todos os endpoints exceto `/api/auth/**` e `/actuator/health` exigem token JWT no header:
```
Authorization: Bearer <token>
```

No Swagger UI, clique no botao **Authorize** (cadeado) e cole o token JWT. O token persiste entre recarregamentos.

---

### 5.2. Saude

`GET /actuator/health` — **Publico**

Retorna status da aplicacao e dependencias (DB, Redis, disco).

```bash
curl http://localhost:8080/actuator/health
```

---

### 5.3. Autenticacao (endpoints)

#### Registrar usuario

`POST /api/auth/register` — **Publico**

```json
// Request
{
  "name": "string (required)",
  "email": "string (required)",
  "password": "string (required)"
}

// Response 200
{
  "token": "string (JWT access token, valido 1h)",
  "refreshToken": "string (JWT refresh token, valido 7d)",
  "expiresIn": 3600,
  "user": {
    "name": "string",
    "email": "string"
  }
}
```

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Usuario","email":"user@exemplo.com","password":"senha1234"}'
```

#### Login

`POST /api/auth/login` — **Publico**

```json
// Request
{
  "email": "string (required)",
  "password": "string (required)"
}

// Response 200 (mesmo formato do register)
```

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"user@exemplo.com","password":"senha1234"}'
```

#### Refresh token

`POST /api/auth/refresh` — **Publico**

```json
// Request
{
  "refreshToken": "string"
}

// Response 200 (novo par access + refresh)
```

---

### 5.4. Chat / Raciocinio

#### Enviar pergunta

`POST /api/chat/reason` — **JWT**

```json
// Request
{
  "question": "string (required)",
  "context": "string (opcional)"
}

// Response 200
{
  "answer": "string (resposta completa do modelo)",
  "steps": ["string (passo 1)", "string (passo 2)", ...],
  "confidence": 0.95 (double)
}
```

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"user@exemplo.com","password":"senha1234"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['token'])")

curl -s http://localhost:8080/api/chat/reason \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"question":"Explique como resolver equacoes do segundo grau"}'
```

#### Streaming (SSE)

`GET /api/chat/stream?question=...&context=...` — **JWT**

Eventos SSE:
```
event: thinking
data: {"type": "thinking"}

event: result
data: {"type": "result", "data": {"answer": "...", "steps": [...], "confidence": 0.0}}

event: complete
data: {"type": "complete"}
```

```bash
curl -N http://localhost:8080/api/chat/stream?question=Quanto+e+2%2B2%3F \
  -H "Authorization: Bearer $TOKEN"
```

---

### 5.5. Sintese de Audio (TTS)

#### Gerar audio

`POST /api/tts/synthesize` — **JWT**

```json
// Request
{
  "text": "string (required)",
  "voice": "string (opcional, default: 'pt')"
}

// Response 200
{
  "audioUrl": "/api/tts/{id}",
  "durationSeconds": 0.0,
  "format": "mp3"
}
```

Vozes disponiveis: `pt` (portugues Brasil), `en` (ingles).

```bash
curl -s http://localhost:8080/api/tts/synthesize \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"text":"Ola, bem-vindo ao EzLearning!","voice":"pt"}'
```

#### Baixar audio

`GET /api/tts/{id}` — **JWT**

Retorna `audio/mpeg` (MP3). Use o `id` retornado pelo endpoint de sintese.

```bash
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/tts/<ID> \
  -o audio.mp3
```

---

### 5.6. Diagramas

#### Gerar diagrama

`POST /api/media/diagram` — **JWT**

```json
// Request
{
  "prompt": "string (required)"
}

// Response 200
{
  "id": "uuid",
  "description": "/api/media/{id}",
  "imageUrl": "/api/media/{id}/image"
}
```

```bash
curl -s http://localhost:8080/api/media/diagram \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"prompt":"diagrama dos passos da formula de Bhaskara"}'
```

#### Gerar midia avancada

`POST /api/media/generate` — **JWT**

```json
// Request
{
  "prompt": "string (required)",
  "style": "string",
  "diagramType": "string",
  "options": {}
}

// Response 200
{
  "id": "uuid",
  "url": "/api/media/{id}",
  "thumbnailUrl": "string ou null",
  "originalName": "string ou null",
  "size": 0,
  "mimeType": "string"
}
```

#### Baixar midia

`GET /api/media/{id}` — **JWT**

Retorna o conteudo binario da midia com o MIME type armazenado.

#### Baixar imagem do diagrama

`GET /api/media/{id}/image` — **JWT**

Retorna `image/png` — renderizacao do diagrama Mermaid pelo Kroki.

```bash
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/media/<ID>/image \
  -o diagrama.png
```

---

### 5.7. Upload de Imagens

#### Enviar imagem

`POST /api/uploads` — **JWT**

Multipart form-data com campo `file`. Formatos aceitos: `image/jpeg`, `image/png`. Maximo: 5 MB.

```json
// Response 200
{
  "id": "uuid",
  "url": "/api/uploads/{id}",
  "thumbnailUrl": "/api/uploads/{id}/thumbnail",
  "originalName": "string",
  "size": 0
}
```

```bash
curl -X POST http://localhost:8080/api/uploads \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@imagem.png"
```

#### Baixar imagem

`GET /api/uploads/{id}` — **JWT**

Retorna a imagem original.

#### Baixar thumbnail

`GET /api/uploads/{id}/thumbnail` — **JWT**

Retorna thumbnail da imagem enviada.

---

### 5.8. Exportacao PDF

#### Exportar duvida em PDF

`POST /api/chat/{messageId}/export` — **JWT**

```json
// Request
{
  "question": "string",
  "context": "string",
  "answer": "string",
  "steps": ["string", ...],
  "confidence": 0.95,
  "mediaIds": ["uuid", ...]
}

// Response 200: application/pdf
// Content-Disposition: attachment; filename="duvida-YYYY-MM-DD.pdf"
```

> O `{messageId}` e um UUID gerado pelo cliente.

```bash
curl -s http://localhost:8080/api/chat/$(uuidgen)/export \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "question":"Como resolver equacoes do segundo grau?",
    "context":"ax^2 + bx + c = 0",
    "answer":"Use a formula de Bhaskara...",
    "steps":["1. Identifique a, b, c","2. Calcule delta"],
    "confidence":0.95,
    "mediaIds":[]
  }' \
  -o duvida.pdf
```

---

## 6. Comandos uteis

```bash
# Subir tudo
docker compose up -d

# Rebuildar apos alteracoes no codigo
docker compose build --no-cache app
docker compose up -d --force-recreate app

# Ver logs
docker compose logs -f app

# Health check
curl -s http://localhost:8080/actuator/health | python3 -m json.tool

# Listar modelos Ollama
docker exec ezlearning-ollama ollama list
```

## 7. Estrutura do Projeto

```
EzLearning/
├── docker-compose.yml          # Orquestracao de todos os servicos
├── Dockerfile                  # Build multi-stage da aplicacao Java
├── pom.xml                     # Dependencias Maven
├── .env / .env.example         # Variaveis de ambiente
│
├── edge-tts/                   # Servico TTS (gTTS / Google)
│   ├── main.py                 # API FastAPI com endpoint /v1/audio/speech
│   └── requirements.txt        # Dependencias Python (gtts, fastapi, uvicorn)
│
├── src/main/
│   ├── java/com/ezlearning/
│   │   ├── EzLearningApplication.java
│   │   ├── config/
│   │   │   ├── AiApiProperties.java           # Properties das APIs externas
│   │   │   ├── CorsConfig.java                # CORS para frontend
│   │   │   ├── JwtAuthenticationFilter.java   # Filtro JWT
│   │   │   ├── OpenApiConfig.java             # Swagger + seguranca Bearer
│   │   │   ├── RateLimitingInterceptor.java   # Rate limiting via Redis
│   │   │   ├── RestTemplateConfig.java        # RestClient/RestTemplate beans
│   │   │   ├── SecurityConfig.java            # Spring Security + whitelist
│   │   │   └── WebSocketConfig.java           # STOMP/SockJS
│   │   ├── controller/
│   │   │   ├── AuthController.java            # /api/auth/*
│   │   │   ├── ChatController.java            # /api/chat/reason, /api/chat/stream
│   │   │   ├── HealthController.java          # /actuator/health
│   │   │   ├── MediaController.java           # /api/media/*
│   │   │   ├── PdfExportController.java       # /api/chat/{id}/export
│   │   │   ├── TtsController.java             # /api/tts/*
│   │   │   ├── UploadController.java          # /api/uploads/*
│   │   │   └── UploadExceptionHandler.java    # Tratamento de erros de upload
│   │   ├── integration/
│   │   │   ├── MediaApiClient.java            # Templates de diagramas Mermaid
│   │   │   ├── ReasoningApiClient.java        # Cliente HTTP Ollama
│   │   │   └── TtsApiClient.java              # Cliente HTTP gTTS
│   │   ├── model/
│   │   │   ├── GeneratedMedia.java            # Entidade generated_media
│   │   │   ├── UploadedImage.java             # Entidade uploaded_images
│   │   │   ├── User.java                      # Entidade users
│   │   │   └── dto/                           # DTOs de request/response
│   │   │       ├── LoginRequest/Response.java
│   │   │       ├── RegisterRequest.java
│   │   │       ├── ReasoningRequest/Response.java
│   │   │       ├── MediaRequest/Response.java
│   │   │       ├── MediaGenerationRequest/Response.java
│   │   │       ├── TtsRequest/Response.java
│   │   │       ├── PdfExportRequest.java
│   │   │       ├── UploadResponse.java
│   │   │       └── ErrorResponse.java
│   │   ├── repository/                        # Spring Data JPA repositories
│   │   ├── service/                           # Logica de negocio
│   │   │   ├── AuthServiceImpl.java           # Registro, login, refresh JWT
│   │   │   ├── JwtService.java                # Geracao/validacao de tokens
│   │   │   ├── MediaServiceImpl.java          # Diagramas (templates + Kroki)
│   │   │   ├── PdfExportServiceImpl.java      # Exportacao PDF (PDFBox)
│   │   │   ├── ReasoningServiceImpl.java      # Orquestracao chat Ollama
│   │   │   ├── TtsServiceImpl.java            # Sintese de audio via gTTS
│   │   │   └── UploadServiceImpl.java         # Upload + thumbnail
│   │   └── websocket/
│   │       ├── ChatWebSocketHandler.java      # Handler WebSocket
│   │       ├── JwtHandshakeInterceptor.java   # JWT no handshake
│   │       └── UserChannelInterceptor.java    # Canal por usuario
│   │
│   └── resources/
│       ├── application.yml                    # Config default (dev)
│       ├── application-dev.yml                # Perfil dev (H2)
│       ├── application-prod.yml               # Perfil prod (PostgreSQL)
│       └── db/migration/                      # Migrations Flyway
│           ├── V1__create_users_table.sql
│           ├── V2__create_uploaded_images_table.sql
│           ├── V3__create_history_tables.sql
│           └── V4__create_generated_media_table.sql
```

## 8. Desenvolvimento local (sem Docker)

```bash
# Precisa de Redis externo em localhost:6379
mvn spring-boot:run -Dspring.profiles.active=dev
```

Usa H2 em memoria e desabilita Flyway. Apenas o app Java — os demais servicos (Ollama, TTS, Kroki) precisam rodar separadamente.
