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

## Iterative Delivery Loop

For ongoing development, follow this loop unless the user explicitly narrows or pauses the work:

1. Before starting new work, check both `matrix` and `matrix-web` for uncommitted changes and unpushed commits.
2. If there are relevant pending project changes, stage only the intended files, commit them, push to `dev`, then ask `openclaw` in Feishu to pull the `dev` branch, deploy, and restart.
3. Test the deployed site at `https://micor.top/` with the test account `19106026235` / `123456`.
4. If bugs are found on the deployed site, fix bugs first before proposing or developing new features.
5. After the site is stable, propose the next requirement from the project vision, then execute the standard requirement flow: draft -> business docs -> prompts -> code + deliverables.
6. After development, commit and push to `dev`, message `openclaw` in Feishu to deploy/restart, test `https://micor.top/` again, and repeat the loop.

When sending deployment instructions through Feishu, use the `openclaw` conversation unless the user specifies another target.
