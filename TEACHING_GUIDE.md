# Instructions For Converting SmallMind Documentation Into Teaching Guides

## Purpose And Scope

Rework the documentation so that each sub-project teaches a reader why it exists, when to use it, and how to accomplish useful tasks with it. Begin with the smallest working example and build toward more involved tasks where the implementation supports them. Follow the teaching material with a clearly separated technical guide, then exceptions, notes, and external references.

This file is an implementation brief for that future documentation work. It does not authorize API changes, new integrations, or library refactoring. Preserve useful existing documentation and verify it against the implementation while reorganizing it.

Apply these instructions to the root-level AsciiDoc chapters and their associated source Javadocs. Cover independently usable child artifacts and feature families, not just Maven aggregator names. An aggregator POM needs selection guidance; it does not need an artificial runtime tutorial.

Read the matching chapter, `README.adoc`, `CODE_STYLE.md`, `DESIGN_PHILOSOPHY.md`, relevant POMs, and nearby implementation before rewriting a module. Use `ASCIIDOC_DOC_AUTHORING_GUIDE.md` for AsciiDoc syntax and presentation. For this migration, the teaching-first order specified here takes precedence over its more flexible section ordering. Preserve its requirements for accurate defaults, explicit ownership, optional dependencies, and restrained prose. Update that authoring guide as part of the eventual migration so future contributors receive consistent instructions.

## What The Current Documentation Provides

The current chapters already offer substantial source-oriented material: artifact selection, conceptual models, examples, configuration defaults, behavioral rules, lifecycle guidance, and troubleshooting. Many introductions explain why their module exists. Reuse this work rather than replacing it with generic introductions.

The principal structural issue is that learning and lookup are mixed together. For example:

- `SCHEDULE.adoc` and `SPARK.adoc` place extensive reference material before their quick starts.
- `MEMCACHED.adoc` introduces large API and architecture catalogs before its usage recipes.
- `NUTSNBOLTS.adoc` primarily organizes learning opportunities inside a package reference.
- `WEB.adoc` mixes purpose, types, setup, usage, and caveats separately within each artifact section.
- `MONGODB.adoc` already has a useful numbered quick start; `DOPPELGANGER.adoc` already walks through annotation, compilation, and generated output. These are useful starting patterns, but need explicit outcomes and further lessons.
- `QUORUM.adoc` explains the reasons for its independent frameworks, but examples and technical descriptions need separate destinations.

The reviewed Javadocs contain valuable precise detail. For example, `BatchParameter` explains parameter identity, `ProxyJob` describes execution bookkeeping and cleanup, `VaultTumbler` explains the cryptographic engine and thread-safety constraint, and Sleuth's `Dependency` explains graph relationships. Preserve this detail at the API level; teach users the relevant consequences before requiring them to understand those internals. This review sampled Javadocs and build files; it is not a certification that every existing statement is correct.

Resolve existing inconsistencies during each chapter's migration:

- The README describes Ansible as provisioning integration, Claxon as alerting, Doppelganger as object cloning, Phalanx as cluster coordination, Quorum as consensus/distributed locking, and Sleuth as tracing. Their chapters describe Vault interoperability, metrics, generated DTO views, broker-backed RPC, resource management, and dependency-aware test execution respectively. Confirm these descriptions against source and correct the front door.
- `ANSIBLE.adoc` claims one dependency in its opening, while `ansible/pom.xml` declares both Bouncy Castle and NutsNBolts. Recheck dependency summaries, including counts, instead of carrying them forward unchanged.
- `LICENSE.adoc` documents the license Maven plugin, while the README's license sentence points to it as though it were the license terms. Give the plugin a proper module entry and distinguish its guide from the actual license texts.
- The README currently links to chapters; it does not assemble them with `include::`. Most chapters are standalone articles, while Quorum and Sleuth use a book-style presentation. Preserve valid document forms during the content migration. Do not describe an assembled book as already present or convert every chapter to an include-safe format without a deliberate publishing decision.

## Required Reader Journey

Use the following major divisions in this order. Within them, scale the number of lessons and reference sections to the module. A small adapter may need only two lessons; an independent utility collection needs several short paths.

### 1. Purpose, Benefits, And Navigation

Open with the concrete problem the module solves, the mechanism it supplies, and the practical benefit to the adopter. Explain what work the caller still owns. Ground benefits in specific behavior: separating a batch invocation contract from Spring Batch is a useful dependency boundary; calling it powerful or seamless teaches nothing.

