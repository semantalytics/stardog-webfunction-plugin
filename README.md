> **Repository moved.** As of 2026-07-05 this project lives at
> [tegmentum/stardog-webfunction-plugin](https://github.com/tegmentum/stardog-webfunction-plugin)
> (previously `semantalytics/stardog-webfunction-plugin`). The old URL
> auto-redirects. Java package base and Maven groupId are now `ai.tegmentum.*`.
> The `http://semantalytics.com/2021/03/ns/stardog/webfunction/` RDF vocabulary
> IRI is preserved for identifier stability.

# Stardog WebFunctions

A [Stardog](http://stardog.com) plugin for executing WebAssembly extensions
inside SPARQL queries. Extensions are Preview 2 components addressable by any
URL scheme (`ipfs://`, `https://`, `file://`, `sha256://`) — loaded on demand,
sandboxed by the [webassembly4j](https://github.com/tegmentum/webassembly4j)
runtime (wasmtime provider), and enforced against an RDF-declared capability
policy administered via SPARQL UPDATE.

Part of a three-binding family sharing one Component Model ABI:

| Binding | Repo |
|---|---|
| Stardog | you are here |
| Apache Jena | [tegmentum/jena-webfunction-plugin](https://github.com/tegmentum/jena-webfunction-plugin) |
| Eclipse RDF4J | [tegmentum/rdf4j-webfunction-plugin](https://github.com/tegmentum/rdf4j-webfunction-plugin) |

The WIT world `tegmentum:webfunction@0.1.0` (canonical package at
[tegmentum/webfunction-wit](https://github.com/tegmentum/webfunction-wit))
is byte-for-byte identical across the three repos, so a single Rust component
(built with [`cargo component`](https://github.com/bytecodealliance/cargo-component))
runs unmodified under any of the three SPARQL engines. Stardog-specific
overlays (`doc.wit`, `planner.wit`) sit alongside the base interfaces.

## Quick start

```sparql
prefix wf: <http://semantalytics.com/2021/03/ns/stardog/webfunction/>
prefix f: <ipfs://QmVx8jryTscgnbJoh8iuUYUiiGBeu4tr1i1A3PmCqcE5Vk/>

select ?result where {
    bind(wf:call(f:toUpper, "stardog") as ?result)
}
```

Functions can be loaded from IPFS to reduce external dependencies and allow
extensions to be called offline once cached.

## SPARQL surfaces

`wf:call` is exposed through four SPARQL shapes; all back onto the same
component's `evaluate` / `aggregate-step` / `aggregate-finish` exports:

| Shape | Syntax | When to use |
|---|---|---|
| Filter | `BIND(wf:call(<url>, args...) AS ?x)` | one value out of one wasm call |
| Aggregate | `SELECT (wf:call(...) AS ?sum) WHERE {...} GROUP BY ...` | reduce query rows to one value |
| Property | `?x wf:call (<url> args...)` | multi-row output, single subject variable |
| SERVICE | `SERVICE <url> { ... }` | multi-row, multi-var output |

## Capability policy

Extensions run against a **default-deny capability model** administered as
RDF triples in a plugin-owned Stardog database (`system-webfunctions-capability`).
Grants are keyed by extension URL and enforced at both instantiation and
per-callback dispatch.

Enable with `webfunctions.capability.enabled=true` (default is off — pre-capability
behavior preserved for existing deployments).

```turtle
@prefix cap: <http://semantalytics.com/2021/03/ns/stardog/webfunction/capability#> .

<ipfs://QmMyExt> cap:trusted true ;
    cap:allowInterface cap:GraphCallbacks, cap:HttpCallbacks ;
    cap:allowMethod cap:GraphCallbacks_ExecuteQuery ;
    cap:allowHost "api.acme.com" ;
    cap:allowHttpPath "api.acme.com/public/" .
```

Grants apply per extension URL (any scheme). Extensions execute under the
invoking user's Shiro subject — Stardog's GRAPH ACLs, database ACLs, and
named-graph permissions apply naturally to what the extension can reach.

### Capability-ask

Extensions can declare what they need via a Turtle document embedded as a
`stardog.capability-ask` WebAssembly custom section. The plugin extracts asks
on load and inserts them into `<urn:stardog:webfunction:capability:asks>` for
admin review. Admins diff asks against grants:

```sparql
prefix cap: <http://semantalytics.com/2021/03/ns/stardog/webfunction/capability#>
select ?ext ?asked where {
    graph <urn:stardog:webfunction:capability:asks> {
        ?ext cap:asksInterface ?asked
    }
    filter not exists { ?ext cap:allowInterface ?asked }
}
```

Author tooling: [tegmentum/wf-authoring-tools](https://github.com/tegmentum/wf-authoring-tools)
provides `wf-embed-ask` CLI + `build.rs` helper for embedding asks at build
time.

## Host callback interfaces

The plugin registers 12 host-callback interfaces from `tegmentum:webfunction@0.1.0`:

| Interface | What extensions can do |
|---|---|
| `graph-callbacks` | Execute SPARQL SELECT / UPDATE against the invoker's Stardog session |
| `http-callbacks` | HTTP GET / POST against allowlisted hosts + paths |
| `wasm-callbacks` | Invoke another extension by URL (multi-level nesting; cycle-detected; caller-shared fuel) |
| `sink-callbacks` | Write named RDF quad sinks (in-memory registry) |
| `sink-query-callbacks` | Scan quads / SPARQL SELECT over a single sink (RDF4J MemoryStore) |
| `document-sink-callbacks` | Opaque-blob storage keyed by RDF term |
| `tracker-sink-callbacks` | Typed row storage via SQLite JDBC |
| `fulltext-callbacks` | In-memory named-index insert / delete / search (reference impl) |
| `prepared-query-callbacks` | Cached SPARQL query handles |
| `observability-callbacks` | Introspection: callback depth, chain, etc. |
| `fast-path-callbacks` | Direct BGP scan primitive |

## Composition

Extensions compose via the [webassembly-component-orchestration](https://github.com/tegmentum/webassembly-component-orchestration)
tool, embedded in the plugin jar as a WebAssembly component. Trigger via
`ComposeAdmin.compose(cbor)` — the composed artifact gets a `sha256://<hex>`
URL, persists to `${stardog.home}/webfunctions-compose/artifacts/`, and lands
in the compositions named graph as first-class extension URL.

```turtle
<sha256://abc123...> cap:hasArtifact <sha256://abc123...> ;
    cap:compositionDigest "abc123..." .
```

Composed URLs load through the same extension pipeline — capability grants +
fuel + ask extraction all apply.

Operator override: `webfunctions.compose.artifact-url-prefix=https://cdn/artifacts/`
causes the emitted RDF URL to use `<prefix><hex>` instead of `sha256://`. Plugin
still persists locally; operator handles upload.

## Fuel + audit

**Fuel metering** — commercial-quota system. Config:
- `webfunctions.fuel.enabled` (default off)
- `webfunctions.fuel.per-invocation.max` (default 100_000)
- `webfunctions.fuel.per-user.monthly` (default 0 = unlimited)
- `webfunctions.fuel.host-callback-toll` (default 1000)

Per-user quota is Kernel-backed (dedicated `system-webfunctions-fuel` DB,
mirroring the capability policy pattern).

**Attribution audit** — in-memory ring per attribution type (fuel + capability),
with optional durable NDJSON rotating-file sink:
- `webfunctions.audit.disk.enabled` (default off)
- `webfunctions.audit.disk.directory` (default `${stardog.home}/logs/webfunctions-audit/`)
- `webfunctions.audit.disk.rotate-bytes` (default 100 MB)

Capability audit rows carry the full `callChain` for multi-level dispatch.

## Deadlines

Cooperative deadline check fires at every host-callback boundary; wasm-level
epoch interruption catches pure-compute frames that never re-enter host
callbacks. Deadline = `min(webfunctions.exec.max.millis, monitor-cancellation)`
— whichever fires first.

- `webfunctions.exec.max.millis` (default unset — no enforcement)
- `webfunctions.epoch.tick-millis` (default 100 — ticker interval for epoch increment)

## Configuration reference

See [CLAUDE.md](./CLAUDE.md) for the full config surface. All properties are
system-property driven and optional.

## Testing

**`mvn test`** — unit tests + component-mode direct-instantiation tests. The
embedded-Stardog `WasmTestSuite` is included but skips cleanly (via JUnit
`assume`) when the machine's installed Stardog native library doesn't match
the Stardog Java jars this build compiles against.

**`mvn verify`** — additionally runs `WasmTestSuiteIT`, `CapabilityDisabledIT`,
`CapabilityDenyIT`, `CapabilityAskIT`, `CapabilityInvokerSubjectIT`, and
`ComposeIntegrationIT` via Testcontainers. Boots a `stardog/stardog` container,
mounts the shaded plugin JAR into `/var/opt/stardog/.ext/`, drops fixture wasms
into `/opt/wasm/`, and runs full round-trip SPARQL queries against the running
server.

Requirements: Docker; `STARDOG_LICENSE_PATH` env pointing at a valid license
file. On Apple Silicon, set `DOCKER_DEFAULT_PLATFORM=linux/amd64` (Stardog
only ships linux/amd64 images).

## Ecosystem

- [webfunction-wit](https://github.com/tegmentum/webfunction-wit) — canonical WIT package
- [webassembly-component-orchestration](https://github.com/tegmentum/webassembly-component-orchestration) — compose orchestrator (embedded in the plugin jar)
- [wf-authoring-tools](https://github.com/tegmentum/wf-authoring-tools) — `wf-embed-ask` CLI for authoring capability-ask
- [webassembly4j](https://github.com/tegmentum/webassembly4j) — Java Component Model runtime
- [wasmtime4j](https://github.com/tegmentum/wasmtime4j) — wasmtime binding

## License

MIT
