# Hackathon changes

## New in this contribution

- Added an independent Java 21 / Spring Boot multi-module Meta Model API suite.
- Added server-side auth/configuration, bounded transport retries, structured output, tool calling, usage hooks, and model error handling.
- Added a runnable fictional skincare commerce demo with product search, deterministic inventory, valid sample product routes, and a visible tool trace. Inventory checks require a prior catalog search match, and tool results keep their nested structure.
- Added server-sent events streaming for chat completions, with usage reporting for stream events.
- Added responsive composition commands (`set_responsive` for grid columns and visibility), per-component variant allowlists, and a `{summary, commands[]}` proposal contract between the Meta adapter, the guard, and the Studio client.
- Added Muse Image, Muse Voice Transcribe, Muse Spark vision, and SAM 3.1 client operations based on current Meta documentation.
- Added an optional Spring Boot starter and integration documentation.
- Added a private Qlick Studio composition adapter that requests schema-constrained commands and stages supported edits through Studio's existing document validation and undo history.
- Added a sage and cream draft theme token and button radius token to Studio draft persistence and canvas rendering.

## Existing work retained

Qlick Studio, its renderer, component registry, editor state, inspector, responsive editing, drafts, history, and the Qlick API existed before this change. Their broader component completion is outside this contribution. The repository already tracked a 138-type registry and a more limited implementation audit; neither count is treated as proof that every component is ready for AI composition.

## API use and live status

The implemented Meta endpoints are the OpenAI-compatible Chat Completions endpoint with structured JSON output and SSE streaming, the Muse Image generation/edit endpoints, Muse Voice Transcribe multipart endpoint, multimodal image input, and the Responses endpoint for SAM 3.1. The commerce assistant uses Muse Spark when `MODEL_API_KEY` is set. The in-stock and out-of-stock demo questions were verified live with a locally supplied key (read from ignored local env, never committed): both followed the search→inventory tool path and returned verified product cards. Other capability adapters have not been live-tested here.

## Private code and publication

The Qlick Studio bridge belongs to the existing private `qlick-api` and `qlick-studio` repositories. The standalone `qlick-meta-ai` repository contains no proprietary Qlick application code. Repository visibility has not been changed and nothing has been published.

## Still future work

The Qlick AI Employees, AI Studio image workflow, 2D storefront builder composition beyond the skincare slice, 3D Builder, video, and proprietary commerce workflows are future integration examples. The 3D Builder was not started here.