Identify the intended reader, prerequisite knowledge, and version convention. Include a compact artifact/task chooser where needed. Make it clear which entries are parent POMs, runtime libraries, optional adapters, annotation processors, or build plugins.

Provide explicit links to **Teaching Guide**, **Technical Guide**, and **Exceptions, Notes, And References**. Give beginners a suggested path and experienced users a direct reference entry. Avoid making a package inventory the first substantial thing a beginner must read.

### 2. Teaching Guide

Organize lessons around tasks and observable results. Use titles such as “Launch A Job And Observe Completion,” rather than class names or “Advanced Features.” Teach only enough terminology to understand the next action, then introduce more concepts when they become useful.

Every lesson should contain:

1. **Outcome and motivation.** State what the reader will produce and why this task matters.
2. **Prerequisites.** Name the earlier lesson, artifacts, services, files, credentials, and tooling needed. Explain how to obtain or prepare example inputs.
3. **Ordered steps.** Give a complete path through dependencies, configuration, implementation, execution, and inspection. Explain the important choices as they arise.
4. **Expected result.** Show a verifiable output, assertion, generated file, observable event, or state change. Account for variable timestamps, ports, ordering, or timing.
5. **Cleanup.** Show how the caller releases clients, streams, workers, containers, or application contexts where applicable.
6. **Next task and reference links.** Link to the natural next lesson and the exact technical sections underlying the behavior just used.

The first lesson must reach a useful result without optional integrations that are unrelated to that result. Use direct construction before container assembly when the module actually supports it. For Spring-specific adapters, processors, and Maven plugins, teach their real operating environment; do not invent a standalone equivalent to satisfy a template.

Continue with one meaningful variation at a time: different input, configuration override, lifecycle control, a real external service, an optional integration, and finally an extension point if one exists. Retain a consistent example domain within a learning path. Independent features may use independent examples.

Make the first example complete enough to run: explicit imports, necessary Maven configuration, actual filenames/resource locations, and an execution command or concrete launch procedure. Later examples may show deltas if they identify exactly which earlier code they modify. Mark illustrative fragments as fragments. Never present undefined beans, unimplemented helper methods, or unexplained ellipses as a runnable tutorial.

Use the project's Java formatting, including two-space indentation and spaces before parentheses in declarations. Define a version placeholder explicitly. In a consuming project, `${project.version}` denotes that consumer's version; use a documented SmallMind version property when the versions differ. Separate plugin configuration, annotation processing, and ordinary dependencies.

Essential correctness constraints belong beside the step they affect even though their full treatment comes later. For example, selecting one logging backend, closing a client, or activating an aspect cannot be deferred until the closing notes.

### 3. Technical Guide

Create one clearly labeled reference division after all teaching paths. It must be usable without reading the tutorials in sequence. Organize it by the concepts readers will look up:

- Artifact boundaries, optional dependencies, runtime providers, and module-path requirements.
- Public types and extension points, with links to generated API documentation where available.
- Configuration parameters, types, units, exact defaults, valid values, and precedence.
- Behavioral contracts: identity, null handling, ordering, retries, fallback, replacement, and persistence semantics where applicable.
- Lifecycle, resource ownership, concurrency, blocking, shutdown, and cancellation.
- Packaging, generated resources, wire formats, and integration contracts relevant to the module.

Use tables for lookup material and focused prose for rules. Distinguish a library default from an example's chosen setting and from an upstream product default. State the construction or assembly path when defaults differ by path.

Place architectural internals here only when they help explain an integration contract, extension mechanism, or diagnostic. A file-by-file catalog is not a substitute for API guidance. Link back to the lesson demonstrating each major contract.

### 4. Exceptions, Notes, And References

End with separate subsections in this order:

1. **Exceptions And Troubleshooting:** exception or symptom, triggering condition, observable effect, and corrective action. Distinguish thrown exceptions, logged failures, asynchronous notifications, and legitimate empty results. Keep method-level `@throws` contracts in Javadocs too.
2. **Notes And Limitations:** edge cases, supported boundaries, interoperability caveats, and verified known issues. Distinguish deliberate limitations from defects and unverified assumptions. Put necessary operational procedures in the technical guide rather than relegating them to miscellaneous notes.
3. **References:** related SmallMind guides, generated API documentation, and relevant official external specifications or documentation. Explain briefly why a link matters. Verify external links and version relevance when authoring the actual chapter; do not rely on remembered upstream behavior.

