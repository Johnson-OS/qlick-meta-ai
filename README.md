# Qlick Meta AI Suite

An independent Java 21 / Spring Boot 4.1 integration suite for the Meta Model API. The modules expose provider-neutral contracts while the Meta adapter speaks Meta's documented OpenAI-compatible endpoints. No Qlick proprietary code is included.

## Run the commerce demo

Install Java 21, then set a Meta Model API key on the server process. The key never belongs in browser code or source control.

```bash
export MODEL_API_KEY="your-key"
./gradlew :meta-commerce-demo:bootRun
```

Open `http://localhost:8080`. Ask whether the Sage + Oat Barrier Serum is available. Muse Spark should choose `search_products`, then `check_inventory`; the server returns the verified stock and price with a working product link. Expand **Tool calls and execution trace** to inspect calls and results.

Configuration:

| Variable | Default | Purpose |
| --- | --- | --- |
| `MODEL_API_KEY` | unset | Server-side Meta bearer token; required for live model calls |
| `META_MODEL_API_BASE_URL` | `https://api.meta.ai/v1` | Meta Model API base URL |
| `META_LANGUAGE_MODEL` | `muse-spark-1.3` | Configurable Muse Spark model ID |
| `PORT` | `8080` | Demo server port |

Without `MODEL_API_KEY`, the demo UI and fictional catalog still start, while live chat reports that credentials are missing. No mock model response is presented as live.

## Modules

- `meta-core`: HTTPS transport, bearer auth, timeouts, bounded retry with jitter, errors, and usage reporting.
- `meta-language`: chat completions, server-sent events streaming, JSON Schema structured output, and a capped, allowlisted function tool loop.
- `meta-image`: Muse Image generation and edit requests.
- `meta-voice`: Muse Voice Transcribe file uploads.
- `meta-vision`: Muse Spark image understanding.
- `meta-segmentation`: SAM 3.1 still image segmentation request. The module returns Meta's response; SAM's output uses a documented token grammar and is not decoded into masks here.
- `meta-commerce-demo`: runnable Spring web app and fictional skincare assistant.
- `meta-spring-boot-starter`: optional Spring Boot auto-configuration.

Image, voice, vision, and segmentation clients are real API adapters; they have not been live-tested in this workspace. The commerce catalog and inventory tools are deterministic fixtures. The language model request is live when a key is configured.

## Tests and build

```bash
./gradlew test
./gradlew :meta-commerce-demo:bootJar
```

Tests use local HTTP fixtures and do not require a Meta key. Use `JAVA_HOME` for a Java 21 installation. A live smoke test can be run manually by setting `MODEL_API_KEY` and starting the demo.

## Use the Spring Boot starter

Add `dev.qlick.meta:meta-spring-boot-starter:0.1.0-SNAPSHOT` and configure:

```yaml
meta:
  model-api:
    base-url: https://api.meta.ai/v1
    api-key: ${MODEL_API_KEY:}
    language-model: muse-spark-1.3
    image-model: muse-image-1.0
    voice-model: muse-voice-transcribe-1.0
    vision-model: muse-spark-1.3
    segmentation-model: sam-3.1
```

Inject `MetaLanguageClient` or `MetaApiClient`. Applications own their public request and response DTOs, authorization, and tool executors.

## Current Meta APIs

This suite targets Meta Model API documented at `https://api.meta.ai/v1`, authenticated with a bearer `MODEL_API_KEY`. The default language model is `muse-spark-1.3`; other model IDs remain configurable. The API's tool choice is `auto`, so agents must handle responses that do not call a tool. Structured output uses the documented JSON Schema `response_format`. Muse Image uses `muse-image-1.0`; Muse Voice Transcribe uses `/asr/transcribe`; image understanding uses Muse Spark multimodal chat input; SAM 3.1 uses `POST /responses` with model `sam-3.1`.

- [Meta API reference](https://dev.meta.ai/docs/api-reference)
- [Structured output](https://dev.meta.ai/docs/structured-output)
- [Tool calling](https://dev.meta.ai/docs/tool-calling)
- [Image generation](https://dev.meta.ai/docs/image-generation)
- [Speech to text](https://dev.meta.ai/docs/speech-to-text)
- [Image understanding](https://dev.meta.ai/docs/image-understanding)
- [SAM overview](https://dev.meta.ai/docs/sam/overview)

## Qlick Studio development bridge

The private `qlick-api` build uses a local Gradle composite build when `../qlick-meta-ai` exists. Keep both repositories as siblings to develop the private adapter. The public suite remains runnable on its own. The Studio workflow is limited to the fictional **Juniper Dew** demo merchant and its beauty fixture IDs.

## Future integration examples

Qlick may later connect private adapters for AI Sales, Support, Inventory, and Analytics Employees; image generation in AI Studio; AI-assisted 2D composition; spatial 3D planning; and future video or asset workflows. These are integration examples only. This repository does not contain the Qlick renderer, 2D builder, 3D builder, or proprietary commerce workflows.
