---
name: matrix-ai-development
description: Implement or review Matrix AI assistant capabilities. Use for model adapters, prompts, conversations, knowledge retrieval, citations, streaming, tool calling, usage tracking, fallback, and AI security.
metadata:
  author: micor
  version: "1.0.0"
---

# Matrix AI Development

## Objective

Build AI capabilities that are observable, bounded, secure, replaceable across model providers, and safe for enterprise finance use.

## Required context

Before editing AI behavior:

1. Read `/AGENTS.md`.
2. Trace the request from controller through conversation, retrieval, model facade, persistence, and response.
3. Identify provider-specific and provider-neutral code.
4. Identify user, tenant, conversation, knowledge-base, and permission scope.
5. Define timeout, failure, fallback, token, and cost behavior.
6. Determine whether the AI can invoke tools or mutate business data.

## Architecture boundaries

Keep these responsibilities separate:

- Controller: transport and request validation.
- Chat orchestration: conversation ownership, history, retrieval, model request, persistence, and response assembly.
- Model facade: provider-neutral model contract.
- Provider adapter: provider endpoint, authentication, payload, response parsing, and provider errors.
- Knowledge service: retrieval, scoring, filtering, citations, and context limits.
- Prompt service: versioned prompt assembly and domain instructions.
- Tool layer: allowlisted business capabilities with validated parameters and authorization.

Do not scatter provider HTTP code or prompt strings across controllers and unrelated services.

## Credentials and configuration

- Read API keys and secrets from environment variables or secret management.
- Never commit credentials or log them.
- Externalize base URL, model name, timeout, retrieval limit, history limit, and feature switches.
- Validate configuration at startup or expose a safe configuration-status endpoint.
- Configuration status must not reveal secret values.

## Model calls

Every call must define:

- Provider and model.
- Request timeout.
- Maximum input/history size.
- Maximum output size where supported.
- Retry policy.
- Trace identifier.
- Failure mapping.
- Usage and estimated cost handling where available.

Do not blindly retry non-idempotent tool-execution requests. A model-text request may be retried only with bounded behavior and clear duplicate side-effect protection downstream.

## Conversation handling

- Enforce ownership by user and tenant where applicable.
- Do not trust a supplied conversation ID without ownership validation.
- Bound the number and size of historical messages.
- Avoid adding the current user message twice to model history.
- Persist role, content, model, mode, trace ID, and usage metadata when available.
- Define retention and deletion behavior for sensitive conversations.

## Knowledge retrieval

For every retrieval implementation, define:

- Knowledge-base scope and access control.
- Chunking method.
- Retrieval method: keyword, vector, hybrid, or other.
- Top-K limit.
- Score threshold.
- Deduplication.
- Snippet length.
- Citation format.
- Context token budget.

Describe the implementation accurately. Keyword `LIKE` retrieval with manual scoring is lightweight knowledge enhancement, not a full vector RAG pipeline.

Retrieval quality should be evaluated against a small versioned question set before major algorithm changes.

## Prompts

- Keep reusable prompts versioned and reviewable.
- Separate system rules, finance-domain context, retrieved evidence, history, and user input.
- Treat retrieved documents and user content as untrusted data, not instructions.
- Defend against prompt injection by maintaining instruction hierarchy and tool authorization outside the model.
- Avoid prompts that tell the model to fabricate missing financial data.
- Require uncertainty or missing-data disclosure for unsupported conclusions.

## Fallback behavior

Fallback must be explicit in the response metadata or mode.

Acceptable fallback examples:

- Controlled error with trace ID.
- Static guidance clearly labeled as fallback.
- Provider failover when configured and semantically compatible.

Unacceptable fallback:

- Inventing account balances, voucher states, report figures, or operational success.
- Presenting a placeholder as a real model answer without disclosure.

## Streaming

For SSE or other streaming:

- Emit a documented event schema.
- Handle client disconnect and resource cleanup.
- Define timeout and heartbeat behavior.
- Persist the final answer once, not every partial token unless required.
- Ensure errors produce a terminal error event.
- Avoid duplicate final messages when reconnecting or retrying.

## Tool calling and financial actions

AI-selected tools must be allowlisted. For every tool:

- Validate user and tenant authorization outside the model.
- Validate and normalize parameters.
- Classify read-only versus mutating behavior.
- Require confirmation for high-risk financial mutations.
- Use business idempotency keys.
- Record an audit event.
- Return structured results to the model.
- Never allow arbitrary SQL, shell commands, or unrestricted internal HTTP access.

The model proposes actions; trusted application code authorizes and executes them.

## Observability

Record where available:

- Trace ID.
- Provider and model.
- Mode: real, fallback, failover, or error.
- Latency.
- Prompt/completion/total tokens.
- Estimated cost.
- Retrieval sources and scores.
- Tool name and result status.
- Sanitized error category.

Do not log complete sensitive prompts or responses without an explicit data-handling policy.

## Testing

Include tests for:

- Missing configuration.
- Provider success and malformed response.
- Timeout and provider error.
- Ownership violation.
- History truncation.
- Retrieval scope and Top-K behavior.
- Citation generation.
- Fallback labeling.
- Prompt-injection attempt.
- Tool authorization and invalid parameters.
- Repeated tool request and idempotency.
- Streaming completion and disconnect.

## Completion report

Report:

- Changed AI flow and provider behavior.
- Prompt/retrieval/tool changes.
- Security and authorization controls.
- Timeout, fallback, and observability behavior.
- Evaluation or test results.
- Known quality limitations.