Do not repeat a generic concluding summary. Finish with useful destinations for the reader.

## Javadoc Responsibilities

Keep AsciiDoc responsible for the progressive learning experience and cross-type integration tasks. Keep Javadoc responsible for precise package, type, and member contracts.

- Add or improve package introductions where they offer a useful entry point. Explain the package's purpose, principal collaborators, minimal call sequence, ownership, and where to find its tutorial.
- Start public type documentation with its role and when to use it. Explain collaborators and lifecycle before implementation machinery.
- Document parameter meaning and units, nullability, return semantics, defaults, side effects, thread safety, and failure conditions where relevant. Preserve useful `@param`, `@return`, `@throws`, `@see`, and `{@link ...}` material.
- Add short examples only where they clarify the API. Do not paste whole chapter tutorials into each class. Use API links between related types and a verified documentation URL for the corresponding lesson.
- Distinguish interface guarantees from behavior supplied only by one implementation. For example, execution and cleanup promises around `ProxyJob` need verification in the scheduler adapter, not merely repetition from the interface comment.
- Verify existing comments against source. A descriptive comment about intended behavior is evidence to investigate, not proof that every implementation enforces it.
- Preserve source headers and avoid executable-code changes during Javadoc edits. Do not add public APIs solely to make an example convenient.

Determine the actual publication location before adding links from generated Javadocs to AsciiDoc guides. Repository-relative `.adoc` paths do not automatically work from published API HTML. Validate the built links rather than inventing a site layout.

The root POM configures Javadoc with `doclint` set to `all,-missing`; a successful Javadoc build therefore does not prove documentation completeness. Inspect coverage and the rendered reader experience as well.

## Chapter-Specific Instructions

Each entry below specifies the teaching progression and the material that needs particular care. The sequences are authoring plans; verify all concrete calls and guarantees before turning them into examples.

### Ansible — `ANSIBLE.adoc`

Teach Java participation in an existing Ansible Vault workflow. Begin with decoding a prepared vault using an explicitly supplied password, then encode/decode a value, add a format 1.2 label, and handle vault-id arguments. Keep low-level `VaultTumbler` use as an optional final lesson. Move cryptographic construction and file-format details to the technical guide. Verify provider dependencies, stream ownership, and the distinction between authentication failure and malformed input. Avoid printing secrets in the tutorial's expected output.

### Artifact — `ARTIFACT.adoc`

Begin by resolving one coordinate to a local file. Extend to a dependency closure, then a child classloader, then change monitoring. Explain the benefit of reusing Maven settings at the moment the reader configures repositories. Keep settings resolution, scope/traversal rules, polling defaults, change detection, offline behavior, and worker/classloader lifecycle in reference sections. Supply a deterministic example artifact and state the network/cache prerequisites; do not equate file-change detection with safe application redeployment.

### Batch — `BATCH.adoc`

Teach the `batch-base` invocation/parameter contract separately from the `batch-spring` runtime. Build one small job, prepare its metadata schema, launch it, inspect completion, then add monitoring, restart, and shutdown quiescence. Explain identifying parameters before the first repeat launch. Move polling cadence, exit-status versus batch-status rules, concurrency settings, and schema resources to reference sections. Verify all required beans and changelog paths so the first lesson is executable.

### Bayeux — `BAYEUX.adoc`

Explain the API/SPI/implementation split and teach one complete handshake, subscription, publish, and receive flow. Include the client-side steps needed to observe delivery, even though this chapter documents a server. Continue with service methods, security/listeners, extensions, and optional Kafka clustering. Keep session/channel lifecycle, packet processing, freezing, reflecting/streaming semantics, executor ownership, and defaults in the technical guide. Separate local server success from cluster delivery guarantees.

### Claxon — `CLAXON.adoc`

Present the module as metrics collection and emission. Teach registry setup and one observable reading with a suitable emitter, then counters/timers, tags and naming, mutable-object observation, and optional annotation-based instrumentation. Give each emitter family a short task path: message, JMX, Prometheus, Datadog, AWS, and noop selection for disabling output. Cover HTTP and exotic features where their independent capabilities warrant lessons. Keep aggregation cadence, meter identity, buffering, naming transformations, feature throttling, and shutdown in reference sections. Show the required weaving setup before an annotation example claims to record anything.

### Doppelganger — `DOPPELGANGER.adoc`

