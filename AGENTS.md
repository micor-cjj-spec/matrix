# Workspace Context

This workspace is developed together with a sibling frontend repository.

When a task involves "development", "fixing bugs", "adding features", "联调", or "writing code", always consider both of these directories first:

- Backend: `C:\Users\20602\IdeaProjects\matrix`
- Frontend: `C:\Users\20602\IdeaProjects\matrix-web`

Working convention:

1. If the request is about Java services, Spring Boot, gateway/auth/finance modules, MyBatis-Plus, configuration, or backend interfaces, work in `matrix`.
2. If the request is about pages, components, routes, UI, browser behavior, frontend API calls, or Vite/Vue code, work in `matrix-web`.
3. If the request sounds like integration, end-to-end flow, data mismatch, login, permissions, vouchers, ledgers, or base data maintenance, inspect both repositories before making changes.
4. Unless the user explicitly narrows the scope, assume these two repositories are part of the same project and should be considered together.

Quick project notes:

- `matrix` is a Java 17 Maven multi-module backend using Spring Boot, Spring Cloud Alibaba, and MyBatis-Plus.
- `matrix-web` is a Vue 3 + Vite frontend and currently uses `vue-router`, `axios`, `pinia`, and `vuetify`.

Default expectation for future threads:

- Re-establish context from this file before starting substantial work.
- Do not assume only the current working directory matters; check whether the sibling repository is also relevant.
