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
- US009B -- Geracao de midias (Gemini, diagramas, cache): concluida
- US009C -- Comunicacao em tempo real (WebSocket/SSE, rate limiting Redis): concluida
- US008 -- Autenticacao (registro, login JWT, refresh token): concluida
- US001 -- Upload de imagem (upload, thumbnail, validacao JPG/PNG <=5MB): concluida
- US005 -- Sintese de audio (Kokoro TTS): concluida

**Testes:** 53 testes passando (JDK 21 + Maven).

## Pre-requisitos

- Debian 13 (ou similar)
- Docker Engine + Docker Compose (veja Setup abaixo)
- NVIDIA GPU com drivers proprietarios (para Kokoro TTS)
- Java 21 JDK (para compilar localmente, via `apt install openjdk-21-jdk`)
- Maven 3.9+ (opcional, ou use ./mvnw)
- Chave de API Google Gemini (gratuita em https://aistudio.google.com/apikey)

## Setup no Debian 13

Guia passo a passo para configurar o ambiente do zero em uma maquina Debian 13 (baseado em Bookworm).

### 1. Instalar Docker Engine

```bash
sudo apt update
sudo apt install -y ca-certificates curl gnupg
sudo install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/debian/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/debian $(. /etc/os-release && echo \"$VERSION_CODENAME\") stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
sudo apt update
sudo apt install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin
sudo systemctl enable docker
sudo usermod -aG docker $USER
```

> **Importante:** E necessario reiniciar a sessao (logout/login) ou reiniciar a maquina para que o grupo `docker` tenha efeito.

### 2. Instalar NVIDIA Container Toolkit (para RTX 3060)

```bash
curl -fsSL https://nvidia.github.io/libnvidia-container/gpgkey | sudo gpg --dearmor -o /usr/share/keyrings/nvidia-container-toolkit-keyring.gpg
curl -s -L https://nvidia.github.io/libnvidia-container/stable/deb/nvidia-container-toolkit.list | sed 's#deb https://#deb [signed-by=/usr/share/keyrings/nvidia-container-toolkit-keyring.gpg] https://#g' | sudo tee /etc/apt/sources.list.d/nvidia-container-toolkit.list
sudo apt update
sudo apt install -y nvidia-container-toolkit
sudo nvidia-ctk runtime configure --runtime=docker
sudo systemctl restart docker
```

### 3. Verificar GPU

Confirme que o Docker consegue acessar a GPU:

```bash
docker run --rm --gpus all nvidia/cuda:12.6.2-base-ubuntu22.04 nvidia-smi
```

Se o comando acima exibir as informacoes da GPU (RTX 3060), a configuracao foi bem-sucedida.

### 4. Criar arquivo .env

```bash
cp .env.example .env
```

Edite o arquivo `.env` e preencha as seguintes variaveis obrigatorias:

- `JWT_SECRET` -- chave secreta para assinar os tokens JWT. Gere uma com:
  ```bash
  openssl rand -base64 64
  ```
- `REASONING_API_KEY` -- chave da API Google Gemini (raciocinio)
- `MEDIA_API_KEY` -- chave da API Google Gemini (geracao de midias)

> **Nota:** Tanto `REASONING_API_KEY` quanto `MEDIA_API_KEY` podem usar a **mesma chave** do Google Gemini, ja que ambos os servicos utilizam a API Gemini. Obtenha sua chave gratuitamente em https://aistudio.google.com/apikey.

### 5. Build o backend (necessario antes do Docker Compose se quiser imagem local)

```bash
./mvnw clean package -DskipTests
```

Ou, se tiver o Maven instalado globalmente:

```bash
mvn clean package -DskipTests
```

### 6. Subir tudo

```bash
docker compose up -d
```

Apos a execucao, os servicos estarao disponiveis em:

- **App:** http://localhost:8080
- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **Kokoro TTS:** http://localhost:5050

## Como Executar

### Docker (ambiente completo)

```bash
docker compose up
```

Sobe o app (`http://localhost:8080`), PostgreSQL 16, Redis 7 e Kokoro TTS (`http://localhost:5050`). O profile ativo e `prod`.

### Desenvolvimento local (H2 + Redis)

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
mvn spring-boot:run -Dspring.profiles.active=dev
```

Usa H2 em memoria e Redis externo (`localhost:6379`). Flyway desabilitado, JPA cria as tabelas automaticamente.

### Compilar

```bash
mvn clean compile
```

### Testar

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
& "C:\Users\Guilherme\Tools\apache-maven-3.9.9\bin\mvn.cmd" test
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
    WebSocketConfig.java         # Configuracao WebSocket/STOMP
    RateLimitingInterceptor.java # Interceptor de rate limiting (Redis)
  controller/
    AuthController.java          # Endpoints /api/auth/*
    HealthController.java        # Endpoint /actuator/health
    MediaController.java         # Endpoints /api/media/*
    ChatController.java          # Endpoint /api/chat/reason
    UploadController.java        # Endpoints /api/uploads/*
    UploadExceptionHandler.java  # Tratamento de erros de upload
    TtsController.java           # Endpoints /api/tts/*
  integration/
    MediaApiClient.java          # Cliente HTTP resiliente para API externa de midias
    ReasoningApiClient.java      # Cliente HTTP resiliente para API de raciocinio
    TtsApiClient.java            # Cliente HTTP para Kokoro TTS
  service/
    AuthService.java             # Interface de autenticacao
    AuthServiceImpl.java         # Implementacao (registro, login, refresh)
    JwtService.java              # Geracao e validacao de tokens JWT
    MediaService.java            # Interface de geracao de midias
    MediaServiceImpl.java        # Implementacao (geracao, cache Redis, fallback BD)
    ReasoningService.java        # Interface de raciocinio
    ReasoningServiceImpl.java    # Implementacao (orquestracao, parsing)
    TtsService.java              # Interface de sintese de audio
    TtsServiceImpl.java          # Implementacao (Kokoro TTS)
    UploadService.java           # Interface de upload
    UploadServiceImpl.java       # Implementacao (upload, thumbnail)
  websocket/
    ChatWebSocketHandler.java    # Handler de mensagens WebSocket
    JwtHandshakeInterceptor.java # Interceptor JWT no handshake WebSocket
    UserChannelInterceptor.java  # Interceptor de canal para usuario
  repository/
    GeneratedMediaRepository.java # Repositorio de midias geradas
    UploadedImageRepository.java
    UserRepository.java
  model/
    GeneratedMedia.java          # Entidade generated_media
    UploadedImage.java           # Entidade uploaded_images
    User.java                    # Entidade users
    dto/
      ChatMessageRequest.java
      ChatStreamEvent.java
      ErrorResponse.java
      LoginRequest.java
      LoginResponse.java
      MediaGenerationRequest.java  # DTO de requisicao de geracao
      MediaRequest.java            # DTO de requisicao de diagrama
      MediaResponse.java           # DTO de resposta de diagrama
      MediaGenerationResponse.java # DTO de resposta de geracao
      ReasoningApiResponse.java    # DTO de resposta bruta da API externa
      ReasoningRequest.java        # DTO de requisicao de raciocinio
      ReasoningResponse.java       # DTO de resposta do raciocinio
      RegisterRequest.java
      TtsRequest.java
      TtsResponse.java
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

| Metodo | Rota                       | Descricao                                 | Autenticacao |
|--------|----------------------------|-------------------------------------------|--------------|
| POST   | `/api/media/generate`      | Gera imagem a partir de prompt textual    | Sim          |
| POST   | `/api/media/diagram`       | Gera diagrama a partir de prompt textual  | Sim          |
| GET    | `/api/media/{id}`          | Serve o conteudo binario da midia gerada  | Sim          |
| GET    | `/api/media/{id}/image`    | Serve a imagem PNG do diagrama            | Sim          |

**Generate Request (POST /api/media/generate):**
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

**Diagram Request (POST /api/media/diagram):**
```json
{
  "prompt": "diagrama de arquitetura em camadas"
}
```

**Diagram Response:**
```json
{
  "id": "uuid",
  "description": "/api/media/{id}",
  "imageUrl": "/api/media/{id}/image"
}
```

**Comportamento:**
- O prompt e hasheado (SHA-256) para servir como chave de cache no Redis
- Em caso de erro na API externa, busca no banco de dados (fallback)
- A midia gerada e armazenada em `uploads/diagrams/{uuid}.md`
- A imagem PNG extraida e armazenada em `uploads/diagrams/{uuid}.png`
- Midias nao referenciadas sao removidas apos 1 hora (limpeza agendada)

### Midias/Diagramas

| Metodo | Rota                | Descricao                   | Autenticacao |
|--------|---------------------|-----------------------------|--------------|
| POST   | `/api/media/diagram`| Gera diagrama               | Sim          |
| GET    | `/api/media/{id}`   | Serve arquivo de midia      | Sim          |

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

### Sintese de Audio

| Metodo | Rota                  | Descricao                     | Autenticacao |
|--------|-----------------------|--------------------------------|--------------|
| POST   | `/api/tts/synthesize` | Sintetiza texto em audio WAV  | Sim          |
| GET    | `/api/tts/{id}`       | Serve arquivo de audio        | Sim          |

**Synthesize Request (POST /api/tts/synthesize):**
```json
{
  "text": "Texto para sintetizar",
  "voice": "af_heart"
}
```

**Synthesize Response:**
```json
{
  "audioUrl": "/api/tts/{uuid}",
  "durationSeconds": 0.0,
  "format": "wav"
}
```

Voice padrao: `af_heart`. Vozes alternativas: `am_michelle`, `af_bella`.

### Comunicacao em Tempo Real

| Metodo | Rota                                   | Descricao                              | Autenticacao |
|--------|----------------------------------------|----------------------------------------|--------------|
| GET    | `/api/chat/stream?question=&context=`  | SSE streaming de raciocinio            | Sim          |
| WS     | `/ws` (STOMP over SockJS)              | WebSocket para chat em tempo real      | Sim (token via query param) |

**SSE Events:**
```
event: thinking
data: {"type": "thinking"}

event: result
data: {"type": "result", "data": {"answer": "...", "steps": [], "confidence": 0.0}}

event: complete
data: {"type": "complete"}
```

**WebSocket:**
- Conectar em `/ws` com `?token={jwt}`
- Enviar para `/app/chat`: `{"question": "...", "context": "..."}`
- Receber eventos em `/topic/chat/{userId}`

### Rate Limiting

- Maximo 5 requisicoes/minuto por usuario (WebSocket e SSE)
- Implementado com Redis (contador com TTL de 60s)
- Resposta 429 ou evento de erro quando excedido

### Documentacao Interativa

- Swagger UI: `/swagger-ui.html`
- OpenAPI JSON: `/api-docs`

## Variaveis de Ambiente

| Variavel              | Default            | Descricao                     | Autenticacao     |
|-----------------------|--------------------|-------------------------------|------------------|
| `DATABASE_URL`        | `jdbc:postgresql://localhost:5432/ezlearning` | URL do PostgreSQL | `DATABASE_USER` + `DATABASE_PASSWORD` |
| `DATABASE_USER`       | `ezlearning`       | Usuario do banco              | --               |
| `DATABASE_PASSWORD`   | `ezlearning`       | Senha do banco                | --               |
| `REDIS_HOST`          | `localhost`        | Host do Redis                 | Nao requerida    |
| `JWT_SECRET`          | -- (obrigatorio)   | Chave secreta para assinar JWT | Nao requerida (usada internamente) |
| `CORS_ALLOWED_ORIGINS`| `http://localhost:5173,http://localhost:3000` | Origens permitidas CORS | Nao requerida |
| `REASONING_API_URL`   | `https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent` | URL da API de raciocinio | API Key (query param) |
| `REASONING_API_KEY`   | -- (obrigatorio)   | Chave da API de raciocinio    | Google Gemini     |
| `MEDIA_API_URL`       | `https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent` | URL da API de midia | API Key (query param) |
| `MEDIA_API_KEY`       | -- (obrigatorio)   | Chave da API de midia         | Google Gemini     |
| `TTS_API_URL`         | `http://localhost:5050/v1/audio/speech` | URL do Kokoro TTS | Nao requerida (Docker local) |

## APIs Externas

| API            | Finalidade                          | Provedor    | URL                                                     | Autenticacao             |
|----------------|-------------------------------------|-------------|---------------------------------------------------------|--------------------------|
| Gemini (flash) | Raciocinio logico (raciocinio)      | Google      | `https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent` | API Key (query param) |
| Gemini (flash) | Geracao de midias (imagens/diagramas)| Google      | `https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent` | API Key (query param) |
| Kokoro TTS     | Sintese de voz (texto para audio)   | Kokoro      | `http://localhost:5050/v1/audio/speech` (Docker local)  | Nao requerida            |