Teach generated request/response views from a domain model. Expand the existing annotate/compile/use sequence into a verifiable first lesson that identifies generated source names and locations. Then add create/update purposes, visibility, constraints, virtual views/pledges, and polymorphism as supported. Put annotation catalogs and processing details in the technical guide. Explain processor setup and compiler diagnostics explicitly. Keep comparisons with other tools brief and evidence-based. This is the canonical guide for `web-json-doppelganger`; Web should link here.

### File — `FILE.adoc`

Provide independent teaching paths for `file-ephemeral` and `file-jailed`. Start with a scoped filesystem and a write/read operation; continue to watch services and delegation for ephemeral storage, and translators and per-request roots for jailed storage. Teach JVM-default-provider installation only after explicit use. Document exact close, copy/move, capacity, link, watch, and path-identity behavior in reference sections. Explain the actual confinement boundary without claiming a broader security guarantee than the implementation supports.

### JavaFX — `JAVAFX.adoc`

Start with a complete minimal JavaFX application using one extras control and describe its visible result. Offer short independent paths for debouncing, dialogs, enum controls/converters, validated text, table columns, image panes, and the Parabox bridge. Progress from a directly driven chart to live MBean polling. Keep thread-affinity, listener/worker cleanup, parsing/validation behavior, and defaults in reference sections. State JavaFX launch/module prerequisites and avoid making readers set up JMX to learn a basic control.

### Kafka — `KAFKA.adoc`

Teach explicit connector setup, one produced record, and a bounded consumer run with cleanup. Continue with admin operations, startup reachability checks, shared connector use, and security overrides. Distinguish helper behavior from broker behavior. Move the large broker configuration catalog into a clearly identified technical subsection; explain which values are recommendations or examples rather than helper-enforced defaults. Verify subscription, offset, timeout, and client-ownership semantics. A successful reachability check must not be presented as proof of delivery or durability.

### License Plugin — `LICENSE.adoc`

Teach plugin configuration in a small disposable sample reactor: prepare notice/license files, generate headers, inspect the diff, then package and inspect copied license texts. Continue to rules, multi-module root selection, and custom stencils. Keep goals, lifecycle phases, glob semantics, rewrite/idempotency behavior, and encoding/line-ending constraints in reference sections. Make source mutation explicit before execution. Explain the tooling benefit without presenting the plugin as a legal-compliance guarantee; link actual repository license texts separately.

### Liquibase — `LIQUIBASE.adoc`

Start with one explicit Spring bean, a disposable database, one changelog, and a verified schema change. Continue to multiple changelogs, contexts, SQL preview, alternate sources/classloaders, and custom types where supported. Treat `schema-hibernate.xml` as a later integration lesson. Put goal/source catalogs, initialization order, transactions, Scribe discovery, and level mapping in the technical guide. Identify output files and database side effects for every goal.

### Memcached — `MEMCACHED.adoc`

Teach a basic cache operation against the utility abstraction and its in-memory implementation, then show the corresponding Cubby client against a real server. Explicitly explain differences between the test implementation and network client. Continue to expiry, CAS updates, counters, bulk operations, codecs, and optional Spring assembly. Move protocol commands, connection internals, configuration, host recovery, and timeout semantics into the technical guide. Preserve the utility/runtime boundary and cross-link Persistence for cache composition. Reassess comparative claims about other clients instead of carrying promotional or unverified assertions into the introduction.

### MongoDB — `MONGODB.adoc`

Provide a utility-only client-configuration path and a separate Throng mapping/query path. Expand the numbered quick start with database prerequisites, sample data, expected query results, and cleanup. Add mapping annotations, query composition, iteration, indexes, lifecycle hooks, and Spring configuration incrementally. Keep codec composition, null behavior, write concerns, index side effects, and ownership in the technical guide. Identify which behavior belongs to the MongoDB driver and which Throng adds.

### NutsNBolts — `NUTSNBOLTS.adoc`

Use a task chooser and multiple short learning paths rather than a single tour through every package. Account for all current package families: CLI parsing; context and scope; resource/property/IO; CSV/XML/JSON/ZIP; templates/mail; HTTP/network/NTP; security/TLS; time/retry/validation; reflection/classloading/annotation processing; layout; Spring/injection; collections; servlet/SOAP; and Shiro namespace support. Group only genuinely related tasks. Small utilities may need one example; context propagation or classloading may need a progression.

