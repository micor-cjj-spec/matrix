# AI Runtime Refactor Phase 4: Discovery and Resilience

## Goal

Phase 4 removes the single fixed-address dependency between `base-service` and the independent `ai-service` while preserving a safe migration path for the current environment.

## Version boundary

`ai-service` remains isolated on:

- Spring Boot 3.5.x
- Spring AI 1.1.x
- Spring Cloud 2025.0.x
- Spring Cloud Alibaba 2025.0.x

Existing business services remain on their current Spring Boot 3.2.x / Spring Cloud 2023.x baseline. `base-service` consumes discovery through the common `DiscoveryClient` abstraction already present in its runtime; it does not import the Boot 3.5 dependency line.

## Nacos compatibility gate

Spring Cloud Alibaba 2025.0.x requires Nacos Server 3.x when using the `public` or empty namespace. Because the current deployment version has not been verified, registration and discovery are disabled by default.

Migration sequence:

1. keep static mode enabled;
2. validate or upgrade the Nacos server;
3. start `ai-service` with registration enabled;
4. verify registered metadata and health;
5. enable discovery in one `base-service` environment;
6. retain static fallback during the observation window;
7. disable static fallback only after discovery is proven stable.

## Endpoint resolution

`AiServiceEndpointResolver` builds an ordered candidate list:

1. all discovered `ai-service` instances;
2. static URL, only when static fallback is enabled;
3. duplicate URLs removed;
4. starting candidate rotated between requests.

Every discovered instance may publish:

```text
matrix.context-path=/api
matrix.runtime=spring-ai
matrix.api-version=v1
```

## Retry policy

A logical request attempts at most `AI_SERVICE_MAX_ATTEMPTS` candidate endpoints.

Retryable failures:

- connection failure;
- request timeout;
- HTTP 408;
- HTTP 429;
- HTTP 5xx;
- internal SSE `error` before any delta is emitted.

Non-retryable failures:

- validation and authentication HTTP 4xx failures;
- malformed local request configuration;
- any streaming failure after at least one delta was emitted.

The no-retry-after-delta rule prevents duplicate prefixes and mixed answers from different model executions.

## Circuit breaker

`SpringAiCircuitBreaker` counts consecutive failed logical requests, not failed individual endpoints.

State transitions:

```text
CLOSED
  -> threshold consecutive failures
OPEN
  -> open duration elapsed
HALF-OPEN style probe
  -> success: CLOSED
  -> failure: OPEN
```

The implementation is intentionally local and dependency-light. It protects one `base-service` process and is not a distributed circuit state.

## Adapter fallback

When all remote attempts fail and both conditions are true:

```text
AI_MODEL_ADAPTER=spring-ai
AI_FALLBACK_ENABLED=true
```

`RoutingAiModelFacade` invokes the existing `prompt-http` adapter.

For streaming calls, fallback is only permitted before the first delta reaches the public SSE client.

## Operational defaults

```text
AI_DISCOVERY_ENABLED=false
AI_SERVICE_DISCOVERY_ENABLED=false
AI_SERVICE_STATIC_FALLBACK_ENABLED=true
AI_SERVICE_MAX_ATTEMPTS=2
AI_SERVICE_CIRCUIT_FAILURE_THRESHOLD=3
AI_SERVICE_CIRCUIT_OPEN_SECONDS=30
AI_FALLBACK_ENABLED=true
```

These defaults make the change backward compatible: the current static endpoint remains active until discovery is explicitly enabled.

## Follow-up

1. aggregate terminal Spring AI metadata for streaming token usage;
2. add Micrometer counters for endpoint attempts, retries, circuit opens, and adapter fallback;
3. add provider routing inside `ai-service` rather than only adapter-level fallback;
4. add tenant, prompt-version, audit, and cost-limit Advisors;
5. introduce read-only finance tools after the runtime path is observable and stable.
