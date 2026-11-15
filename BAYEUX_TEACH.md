# Bayeux Teaching-Guide Migration Notes

Everything learned from measuring `BAYEUX.adoc` against `TEACHING_GUIDE.md`, the one code defect found and fixed along the way, and the ordered work remaining to bring the chapter to the new standard. Survey as of 2026-11-15 (`main` at `3b4958ae7`, plus the uncommitted working-tree change described in section 4.5).

**Confidence markers.** Every claim below carries one of three levels:

- **(source)** — checked directly against the implementation under `bayeux/oumuamua/server/{api,spi,impl}` during this review.
- **(verified)** — reported by a review agent and survived three independent refutation passes (textual misreading, materiality under the guide's own latitude, reader-experience/source feasibility); not re-read by hand.
- **(unverified)** — stated by the chapter or an agent and not checked; treat as a question, not a fact.

Line numbers refer to `BAYEUX.adoc` before the edit in section 4.5; lines after 868 are now offset by +2.

---

## 0. Verdict

**Overall conformance: about 2.5 / 5.** The chapter is the case `TEACHING_GUIDE.md` describes under "What The Current Documentation Provides": nearly all the raw material a teaching chapter needs already exists (a concrete problem statement, an artifact chooser, a worked Quick Start with callouts, a complete `EchoService`, lifecycle/threading/defaults reference, symptom-driven troubleshooting), but learning and lookup are interleaved and the three required divisions — Teaching Guide, Technical Guide, Exceptions/Notes/References — do not exist as labelled sections. Separately, several code examples do not compile and a handful of reference statements contradict the implementation; the guide's acceptance criteria require verified APIs, so those are blocking regardless of structure.

| Guide division | Rating | One-line verdict |
|---|---|---|
| 1. Purpose, Benefits, Navigation | 3 | Good problem/mechanism opening; no audience/version lines, no division links, beginner path routes through a glossary and module-descriptor block |
| 2. Teaching Guide | 2 | Lesson-like sections exist but none has the six required elements; first lesson not runnable; no observed receive flow |
| 3. Technical Guide | 2 | Content largely present and mostly correct, but split across six top-level sections, two of them *before* the remaining lessons |
| 4. Exceptions, Notes, References | 2 | Troubleshooting is solid; ends with a generic Summary; no References section at all |
| Factual accuracy vs source | 3 | 19 claims verified correct; 4 majors and ~8 moderates wrong |
| Structure / acceptance criteria | 2 | Valid standalone article, module-path claims verify exactly; fails most structural acceptance items |

## 1. How the review was done

- Six independent review lenses (opening, lessons, technical guide, closing, factual accuracy against source, structure/acceptance criteria), each reading the full chapter and the full guide. The accuracy lens grepped source and POMs for every class, method, default, and coordinate it checked.
- 62 raw findings, deterministically merged to 47; each merged finding judged by three skeptics with distinct lenses; a finding survived only if fewer than two refuted it. 45 survived, 2 were refuted (section 6).
- A completeness critic walked every rule in the guide against the confirmed list and added 6 more findings, which went through the same three-skeptic pass; all 6 survived.
- The highest-impact accuracy findings were then re-checked by hand against source (marked **(source)** below).
- No example was compiled with `javac`; the compile-error claims rest on reading signatures. No rendering of the AsciiDoc was performed.

## 2. What already conforms — keep it

- **(source)** `BAYEUX.adoc:9-26` opens with the concrete problem (HTTP assumes the client speaks first) and the mechanism Bayeux supplies (meta channels, user channels, handshake-time transport negotiation). Exactly what division 1 asks for.
- **(verified)** Most benefit bullets are grounded in named behavior: `setStreamingPaths`, `overflowLogLevel`, `maxLongPollQueueSize`, the SPI interface list, `OrthodoxValue`/`OrthodoxCodec` and the `V` type parameter.
- **(source)** `At A Glance` (103-131) already has a beginner row, an expert row ("every class in the API surface"), and a troubleshooting row — the right shape, wrong targets.
- **(source)** `Choose The Right Artifact` (226-266) states the library-vs-application rule; the optional-Kafka boundary matches `spi/pom.xml` (`kafka-utility` is `<optional>true</optional>`); module names agree with `README.adoc:67-69`.
- **(verified)** Every module-descriptor claim in 268-324 (exports, `requires transitive`, `requires static`) verifies against the three `module-info.java` files, with one omission (section 4.3).
- **(source)** Quick Start callouts 1–6 put correctness constraints beside the step: codec required, URL pattern must match `WebsocketConfiguration.oumuamuaUrl`, the context listener stops the server. This is the guide's "essential constraints beside the step" rule.
- **(source)** `Writing a Bayeux Service` (1153) shows a complete class, not a fragment, and callout 1 places the `successful`/`error` rule beside the line that creates the response.
- **(verified)** Lesson order (service → listeners → Kafka → extensions) approximately follows the Bayeux entry's progression; the Kafka IMPORTANT admonition ("sessions and subscriptions are node-local state") partly separates local from cluster semantics.
- **(source)** `Troubleshooting` (1784-1836) is symptom → cause → corrective action, the required shape for division 4's first subsection.
- **(verified)** The chapter is a well-formed standalone article (one title, valid attributes, long-form kebab-case anchors) and correctly defers repository-wide module-path rules to `README.adoc#module-path`.

## 3. Factual errors against source

These block the migration independently of restructuring. Fix them in the first pass.

### 3.1 Examples that do not compile

| Line | Chapter | Reality | Level |
|---|---|---|---|
| 524 | `server.getCodec().getFactory().createObjectValue()` | `Codec` declares no `getFactory()`; the factory method on `ValueFactory` is `objectValue()`. Reach the factory through a `Message`/`Value` (`getFactory()`) or whatever the lesson's server exposes | (source) |
| 1126 | `new OrthodoxCodec<>(new JaxbDeserializer())` | `OrthodoxCodec` is not generic (`spi/.../json/orthodox/OrthodoxCodec.java:45`); the diamond is a compile error. Correct form: `new OrthodoxCodec(new JaxbDeserializer<>())` | (source) |
| 1161 | `private static final Route ECHO_ROUTE = new DefaultRoute("/service/echo");` | `DefaultRoute(String) throws InvalidPathException`, which is checked (`OumuamuaException` → `FormattedException extends Exception`). A field initializer cannot throw a checked exception | (source) |
| 1216 | `log.info(...)` in `AuditPacketListener` | `log` is never declared or imported; no logging API is named. The module uses `LoggerManager.getLogger(...)` | (verified) |
| 1138-1143 | `servletContext`, `servletConfig` in the plain-Java lesson | Undefined, no origin stated, no imports; not marked as a fragment | (verified) |
| 524 | `Channel<V> channel = ...` in Quick Start | `V` is unbound in a snippet presented as runnable | (verified) |

### 3.2 Behavioral claims that contradict the implementation

| Line | Chapter says | Source says | Level |
|---|---|---|---|
| 868 | `start` is idempotent; each protocol's `init` called exactly once | No started-guard existed; a second `start` re-ran every `protocol.init` (re-registering the WebSocket endpoint → `DeploymentException`) and orphaned the first pair of idle executors. **Fixed — see 4.5** | (source) |
| 1562, 1712 | "A supplied executor is used as given" | `OumuamuaServer.stop()` ends with `executorService.shutdown()` unconditionally; a caller-supplied executor is shut down by the server | (source) |
| 1025, 1503 | Streaming "matters only for long-poll transports; WebSocket always dispatches immediately" | Inverted. `OumuamuaSession.deliver` takes the immediate branch only when `fromChannel.isStreaming() && !transport.isLongPolling()`; long-polling always queues | (verified) |
| 192-193 | Handshake "selects the strongest supported protocol"; `Session.isLongPolling()` "records what was chosen" | `TransportUtility.accumulateSupportedTransportNames` unions every registered transport; `Session.setLongPolling` is called only by `AckExtension` after a successful handshake | (verified) |
| 448 | Constructor validates "at least one protocol is defined" | Throws only when `getProtocols() == null`; an empty array is accepted and yields a server with no transports | (verified) |
| 459, 1391 | `maxAckQueueSize` caps retained *packets* | `AckExtension` counts `packet.getMessages().length` across retained packets and evicts whole packets until the message total is at or below the cap | (verified) |
| 588 | `Transport.isLocal()` means "messages must not cross the backbone" | Both `Transports` constants return `false`; no server code consults the flag | (verified) |
| 794, 457, 1846 | WebSocket endpoint "looks the server up from the servlet context" / is "installed by the application's container configuration" | `WebSocketTransport.init` registers the endpoint with the JSR-356 `ServerContainer` during `server.start`, injecting the server via `ServerEndpointConfig.getUserProperties()` | (source for registration; verified for the user-properties detail) |
| 957 | `void dispatch(Packet<V>)`; no `initiateDisconnect()` | `api/Session.java` declares `boolean dispatch (Packet<V>)` and `void initiateDisconnect ()` | (verified) |
| — | `stop()` behavior | Never documented. Order is: backbone `shutDown()` (exceptions logged at ERROR), both idle executors `shutdown()` + `awaitTermination(idleCheckingTerminationTimeoutSeconds)` with an ERROR log on timeout, then `executorService.shutdown()`. `stop()` is called twice in the documented wiring (servlet `destroy` and `OumuamuaServletContextListener.contextDestroyed`) and tolerates it | (source) |

### 3.3 Wrong names, coordinates, and descriptor details

- **(verified)** `:355` `smallmind-web-grizzly` → actual artifactId `web-grizzly` (`web/grizzly/pom.xml`). `:364` `smallmind-kafka-utility` → `kafka-utility` (`kafka/utility/pom.xml`).
- **(source)** The Quick Start XML has no filename; it mirrors `bayeux/oumuamua/server/impl/src/test/resources/spring/org/smallmind/bayeux/oumuamua/server/impl/oumuamua-grizzly.xml`, which the chapter never names.
- **(verified)** `:315-323` names `jakarta.websocket.client` as the impl descriptor's only `requires static`; `impl/module-info.java` also has `requires static jakarta.annotation` and `requires static org.smallmind.web.json.doppelganger`.
- **(verified)** `Message` constant list omits `MINIMUM_VERSION`.

### 3.4 Defaults Reference gaps (`:1665-1740`)

- **(verified)** Omits `idleCheckingTerminationTimeoutSeconds` (default `10`, `OumuamuaConfiguration.java:98`, used by `stop()`).
- **(verified)** Omits the null-default `backbone`, `services`, `reflectingPaths`, `streamingPaths`, and the `AckExtension` overflow log level.
- **(verified)** Presents `longPollTimeoutMilliseconds` as having a default; it is a required constructor argument (the examples choose `500` ms).
- **(verified)** Columns are Setting | Default | Source only — no type, unit, valid values, or precedence; no distinction between library default, example setting, and construction path.

### 3.5 The `perApplicationScope` property in Quick Start (`:421`)

**(verified)** `<property name="perApplicationScope" ref="org.smallmind.spark.perApplicationScope"/>` makes the Spring refresh fail with `BeanCreationException` caused by `NoSuchBeanDefinitionException` anywhere except under the Tanuki wrapper with its scope on — callout 7 says so itself. The first lesson therefore does not run as printed outside that one host.

## 4. Code defect found and fixed: `OumuamuaServer.start()` ran more than once

### 4.1 What was wrong (source)

`OumuamuaServer.start` had no started-guard. A second call:

1. Re-invoked `backbone.startUp(this)` — harmless; `KafkaBackbone` is guarded by `componentModulator.compareAndSet(STOPPED, STARTING)` and returns when already `STARTED`. (The agent claim that this throws `ComponentStateException` was wrong; that happens only after `shutDown`.)
2. Re-ran `protocol.init(this, servletConfig)` for every protocol. `WebSocketTransport.init` calls `container.addEndpoint(config)` on the same path again; JSR-356 containers reject the duplicate with `DeploymentException`, wrapped as `ServletException`.
3. Reassigned `idleChannelExecutor` and `idleSessionExecutor` to fresh executors, orphaning the first pair (daemon threads, references lost, never shut down). With long-polling only, the second `start` "succeeded" and left two idle-maintenance cycles running against the same maps.

Reachability: `start` is called only from `OumuamuaServlet.init` and `AsyncOumuamuaServlet.init`. The documented wiring registers one servlet once, so the defect was latent — reachable by registering both servlet classes, mapping one class twice, or a container re-initialising the servlet against the same server instance.

### 4.2 What was changed (uncommitted, in the working tree)

- `impl/.../OumuamuaServer.java`: `private final AtomicBoolean started = new AtomicBoolean(false);` and, first thing in `start`, `if (!started.compareAndSet(false, true)) throw new ServletException("The OumuamuaServer has already been started");`. Javadoc explains why and adds the `@throws` case. `stop()` untouched. The flag is not reset on a failed start; a retry after failure is not a supported path (the backbone refuses restart after `shutDown`, and `executorService` is shut down by `stop`).
- `impl/src/test/.../OumuamuaServerTest.java`: `testSecondStartFailsBeforeReinitializingProtocols` — start once, assert the second `start` throws `ServletException`, verify `protocol.init` ran exactly once, stop.
- `BAYEUX.adoc:868`: the "idempotent" bullet now states the real contract (runs once; second call throws; the servlet's `init` is the one expected caller).

### 4.3 Verification run

- `mvn -q -pl bayeux/oumuamua/server/impl -Dtest=OumuamuaServerTest test` → 37 tests, 0 failures; the new test ran.
- `OumuamuaServletTest` 11/11, `AsyncOumuamuaServletTest` 18/18, `OumuamuaServletContextListenerTest` 2/2 — every path that drives `start` through `init` still passes.
- `LongPollingIntegrationTest` failed in `AbstractGroundwaterTest.beforeClass` (connection refused to `localhost:2375`): the Docker-daemon gate, not this change; no test method ran.

### 4.4 Still to do for this fix

- Commit it (not yet done; message should cite the re-registered endpoint and orphaned executors).
- Add the double-start consequence to the Technical Guide's lifecycle subsection when it is written (section 8, item 6), and to the Troubleshooting entry for "server fails during servlet init".

## 5. Structural gaps by division

### 5.1 Opening and navigation (division 1)

- **(verified)** No links to Teaching / Technical / Exceptions-Notes-References divisions, because none exist. `At A Glance` points at flat sections instead.
- **(verified)** No audience line, no prerequisite-knowledge line, no versioning line. `${project.version}` in the Installation snippets is never defined; in an adopter's POM it resolves to the adopter's own version.
- **(verified)** The beginner path's first stop is `Conceptual Model` (134-224), an eleven-term glossary of which three or four terms are needed to send one message; the next stop spends 268-324 on module descriptors.
- **(verified)** The artifact chooser has no "kind" column and never mentions the two aggregator POMs (`smallmind-bayeux`, `oumuamua-server`), so a reader cannot tell they are not dependable artifacts.
- **(verified)** The CometD comparison (28-36, 58-66: "a dozen artifacts", "bean graph not cheap to reason about", "much more linear code path") is the unverified comparative claim the guide tells authors to drop; "compact", "clean", "no annotation magic" are the adjectives it forbids.
- **(verified)** Caller responsibilities (provide a servlet container, mount the servlet, match URL patterns, choose an executor, own the scope, stop the server) are scattered rather than stated in the opening.

### 5.2 Teaching Guide (division 2)

- **(verified)** No labelled teaching division. Lessons are interleaved with reference: Quick Start (374) → Package Structure (531) → API Reference (809) → Wiring Without Spring (1118) → Service (1153) → Listeners (1203) → Kafka (1233) → Extensions (1367).
- **(verified)** None of the six lesson-like sections has the six required elements (outcome, prerequisites, ordered steps, expected result, cleanup, next/reference links). The service lesson has no client call and no expected response; the listener lesson has no expected log line; the Kafka lesson has no peer-node expected result.
- **(verified)** The first lesson is not runnable: no filename, no `web-grizzly` dependency or Grizzly server bean in the Maven block, no launch procedure, the failing `perApplicationScope` ref (3.5), the non-compiling publish snippet (3.1), and the extraneous `AckExtension`.
- **(verified)** The Bayeux entry's required handshake → subscribe → publish → receive flow *observed on the client* is met only by a four-line JavaScript fragment with no statement of where it runs, how the CometD library is served, or what output to expect.
- **(verified)** Direct construction appears 740 lines after the Spring Quick Start and behind two reference catalogs, although the module supports it and the guide prefers it first.
- **(verified)** The two assembly paths are not kept parallel: services register only via `addService`, the backbone only via XML, the security policy only in prose.
- **(verified)** No `SecurityPolicy` lesson (denied subscribe → `error` field). No channel-configuration lesson (`ChannelInitializer`, `setReflectingPaths`). `AsyncOumuamuaServlet` and the Claxon latency listener have neither a lesson nor a stated supporting role.
- **(verified)** Lesson order puts Kafka (needs a broker and the optional artifact) before the two shipped extensions that need nothing.
- **(verified)** Titles are feature labels or class names (`Extensions (AckExtension, TimesyncExtension)`, `=== AckExtension`).
- **(verified)** Quick Start callout 7 (~34 lines) and Kafka callout 8 (~34 lines) each restate the full `PerApplicationScope.resolve` contract inside a lesson.
- **(verified)** Java examples omit the space before `(` in declarations; one-line `@Override ... { return packet; }` methods depart from house style.
- **(verified)** Example domain drifts: `/chat/room/42` → `/service/echo` → no channel at all in later lessons.

### 5.3 Technical Guide (division 3)

- **(verified)** No single labelled reference division. Reference material is in six top-level sections, two of them (`Package Structure`, `API Reference`) ahead of the remaining lessons.
- **(verified)** `Package Structure` (531-806) is a ~275-line type-by-type catalog — the "file-by-file catalog" the guide says is not a substitute for API guidance — and the first substantial thing after the Quick Start.
- **(verified)** Several operational contracts exist only inside lesson callouts (scope resolution, backbone lifecycle and wire format, loopback filtering, consumer-group model) and nowhere in reference.
- **(source)** `stop()` is never documented (see 3.2).
- **(verified)** The client-observable `advice` contract (`advice.reconnect` values per meta response, what `sessionConnectIntervalSeconds` drives) is never stated.
- **(verified)** Listener chain order is never stated (registration order: configuration `listeners` array first, then `addListener`); it matters for placing transforming listeners before `AckExtension`.
- **(verified)** Reference subsections do not link back to the lessons that demonstrate them; within 1408-1782 the only cross-reference is `<<executor-and-scope>>`.
- **(verified)** Major contracts (freezing, persistent/reflecting/streaming) are stated two or three times in different sections with no statement marked authoritative.
- **(verified)** `Thread Safety` lists data-structure choices (`ConcurrentHashMap`, `ConcurrentLinkedQueue`, `ConcurrentSkipListMap`) rather than caller-visible guarantees; those belong in Javadoc.
- **(verified)** `Error Model` (1620) belongs in the technical guide as a failure-signature table but overlaps `Troubleshooting`, which carries the more precise path rules.

### 5.4 Closing (division 4)

- **(verified)** Ends with `== Summary` (1866-1880), the generic recap the guide says not to write.
- **(verified)** No `References` section at all; the only external link is the CometD docs at line 28; no link to the Bayeux specification.
- **(verified)** `Limitations and Known Issues` lists five deliberate design constraints without labelling them as such, and records no verified known issues or unverified assumptions.
- **(verified)** Troubleshooting entries do not say which channel a failure arrives through (thrown / returned to client / logged / asynchronous). "Client subscribes but never receives" points at the server log for a `SecurityPolicy` rejection that is actually returned to the client in the meta-subscribe `error` field.
- **(verified)** Closing entries name tunables without linking to the sections that define them.

### 5.5 Cross-cutting

- **(verified)** `README.adoc:24` describes the module as "CometD/Bayeux protocol support for server-sent events and WebSocket messaging"; the chapter describes a standalone Bayeux *server* (Oumuamua) that is an alternative to CometD and uses CometD only as a client. The front door needs correcting.
- **(verified)** Existing anchors (`conceptual-model`, `installation`, `quick-start`, `api-reference`, `writing-a-bayeux-service`, `clustering-with-kafka`, `executor-and-scope`, `defaults-reference`, `extensibility`, `troubleshooting`) are unprefixed. The guide says to **retain** them and to prefix only **new** anchors with `bayeux-`.
- **(verified)** Heading case is inconsistent with house style in places (`=== `Server.Listener` hierarchy`, `Writing a Bayeux Service`, `Listeners and Observability`).

## 6. Findings that were refuted — do not act on these

- "All existing anchors must be re-prefixed `bayeux-`." Refuted: `TEACHING_GUIDE.md` step 5 says retain old anchors where possible and prefix *new* ones.
- "Missing links to generated Javadoc is a chapter defect." Refuted: the guide says to determine the publication location before adding such links; absence is correct until that decision is made.
- "A second `start` throws `ComponentStateException` from the backbone." Refuted by hand (4.1); the real failure is the endpoint re-registration and executor leak.

## 7. Section-to-division mapping (migration procedure step 2)

| Current section (line) | Destination | Action |
|---|---|---|
| The Problem This Solves (9) | 1 Purpose | Keep; trim CometD comparison to 1–2 factual sentences; add caller-responsibilities paragraph; add Audience / Versioning lines |
| At A Glance (103) | 1 Navigation | Replace targets with the three division xrefs plus beginner path and expert entry |
| Conceptual Model (134) | 1 Purpose (trimmed) + 3 Technical | Keep channel, session/handshake/connect, broadcast vs `/service/`, long-poll vs WebSocket; move Route, Packet, freezing, Codec/Value, Backbone, ack mechanics to Technical Guide |
| Choose The Right Artifact (226) | 1 Navigation (table) + 3 Technical (descriptors) | Add Kind column and aggregator rows; move 268-324 module-descriptor detail to "Artifacts And Module Path" in Technical Guide |
| Installation (327) / Runtime Prerequisites (352) | 2 Teaching (lesson 1 prerequisites) + 3 Technical | Fix artifactIds; define `${smallmind.version}`; keep a full Maven block in lesson 1 |
| Quick Start (374) | 2 Teaching, lesson 1 | Rebuild (section 8, item 3) |
| Package Structure (531) | Delete; residue → 3 Technical / Javadoc | Fold servlet/listener duties into Lifecycle, `DebonedPacket`/`RecordUtility` into Backbone Wire Format, config view into Diagnostics; rest to Javadoc |
| API Reference (809) | 3 Technical "Public Types And Extension Points" | Keep listings; fix `Session` drift; add listener-order rule; link each type to its lesson |
| Wiring It Up Without Spring (1118) | 2 Teaching, lesson 1 or 2 | Make primary path or explicit equivalent; fix `OrthodoxCodec` diamond; define `servletContext`/`servletConfig` or mark fragment |
| Writing a Bayeux Service (1153) | 2 Teaching | Fix `DefaultRoute` static init; add client call, expected response, Spring `services` equivalent |
| Listeners and Observability (1203) | 2 Teaching | Declare logger; show expected log line; add `SecurityPolicy` lesson beside it |
| Clustering With Kafka (1233) | 2 Teaching (last lesson) + 3 Technical | Lesson keeps config + two-node expected result + "what local success does not prove"; callout 8 content → Backbone Contract subsection |
| Extensions (1367) | 2 Teaching (two lessons, before Kafka) | Retitle by outcome; add expected results (reconnect replay; `timesync` fields) |
| Behavioral Reference (1408) and subsections | 3 Technical "Behavioral Contracts" | Keep; make these the single authoritative statements; add "Demonstrated in" pointers |
| Thread Safety (1524) | 3 Technical (guarantees) + Javadoc (structures) | Split |
| Executor And Scope (1555) | 3 Technical "Lifecycle, Executor, And Scope" | Add `stop()` ordering, double-start rule, executor shutdown on stop, `idleCheckingTerminationTimeoutSeconds` |
| Error Model (1620) | 3 Technical "Failure Signatures" table | Columns: condition / raised by / signal (thrown, returned `SecurityRejection`, client `error` field, logged) / effect |
| Defaults Reference (1665) | 3 Technical "Configuration" | Rebuild (section 8, item 7) |
| Extensibility (1741) | 3 Technical "Extension Points" | Keep; link each point to its lesson or mark "no lesson; supporting role" |
| Troubleshooting (1784) | 4.1 Exceptions And Troubleshooting | Keep; add channel tag per entry; add "See <<...>>" links; add double-start entry |
| Limitations and Known Issues (1837) | 4.2 Notes And Limitations | Retitle; group as Deliberate / Interoperability caveats / Unverified |
| Summary (1866) | Delete | Replace with 4.3 References |

## 8. Next steps, in order

1. **Commit the `start()` guard** (section 4) as its own change.
2. **Fix the factual errors** (section 3) in place before any restructuring, so the content being moved is already true: the three compile errors, the artifactIds, the streaming/negotiation/`isLocal`/ack-cap/`dispatch` statements, the executor-shutdown statement, the WebSocket registration description, the Defaults rows, and remove the `perApplicationScope` ref from the Quick Start. Compile the corrected snippets against `oumuamua-server-impl` (a throwaway test class in `impl/src/test` is the cheapest harness).
3. **Rebuild lesson 1** as "Deliver One Message To A Subscribed Browser Client": `OrthodoxCodec` + `ServletProtocol` only; a named Spring file (or the direct-construction equivalent, see 4); a Maven block with `web-grizzly` and the Grizzly server bean and a defined `${smallmind.version}`; a served HTML page using the CometD JS client (state the script URL and page location) or `cometd-java-client`; the exact expected console output with a timeout for the long-poll case; a Cleanup step (`stop()` / context shutdown); Next links to the service lesson and to Lifecycle, Packet Pipeline, and Defaults.
4. **Decide the primary assembly path.** Preferred by the guide: direct construction first, Spring XML second as the equivalent. Whichever is chosen, every later lesson shows the delta on that path plus a one-block equivalent on the other (`services`/`listeners`/`backbone`/`securityPolicy` properties ↔ `addService`/`addListener`/`setBackbone`/`setSecurityPolicy`).
5. **Impose the four divisions.** `[[bayeux-teaching-guide]] == Teaching Guide` after Installation with lessons as `===`; `[[bayeux-technical-guide]] == Technical Guide` after the last lesson; `[[bayeux-exceptions-and-troubleshooting]]`, `[[bayeux-notes-and-limitations]]`, `[[bayeux-references]]` at the end. Keep every existing anchor; prefix only new ones. Rewrite `At A Glance` as the division navigation.
6. **Write the lessons** with the six labelled elements each, in this order: deliver one message → build programmatically and stop → answer a request on a service channel → deny a subscribe with a `SecurityPolicy` → log every request with a packet listener → guarantee redelivery after reconnect (ack) → let clients estimate server time (timesync) → make a channel persistent and reflecting → cluster two nodes over Kafka (broker prerequisite stated; two-node expected result; "what local success does not prove") → implement a minimal custom extension point. Carry the `/chat/room/42` domain throughout. Declare every symbol or mark the block as a fragment.
7. **Write the Technical Guide** by lookup concept: Artifacts And Module Path (from 268-324); Public Types And Extension Points (from API Reference + Extensibility, with listener-order rule and `Session` fixes); Configuration (rebuilt Defaults table: Setting | Type | Unit | Library default | Valid values | Set via, with the missing rows and "required, no default" for the long-poll timeout); Behavioral Contracts (lifecycle, packet pipeline, freezing, reflecting/streaming corrected, path patterns, meta responses and `advice`); Lifecycle, Executor, And Scope (single authoritative scope statement; `start` once; `stop` ordering; supplied executor is shut down); Backbone Contract And Wire Format (from Kafka callouts); Failure Signatures (from Error Model); Diagnostics (the `@Doppelganger` configuration view). End each subsection with "Demonstrated in <<lesson>>".
8. **Cut the scope callouts** (Quick Start 7, Kafka 8) to two sentences plus a link to the Lifecycle/Scope subsection.
9. **Write the closing**: tag each Troubleshooting entry with its channel and link its defining section; add a double-start entry; group Notes And Limitations; write References (SPARK per-application scope, KAFKA, WEB Grizzly installers, the Bayeux 1.0 specification, the CometD JavaScript client docs, each with a one-clause reason; verify every link).
10. **Fix the front door**: `README.adoc:24` to describe a standalone Bayeux server, not "CometD/Bayeux protocol support".
11. **Javadoc pass** (guide's Javadoc Responsibilities): move the data-structure detail from `Thread Safety` into `OumuamuaServer`/`OumuamuaSession`/`AckExtension` Javadocs; make `Session.dispatch`/`initiateDisconnect`, `Transport.isLocal` (unused), and `Server.start`/`stop` contracts precise; do not add links to the `.adoc` until the publication location is decided.
12. **Update `ASCIIDOC_DOC_AUTHORING_GUIDE.md`** with the teaching-first order, as `TEACHING_GUIDE.md` requires for the eventual migration (can be done once, with the first migrated chapter).

## 9. Verification plan and what remains unverified

- Compile every lesson example (step 2 harness). Until then, the compile-error claims in 3.1 are reading-based, not `javac`-based.
- Run `OumuamuaServerTest`, the three servlet/listener test classes, and — with a Docker daemon available — `LongPollingIntegrationTest`, `AckExtensionIntegrationTest`, and the multi-node Kafka fan-out test for the Kafka lesson's expected result. Record Docker-gated runs separately from unit runs, per the guide's acceptance criterion on distinguishing checked examples from infrastructure-dependent scenarios.
- Render `BAYEUX.adoc` and check anchors, xrefs (including into `SPARK.adoc`, `KAFKA.adoc`, `README.adoc#module-path`), tables, and callout numbering after the move.
- **(unverified)** Whether the CometD JavaScript client the lesson relies on is version-compatible with the Bayeux version the server advertises; check against `Message.MINIMUM_VERSION` and the CometD release used.
- **(unverified)** Exact `advice.reconnect` values per meta response; the critic cited `spi/meta/Meta.java` but the table in step 7 must be written from source.
- **(unverified)** Whether the Grizzly WebSocket runtime exposes `ServerContainer` early enough that `OumuamuaServlet.init` (which triggers `WebSocketTransport.init`) succeeds with `loadOnStartup=1`; the test context works, but the lesson should state the required Grizzly `WebSocketOption`.
- Read the finished lesson 1 as a new adopter with only its stated prerequisites, and the Technical Guide as someone troubleshooting without tutorial context (guide step 10).