Use the existing examples package where applicable, checking it against current APIs. Preserve the optional-dependency matrix and turn the current package reference into the technical guide. Teach thread-bound context separately from `ScopedValue` scope and explain propagation explicitly. Place insecure or permissive TLS helper limitations beside their usage as well as in closing notes; do not use them as an unexplained default connection recipe.

### Persistence — `PERSISTENCE.adoc`

Teach the Durable/DAO vocabulary with one chosen storage path before introducing the full stack. Establish a working store and transaction boundary, then add cache behavior one concern at a time: `@CacheAs`, `@CachedWith`, and coherence where appropriate. Offer separate paths for SQL/JPA/Hibernate and Throng-backed usage; cover connection pooling and sequence generation as focused tasks. Keep annotation semantics, aspect precedence, boundary propagation, session ownership, registration order, and cache-key/expiry rules in the technical guide. Demonstrate weaving and required assembly explicitly. Cross-reference MongoDB, Memcached, Liquibase, and Quorum rather than duplicating their manuals.

### Phalanx — `PHALANX.adoc`

Teach interface-based RPC with the in-process mock transport first: define an interface, register an implementation, create a proxy, invoke it, and observe the result. Then move that same example across one real broker; offer RabbitMQ, Kafka, and JMS as alternative paths with their own prerequisites. Add wire contexts/scopes and controlled failure/timeout examples. Put serialization, error propagation, threading, workers, and transport lifecycle in reference sections. Explain remote-call consequences even though the call syntax resembles a local invocation.

### Quorum — `QUORUM.adoc`

Keep four distinct teaching paths: pool, juggler, bucket, and namespace. Teach borrowing/returning a simple pooled resource before complex pool policies; one balancing task before recovery; one rate limit before weighted or per-tenant limits; and one naming lookup before pooled contexts or a new backing store. Reserve pooled, balanced, rate-limited composition for a final optional lesson. Place algorithms, validation, blocking/timeout rules, thread safety, and lifecycle in the technical guide. Describe this as resource management, not a distributed consensus implementation.

### Schedule — `SCHEDULE.adoc`

Move the quick-start work ahead of the API catalogs. Teach one `QuartzProxyJob`, required beans, a cron schedule, observed execution, and shutdown. Explain what `schedule-base` contributes to callers that only need the job contract. Continue to allowlists, retiring schedules, persistence/schema setup, and JMX control. Keep completion logging, enabled-state handling, job resolution, schedule comparison/synchronization, interruption, and startup/shutdown ordering in reference sections. Clarify the difference between scheduling work and Batch's job execution model.

### Scribe — `SCRIBE.adoc`

Start with the library/application distinction: a reusable library uses the logging contract; an application selects the backend. Teach one emitted message with one backend, then formatting/context, filters, file rollover, and asynchronous output. Provide optional paths for SLF4J/Commons bridges, JDK/Log4j backends, Spring plans, and custom blueprints/appenders. Keep discovery, provider uniqueness, template precedence, queueing, flushing/shutdown, and live-configuration semantics in reference sections. Explain bridge direction and valid classpath combinations before adding dependencies.

### Sleuth — `SLEUTH.adoc`

Teach test execution, not tracing. Begin with one suite run through the Surefire provider and a recognizable report; provide programmatic embedding as a second entry path. Add setup/teardown, ordering versus success dependencies, groups, shared state, listeners, and concurrency progressively. Include a deliberately failing prerequisite to teach skip/culprit propagation. Put annotation catalogs, graph internals, provider settings, cancellation, and lifecycle ordering in the technical guide. State the verified limits of TestNG interoperability and `testng.xml` support.

### Spark — `SPARK.adoc`

Provide separate paths for Singularity executable jars and Tanukisoft service distributions. Move both quick starts before parameter/classloading catalogs. Begin with a tiny main program, plugin configuration, package command, generated artifact, and launch result. Add exclusions/resources/modular mode for Singularity and wrapper lifecycle/configuration for Tanukisoft. Keep goals, custom packaging types, manifests, templates, classloader rules, attachment behavior, exit codes, and native platform requirements in reference sections. Treat installing a system service as a distinct later task with its effects and cleanup explained.

### Testbench — `TESTBENCH.adoc`

Retain its positioning as SmallMind's internal test harness. Teach one service fixture with readiness checking and guaranteed teardown, then conditions and their polling, Groundwater, test logging, and the Foundation Spring context. Give style/build-file tools a separate task path. Cover each child artifact without making all of them prerequisites for the first fixture. Put the Docker catalog, daemon selection, mount handling, lifecycle, defaults, and environment assumptions in reference sections. State when an example needs Docker or external software and how to recognize unavailable infrastructure.

