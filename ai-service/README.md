# Matrix AI Service

`ai-service` is the independent model-runtime boundary for Matrix. It runs on Spring Boot 3.5.x and Spring AI 1.1.x while the existing business services remain on their current Spring Boot baseline.

## Responsibility boundary

`base-service` continues to own:

- user authentication and conversation ownership
- conversation and message persistence
- knowledge retrieval and citations
- public `/ai/**` API compatibility

`ai-service` owns:

- Spring AI `ChatClient` integration
- provider-neutral Prompt construction
- synchronous model generation
- streaming model generation
- model response metadata and token usage
- internal model API authentication

## Required environment variables

```text
AI_API_KEY=your-provider-key
AI_BASE_URL=https://api.openai.com
AI_COMPLETIONS_PATH=/v1/chat/completions
AI_CHAT_MODEL=gpt-4o-mini
AI_INTERNAL_TOKEN=use-a-long-random-shared-secret
```

For an OpenAI-compatible provider, set `AI_BASE_URL`, `AI_COMPLETIONS_PATH`, and `AI_CHAT_MODEL` to the provider's compatible values.

## Run

From the repository root:

```bash
mvn -pl ai-service spring-boot:run
```

The service listens on port `10020` by default with context path `/api`.

## Internal endpoints

All endpoints require:

```text
X-Matrix-Internal-Token: <AI_INTERNAL_TOKEN>
```

Endpoints:

```text
GET  /api/internal/model/status
POST /api/internal/model/chat
POST /api/internal/model/chat/stream
```

The streaming endpoint uses named SSE events:

```text
start

delta

done

error
```

## Enable from base-service

```text
AI_MODEL_ADAPTER=spring-ai
AI_SERVICE_BASE_URL=http://127.0.0.1:10020/api
AI_INTERNAL_TOKEN=the-same-shared-secret
```

The public API remains in `base-service`; only model execution is delegated to `ai-service`.

## Current limitations

- streaming token usage is not yet aggregated
- provider fallback and circuit breaking are not yet implemented
- service discovery is not yet wired
- RAG still runs in `base-service`
- tools and human confirmation are not yet enabled
