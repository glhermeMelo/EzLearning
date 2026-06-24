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
- US008 -- Autenticacao (registro, login JWT, refresh token): concluida
- US001 -- Upload de imagem (upload, thumbnail, validacao JPG/PNG <=5MB): concluida
- US009A -- API de raciocinio (integracao com Gemini, chat com raciocinio logico): concluida
- US009B -- Geracao de midias (Gemini, diagramas, cache Redis + banco): concluida
- US009C -- Comunicacao em tempo real (WebSocket/SSE, rate limiting Redis): concluida
- US005 -- Sintese de audio (Kokoro TTS): concluida
- US006 -- Exportacao PDF (Apache PDFBox, cabecalho + pergunta + resposta com passos + diagramas, cache SHA-256): concluida

**Testes:** 59 testes passando (JDK 21 + Maven), 0 falhas, 0 erros.

## Pre-requisitos

- Debian 13 (ou similar)
- Docker Engine + Docker Compose (veja Setup abaixo)
- NVIDIA GPU com drivers proprietarios (para Kokoro TTS)
- Java 21 JDK (para compilar localmente, via `apt install openjdk-21-jdk`)
- Maven 3.9+ (opcional, ou use ./mvnw)
- Chave de API Google Gemini (gratuita em https://aistudio.google.com/apikey)

## Setup no Debian 13

Guia passo a passo para configurar o ambiente do zero em uma maquina Debian 13.

### 1. Instalar Docker Engine

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

> **Importante:** E necessario reiniciar a sessao (logout/login) ou reiniciar a maquina para que o grupo `docker` tenha efeito.

### 2. Instalar o driver NVIDIA

O Debian 13 nao inclui drivers proprietarios NVIDIA por padrao. E necessario habilitar os repositorios `non-free` antes de instalar.

#### 2.1. Habilitar repositorios non-free

Edite o arquivo `/etc/apt/sources.list`:

```bash
sudo nano /etc/apt/sources.list
```

Garanta que as linhas contenham `contrib non-free non-free-firmware`:

```
deb http://deb.debian.org/debian trixie main contrib non-free non-free-firmware
deb http://deb.debian.org/debian-security trixie-security main contrib non-free non-free-firmware
```

#### 2.2. Instalar o driver

```bash
sudo apt update
sudo apt install -y nvidia-driver firmware-misc-nonfree
```

#### 2.3. Reiniciar (obrigatorio)

```bash
sudo reboot
```

#### 2.4. Compilar o modulo do kernel (DKMS)

Apos reiniciar, o modulo NVIDIA pode nao ter sido compilado automaticamente para o kernel atual. Instale os headers e force a compilacao via DKMS:

```bash
# Instala os headers do kernel atual
sudo apt install -y linux-headers-$(uname -r)

# Compila e instala o modulo (substitua a versao se diferente)
sudo dkms install nvidia/550.163.01 -k $(uname -r)
```

> **Nota:** A linha `Autoinstall on <kernel> succeeded for module(s) nvidia-current` confirma que a compilacao foi bem-sucedida. O erro `Could not find module source directory` ao final pode ser ignorado -- e um artefato do comando manual e nao afeta o resultado.

Verifique se o modulo ficou com status `installed`:

```bash
sudo dkms status
```

#### 2.5. Carregar o modulo e verificar

```bash
sudo modprobe nvidia
nvidia-smi
```

A saida deve exibir informacoes da GPU (modelo, Driver Version, CUDA Version):

```
+-----------------------------------------------------------------------------------------+
| NVIDIA-SMI 550.163.01   Driver Version: 550.163.01   CUDA Version: 12.4               |
|-----------------------------------------+------------------------+----------------------+
|   0  NVIDIA GeForce RTX 3060 ...    Off |   00000000:01:00.0 Off |                  N/A |
+-----------------------------------------------------------------------------------------+
```

### 3. Instalar NVIDIA Container Toolkit

```bash
curl -fsSL https://nvidia.github.io/libnvidia-container/gpgkey | sudo gpg --dearmor -o /usr/share/keyrings/nvidia-container-toolkit-keyring.gpg
curl -s -L https://nvidia.github.io/libnvidia-container/stable/deb/nvidia-container-toolkit.list | sed 's#deb https://#deb [signed-by=/usr/share/keyrings/nvidia-container-toolkit-keyring.gpg] https://#g' | sudo tee /etc/apt/sources.list.d/nvidia-container-toolkit.list
sudo apt update
sudo apt install -y nvidia-container-toolkit
sudo nvidia-ctk runtime configure --runtime=docker
sudo systemctl restart docker
```

### 4. Verificar GPU no Docker

Confirme que o Docker consegue acessar a GPU:

```bash
docker run --rm --gpus all nvidia/cuda:12.6.2-base-ubuntu22.04 nvidia-smi
```

Se o comando exibir as informacoes da GPU, a configuracao foi bem-sucedida.

### 5. Criar arquivo .env

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

> **Nota:** Apenas **1 chave externa** e necessaria: a chave do Google Gemini (gratuita em https://aistudio.google.com/apikey). Tanto `REASONING_API_KEY` quanto `MEDIA_API_KEY` podem usar a **mesma chave**, ja que ambos os servicos utilizam a API Gemini. O Kokoro TTS roda localmente via Docker e nao requer chave.

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
    PdfExportController.java     # Endpoint /api/chat/{messageId}/export
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
    PdfExportService.java        # Interface de exportacao PDF
    PdfExportServiceImpl.java    # Implementacao (Apache PDFBox, cache SHA-256)
  websocket/
    ChatWebSocketHandler.java    # Handler de mensagens WebSocket
    JwtHandshakeInterceptor.java # Interceptor JWT no handshake WebSocket
    UserChannelInterceptor.java  # Interceptor de canal para usuario
  repository/
    GeneratedMediaRepository.java
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
      MediaGenerationRequest.java
      MediaGenerationResponse.java
      MediaRequest.java
      MediaResponse.java
      PdfExportRequest.java
      ReasoningApiResponse.java
      ReasoningRequest.java
      ReasoningResponse.java
      RegisterRequest.java
      TtsRequest.java
      TtsResponse.java
      UploadResponse.java
  EzLearningApplication.java

src/main/resources/
  application.yml
  application-dev.yml
  application-prod.yml
  db/migration/
    V1__create_users_table.sql
    V2__create_uploaded_images_table.sql
```

## API Endpoints

### Saude

| Metodo | Rota               | Descricao    | Autenticacao |
|--------|--------------------|--------------|--------------|
| GET    | `/actuator/health` | Health check | Nao          |

### Autenticacao

| Metodo | Rota                 | Descricao                        | Autenticacao |
|--------|----------------------|----------------------------------|--------------|
| POST   | `/api/auth/register` | Cadastro de usuario              | Nao          |
| POST   | `/api/auth/login`    | Login (retorna access + refresh) | Nao          |
| POST   | `/api/auth/refresh`  | Renova access token via refresh  | Nao          |

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

| Metodo | Rota                         | Descricao                 | Autenticacao |
|--------|------------------------------|---------------------------|--------------|
| POST   | `/api/uploads`               | Upload de imagem (<=5MB)  | Nao          |
| GET    | `/api/uploads/{id}`          | Servir imagem original    | Nao          |
| GET    | `/api/uploads/{id}/thumbnail`| Servir thumbnail          | Nao          |

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

| Metodo | Rota                    | Descricao                              | Autenticacao |
|--------|-------------------------|----------------------------------------|--------------|
| POST   | `/api/media/generate`   | Gera imagem a partir de prompt textual | Sim          |
| POST   | `/api/media/diagram`    | Gera diagrama a partir de prompt       | Sim          |
| GET    | `/api/media/{id}`       | Serve o conteudo binario da midia      | Sim          |
| GET    | `/api/media/{id}/image` | Serve a imagem PNG do diagrama         | Sim          |

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

**Diagram Request:**
```json
{
  "prompt": "diagrama de arquitetura em camadas"
}
```

**Comportamento:**
- O prompt e hasheado (SHA-256) para servir como chave de cache no Redis
- Em caso de erro na API externa, busca no banco de dados (fallback)
- Midias nao referenciadas sao removidas apos 1 hora (limpeza agendada)

### Raciocinio

| Metodo | Rota               | Descricao                                      | Autenticacao |
|--------|--------------------|------------------------------------------------|--------------|
| POST   | `/api/chat/reason` | Envia pergunta e recebe resposta com raciocinio | Sim          |

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
- Retry com backoff exponencial (3 tentativas: 1s / 2s / 4s)
- Timeout de leitura: 30s
- Erros 4xx sem retry; 5xx e timeout disparam retry

### Sintese de Audio

| Metodo | Rota                  | Descricao                    | Autenticacao |
|--------|-----------------------|------------------------------|--------------|
| POST   | `/api/tts/synthesize` | Sintetiza texto em audio WAV | Sim          |
| GET    | `/api/tts/{id}`       | Serve arquivo de audio       | Sim          |

**Synthesize Request:**
```json
{
  "text": "Texto para sintetizar",
  "voice": "af_heart"
}
```

Voice padrao: `af_heart`. Alternativas: `am_michelle`, `af_bella`.

### Exportacao PDF

| Metodo | Rota                           | Descricao                        | Autenticacao |
|--------|--------------------------------|----------------------------------|--------------|
| POST   | `/api/chat/{messageId}/export` | Exporta duvida + resposta em PDF | Sim          |

**Export Request:**
```json
{
  "question": "Como resolver uma equacao de segundo grau?",
  "context": "ax^2 + bx + c = 0",
  "answer": "Para resolver, use a formula de Bhaskara...",
  "steps": [
    "Identifique os coeficientes a, b e c",
    "Calcule o discriminante: Delta = b^2 - 4ac",
    "Aplique a formula: x = (-b +- sqrt(Delta)) / 2a"
  ],
  "confidence": 0.95,
  "mediaIds": ["uuid-do-diagrama"]
}
```

**Export Response:** `application/pdf` com `Content-Disposition: attachment; filename="duvida-YYYY-MM-DD.pdf"`

### Comunicacao em Tempo Real

| Metodo | Rota                                  | Descricao                        | Autenticacao                    |
|--------|---------------------------------------|----------------------------------|---------------------------------|
| GET    | `/api/chat/stream?question=&context=` | SSE streaming de raciocinio      | Sim                             |
| WS     | `/ws` (STOMP over SockJS)             | WebSocket para chat em tempo real| Sim (token via query param)     |

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
- Resposta 429 quando excedido

### Documentacao Interativa

- Swagger UI: `/swagger-ui.html`
- OpenAPI JSON: `/api-docs`

## Variaveis de Ambiente

| Variavel               | Default                                                                 | Descricao                        |
|------------------------|-------------------------------------------------------------------------|----------------------------------|
| `DATABASE_URL`         | `jdbc:postgresql://localhost:5432/ezlearning`                           | URL do PostgreSQL                |
| `DATABASE_USER`        | `ezlearning`                                                            | Usuario do banco                 |
| `DATABASE_PASSWORD`    | `ezlearning`                                                            | Senha do banco                   |
| `REDIS_HOST`           | `localhost`                                                             | Host do Redis                    |
| `JWT_SECRET`           | -- (obrigatorio)                                                        | Chave secreta para assinar JWT   |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173,http://localhost:3000`                           | Origens permitidas CORS          |
| `REASONING_API_URL`    | `https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent` | URL da API de raciocinio |
| `REASONING_API_KEY`    | -- (obrigatorio)                                                        | Chave da API Google Gemini       |
| `MEDIA_API_URL`        | `https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent` | URL da API de midia     |
| `MEDIA_API_KEY`        | -- (obrigatorio)                                                        | Chave da API Google Gemini       |
| `TTS_API_URL`          | `http://localhost:5050/v1/audio/speech`                                 | URL do Kokoro TTS                |

## APIs Externas

| API            | Finalidade                           | Provedor | Autenticacao          |
|----------------|--------------------------------------|----------|-----------------------|
| Gemini (flash) | Raciocinio logico                    | Google   | API Key (query param) |
| Gemini (flash) | Geracao de midias (imagens/diagramas)| Google   | API Key (query param) |
| Kokoro TTS     | Sintese de voz (texto para audio)    | Local    | Nao requerida         |