### Web — `WEB.adoc`

Replace the repeated mixed mini-manuals with a teaching division containing independent artifact paths, followed by a technical division organized by those same artifact names. Start with an artifact chooser; adopting one helper must not imply adopting the entire web stack.

- **Grizzly and Jetty:** teach alternative minimal servers with a visible request/response and shutdown; follow with installers, TLS, session or request-scope concerns, and supported WebSocket/SOAP integrations.
- **Jersey:** teach one resource and JSON response, then exception mapping, CORS, paging, parameter injection, and client proxies.
- **HTTP:** teach one request with resource handling, then timeout/pooling configuration; explain when this API or `JsonTarget` fits.
- **JSON scaffold:** teach a response/fault representation and its resulting payload.
- **JSON query:** teach a predicate tree and an available backend translation, then composition and UI metadata. Identify which backend capabilities actually exist.
- **JWT:** distinguish parsing from signature verification and claim validation; show exactly what the available API checks. Do not label a decoded token trusted unless the example establishes that property.
- **Schema plugin:** teach a Maven validation run with one passing and one failing input and their observable results.
- **Doppelganger:** provide a task-oriented link to its dedicated chapter rather than a competing tutorial.

Retain per-artifact dependencies and module-path details in the technical guide. Consolidate troubleshooting without losing artifact-specific symptoms.

## Migration And Verification Procedure

1. Inventory root chapters, Maven reactor children, source packages, resource files, tests, and existing examples. Record which tutorial and reference section will cover each published artifact or independent feature family. Include plugins and internal tools.
2. For each chapter, map every existing section to purpose, lesson, technical reference, or closing material before moving it. Preserve useful contracts and record questionable claims for source verification.
3. Draft the task progression and expected outcomes first. Confirm the first task is feasible using the real API and documented prerequisites. Then adapt existing examples into lessons and add only the missing instructional steps.
4. Consolidate technical facts into authoritative reference sections. Keep small, necessary reminders in lessons with links to those sections. Update related Javadocs where contracts are missing or inconsistent.
5. Preserve existing filenames and useful anchors. When moving content, retain old anchors where possible; otherwise update all incoming links. Prefix new anchors with the module name, such as `schedule-teaching-first-job` and `schedule-reference-lifecycle`, to avoid collisions if chapters are later assembled.
6. Maintain the current standalone/front-door publishing model unless a separate decision changes it. Preserve one top-level title and valid article/book attributes; apply the anchor, `[partintro]`, audience, and versioning rules if a chapter is deliberately converted to include-safe form.
7. Correct the README descriptions and module navigation as each chapter is verified. Keep repository-wide module-path explanation centralized, with module-specific requirements in chapter references.
8. Compile or execute the changed examples using the affected module's supported build path. Verify packaged XML, service registrations, processor output, plugin descriptors, and resources where those are part of a lesson. Use focused checks; do not require an unrelated full reactor build for every prose change.
9. Build the affected Javadocs and render the AsciiDoc using the repository's available tooling, or record explicitly when rendering cannot be performed. Check links, anchors, tables, code blocks, navigation, and generated API links. Run the relevant documentation build without inadvertently applying source-mutating plugin goals to unrelated files.
10. Read the first lesson as a new adopter with only its stated prerequisites, and read the technical guide as someone troubleshooting without tutorial context. Record any external-service or platform checks that remain unverified instead of claiming they passed.

## Acceptance Criteria

A migrated chapter is complete when:

- Its opening explains a concrete problem, the module's benefit, the intended audience, and the caller's responsibilities.
- Every independently usable artifact or feature family has an appropriate learning path or an explicit explanation of its supporting role.
- A beginner can choose an artifact and complete a useful first task without reading the full reference catalog.
- Lessons progress deliberately, include observable outcomes and cleanup, and use verified APIs, coordinates, resources, and configuration.
- Teaching, technical reference, and closing exceptions/notes/references are visibly separate and cross-linked in both directions where useful.
- Reference sections preserve exact operational contracts, optional integration boundaries, and lifecycle ownership.
- Javadocs remain precise and navigable, with no unsupported promises or broken links to tutorials.
- README descriptions, old anchors, module navigation, and authoring instructions agree with the revised documentation.
- Validation results distinguish checked examples from unexecuted infrastructure-dependent scenarios.
- The change teaches the existing library without silently changing its behavior or inflating its claims.
