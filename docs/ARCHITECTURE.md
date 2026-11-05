# Architecture

## Dependency shape

`meta-core` owns configuration and HTTP transport. Capability modules depend on it and expose small Java request/response operations. The optional starter supplies Spring beans. The demo composes the language client with deterministic catalog and inventory tools.

```text
meta-commerce-demo ── meta-language ── meta-core
        │                  │
        ├── fictional catalog tools
        └── static demo UI

meta-image / meta-voice / meta-vision / meta-segmentation ── meta-core
meta-spring-boot-starter ── meta-language / meta-image / meta-voice / meta-vision / meta-segmentation ── meta-core
```

## Request flow and safety

1. The server sends the customer's prompt and approved tool schemas to Muse Spark.
2. Muse Spark may return a tool call or a text answer. Tool choice is `auto` in the documented API.
3. The language loop checks the requested name against the supplied allowlist, parses JSON arguments, applies a turn and call cap, then invokes application code.
4. Commerce code searches the fictional catalog and reads inventory by an internal product ID. The model cannot set price, stock, merchant ID, or checkout state.
5. The server returns the model answer, per-call arguments/results, and verified product records. Availability requests fail if search and inventory could not both be verified.

The transport retries HTTP 429, 5xx, and network failures with bounded exponential backoff and jitter. It does not retry other 4xx responses. Logs exclude request bodies and credentials. Usage is passed to a replaceable `MetaUsageReporter`. Chat completions can also be consumed incrementally through a server-sent events streaming call; usage events in the stream are reported the same way.

## Studio composition path

The private Qlick API exposes a provider-neutral composition port and a Meta adapter. The server requests a schema-constrained `{summary, commands[]}` proposal. It binds the authenticated page route for persisted pages, or uses a `CONTENT_MANAGE` protected demo-only route for local Juniper Dew drafts. Both routes limit component kinds, per-component variants, responsive column/visibility settings, and catalog IDs to the fictional beauty catalog, and component-scoped requests may only edit the selected block. The Angular client rechecks target IDs, registry variants, catalog IDs, tree validation, and theme tokens, then stages a single editor proposal. Applying it uses Studio history and autosave.

The public suite does not import or package any Qlick type. The composite build is only a local development dependency substitution; it does not publish either repository.

## Boundaries and known limits

- The commerce UI is a standalone sample, not a production checkout.
- Voice uploads currently support bounded files through the documented transcription endpoint; realtime WebSocket streaming is not implemented.
- SAM requests return the API response. A typed mask parser/rendering layer is not included.
- 2D composition, 3D planning, and video have no feature modules. They can be added behind capability contracts when an application has a working use case.
- Meta API live checks require a key. The commerce demo's in-stock and out-of-stock paths were verified live with a locally supplied key; image, voice, vision, and segmentation adapters have not been live-tested here.
