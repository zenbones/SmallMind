# Jigsaw: Giving Every Artifact A Module Descriptor

This document is a work order for a later session (or several). It describes how to take the 59 artifacts in this repository from "plain jars with no module identity" to "explicitly named Java Platform Module System (JPMS) modules with `module-info.class`", in stages that each leave the build green and the published artifacts usable.

Read `AGENTS.md`, `CODE_STYLE.md`, and `DESIGN_PHILOSOPHY.md` before starting. Everything below assumes that style: explicit wiring, caller-owned lifecycle, optional integrations kept optional, and documentation kept in sync with code. A module descriptor is a *public contract*; treat every `exports`, `opens`, `requires static`, and `provides` as API.

Line numbers and version strings cited here were accurate when this document was written (`7.4.0-SNAPSHOT`, Maven 3.9.14, JDK 25, `maven-compiler-plugin` 3.14.1, `maven-surefire-plugin` 3.5.4, AspectJ 1.9.25.1 via `dev.aspectj:aspectj-maven-plugin` 1.14.1) and will drift. Re-verify before editing.

## Progress

Update this block at the end of every stage or wave; it is the entry point for a new session.

| Stage | Status |
|---|---|
| Stage 0 — `Automatic-Module-Name` everywhere (§4) | **Done** 2026-10-03 |
| Stage 1 — build plumbing and spikes (§5) | **Done** 2026-10-03 |
| Stage 2 — descriptors, waves A–I (§6) | Waves A–G **done** (2026-10-03 through 2026-10-05). **Next: Wave H**. Read the Wave A–G outcomes first |
| Stage 3 — documentation pass (§7) | Repository-wide parts done with Wave A (`README.adoc` "Module Path And Classpath" + *Kind* column, `CODE_STYLE.md` "Module Descriptors", `DESIGN_PHILOSOPHY.md`); per-chapter parts continue with each wave |
| Stage 4 — module-path verification (§8) | Not started |

No open decisions. (§5.7 settled 2026-10-03: `aspectjrt` stays optional.)

---

## 1. Why This Lift Exists

Today no artifact declares a module name — not even `Automatic-Module-Name`. A consumer who puts `smallmind-nutsnbolts-7.4.0-SNAPSHOT.jar` on a module path gets an automatic module called `smallmind.nutsnbolts` derived from the filename, which changes if the jar is renamed and which this project has never promised. Any consumer that is itself modular is therefore depending on an accident.

Doing the lift properly buys three things that align with the repository's stated goals:

- **Stable names.** `requires org.smallmind.nutsnbolts;` is a contract this project controls.
- **Enforceable optionality.** Every `<optional>true</optional>` in a POM becomes a `requires static`. The compiler and the runtime then enforce the boundary the documentation currently only describes. See §3.4 for the one behavioral surprise this introduces.
- **Honest surface.** `exports` makes the public package set explicit. Nothing in this repository uses an `internal` package convention, so the initial descriptors export everything; the point is that the list now exists and can be narrowed deliberately later.

What this lift is **not**: it does not introduce `jlink`, `jpackage`, shading, multi-release jars, or any change in behavior for consumers who keep these jars on the classpath. A jar with a `module-info.class` on the classpath behaves exactly as it does today; `provides`/`opens`/`requires static` are simply ignored there.

## 2. What The Survey Found

Everything in this section was verified against the working tree and the local Maven repository (`~/.m2`), not inferred. Re-run the checks in §9 if the tree has moved.

### 2.1 Shape of the repository

- 59 artifact-producing modules: 55 `jar`, 4 `maven-plugin` (`license`, `spark/singularity/mojo`, `spark/tanukisoft/mojo`, `web/schema`). 24 `pom` aggregators.
- `maven.compiler.release` is 25. `maven-compiler-plugin` 3.14.1 compiles `module-info.java` natively; no extra plugin (ModiTect etc.) is needed.
- No `module-info.java` and no `Automatic-Module-Name` anywhere.
- **No split packages among the 59 modules.** 236 package→module pairs, zero packages owned by more than one artifact. This is the one structural condition JPMS flatly refuses and it is already satisfied.
- No jar built with `maven-jar-plugin` configuration anywhere; the plugin is implicit. Stage 0 adds it.

### 2.2 Internal dependency graph

Compile/provided/runtime dependencies between the 59 modules form a DAG with nine layers. Leaf-first depth, used for the wave order in §6:

| Depth | Modules |
|---|---|
| 0 | `nutsnbolts`, `batch/base`, `spark/singularity/boot`, `license`†, `web/schema`† |
| 1 | `ansible`, `scribe/pen`, `memcached/utility`, `mongodb/throng`, `bayeux/oumuamua/server/api`, `file/ephemeral`, `file/jailed`, `mongodb/utility`, `web/http`, `schedule/base`, `sleuth/runner`, `spark/tanukisoft/integration`†, `testbench/foundation`, `testbench/style`, `spark/singularity/mojo`†, `spark/tanukisoft/mojo`† |
| 2 | `web/json/scaffold`, `testbench/docker`, `kafka/utility`, `javafx/extras`, `memcached/cubby`, `schedule/quartz`, `scribe/apache`, `scribe/ink/indigenous`, `scribe/ink/jdk`, `scribe/ink/log4j`, `scribe/slf4j`, `testbench/logger`, `artifact/maven`†, `sleuth/maven/surefire`† |
| 3 | `web/json/doppelganger`, `testbench/condition`, `web/jersey`, `web/jwt` |
| 4 | `claxon/registry`, `testbench/groundwater`, `web/grizzly`, `web/jetty` |
| 5 | `quorum`, `bayeux/oumuamua/server/spi`, `claxon/emitter/{aws,datadog,jmx,message,noop,prometheus}`, `claxon/exotic`, `claxon/http` |
| 6 | `persistence`, `bayeux/oumuamua/server/impl`, `phalanx` |
| 7 | `liquibase`, `web/json/query` |
| 8 | `batch/spring` |

† Stays automatic-only (Stage 0 name, no descriptor) — see §3.7.

`nutsnbolts` has 41 direct dependents and 46 packages; it is the keystone and the first real descriptor.

### 2.3 Resources that live in package-shaped directories

The root POM declares six resource roots (`src/main/resources/{freemarker,liquibase,meta,property,script,spring}`); modules also use `image`, `xslt`, `xsd`, `quartz`. Files under those roots whose relative path looks like a package land in the jar *inside that package*, and under JPMS a resource inside a named module's package is **encapsulated**: `ClassLoader.getResource("org/smallmind/persistence/hibernate.xml")` from another module returns `null` unless the package is `opens` unconditionally. Spring's `classpath:` resolution uses exactly that call.

| Module | Resource | Package | Package has classes? |
|---|---|---|---|
| `nutsnbolts` | `property/…/examples/{database-mysql.properties,global.properties,global.yaml}`, `spring/…/examples/{foundation,jmx,realm,rmi}.xml`, `xsd/…/examples/smallmind-command-1.0.xsd` | `org.smallmind.nutsnbolts.examples` | **No — resource-only** |
| `claxon/registry` | `spring/…/claxon/registry/claxon.xml` | `org.smallmind.claxon.registry` | yes |
| `persistence` | `spring/…/persistence/{hibernate,mongo}.xml` | `org.smallmind.persistence` | yes |
| `liquibase` | `spring/…/liquibase/spring/schema-hibernate.xml` (moved from `persistence` in Wave G) | `org.smallmind.liquibase.spring` | yes |
| `batch/spring` | `liquibase/…/Batch.changelog.xml`, `spring/…/{batch,batch-liquibase}.xml` | `org.smallmind.batch.spring` | yes |
| `javafx/extras` | `image/…/dialog/dialog_*.png` | `org.smallmind.javafx.extras.dialog` | yes |
| `memcached/cubby` | `spring/…/cubby/memcached.xml` | `org.smallmind.memcached.cubby` | yes |
| `phalanx` | `spring/…/wire/{mock-wire,rabbitmq-wire}.xml` | `org.smallmind.phalanx.wire` | yes |
| `schedule/quartz` | `liquibase/…/Quartz.changelog.xml`, `spring/…/{job,quartz,quartz-liquibase}.xml` | `org.smallmind.schedule.quartz` | yes |
| `testbench/foundation` | `property/…/foundation/global.yaml`, `spring/…/foundation/foundation.xml` | `org.smallmind.testbench.foundation` | **No — the module has zero Java sources** |
| `testbench/logger` | `spring/…/logger/logging.xml` | `org.smallmind.testbench.logger` | yes |
| `testbench/style` | `xslt/…/style/pretty-print.xslt` | `org.smallmind.testbench.style` | yes |
| `web/grizzly` | `spring/…/grizzly/grizzly.xml` | `org.smallmind.web.grizzly` | yes |

Resources at a jar root or under `META-INF/` (`quartz/tables_*.sql`, `meta/commons-logging.properties`, service files, `plexus/components.xml`) are **not** encapsulated and need nothing.

No resource package is owned by two modules and no resource-only package collides with a class package elsewhere, so there are no resource-induced split packages.

Consumer-facing documentation references these paths (`classpath:org/smallmind/persistence/hibernate.xml`, `classpath:org/smallmind/batch/spring/batch.xml`, `classpath:org/smallmind/testbench/foundation/global.yaml`, …). Moving the files would be an API break; §3.8 keeps them where they are and opens the packages instead.

### 2.4 Service providers and consumers

| Module | Role | Service | Implementation |
|---|---|---|---|
| `file/ephemeral` | provides | `java.nio.file.spi.FileSystemProvider` | `org.smallmind.file.ephemeral.EphemeralFileSystemProvider` |
| `file/jailed` | provides | `java.nio.file.spi.FileSystemProvider` | `org.smallmind.file.jailed.JailedFileSystemProvider` |
| `scribe/ink/indigenous` | provides | `org.smallmind.scribe.pen.adapter.LoggingBlueprint` | `IndigenousLoggingBlueprint` |
| `scribe/ink/jdk` | provides | `…LoggingBlueprint` | `JDKLoggingBlueprint` |
| `scribe/ink/log4j` | provides | `…LoggingBlueprint` | `Log4JLoggingBlueprint` |
| `scribe/slf4j` | provides | `org.slf4j.spi.SLF4JServiceProvider` | `ScribeSLF4JServiceProvider` |
| `sleuth/maven/surefire` | provides | `org.apache.maven.surefire.api.provider.SurefireProvider` | `SleuthProvider` (stays automatic-only; file suffices) |
| `web/json/doppelganger` | provides | `javax.annotation.processing.Processor` | `DoppelgangerAnnotationProcessor` — the services file is **generated** by `@AutoService`, so it never appears in `src/` |
| `scribe/pen` | **uses** | `org.smallmind.scribe.pen.adapter.LoggingBlueprint` | `LoggingBlueprintFactory` (`ServiceLoader.load(LoggingBlueprint.class, TCCL)`) |
| `scribe/pen` (test) | provides | `…LoggingBlueprint` | `RecordingLoggingBlueprint` via `src/test/resources/meta/META-INF/services/…` — this is why tests stay on the classpath, §3.6 |
| `liquibase` | documented consumer-side provider | `liquibase.logging.LogService` | `ScribeLogService` — the module deliberately ships no services file (`LIQUIBASE.adoc`, "How Liquibase discovers the bridge"); §3.5 explains why JPMS needs an extra paragraph there |

### 2.5 Reflection consumers (what needs `opens`)

| Framework | Where | Target module for `opens … to` |
|---|---|---|
| Spring XML bean definitions | every module in §2.3 with a `spring/` resource, plus `nutsnbolts.spring.*`, `nutsnbolts.security.spring`, `nutsnbolts.namespace.shiro.realm.spring`, `nutsnbolts.resource`, `scribe.pen.spring.*`, `scribe.pen`, `persistence.orm.spring.*`, `persistence.sql.pool.spring`, `persistence.cache.memcached.spring`, `web.jersey.spring`, `web.jersey.proxy.spring`, `web.jersey.cors.spring`, `claxon.registry.*`, `phalanx.wire.spring`, `phalanx.wire.transport.amqp.rabbitmq.spring` | `spring.core`, `spring.beans` (and `spring.context` where `@Configuration`/lifecycle callbacks are used). These are automatic module names declared by Spring itself and are stable. |
| JAXB (`@XmlRootElement` etc.) | `web/json/scaffold` (6 files), `web/json/query` (25), `phalanx.wire.signal` (5), `nutsnbolts.json` (1), `file/jailed` (1), `web.jersey.json`/`web.jersey.proxy` (1), `claxon/registry` (via scaffold types) | `jakarta.xml.bind`. The API module delegates `addOpens` to the implementation (`org.glassfish.jaxb.runtime`), so opening to the API module is sufficient. |
| Jackson 3 (`tools.jackson.databind`) on the same JAXB types | same packages as above where Jackson serializes them | `tools.jackson.databind` |
| JPA / Hibernate ORM | `persistence.orm.jpa`, `persistence.orm.hibernate`, entity-bearing packages (3 files) | `org.hibernate.orm.core` (automatic name declared by Hibernate). Consumers' entity packages must be opened by the consumer. |
| Throng (this project's MongoDB mapper) | `mongodb/throng` reads consumer classes annotated with *its own* `@Entity`/`@Embedded`/`@Id` | consumers open entity packages **to `org.smallmind.mongodb.throng` and `org.smallmind.nutsnbolts`** (corrected in Wave B and verified on a module path: field access goes through `nutsnbolts`' `FieldUtility`) — documented in `MONGODB.adoc` |
| `nutsnbolts.reflection` (bean utilities, `setAccessible`) | reflects over caller-supplied classes | consumers open the relevant packages **to `org.smallmind.nutsnbolts`** — document in `NUTSNBOLTS.adoc` |
| Quartz | instantiates job classes | consumers open job packages to `org.quartz` |
| Jersey / HK2 | `web/jersey`, `web/grizzly`, `web/jetty`, `claxon/http` resource and provider classes | HK2 instantiates resources reflectively; on a module path resource packages typically need `opens … to org.glassfish.hk2.locator, org.glassfish.hk2.utilities` or must be exported. **Resolved by the §5.5 spike:** SmallMind needs `opens` only for `web.jersey.aop` and `web.jersey.page`; consumers open resource packages to `org.glassfish.hk2.locator, org.glassfish.hk2.utilities, org.glassfish.jersey.core.server`. |
| JMX / `MBeanServer` | `quorum.pool.complex.jmx`, `phalanx.wire.jmx`, `claxon/emitter/jmx`, `claxon/exotic`, `javafx/extras`, `nutsnbolts.spring.jmx` | Standard MBeans need the interface and implementation accessible to `java.management`; exporting the package is sufficient for public types. |

### 2.6 Compile-time AspectJ weaving

`dev.aspectj:aspectj-maven-plugin` runs `compile` at `process-classes` and `test-compile` at `process-test-classes`, i.e. **after** javac. It recompiles the module's sources with `ajc` into `target/classes`. It is applied in:

- `nutsnbolts` (`inject.LazyFieldAspect`), `scribe/pen` (no aspects of its own), `persistence` (7 aspects in `cache.aop` and `orm.aop`), `web/jersey` (`aop.ResourceMethodAspect`, `aop.ValidatedAspect`), `sleuth/runner` (none of its own), and — via the `claxon/pom.xml` `<build><plugins>` block, which is inherited — **every** `claxon/*` module: `registry` (`aop.InstrumentedAspect`), `exotic`, `http`, and all six `emitter/*`.

Two consequences:

1. `ajc` will see `module-info.java` in the source tree. Whether AspectJ 1.9.25.1 compiles it correctly, ignores it, or chokes is the single largest technical unknown in this lift. §5.3 is the spike.
2. Woven classes reference `org.aspectj.runtime` (the `Automatic-Module-Name` of `aspectjrt`). The root POM declares `aspectjrt` as `compile` + `optional=true` for *every* module, so the matching clause is `requires static org.aspectj.runtime;` in woven modules. That mirrors the POM; whether the POM is right to call it optional for woven code is a pre-existing question, flagged in §5.7.

### 2.7 Annotation processors

`<annotationProcessorPaths>` + `<proc>full</proc>` appear in `web/json/doppelganger` (`auto-service`), `claxon/registry` and `bayeux/oumuamua/server/impl` (`web-json-doppelganger` as processor), `persistence` (`querydsl-apt` + `jakarta.persistence-api`), and `web/json/query` (verify which processor when you get there). Processors on `annotationProcessorPaths` are loaded from a classpath-style processor path regardless of whether the compiled module has a descriptor; `module-info.java` does not disturb them. `persistence` additionally runs `hibernate-enhance-maven-plugin`, which rewrites entity bytecode in place and is indifferent to `module-info.class`.

### 2.8 Third-party dependencies

All compile/provided/runtime third-party jars were inspected in `~/.m2`:

- **54 explicit modules** (ship `module-info.class`): all `jakarta.*` APIs, Jackson 3 (`tools.jackson.core`, `tools.jackson.databind`, `tools.jackson.module.afterburner`, `tools.jackson.module.jakarta.xmlbind`), Jackson 2 annotations (`com.fasterxml.jackson.annotation`), Jetty 12 (`org.eclipse.jetty.*`), Jersey 4 (`org.glassfish.jersey.*`), Grizzly 5 (`org.glassfish.grizzly*`), Tyrus 2 (`org.glassfish.tyrus.*`), Log4j 2 (`org.apache.logging.log4j`, `.core`), Hibernate Validator (`org.hibernate.validator`), ASM (`org.objectweb.asm`, `.util`), Bouncy Castle (`org.bouncycastle.provider`, `org.bouncycastle.pkix`), SLF4J 2 (`org.slf4j`), SnakeYAML (`org.yaml.snakeyaml`), `commons-logging` 1.3 (`org.apache.commons.logging`), `commons-net` (`org.apache.commons.net` — verify), `jfxtras-all` (single descriptor oddly named `jfxtras.icalendaragenda`), OpenJFX platform jars (`javafx.base`, `javafx.controls`, `javafx.graphics` — the classifier-less jars in the POM are empty stubs that pull the platform jars transitively).
- **44 automatic modules** (manifest `Automatic-Module-Name`): Spring 7 (`spring.core`, `spring.beans`, `spring.context`, `spring.web`, `spring.orm`, `spring.jcl`), `spring.batch.core`, Hibernate ORM (`org.hibernate.orm.core`), MongoDB (`org.mongodb.bson`, `org.mongodb.driver.core`, `org.mongodb.driver.sync.client`), QueryDSL (`com.querydsl.core`, `com.querydsl.jpa`), Liquibase (`liquibase.core`), Quartz (`org.quartz`), AWS SDK v2 (`software.amazon.awssdk.*`), docker-java (`com.github.dockerjava`, `.transport.httpclient5`), `com.rabbitmq.client`, HttpComponents 5 (`org.apache.httpcomponents.*`), `org.jose4j`, `freemarker`, `org.apache.shiro.spring`, `io.whitfin.siphash`, `org.testng`, Maven Resolver (`org.apache.maven.resolver.*`), `org.aspectj.runtime`, `com.google.auto.service` (annotations).
- **27 unnamed jars** (name derived from filename at runtime). All of them derive successfully with `jar --describe-module` on JDK 25, so none is barred from a module path, but every one of these names is fragile across upgrades: `kafka.clients`, `HdrHistogram`, `syslog4j`, `jackson.dataformat.msgpack`, `java.dogstatsd.client`, `wrapper` (Tanuki), `grizzly.http.server.jaxws`, `auto.service`, `querydsl.apt`, `maven.*` (all Maven core jars), `maven.plugin.annotations`, `surefire.api`, and test-only jars.
- **Split packages among third-party jars** that could matter to a consumer who assembles a module path: `jakarta.mail-api` vs `com.sun.mail:jakarta.mail` (test-only here), `commons-logging` vs `spring-jcl` (`scribe/apache` requires the former; `persistence` optionally declared the latter until Wave G replaced it with `commons-logging`, which Spring 7 itself depends on), `grizzly-http-servlet-server` vs the individual Grizzly jars (test-only in `bayeux/…/impl`), and the Maven core jars among themselves (plugin modules only). None affects the descriptors in this repository; all belong in the troubleshooting sections of the affected chapters.
- **Module-path blockers in the web stack, found in Stage 1 (§5.5).** Checked against each module's *runtime* dependency set: `web/grizzly` cannot form a module path because `org.glassfish.metro:webservices-api-osgi` duplicates `jakarta.xml.ws` (10 packages) and `jakarta.xml.soap` from the API jars; it arrives through the compile dependency `grizzly-http-server-jaxws` → `webservices-osgi`. `web/jersey`, `web/jetty`, and `web/grizzly` ship no Jersey injection manager (`jersey-hk2` is test-scoped in `web/jetty` and `web/grizzly`, and arrives in `web/jersey` only through its optional `jersey-spring6`), so every working deployment adds `jersey-hk2`, which brings HK2's `aopalliance-repackaged` (duplicates `org.aopalliance.*` from `spring-aop`); `jersey-spring6` additionally brings HK2 `class-model`'s `asm-commons` without `asm-tree`. Excluding `aopalliance-repackaged` clears the conflict (verified in §5.5). `web/jetty`, `claxon/http`, `phalanx`, and `bayeux/oumuamua/server/impl` are clean. (`grizzly-npn-api`/`grizzly-npn-bootstrap`, which also split a package and patch `sun.security.ssl`, are test-scoped in `web/grizzly` and never reach consumers.) **Update (2026-10-05):** the `web/grizzly` row is cleared; see the Wave E follow-up.

## 3. Decisions That Govern Every Descriptor

Settle these once, here, so the 50-odd descriptors are consistent.

### 3.1 Module names

`org.smallmind.` + the module's directory path with `/` → `.`. This coincides with the root package for almost every module and is unique by construction. Exceptions worth noting (name ≠ exact root package, which is allowed — module names and package names are separate namespaces): `liquibase` (root package `org.smallmind.liquibase.spring`, module `org.smallmind.liquibase`), `claxon/exotic` (`…exotic.jvm` / `org.smallmind.claxon.exotic`), `web/http` (`…web.http.apache` / `org.smallmind.web.http`), `web/json/query` (packages under `org.smallmind.web.json.*` / `org.smallmind.web.json.query`), the four Maven plugins and the two `mojo` directories (`…singularity.mojo`, `…tanukisoft.mojo`). The full table is in §4.2.

### 3.2 `exports`

Export every package that contains classes. This repository has no `internal`/`impl` convention and its documentation treats every public type as public API. Narrowing exports is a separate, later decision; do not combine it with this lift.

Do **not** export resource-only packages (`org.smallmind.nutsnbolts.examples`, `org.smallmind.testbench.foundation`); `opens` them instead (§3.8).

### 3.3 `requires`, `requires transitive`

- `requires` the module that **owns each package you import**, not merely the artifacts listed in the POM. Transitive Maven dependencies are frequently the real owner (`spring-core` behind `spring-beans`, `shiro-core` behind `shiro-spring`, `jackson-annotations` behind `jackson-databind`). `jdeps` reports the true set (§5.4). When a descriptor needs a module whose artifact is not declared in the module's POM, **add the POM dependency too** — the descriptor and the POM should agree.
- `requires transitive X` when a type from `X` appears in the signature of an exported public type. Enable `-Xlint:exports` (§5.2); it flags exactly this. Prefer `requires transitive` only for explicit modules (Jakarta APIs, Jackson, Jersey). For automatic modules (Spring, Hibernate, MongoDB) do **not** use `transitive` — javac warns (`requires-transitive-automatic`) because the name is not guaranteed stable — and instead state in the chapter that consumers must `requires` it themselves.
- JDK modules beyond `java.base`, by module (from main-source imports): `nutsnbolts` → `java.compiler`, `java.management`, `java.naming`, `java.rmi`, `java.xml`; `web/json/doppelganger` → `java.compiler`; `quorum`, `phalanx` → `java.management`, `java.naming`; `persistence` → `java.sql`, `java.logging`, `java.transaction.xa`; `liquibase` → `java.sql`, `java.logging`; `batch/spring` → `java.sql`; `claxon/emitter/jmx`, `claxon/exotic`, `javafx/extras`, `schedule/quartz` → `java.management` (`claxon/exotic` likely also `jdk.management` for `com.sun.management` — verify); `scribe/ink/jdk` → `java.logging`; `testbench/style` → `java.xml`, `jdk.jdi`; `web/jetty` → `jdk.httpserver` (`JettyInitializingBean` imports `com.sun.net.httpserver.HttpContext`); `web/schema` → `java.xml` (automatic-only anyway). Re-derive with `jdeps`; this list is from import scanning.

### 3.4 `requires static` ≡ `<optional>true</optional>`

Every optional POM dependency becomes `requires static`. Fifteen POMs declare optional dependencies; `nutsnbolts` alone has 19 and `persistence` 14 (including optional *internal* modules: `claxon-registry`, `memcached-utility`, `mongodb-throng`, `smallmind-quorum`, `testbench-condition`).

**Correction from Wave A (verified):** resolving *any* automatic module resolves *every* automatic module on the module path, so the Hibernate example below only happens when nothing in the graph resolves an automatic module. In practice the surprise hits **explicit** optional modules (Jakarta APIs, Bouncy Castle, ASM, SnakeYAML, …), which nothing pulls in. `README.adoc` "Module Path And Classpath" states it that way; chapters link there rather than repeating it.

**The behavioral surprise, which every affected chapter must state:** a `requires static` dependency is resolved at compile time but **not** at run time unless something else resolves it. If a consumer puts `smallmind-persistence` and `hibernate-core` on the module path but nothing `requires org.hibernate.orm.core` non-statically, Hibernate is simply not in the module graph and `persistence.orm.hibernate` fails with `NoClassDefFoundError` — even though the jar is right there. Consumers must either `requires` the optional module themselves (the normal case: they are using Hibernate directly) or add `--add-modules org.hibernate.orm.core`. On the classpath nothing changes. This is the same failure mode as today's "optional dependency missing", but it now triggers when the jar is *present and unrequired*, which will surprise people.

Also: a named module **cannot read the classpath**. If a consumer puts `org.smallmind.nutsnbolts` on the module path but leaves Spring on the classpath, `nutsnbolts.spring` cannot see Spring at all. The guidance is "all on the module path, or all on the classpath"; automatic modules are the bridge that makes the former workable.

### 3.5 Services

- Keep the `META-INF/services` files **and** add `provides … with …`. On the module path only `provides` counts; on the classpath only the file counts. Both are needed for the dual-path story the chapters already tell.
- `scribe/pen` gets `uses org.smallmind.scribe.pen.adapter.LoggingBlueprint;`. Without it `ServiceLoader.load` throws `ServiceConfigurationError` rather than returning nothing. The existing `ServiceLoader.load(Class, TCCL)` call does not need to change.
- `web/json/doppelganger` gets `provides javax.annotation.processing.Processor with org.smallmind.web.json.doppelganger.DoppelgangerAnnotationProcessor;` alongside the `@AutoService`-generated file.
- The Liquibase bridge pattern — "the module ships `ScribeLogService`, the consumer ships the services file" — **does not translate** to a modular consumer: `provides X with Y` requires `Y` to be declared in the same module as the descriptor. `LIQUIBASE.adoc` gains a short paragraph: a modular consumer writes `public class AppLogService extends ScribeLogService {}` in its own module and declares `provides liquibase.logging.LogService with AppLogService;`. `ScribeLogService` is a non-final public class with the needed no-arg constructor, so this works as-is. Do **not** add `provides` to `smallmind-liquibase`; the chapter already explains why forcing the bridge on every consumer is wrong.

### 3.6 Tests stay on the classpath

Set `<useModulePath>false</useModulePath>` on `maven-surefire-plugin` in the root `pluginManagement`. Reasons, all verified:

- 869 test sources live in the same packages as main sources; surefire's `--patch-module` handling works but adds a second axis of failure to every module.
- `scribe/pen`'s `LoggingBlueprintFactoryTest` depends on a **test-resource** services file. Patched into a named module, that file is not consulted and the test finds no provider.
- Tests use `com.sun.net.httpserver` (`jdk.httpserver`) in `nutsnbolts`, `web/http`, `web/jersey` — fine on the classpath, one more `--add-modules` on the module path.
- The surefire `argLine` already injects the Mockito agent; Mockito's inline mock maker needs `--add-opens` on a module path.
- Test-scope dependencies include jars that split packages with main-scope ones (`com.sun.mail:jakarta.mail`, `grizzly-http-servlet-server`).

**Test compilation is not on the classpath.** `useModulePath=false` only controls how surefire *runs* tests. Once `src/main/java/module-info.java` exists, `maven-compiler-plugin`'s `testCompile` compiles the tests patched into the named module (`--patch-module`, with test-scoped jars added via `--add-reads …=ALL-UNNAMED`). Anything a test imports from a JDK module the main descriptor does not require is then invisible — verified in the §5.3 spike, where `nutsnbolts`'s `HttpTransmitterTest` failed with `package com.sun.net.httpserver is not visible`. The fix, per affected module (`nutsnbolts`, `web/http`, `web/jersey`; re-check per wave), is test-only compiler arguments on the `default-testCompile` execution:

```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-compiler-plugin</artifactId>
  <executions>
    <execution>
      <id>default-testCompile</id>
      <configuration>
        <compilerArgs combine.children="append">
          <arg>--add-modules</arg>
          <arg>jdk.httpserver</arg>
          <arg>--add-reads</arg>
          <arg>org.smallmind.nutsnbolts=jdk.httpserver</arg>
        </compilerArgs>
      </configuration>
    </execution>
  </executions>
</plugin>
```

With that in place, `nutsnbolts` compiled, wove, and passed all 1021 tests on the classpath with a descriptor present. Add these blocks in the same commit as the module's descriptor (Stage 2), not before — without a descriptor there is no named module to add reads to.

The shipped jars are modular; the tests exercise them as classpath consumers, which is also the dominant real-world consumer. Module-path verification is done once, explicitly, in §8 — not in every module's unit tests.

### 3.7 Modules that stay automatic-only

These get a Stage 0 `Automatic-Module-Name` and **never** a descriptor:

- The four `maven-plugin` artifacts (`license`, `spark/singularity/mojo`, `spark/tanukisoft/mojo`, `web/schema`). Maven loads plugins through Plexus class realms on a classpath; a descriptor gains nothing and the Maven core jars they compile against split packages among themselves (§2.8).
- `artifact/maven` and `sleuth/maven/surefire`: same reason — they consume Maven/Surefire internals that are unnamed jars with split packages.
- `spark/tanukisoft/integration` — **demoted in Wave B (2026-10-04).** Its only external dependency is Tanuki `wrapper`, an unnamed jar whose derived name `wrapper` is as fragile as names get, and its unit tests shadow `org.tanukisoftware.wrapper.WrapperManager`/`WrapperListener` with test doubles in that same package. With a descriptor, test compilation runs patched into the named module and fails with `package exists in another module: wrapper`. Keeping it automatic avoids both problems; `SPARK.adoc` documents the derived-name rule.

### 3.8 Resource-only packages and encapsulated resources

- Packages that contain resources consumers load by `classpath:` path (§2.3) get an **unconditional** `opens`. Spring would need the open anyway, and relocating the files would break documented paths.
- **Correction from Wave A (verified):** javac does *not* refuse `opens` of a package without classes; it emits the lint warning `[opens] package is empty or does not exist` and writes the clause anyway, and a `package-info.java` does not change that (javac does not count it, even with `-Xpkginfo:always`). At run time the package exists because the jar contains its resource files, and the open works. The warning is expected and documented in the chapter. (`exports` of such a package *is* an error, which is why §3.2 only opens them.) The original text follows: javac refuses `opens`/`exports` of a package that contains no compiled class ("package is empty or does not exist"). The two legitimate resource-only packages therefore each gain a `package-info.java` (with the standard copyright prologue and a one-line Javadoc saying the package exists to carry resources). Affected: `org.smallmind.nutsnbolts.examples`, `org.smallmind.testbench.foundation`.
- `testbench/style`'s `pretty-print.xslt` and `javafx/extras`'s `dialog_*.png` are loaded from inside their own module (check: if via `Class.getResource` from a class in the same package, no `opens` is needed; if via `ClassLoader.getResource`, it is).

### 3.9 AspectJ and `module-info.java`

Default plan: exclude `module-info.java` from `ajc`'s sources so javac remains the sole producer of `module-info.class`:

```xml
<!-- root pom.xml, pluginManagement, dev.aspectj:aspectj-maven-plugin <configuration> -->
<excludes>
  <exclude>**/module-info.java</exclude>
</excludes>
```

`ajc` then compiles the remaining sources against the classpath (it does not need to understand the module graph), and the `module-info.class` javac produced earlier in the same `target/classes` stays in place. The spike in §5.3 must confirm (a) the plugin version honors `<excludes>`, (b) `ajc` does not clear `target/classes` before writing, (c) weaving still happens (`showWeaveInfo` is already on), (d) the resulting jar still describes as a named module. Fallbacks, in order: let `ajc` compile `module-info.java` itself if the spike shows it simply works; switch the plugin to post-compile binary weaving (`<weaveDirectories>` on `target/classes`, no sources); as a last resort leave woven modules automatic-only.

### 3.10 Keep `Automatic-Module-Name` after the descriptor lands

Harmless redundancy (`module-info.class` wins), and it survives tooling that strips descriptors (shading, some repackagers). Also keeps the Stage 0 table in §4.2 true forever.

## 4. Stage 0 — A Stable Name For Every Artifact

Cheap, zero risk, independently shippable. Do this first and release it.

**Status: done** (2026-10-03). `maven-jar-plugin` 3.5.1 is pinned and configured in the root POM, all 59 artifact POMs set `jigsaw.module.name`, every jar verified with §4.3, and the §4.4 documentation is in place (`README.adoc` "Module Names" table plus one module-name mention per artifact in each chapter's installation or artifact-selection material). The §2.1 statements about no `Automatic-Module-Name` and no `maven-jar-plugin` configuration describe the tree before this stage.

### 4.1 Build change

Root `pom.xml`:

1. Add `<maven.jar.plugin.version>` to `<properties>` (pin a current 3.x release; check Maven Central — 3.5.1 was used).
2. In `<pluginManagement>`, add:

```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-jar-plugin</artifactId>
  <version>${maven.jar.plugin.version}</version>
  <configuration>
    <archive>
      <manifestEntries>
        <Automatic-Module-Name>${jigsaw.module.name}</Automatic-Module-Name>
      </manifestEntries>
    </archive>
  </configuration>
</plugin>
```

3. In the root `<build><plugins>` list (next to `maven-source-plugin`), reference `maven-jar-plugin` so the configuration applies everywhere. `maven-plugin` packaging also uses `jar:jar`, so the four plugins are covered.

Each of the 59 artifact POMs: add `<properties><jigsaw.module.name>…</jigsaw.module.name></properties>` with the value from §4.2. Aggregator POMs do not set it. An unset property would be written literally as `${jigsaw.module.name}` into the manifest, so §4.3 verifies every jar.

### 4.2 Names

| Path | artifactId | Module name | Descriptor? |
|---|---|---|---|
| `nutsnbolts` | `smallmind-nutsnbolts` | `org.smallmind.nutsnbolts` | yes (wave A) |
| `batch/base` | `batch-base` | `org.smallmind.batch.base` | yes (A) |
| `spark/singularity/boot` | `spark-singularity-boot` | `org.smallmind.spark.singularity.boot` | yes (A) |
| `license` | `smallmind-license-maven-plugin` | `org.smallmind.license` | **no** |
| `web/schema` | `schema-validation-maven-plugin` | `org.smallmind.web.schema` | **no** |
| `ansible` | `smallmind-ansible` | `org.smallmind.ansible` | yes (B) |
| `scribe/pen` | `scribe-pen` | `org.smallmind.scribe.pen` | yes (B) |
| `memcached/utility` | `memcached-utility` | `org.smallmind.memcached.utility` | yes (B) |
| `mongodb/throng` | `mongodb-throng` | `org.smallmind.mongodb.throng` | yes (B) |
| `bayeux/oumuamua/server/api` | `oumuamua-server-api` | `org.smallmind.bayeux.oumuamua.server.api` | yes (B) |
| `file/ephemeral` | `file-ephemeral` | `org.smallmind.file.ephemeral` | yes (B) |
| `file/jailed` | `file-jailed` | `org.smallmind.file.jailed` | yes (B) |
| `mongodb/utility` | `mongodb-utility` | `org.smallmind.mongodb.utility` | yes (B) |
| `web/http` | `web-http` | `org.smallmind.web.http` | yes (B) |
| `schedule/base` | `schedule-base` | `org.smallmind.schedule.base` | yes (B) |
| `sleuth/runner` | `sleuth-runner` | `org.smallmind.sleuth.runner` | yes (B) |
| `spark/tanukisoft/integration` | `spark-tanukisoft-integration` | `org.smallmind.spark.tanukisoft.integration` | **no** (demoted in B, §3.7) |
| `testbench/foundation` | `testbench-foundation` | `org.smallmind.testbench.foundation` | yes (B), resources only |
| `testbench/style` | `testbench-style` | `org.smallmind.testbench.style` | yes (B) |
| `spark/singularity/mojo` | `spark-singularity-maven-plugin` | `org.smallmind.spark.singularity.mojo` | **no** |
| `spark/tanukisoft/mojo` | `spark-tanukisoft-maven-plugin` | `org.smallmind.spark.tanukisoft.mojo` | **no** |
| `web/json/scaffold` | `web-json-scaffold` | `org.smallmind.web.json.scaffold` | yes (C) |
| `testbench/docker` | `testbench-docker` | `org.smallmind.testbench.docker` | yes (C) |
| `kafka/utility` | `kafka-utility` | `org.smallmind.kafka.utility` | yes (C) |
| `javafx/extras` | `smallmind-javafx-extras` | `org.smallmind.javafx.extras` | yes (C) |
| `memcached/cubby` | `memcached-cubby` | `org.smallmind.memcached.cubby` | yes (C) |
| `schedule/quartz` | `schedule-quartz` | `org.smallmind.schedule.quartz` | yes (C) |
| `scribe/apache` | `scribe-apache` | `org.smallmind.scribe.apache` | yes (C) |
| `scribe/ink/indigenous` | `scribe-ink-indigenous` | `org.smallmind.scribe.ink.indigenous` | yes (C) |
| `scribe/ink/jdk` | `scribe-ink-jdk` | `org.smallmind.scribe.ink.jdk` | yes (C) |
| `scribe/ink/log4j` | `scribe-ink-log4j` | `org.smallmind.scribe.ink.log4j` | yes (C) |
| `scribe/slf4j` | `scribe-slf4j` | `org.smallmind.scribe.slf4j` | yes (C) |
| `testbench/logger` | `testbench-logger` | `org.smallmind.testbench.logger` | yes (C) |
| `artifact/maven` | `artifact-maven` | `org.smallmind.artifact.maven` | **no** |
| `sleuth/maven/surefire` | `surefire-sleuth-provider` | `org.smallmind.sleuth.maven.surefire` | **no** |
| `web/json/doppelganger` | `web-json-doppelganger` | `org.smallmind.web.json.doppelganger` | yes (D) |
| `testbench/condition` | `testbench-condition` | `org.smallmind.testbench.condition` | yes (D) |
| `web/jersey` | `web-jersey` | `org.smallmind.web.jersey` | yes (D) |
| `web/jwt` | `web-jwt` | `org.smallmind.web.jwt` | yes (D) |
| `claxon/registry` | `claxon-registry` | `org.smallmind.claxon.registry` | yes (E) |
| `testbench/groundwater` | `testbench-groundwater` | `org.smallmind.testbench.groundwater` | yes (E) |
| `web/grizzly` | `web-grizzly` | `org.smallmind.web.grizzly` | yes (E) |
| `web/jetty` | `web-jetty` | `org.smallmind.web.jetty` | yes (E) |
| `quorum` | `smallmind-quorum` | `org.smallmind.quorum` | yes (F) |
| `bayeux/oumuamua/server/spi` | `oumuamua-server-spi` | `org.smallmind.bayeux.oumuamua.server.spi` | yes (F) |
| `claxon/emitter/aws` | `claxon-emitter-aws` | `org.smallmind.claxon.emitter.aws` | yes (F) |
| `claxon/emitter/datadog` | `claxon-emitter-datadog` | `org.smallmind.claxon.emitter.datadog` | yes (F) |
| `claxon/emitter/jmx` | `claxon-emitter-jmx` | `org.smallmind.claxon.emitter.jmx` | yes (F) |
| `claxon/emitter/message` | `claxon-emitter-message` | `org.smallmind.claxon.emitter.message` | yes (F) |
| `claxon/emitter/noop` | `claxon-emitter-noop` | `org.smallmind.claxon.emitter.noop` | yes (F) |
| `claxon/emitter/prometheus` | `claxon-emitter-prometheus` | `org.smallmind.claxon.emitter.prometheus` | yes (F) |
| `claxon/exotic` | `claxon-exotic` | `org.smallmind.claxon.exotic` | yes (F) |
| `claxon/http` | `claxon-http` | `org.smallmind.claxon.http` | yes (F) |
| `persistence` | `smallmind-persistence` | `org.smallmind.persistence` | yes (G) |
| `bayeux/oumuamua/server/impl` | `oumuamua-server-impl` | `org.smallmind.bayeux.oumuamua.server.impl` | yes (G) |
| `phalanx` | `smallmind-phalanx` | `org.smallmind.phalanx` | yes (G) |
| `liquibase` | `smallmind-liquibase` | `org.smallmind.liquibase` | yes (H) |
| `web/json/query` | `web-json-query` | `org.smallmind.web.json.query` | yes (H) |
| `batch/spring` | `batch-spring` | `org.smallmind.batch.spring` | yes (I) |

### 4.3 Verification

```sh
mvn -q -DskipTests install
for j in $(find . -path '*/target/*-7.4.0-SNAPSHOT.jar' -not -name '*-sources.jar' -not -name '*-tests.jar' -not -name '*-javadoc.jar'); do
  printf '%-70s ' "$j"; jar --describe-module --file "$j" | tr -d '\r' | grep -m1 '@'
done
```

For a jar without a descriptor, `jar --describe-module` prints a "No module descriptor found. Derived automatic module." header and a blank line before the name, so filter for the `@` line rather than taking the first line. The version filter skips stale jars left in `target/` by retired modules. Every line must show `<name>@7.4.0-SNAPSHOT automatic`; a literal `${jigsaw.module.name}` or a filename-derived name means a POM was missed.

### 4.4 Documentation

- `README.adoc` module index: add the module name beside each artifact.
- Each chapter's artifact-selection section gains one line: "Module name (for `requires`): `org.smallmind.…`". Follow `ASCIIDOC_DOC_AUTHORING_GUIDE.md` for placement; do not add a new top-level section just for this.

## 5. Stage 1 — Build Plumbing And Spikes (no descriptors committed)

**Status: done** (2026-10-03). Committed: §5.1 `useModulePath=false`, §5.2 `-Xlint:exports,module`, the §3.9 AspectJ `<excludes>`, and the §5.6 `package-info.java` files; a full `clean install` with tests was green (the one failure seen, `AsyncOumuamuaServletIntegrationTest`, was a pre-existing servlet bug since fixed separately). Spike outcomes are recorded in §5.3–§5.5 and fed into §2.8, §3.6, and the Wave D–F rows. No descriptor was committed.

### 5.1 Surefire

**Done (2026-10-03).**

Root `pluginManagement`, `maven-surefire-plugin` `<configuration>`: add `<useModulePath>false</useModulePath>` next to the existing `useSystemClassLoader=false`. Run the full test suite; nothing should change (no descriptors exist yet). This isolates any surefire surprises from the descriptor work.

### 5.2 Compiler lint

**Done (2026-10-03).** No lint output yet, as expected without descriptors.

Root `maven-compiler-plugin` `<configuration>`: add

```xml
<compilerArgs>
  <arg>-Xlint:exports,module</arg>
</compilerArgs>
```

No `-Werror`. `exports` flags exported signatures that leak types from non-transitive dependencies (the `requires transitive` question, §3.3). `module` flags descriptor-level issues. `requires-transitive-automatic` is on by default and should stay on — it is the guard rail for §3.3. Expect zero output until descriptors appear.

### 5.3 AspectJ spike (do not merge the descriptor)

On a scratch branch, in `nutsnbolts`:

1. Add a minimal `module-info.java` (`module org.smallmind.nutsnbolts { }` — no clauses; javac will complain about nothing because nothing is exported yet).
2. Build with `mvn -pl nutsnbolts -am -DskipTests install` **without** any AspectJ change. Record whether `ajc` fails, warns, or silently succeeds, and whether `target/classes/module-info.class` is present afterwards.
3. Add the `<excludes>` from §3.9 to the root `pluginManagement` and repeat. Confirm `module-info.class` is present, `jar --describe-module` reports `org.smallmind.nutsnbolts` as a non-automatic module, and the weave log still shows `LazyFieldAspect` being applied.
4. Repeat step 3 in `claxon/registry` (inherited plugin block + annotation processor + `InstrumentedAspect`) to cover the inherited-configuration path.

Decide the fallback (§3.9) from the results. Record the outcome in this file.

**Outcome (2026-10-03): the §3.9 default plan works; the `<excludes>` is committed in the root POM.**

- Step 1 as written cannot work: an empty descriptor makes javac compile `nutsnbolts` as a named module that reads only `java.base`, so every third-party import fails. The spike used the `jdeps` descriptor (§5.4) instead.
- Step 2 (no AspectJ change): javac compiles `module-info.java` and writes `module-info.class`; then `ajc` recompiles `module-info.java` itself against a classpath and fails with `<module> cannot be resolved to a module` for every `requires`. Build fails.
- Step 3 (`<excludes>`): `ajc` succeeds and leaves javac's `module-info.class` in place; the jar describes as `org.smallmind.nutsnbolts@7.4.0-SNAPSHOT jar:file:…!/module-info.class` (named, not automatic). `LazyFieldAspect` reports `adviceDidNotMatch` — identical to the build without a descriptor, because `nutsnbolts` has no `@LazyField` methods of its own; `ajc` still compiles it as an aspect. Test compilation then needed the §3.6 test-only arguments; after that, 1021/1021 tests passed.
- Step 4 (`claxon/registry`): with a `jdeps` descriptor (plus the fix below), `ajc` main and test passes succeed, the weave log shows `InstrumentedAspect` advising four join points, the jar is a named module, and 209/209 tests pass. The inherited `claxon/pom.xml` plugin block picks up the root `<excludes>` without change.
- Side finding: code generated by the Doppelganger processor imports `jakarta.annotation.Generated`, which has source retention, so `jdeps` does not report it — but javac does need it. Every module that runs Doppelganger (`claxon/registry`, `bayeux/oumuamua/server/impl`, and any consumer) needs `requires static jakarta.annotation;`.

### 5.4 `jdeps` dry run

For each module after a full install, generate a candidate descriptor and keep it as scratch input for Stage 2 (do not commit `jdeps` output):

```sh
mvn -q dependency:build-classpath -Dmdep.outputFile=target/cp.txt -pl <module>
jdeps --multi-release 25 --module-path "$(cat <module>/target/cp.txt)" \
      --generate-module-info <module>/target/jdeps <module>/target/<artifact>.jar
```

`jdeps` names the module after the jar; rename in your head. Its `requires` list is the true owner-of-package set (§3.3) and will include things not in the POM. Its `requires transitive` guesses are usually too generous; filter with `-Xlint:exports` instead.

Notes from the Stage 1 runs:

- Use `-DincludeScope=compile` (covers compile, provided, system). The parameter has no `mdep.` prefix: `-Dmdep.includeScope=…` is silently ignored and yields the full *test* path, test-only split packages included. (The Stage 1 `jdeps` runs on `nutsnbolts` and `claxon/registry` made exactly this mistake; their candidate descriptors were unaffected because `jdeps` reports only what the jar references, but re-run with the correct flag in Stage 2.)
- `jdeps` resolves the *module graph* of everything on the path, so a downstream module's run fails if an upstream SmallMind descriptor uses plain `requires` for an optional dependency (`Module org.bouncycastle.pkix not found, required by org.smallmind.nutsnbolts`). This is §3.4 in miniature; with upstream optional dependencies as `requires static` it resolves.
- It misses source-retention annotations (`jakarta.annotation.Generated`, §5.3) and anything referenced only from generated sources.
- `nutsnbolts`: `jdeps` reports `org.apache.shiro.core`, `org.apache.shiro.crypto.hash`, and `org.apache.shiro.lang` (Shiro 2's split artifacts, arriving transitively behind `shiro-spring`), and confirms the `org.apache.commons.net` name. Per §3.3, those Shiro artifacts need explicit POM dependencies when the descriptor lands.
- `web/jersey` and `web/grizzly` cannot be run through `jdeps` with their real dependency sets; see §2.8 and §5.5.

### 5.5 Jersey/HK2 spike

On a scratch branch, give `web/jersey` and `web/grizzly` draft descriptors from `jdeps`, then run `web/grizzly`'s existing server tests **on the module path** once (temporarily `useModulePath=true` for that module) to learn exactly which `opens` HK2 demands for resource and provider classes. Record the required targets; they go into `web/jersey`, `web/grizzly`, `web/jetty`, `claxon/http`, and into `WEB.adoc` as consumer guidance for resource classes. Then restore `useModulePath=false`.

**Outcome (2026-10-03).** The spike as first attempted never reached HK2: `jdeps` failed on split packages. Those first runs used the full test path by mistake (§5.4); re-checked against each module's runtime dependency set, the picture is narrower. `web/grizzly` is blocked by default; `web/jersey` is blocked only when a consumer adds its optional `jersey-spring6`; `web/jetty` and `claxon/http` are clean, so the HK2 `opens` spike *can* be run against `web/jetty` without curating anything.

**Spike result (2026-10-03).** Run as a standalone modular application rather than through surefire (surefire's module-path mode would leave the test-scoped `jersey-hk2` on the classpath, which is not the topology that matters): a scratch module `spike.app` with a JAX-RS resource, booting `JettyInitializingBean` through a Spring `GenericApplicationContext` exactly as `JettyServerBootTest` does, with **everything** on the module path — `web/jetty`'s runtime dependencies, `jersey-hk2` and its closure minus `aopalliance-repackaged`, and scratch named-module jars of `web/jersey`, `web/jetty`, and `claxon/http` built from `jdeps` descriptors. It exercised a plain resource, a resource with a private `@Context` field, a JSON entity written by SmallMind's `JsonProvider`, `EntityParamExtension`, `PageRangeResponseExtension`, `XmlAdapterParamConverterProvider`, `ThrowableExceptionMapper`, and `claxon/http`'s `EmitterResource` registered by class. Final state: every request served, no access failures. What it took:

- **`web/jersey` needs HK2 `opens` for exactly two packages**, because they hold the only SmallMind classes Jersey/HK2 instantiate with non-public members (`EntityAwareContextResolver`'s private `@Context` field, `EntityParamResolver$EntityParamValueParamProvider`'s private constructor, `ResourceMethodFilter`'s and `PageRangeResponseFilter`'s package-private `@Context` fields). Without them: `InaccessibleObjectException … does not "opens org.smallmind.web.jersey.aop" to module org.glassfish.hk2.utilities`.

  ```java
  opens org.smallmind.web.jersey.aop to org.glassfish.hk2.locator, org.glassfish.hk2.utilities, org.glassfish.jersey.core.server;
  opens org.smallmind.web.jersey.page to org.glassfish.hk2.locator, org.glassfish.hk2.utilities, org.glassfish.jersey.core.server;
  ```

  Every other class `web/jersey` registers (`JsonProvider`, `XmlAdapterParamConverterProvider`, `ThrowableExceptionMapper`, `CorsFilter`, `MultiPartFeature`) is public with a public or default constructor and no injected fields; the unconditional `exports` (§3.2) is enough.
- **`web/jetty` needs no HK2 `opens`.** Jersey never reflects into it; Spring reaches `JettyInitializingBean` through public setters of an exported type.
- **`claxon/http` needs no `opens`.** `EmitterResource` is public with public constructors and no injected fields; registered by class, HK2 constructed it and Jersey invoked it with only the unqualified `exports`.
- **`web/jersey` uses two non-exported Jersey internals:** `org.glassfish.jersey.innate.inject.InternalBinder` (`jersey-common`) in `EntityParamResolver`, and `org.glassfish.jersey.server.validation.internal.InjectingConstraintValidatorFactory` (`jersey-bean-validation`) in `EntityAwareContextResolver`. With a descriptor, javac refuses both (`package … is not visible`), so `web/jersey`'s POM needs compiler arguments `--add-exports org.glassfish.jersey.core.common/org.glassfish.jersey.innate.inject=org.smallmind.web.jersey` and `--add-exports org.glassfish.jersey.ext.bean.validation/org.glassfish.jersey.server.validation.internal=org.smallmind.web.jersey`. At run time a module-path consumer needs the same two flags on the `java` command line, or `EntityParamExtension` fails with `IllegalAccessError: superclass access check failed: … cannot access class org.glassfish.jersey.innate.inject.InternalBinder`. Removing that dependency on Jersey internals is a code change for a separate commit; until then the flags are part of `web/jersey`'s module-path contract.

**Consumer rules the spike established** (they go into `WEB.adoc`):

- Open each JAX-RS resource package: `opens com.example.rest to org.glassfish.hk2.locator, org.glassfish.hk2.utilities, org.glassfish.jersey.core.server;`. A qualified `exports … to org.glassfish.hk2.locator, org.glassfish.jersey.core.server` is enough only when resources have no non-public injected members; `opens` covers both.
- Open each package whose types `JsonProvider` serializes to `tools.jackson.databind` (otherwise `DatabindException: access to public member failed`).
- Supply an injection manager: `jersey-hk2`, as on the classpath today (not yet documented in `WEB.adoc` at all), and exclude `org.glassfish.hk2.external:aopalliance-repackaged` from it — `spring-aop` provides the same `org.aopalliance` packages and HK2 ran on them.
- Pass the two `--add-exports` flags above when using `EntityParamExtension` or the `web.jersey.aop` validation support.
- Explicit modules reached only through automatic modules are not resolved automatically, so add them with `--add-modules` (or `requires`) if nothing else does: `org.apache.commons.logging` (Spring), `jakarta.el` and `org.glassfish.expressly` (Hibernate Validator), and — until `web/json/scaffold` gets its Wave C descriptor with `requires transitive` — `tools.jackson.databind`, `tools.jackson.module.jakarta.xmlbind`, `tools.jackson.module.afterburner`. Symptom: `NoClassDefFoundError` for a class whose jar is on the module path.

**Decision (2026-10-03, revised after the spike):** `web/jersey`, `web/jetty`, and `claxon/http` get real descriptors with the `opens` above and module-path support documented with the consumer rules; `web/grizzly` gets a descriptor but stays **classpath-only** in its documentation until the Metro row below clears, and Jersey's Spring integration (`jersey-spring6`) is documented as classpath-only for the `asm` row. **Superseded (2026-10-05, owner):** the Metro row was cleared in the Wave E follow-up, and `web/grizzly` is documented with module-path support.

**What still blocks the module path.** Each row clears either upstream or by a deliberate SmallMind dependency change; the last column is what would make the row go away.

| Blocker | Jars | How it arrives | Affects | What clears it |
|---|---|---|---|---|
| `jakarta.xml.ws` (10 packages) and `jakarta.xml.soap` in two modules | `org.glassfish.metro:webservices-api-osgi` vs `jakarta.xml.ws:jakarta.xml.ws-api` / `jakarta.xml.soap:jakarta.xml.soap-api` | `web/grizzly` → `grizzly-http-server-jaxws` (compile) → `webservices-osgi` → `webservices-api-osgi` | `web/grizzly` and everything that depends on it | **Cleared (2026-10-05):** `web/grizzly` excludes both OSGi bundles (`webservices-osgi`, `jaxb-osgi`) and depends on Metro's non-OSGi `jaxws-rt` 4.0.5, which Grizzly compiles against. See the Wave E follow-up. |
| `org.aopalliance.intercept`, `org.aopalliance.aop` in two modules | `org.glassfish.hk2.external:aopalliance-repackaged` vs `org.springframework:spring-aop` | `jersey-spring6` → `jersey-hk2` → `hk2-locator` → `aopalliance-repackaged`; `spring-context` → `spring-aop` | every Jersey deployment, because each must add `jersey-hk2` (or `jersey-spring6`) |  HK2 or Spring dropping its embedded copy of AOP Alliance. Workaround, verified in the spike: exclude `aopalliance-repackaged`; HK2 runs on `spring-aop`'s copy. |
| `org.objectweb.asm.tree` not found, required by `org.objectweb.asm.commons` | `asm-commons` (explicit module) without `asm-tree` | `jersey-spring6` → `hk2` → `class-model` → `asm-commons` | consumers who add `jersey-spring6` (plain `jersey-hk2` does not bring `class-model`) | HK2 declaring the `asm` dependencies it needs; or the consumer adding `asm-tree` (and `asm`) at runtime. Latent rather than fatal: nothing *requires* `org.objectweb.asm.commons`, so it only fails if `class-model` uses it at run time. |

**How to check.** After `mvn install`, from the repository root:

```sh
for m in web/jersey web/grizzly web/jetty claxon/http; do
  mvn -q dependency:build-classpath -DincludeScope=runtime -Dmdep.outputFile=target/rt.txt -pl $m
  printf '%-14s ' $m
  java --module-path "$(cat $m/target/rt.txt)" --validate-modules > /dev/null 2>&1 \
    && echo clean || echo blocked
done
```

`--validate-modules` fails on any package contained in two modules on the path; a split like that is fatal whenever both modules end up in the graph, which is effectively always once automatic modules are involved (resolving one automatic module resolves them all). For a `blocked` module, list the conflicts and the jars that contribute them:

```sh
java --module-path "$(cat web/grizzly/target/rt.txt)" --validate-modules 2>&1 | grep -B1 conflicts
```

A stricter, noisier second check resolves *every* module on the path:

```sh
java --module-path "$(cat web/jersey/target/rt.txt)" --add-modules ALL-MODULE-PATH --list-modules > /dev/null
```

It also reports explicit modules whose own `requires` are missing (`Module X not found, required by Y`). Those only matter if something in a real application's graph requires `Y`; for example, `phalanx`'s optional `amqp-client` brings `io.netty.codec.marshalling`, which requires the absent `org.jboss.marshalling`, but no module requires `io.netty.codec.marshalling`, so a real application never resolves it. Treat such reports as leads, not blockers.

Notes: pass `-DincludeScope=runtime` exactly (see §5.4 — `-Dmdep.includeScope` is ignored and yields the test path). `web/jersey`'s own runtime set includes its optional `jersey-spring6`, so it reports the AOP Alliance blocker even though consumers of `web/jersey` do not inherit it. `build-classpath` writes the platform path separator (`;` on Windows), which is what `java --module-path` expects on that platform.

When `web/grizzly` prints `clean`, its classpath-only limitation can be lifted: re-run the §5.5 spike harness against `web/grizzly` (it uses the same Jersey layer, so the `web/jersey` `opens` should already cover it), then replace the classpath-only statement in `WEB.adoc`. If an exclusion route is chosen instead of waiting, put the exclusion in `web/grizzly`'s POM (so consumers inherit it), re-run the check, and record which row it cleared. The same applies to the `jersey-spring6` `asm` row.

### 5.6 Resource-only packages

- Add `package-info.java` to `nutsnbolts/src/main/java/org/smallmind/nutsnbolts/examples/` and `testbench/foundation/src/main/java/org/smallmind/testbench/foundation/` (creating `src/main/java` for the latter; it is currently resources-only). Standard copyright prologue; Javadoc: "Carrier package for the resources shipped under this path; it contains no classes."

**Done (2026-10-03).** Wave A showed the files are not what makes the `opens` compile (see the §3.8 correction); they stay as package documentation. For `testbench/foundation` (Wave B) this means its descriptor will have an `opens` and no `exports`, and javac will warn once for that line.

### 5.7 Decide the `aspectjrt` optionality question

**Decision (2026-10-03, owner): keep `aspectjrt` optional.** No POM change. Every module that references AspectJ declares `requires static org.aspectj.runtime;` (the woven modules: `nutsnbolts`, `persistence`, `web/jersey`, `claxon/registry`, plus any module where `jdeps` reports the reference). In Stage 3, the chapters that describe weaving (`PERSISTENCE.adoc`, `CLAXON.adoc` "AOP weaving", `WEB.adoc`, `NUTSNBOLTS.adoc`) say that the consumer supplies `aspectjrt` when it uses the aspects, and that on the module path it must also resolve `org.aspectj.runtime` itself (§3.4).

Root POM: `aspectjrt` is `compile` + `optional=true` for all 59 modules. Woven modules (`nutsnbolts`, `persistence`, `web/jersey`, `claxon/registry`) will throw `NoClassDefFoundError` at runtime without it, which the chapters presumably document as "add `aspectjrt` if you use the aspects". The descriptor will say `requires static org.aspectj.runtime;` to mirror the POM. If the owner prefers honesty over mirroring, make `aspectjrt` non-optional in the woven modules' POMs and use plain `requires` — but that is a dependency-graph change for consumers and belongs in its own commit.

**Evidence (2026-10-03).** After a full build, no main class in `nutsnbolts`, `persistence`, `web/jersey`, `claxon/registry`, `scribe/pen`, or `sleuth/runner` carries woven advice: the only classes referencing `org/aspectj` are the aspects themselves (`LazyFieldAspect`; seven in `persistence.cache.aop`/`persistence.orm.aop`; `ResourceMethodAspect`, `ValidatedAspect`; `InstrumentedAspect`) and `nutsnbolts.reflection.aop.AOPUtility`. Advice lands in the *consumer's* classes when the consumer weaves, and weaving already requires `aspectjrt`. So a consumer who never touches the aspects or `AOPUtility` never needs `aspectjrt`, and `optional` + `requires static org.aspectj.runtime` is accurate rather than merely mirrored. No chapter currently mentions `aspectjrt`; the chapters that describe weaving (`PERSISTENCE.adoc`, `CLAXON.adoc` "AOP weaving", `WEB.adoc`, `NUTSNBOLTS.adoc`) should say the consumer supplies it.

## 6. Stage 2 — Descriptors, Wave By Wave

Work leaf-first. Each wave: write the descriptors, build with `mvn -pl <modules> -am install` (tests on), run `-Xlint` clean, run `jar --describe-module` on each jar, update the chapter, commit. A later wave can `requires` an earlier wave's module; nothing in an earlier wave is touched again except to add a `requires transitive` discovered by lint.

Conventions in the tables: **bold** = `requires transitive` candidate (confirm with lint); *italic* = `requires static`; `→ X` after `opens` = `opens … to X`.

### Wave A — depth 0

Pre-flight, carrying Stage 1 findings that apply here:

1. `aspectjrt` is optional (§5.7, decided): `requires static org.aspectj.runtime;` in `nutsnbolts`.
2. Generate `jdeps` input with `-DincludeScope=compile` (not `-Dmdep.includeScope`, §5.4) after a fresh `mvn install`.
3. `nutsnbolts`: add explicit POM dependencies for `org.apache.shiro:shiro-core`, `shiro-crypto-hash`, and `shiro-lang` (optional, like `shiro-spring`), because the descriptor will name them (§5.4, §3.3).
4. `nutsnbolts`: add the §3.6 `default-testCompile` arguments (`--add-modules jdk.httpserver`, `--add-reads org.smallmind.nutsnbolts=jdk.httpserver`) in the same commit as the descriptor.
5. Third-party optional dependencies are `requires static`; JDK modules are plain `requires` (§3.4). The Stage 1 spike confirmed this shape compiles, weaves, and passes all 1021 `nutsnbolts` tests.
6. Remember the downstream effect (§5.4): until a wave's optional dependencies are `requires static`, `jdeps` runs for later waves fail to resolve.

| Module | `requires` (JDK / third-party) | `opens` / `provides` / notes |
|---|---|---|
| `org.smallmind.nutsnbolts` | `java.compiler`, `java.management`, `java.naming`, `java.rmi`, `java.xml`; all third-party are *static*: `org.apache.commons.net` (verify name), `jakarta.activation`, `jakarta.mail`, `jakarta.servlet`, `jakarta.validation`, `jakarta.xml.bind`, `jakarta.xml.soap`, `jakarta.xml.ws`, `org.apache.shiro.spring` (+ `org.apache.shiro.core` — `jdeps` will show it), `org.bouncycastle.provider`, `org.bouncycastle.pkix`, `freemarker`, `org.objectweb.asm`, `org.objectweb.asm.util`, `spring.beans`, `spring.context`, `spring.core`, `spring.web`, `org.yaml.snakeyaml`, `org.aspectj.runtime` | `opens org.smallmind.nutsnbolts.examples;` (unconditional, after §5.6). `opens` to `spring.core, spring.beans` for `nutsnbolts.spring`, `nutsnbolts.spring.jmx`, `nutsnbolts.spring.property`, `nutsnbolts.spring.remote`, `nutsnbolts.spring.web`, `nutsnbolts.security.spring`, `nutsnbolts.namespace.shiro.realm.spring`, `nutsnbolts.resource`. `opens org.smallmind.nutsnbolts.json to jakarta.xml.bind;`. Many exported signatures will reference optional types (e.g. `jakarta.servlet` in `lang.web`) — use `requires static transitive` where lint demands it. Chapter: `NUTSNBOLTS.adoc` — add the "consumers must open packages to `org.smallmind.nutsnbolts` for `reflection.*` utilities" rule and the §3.4 warning. 46 packages: generate the `exports` list from the directory tree, do not type it. |
| `org.smallmind.batch.base` | none | trivial. `BATCH.adoc`. |
| `org.smallmind.spark.singularity.boot` | none | The boot launcher builds its own class loader for nested jars at runtime; everything it loads runs in the unnamed module and is unaffected. `SPARK.adoc`. |

**Wave A outcome (2026-10-03).** Descriptors committed for all three modules; `mvn install` of the three (tests on: 1021 + 25 + 10 + 44 green) and a full repository `install -DskipTests` (83 reactor projects) are green; every jar describes as a named, non-automatic module; `jdeps --check` reports exactly the declared `requires` set (it ignores `static` and suggests `transitive` for automatic modules, both deliberate). Findings that change how later waves work:

- **javac caps warnings at 100**, so the Maven log truncates `-Xlint:exports` output (in `nutsnbolts` the deprecation warnings alone use a quarter of it). Review lint by running javac directly: `javac -d /tmp/out --module-path "$(cat <module>/target/cp.txt)" -Xlint:exports,module -Xmaxwarns 10000 $(find <module>/src/main/java -name '*.java')`.
- **Clause grouping settled** (recorded in `CODE_STYLE.md` "Module Descriptors"): `requires`, `requires transitive`, `requires static`, `exports`, `opens`, `uses`, `provides`. Lint asked for `requires transitive` on `java.compiler`, `java.naming`, `java.rmi`, `java.xml` (done) and on the optional explicit modules that appear in exported signatures (`nutsnbolts`: the six Jakarta APIs except `jakarta.xml.soap`, and `org.bouncycastle.pkix`).
- **Never `requires static transitive` (verified, corrected the same day).** Wave A first used it for those seven optional modules. A consumer module that only does `requires org.smallmind.nutsnbolts;` then fails to compile with `module not found: jakarta.mail` (and the other six): `javac` treats a `static transitive` dependency of a module it reads as mandatory. That makes the optional dependency mandatory for every consumer, so optional modules are plain `requires static`, lint's `[exports]` warnings for them are accepted like those for automatic modules, and the chapter lists them for consumers to require themselves. §3.3 and the §6 table conventions ("**bold** = `requires transitive` candidate") apply to *mandatory* explicit modules only.
- **POM ↔ descriptor exception:** `shiro-spring` stays an optional POM dependency but is *not* in the descriptor — no `nutsnbolts` class references it; only the shipped `examples/realm.xml` names `org.apache.shiro.spring.LifecycleBeanPostProcessor`. `CODE_STYLE.md` words the rule to allow this; expect the same for other modules whose Spring XML names classes from an otherwise-unused optional jar. The three Shiro split artifacts were added to the root `dependencyManagement` (`shiro-crypto-hash`, `shiro-lang`) and to `nutsnbolts` as optional.
- **Multi-release third-party jars** (`commons-net`, `snakeyaml`, `bcprov`, `bcpkix`) carry their descriptor under `META-INF/versions/9`; `jar --describe-module` without `--release` prints "No root module descriptor" for them. They are explicit modules on JDK 9+.
- **Singularity jars stay non-modular:** `GenerateSingularityMojo.copyBootClasses` copies only `org/smallmind/spark/singularity/boot/**`, so `module-info.class` never reaches a generated Singularity jar. Documented in `SPARK.adoc`. **Follow-up (2026-10-03):** Singularity gained an opt-in modular mode (`<modular>true</modular>`, `SPARK.adoc` "Modular Mode") that resolves the project's module graph at build time and defines a module layer at launch, all modules mapped to the one `SingularityClassLoader`. Verified with a modular app over `nutsnbolts` (explicit), Spring (automatic), and SnakeYAML (multi-release): Spring's `classpath:` lookup into the opened `org.smallmind.nutsnbolts.examples` package works, and an unopened package's resource is hidden — the first end-to-end confirmation of the §3.8 resource design.
- **Stage 0 regression, fixed (2026-10-03):** the `maven-jar-plugin` 3.5.1 pinned in §4.1 writes zip entries with data descriptors (the 7.3.0 jars had none), so `JarInputStream` reports `JarEntry.getSize() == -1`. `GenerateSingularityMojo.copyBootClasses` sized its reads from `getSize()` and failed with `IndexOutOfBoundsException` against any 7.4.0-SNAPSHOT boot jar — i.e. `generate-singularity` was broken for every consumer since Stage 0. The reactor build hid it because there the boot dependency resolves to `target/classes`, which the tests restage as a STORED jar; `mvn -pl spark/singularity/mojo` reproduces it. Fixed by reading each entry to its end. No other main code reads entry sizes. Lesson for later stages: build changes that alter *how* jars are written need a check of every in-repo jar reader, not just `jar --describe-module`.
- **License header:** the build's `generate-notice-headers` goal normalizes `module-info.java` headers like any other source; still write the full prologue by hand (copy lines 1–32 of an existing source file, including the closing ` */`).
- **Per-wave doc chores:** flip the wave's rows to `explicit` in the `README.adoc` *Kind* column; add a module-path paragraph (or, for large modules, a `[[module-path]]` subsection under Installation as in `NUTSNBOLTS.adoc`) linking `xref:README.adoc#module-path[Module Path And Classpath]`.

### Wave B — depth 1

| Module | `requires` | `opens` / `provides` / notes |
|---|---|---|
| `org.smallmind.ansible` | **`org.smallmind.nutsnbolts`** | `ANSIBLE.adoc` |
| `org.smallmind.scribe.pen` | **`org.smallmind.nutsnbolts`**; *static*: `jakarta.activation`, `jakarta.xml.bind`, `jackson.dataformat.msgpack` (unnamed), `com.fasterxml.jackson.*` (Jackson 2, pulled by msgpack — `jdeps` will name the exact modules), `spring.beans`, `syslog4j` (unnamed), `software.amazon.awssdk.auth`, `software.amazon.awssdk.awscore`, `software.amazon.awssdk.regions`, `software.amazon.awssdk.services.cloudwatchlogs`, `tools.jackson.core`, `tools.jackson.databind`, `org.aspectj.runtime` | `uses org.smallmind.scribe.pen.adapter.LoggingBlueprint;`. `opens` to `spring.core, spring.beans` for `scribe.pen`, `scribe.pen.spring`, `scribe.pen.spring.plan` (loggers/appenders are configured from Spring XML, e.g. `testbench/logger`). Two unnamed optional modules — call them out in `SCRIBE.adoc` troubleshooting. |
| `org.smallmind.memcached.utility` | `org.smallmind.nutsnbolts`; *static* `spring.beans` | `MEMCACHED.adoc` |
| `org.smallmind.mongodb.throng` | `org.smallmind.nutsnbolts`, **`org.mongodb.bson`**, **`org.mongodb.driver.core`**, **`org.mongodb.driver.sync.client`** (automatic — state in docs rather than `transitive`); *static* `tools.jackson.databind` | Throng reflects over consumer entity classes: `MONGODB.adoc` must say "open your entity packages to `org.smallmind.mongodb.throng`". |
| `org.smallmind.bayeux.oumuamua.server.api` | `org.smallmind.nutsnbolts`, **`jakarta.servlet`** | `BAYEUX.adoc` |
| `org.smallmind.file.ephemeral` | `org.smallmind.nutsnbolts` | `provides java.nio.file.spi.FileSystemProvider with org.smallmind.file.ephemeral.EphemeralFileSystemProvider;`. `FILE.adoc`. |
| `org.smallmind.file.jailed` | `org.smallmind.nutsnbolts`; *static* `jakarta.xml.bind` | `provides java.nio.file.spi.FileSystemProvider with org.smallmind.file.jailed.JailedFileSystemProvider;`; `opens org.smallmind.file.jailed to jakarta.xml.bind;` (one JAXB-annotated type). `FILE.adoc`. |
| `org.smallmind.mongodb.utility` | `org.smallmind.nutsnbolts`, `org.mongodb.bson`, `org.mongodb.driver.core`, `org.mongodb.driver.sync.client`; *static* `spring.beans` | `MONGODB.adoc` |
| `org.smallmind.web.http` | `org.smallmind.nutsnbolts`, `org.apache.httpcomponents.client5.httpclient5`, `org.apache.httpcomponents.core5.httpcore5`, `org.apache.httpcomponents.core5.httpcore5.h2` | `WEB.adoc` |
| `org.smallmind.schedule.base` | `org.smallmind.nutsnbolts` | `SCHEDULE.adoc` |
| `org.smallmind.sleuth.runner` | `org.smallmind.nutsnbolts`, **`org.testng`**; `org.aspectj.runtime` *static* (plugin applied, no aspects of its own) | Reflects over test classes; irrelevant while tests run on the classpath, but `SLEUTH.adoc` should say so. |
| ~~`org.smallmind.spark.tanukisoft.integration`~~ | — | Demoted to automatic-only (§3.7, Wave B outcome). `SPARK.adoc` documents the Tanuki jar's derived name. |
| `org.smallmind.testbench.foundation` | `org.smallmind.nutsnbolts` (declared; no code uses it — consider dropping the POM dependency) | `opens org.smallmind.testbench.foundation;` after §5.6; **no** `exports`. `TESTBENCH.adoc`. |
| `org.smallmind.testbench.style` | `org.smallmind.nutsnbolts`, `java.xml`, `jdk.jdi` | `opens org.smallmind.testbench.style;` only if the XSLT is loaded through a `ClassLoader` (§3.8). `TESTBENCH.adoc`. |

**Wave B outcome (2026-10-04).** Descriptors committed for 13 of the 14 modules; `spark/tanukisoft/integration` was demoted to automatic-only (§3.7). Tests: `ansible` 21, `scribe/pen` 272, `memcached/utility` 15, `bayeux/oumuamua/server/api` 101, `file/ephemeral` 168, `file/jailed` 127 (6 skipped), `mongodb/utility` 28, `web/http` 6, `sleuth/runner` 94, `spark/tanukisoft/integration` 18 — all green. `mongodb/throng`: 152 run without failure, but its four Docker-backed integration classes (`DriverCompatibilitySmokeTest`, `ThrongClientIntegrationTest`, `ThrongClientAdvancedIntegrationTest`, `ThrongClientPipelineIntegrationTest`) failed in `@BeforeClass` because no Docker daemon was running (`Connect to http://localhost:2375 failed`). Re-run with Docker up (2026-10-04): 185 run, 0 failures, 0 skipped, all four classes included. A full repository `install -DskipTests` is green; every wave jar describes as a named module; `jdeps --check` differs from the descriptors only in the deliberate ways noted under Wave A. Findings:

- **`requires transitive org.smallmind.nutsnbolts`** in ten modules (`ansible`, `bayeux/oumuamua/server/api`, `file/jailed`, `memcached/utility`, `mongodb/throng`, `mongodb/utility`, `schedule/base`, `scribe/pen`, `sleuth/runner`, `web/http`): lint reports `nutsnbolts` exception and utility types in their exported signatures. Not in `file/ephemeral` or `testbench/style`. Also `requires transitive jakarta.servlet` (`bayeux` API) and `java.xml` (`testbench/style`). Downstream waves therefore read `nutsnbolts` through any of these.
- **No `org.aspectj.runtime` in `scribe/pen` or `sleuth/runner`.** The AspectJ plugin runs there but `jdeps` finds no reference, so the §6 table's *static* entry was dropped (as in `batch/base`).
- **`jdeps` misses dependencies that arrive only through a supertype.** `scribe.pen.json.LevelEnumXmlAdapter` extends `nutsnbolts`' `EnumXmlAdapter`, so its class file never names `jakarta.xml.bind`, but `javac` needs the module (`cannot access jakarta.xml.bind.annotation.adapters.XmlAdapter`). Treat a module-aware compile as the ground truth, not `jdeps`.
- **`scribe/pen` uses Jackson 2, not Jackson 3.** `FluentBitAppender`/`MessagePackFormatter` import `com.fasterxml.jackson.databind` (brought by `jackson-dataformat-msgpack`). The POM's optional `tools.jackson.core:jackson-core`/`jackson-databind` were referenced by no main or test class and were removed; `com.fasterxml.jackson.core:jackson-core`/`jackson-databind` were added as optional, managed in the root POM at `${jackson2.version}` = 2.18.4 (the version msgpack already resolved, so nothing changes at run time). `DateTimeFormatterFactoryBean` uses JSpecify's `@Nullable` (arrives through `spring-core`), so `org.jspecify:jspecify` became an optional dependency (root `${jspecify.version}` = 1.0.0) with `requires static org.jspecify`. `SCRIBE.adoc`'s optional-feature table was wrong on both counts (it named Jackson 3 for Fluent Bit, and JAXB/Activation for `XMLFormatter`, which uses neither) and was corrected; `EmailAppender`'s Mail/Activation needs were added.
- **Resource-driven `requires` in `testbench/foundation`.** It has no classes, but its `foundation.xml`/`global.yaml` need `nutsnbolts`, Bouncy Castle, and SnakeYAML, so the descriptor requires all three and a consumer that requires `org.smallmind.testbench.foundation` gets them resolved. `jdeps` suggests none of them; that is expected. SnakeYAML moved from `runtime` to `compile` scope because `javac` must see a module named in `requires`. The `[opens] package is empty` warning appears as predicted in §5.6.
- **`testbench/style` loads `pretty-print.xslt` through the TCCL** (`ClassLoader.getResourceAsStream`), so it opens its package unconditionally (§3.8).
- **Spring `FactoryBean` packages** in `memcached/utility` and `mongodb/utility` are opened to `spring.core, spring.beans`, matching how Wave A treated `nutsnbolts`' Spring packages.
- **`web/http`** needed the §3.6 `default-testCompile` arguments (`jdk.httpserver`), as predicted. No other wave module did.
- **Module-path spot checks** (scratch apps, not committed; partial evidence ahead of Stage 4): `org.smallmind.scribe.pen` binds the automatic `org.smallmind.scribe.ink.indigenous` through `uses` and logs; `java.base` binds `org.smallmind.file.ephemeral` and `FileSystems.getFileSystem(URI.create("ephemeral:///"))` works; Throng encodes and decodes an entity only when the entity package is opened to both `org.smallmind.mongodb.throng` and `org.smallmind.nutsnbolts`. The two failure signatures are recorded in `MONGODB.adoc`.
- **Fixed after the wave:** lint reported `ConversionPatternRule` (`scribe/pen`) exposing its private enum `Padding` from a public constructor that nothing outside the class could call (`[exports] ... not accessible to clients`). The constructor is now `private`.

### Wave C — depth 2

| Module | `requires` | `opens` / `provides` / notes |
|---|---|---|
| `org.smallmind.web.json.scaffold` | `org.smallmind.nutsnbolts`, `org.smallmind.scribe.pen`, **`com.fasterxml.jackson.annotation`**, **`jakarta.xml.bind`**, `spring.beans`, **`tools.jackson.core`**, **`tools.jackson.databind`** (public API exposes `JsonNode`/`ObjectNode`), `tools.jackson.module.afterburner`, `tools.jackson.module.jakarta.xmlbind` | `opens` the JAXB-annotated packages `→ jakarta.xml.bind, tools.jackson.databind` (6 files; list them from `grep -l @Xml`). `WEB.adoc`. |
| `org.smallmind.testbench.docker` | `org.smallmind.scribe.pen`, `com.github.dockerjava`, `com.github.dockerjava.transport.httpclient5` | `TESTBENCH.adoc` |
| `org.smallmind.kafka.utility` | `org.smallmind.scribe.pen`, **`kafka.clients`** (unnamed), `spring.beans` | `KAFKA.adoc`: derived-name fragility. |
| `org.smallmind.javafx.extras` | `org.smallmind.nutsnbolts`, `org.smallmind.scribe.pen`, **`javafx.base`**, **`javafx.controls`**, **`javafx.graphics`**, `jfxtras.icalendaragenda` (the one descriptor in `jfxtras-all`; confirm it exports the packages used), `java.management` | Images loaded from the same module need no `opens` if via `Class.getResource`. `JAVAFX.adoc`. |
| `org.smallmind.memcached.cubby` | `org.smallmind.nutsnbolts`, `org.smallmind.scribe.pen`, `org.smallmind.memcached.utility`, `spring.beans`; *static* `io.whitfin.siphash` | `opens org.smallmind.memcached.cubby;` (Spring XML lives there). `MEMCACHED.adoc`. |
| `org.smallmind.schedule.quartz` | `org.smallmind.nutsnbolts`, `org.smallmind.scribe.pen`, `org.smallmind.schedule.base`, **`org.quartz`**, `spring.beans`, `spring.context`, `java.management` | `opens org.smallmind.schedule.quartz;` (Spring XML + Liquibase changelog). `SCHEDULE.adoc`: consumers open job packages to `org.quartz`. |
| `org.smallmind.scribe.apache` | `org.smallmind.scribe.pen`, `org.apache.commons.logging` | `commons-logging.properties` is a root resource (fine). `LogFactory` instantiates the bridge by name via TCCL — needs the package exported (it is). `SCRIBE.adoc`. |
| `org.smallmind.scribe.ink.indigenous` | `org.smallmind.scribe.pen` | `provides org.smallmind.scribe.pen.adapter.LoggingBlueprint with org.smallmind.scribe.ink.indigenous.IndigenousLoggingBlueprint;` |
| `org.smallmind.scribe.ink.jdk` | `org.smallmind.nutsnbolts`, `org.smallmind.scribe.pen`, `java.logging` | `provides … with org.smallmind.scribe.ink.jdk.JDKLoggingBlueprint;` |
| `org.smallmind.scribe.ink.log4j` | `org.smallmind.nutsnbolts`, `org.smallmind.scribe.pen`, `org.apache.logging.log4j`, `org.apache.logging.log4j.core` | `provides … with org.smallmind.scribe.ink.log4j.Log4JLoggingBlueprint;`. If the ink defines Log4j `@Plugin`s, Log4j's plugin registry (`Log4j2Plugins.dat`) is a root resource and works unchanged. |
| `org.smallmind.scribe.slf4j` | `org.smallmind.nutsnbolts`, `org.smallmind.scribe.pen`, `org.slf4j` | `provides org.slf4j.spi.SLF4JServiceProvider with org.smallmind.scribe.slf4j.ScribeSLF4JServiceProvider;`. SLF4J 2 on a module path finds providers **only** through `provides`. |
| `org.smallmind.testbench.logger` | `org.smallmind.scribe.pen` | `opens org.smallmind.testbench.logger;` (Spring XML). |

**Wave C outcome (2026-10-04).** Descriptors committed for all 12 modules. Tests: `web/json/scaffold` 200, `scribe/ink/indigenous` 12, `memcached/cubby` 136, `kafka/utility` 20, `schedule/quartz` 58, `scribe/ink/jdk` 90, `scribe/ink/log4j` 36, `scribe/slf4j` 42, `scribe/apache` 9 — all green (`testbench/docker`, `testbench/logger`, and `javafx/extras` have no tests). A full repository `install -DskipTests` is green; every wave jar describes as a named module; `jdeps --check` reports no errors; the only remaining `[exports]` lint warnings name automatic or derived modules (Spring, Quartz, docker-java, `kafka.clients`), which the chapters tell consumers to require. Findings:

- **Two third-party aggregate jars made a module path impossible; both were replaced with the artifacts that own the imported packages (§3.3).**
  - `docker-java` (the aggregate) contains a single class, `com.github.dockerjava.core.DockerClientBuilder`, in a package that `docker-java-core` also owns, so `jdeps` failed with `Module com.github.dockerjava contains package com.github.dockerjava.core, module com.github.dockerjava.core exports package com.github.dockerjava.core to com.github.dockerjava`. It also brings the Netty and Jersey transports and `jcl-over-slf4j` (which splits `org.apache.commons.logging` with `commons-logging` and `spring-jcl`). `testbench/docker` imports only from `docker-java-api` and `docker-java-core`; it now depends on those two (managed in the root POM). `testbench/condition` (Wave D) imports only from `docker-java-api` and should get the same swap.
  - `jfxtras-all` is an uber-jar that repeats the packages of every JFXtras component jar it depends on, and it declares the module name `jfxtras.icalendaragenda`, which `jfxtras-icalendaragenda` also declares; its graph also requires `java.xml.ws.annotation`, which no longer exists. `javafx/extras` uses only `jfxtras.util.PlatformUtil` (owned by `jfxtras-common`, explicit module `jfxtras.common`) and now depends on `jfxtras-common` (managed in the root POM). This narrows what consumers receive transitively; `JAVAFX.adoc` says so. Verified: `jfxtras-all` on a module path is harmless until something resolves `jfxtras.icalendaragenda`, and then startup fails with `FindException: Module jfxtras.icalendarfx not found, required by jfxtras.icalendaragenda`.
- **Direct POM dependencies added (§3.3):** `testbench/docker` and `kafka/utility` import `nutsnbolts` but received it only through `scribe-pen`; both now declare `smallmind-nutsnbolts` and `requires transitive org.smallmind.nutsnbolts`.
- **`requires transitive`** per lint: `nutsnbolts` in `web/json/scaffold`, `testbench/docker`, `kafka/utility`, `javafx/extras`, `memcached/cubby`, `schedule/quartz`; `org.smallmind.scribe.pen` in all three inks and `scribe/slf4j`; `org.smallmind.memcached.utility` (cubby); `org.smallmind.schedule.base` and `java.management` (quartz); `javafx.base`, `javafx.controls`, `javafx.graphics`, `java.management` (JavaFX); `java.logging` (`scribe/ink/jdk`); `org.apache.logging.log4j` and `.core` (`scribe/ink/log4j`); `org.slf4j` (`scribe/slf4j`); `org.apache.commons.logging` (`scribe/apache`); and, in `web/json/scaffold`, `jakarta.xml.bind`, `tools.jackson.core`, `tools.jackson.databind`, `com.fasterxml.jackson.annotation`. The §5.5 note asking module-path consumers to `--add-modules` the Jackson 3 modules no longer applies to anyone who requires `org.smallmind.web.json.scaffold`: it re-exports `tools.jackson.databind` and requires `tools.jackson.module.jakarta.xmlbind`, so both are resolved. `tools.jackson.module.afterburner` is not required: `JsonCodec`'s `AfterburnerModule` registration is commented out and nothing else references it. The POM's compile-scope Afterburner dependency is therefore unused; it was left alone because removing a mandatory dependency changes consumers' classpaths.
- **Unconditional `opens` for resources loaded by path:** `memcached.cubby` (`memcached.xml`), `schedule.quartz` (Spring XML and Liquibase changelog), `testbench.logger` (`logging.xml`), and `javafx.extras.dialog` (`dialog_*.png`, loaded through the TCCL). The JavaFX chart CSS files are loaded with `Class.getResource` from inside the module and need nothing. The classifier-less OpenJFX jars contain no classes and are ignored on a module path; `javafx.*` resolves from the platform jars in either order (verified).
- **`schedule/quartz`: jobs are Spring beans.** `SpringJobFactory` fetches job instances from the application context, so consumers open job packages to `spring.beans` (and `spring.core`), or to `org.quartz` if they use Quartz's own job factory. The §2.5 "open job packages to `org.quartz`" line only covers the second case. This is source-derived and not yet run on a module path; Stage 4 should exercise it.
- **Module-path spot checks** (scratch apps, not committed):
  - `org.slf4j` binds `org.smallmind.scribe.slf4j` through `provides`, with no `requires` from the application.
  - `org.smallmind.scribe.pen` binds the now-explicit `org.smallmind.scribe.ink.indigenous`.
  - `JsonCodec` round-trips a consumer type with JAXB annotations when the package is opened (or exported) to `tools.jackson.databind`, and fails with `DatabindException: access to public member failed` otherwise.
  - **`scribe/apache` is not discovered by service binding.** Commons Logging finds `CommonsLogWrapper` through the root `commons-logging.properties`, so nothing pulls `org.smallmind.scribe.apache` into the graph. Unless the application requires it (or uses `--add-modules`), `LogFactory.getLog` silently returns `Jdk14Logger`. When `org.slf4j` is in the graph, Commons Logging 1.3 prefers SLF4J on both paths, as it already does on the classpath. All of this is documented in `SCRIBE.adoc`, with a troubleshooting entry.

### Wave D — depth 3

| Module | `requires` | `opens` / `provides` / notes |
|---|---|---|
| `org.smallmind.web.json.doppelganger` | `org.smallmind.nutsnbolts`, `org.smallmind.web.json.scaffold`, `java.compiler`, `jakarta.annotation`, `jakarta.validation`, `jakarta.xml.bind`, `tools.jackson.databind`; *static* `com.google.auto.service` (annotations only). `auto-service` (the processor) belongs on the processor path, not in `requires` — change its POM scope to `provided` if it is currently `compile`. | `provides javax.annotation.processing.Processor with org.smallmind.web.json.doppelganger.DoppelgangerAnnotationProcessor;`. Generated code in **consumer** modules references scaffold, Jackson, JAXB types — `DOPPELGANGER.adoc` must list the `requires` a consuming module needs. |
| `org.smallmind.testbench.condition` | `org.smallmind.nutsnbolts`, `org.smallmind.testbench.docker`, `com.github.dockerjava.api` (not `com.github.dockerjava` — see Wave C outcome; swap its POM's `docker-java` for `docker-java-api` as was done for `testbench/docker`), `com.rabbitmq.client` | `TESTBENCH.adoc`. Once it no longer declares `docker-java`, nothing does: drop the `docker-java` managed entry and its Jersey-transport exclusion from the root POM, and remove the `docker-java` notes from `TESTBENCH.adoc` (installation note, troubleshooting entry, defaults row). |
| `org.smallmind.web.jersey` | `org.smallmind.nutsnbolts`, `org.smallmind.scribe.pen`, `org.smallmind.web.http`, **`org.smallmind.web.json.scaffold`**, **`jakarta.inject`**, `jakarta.servlet`, **`jakarta.validation`**, **`jakarta.ws.rs`**, **`jakarta.xml.bind`**, `org.glassfish.jersey.core.client`, `org.glassfish.jersey.core.common`, **`org.glassfish.jersey.core.server`**, `org.glassfish.jersey.ext.bean.validation`, `org.glassfish.jersey.media.multipart`, `org.hibernate.validator`, `spring.beans`, `spring.context`, `spring.web`, `tools.jackson.core`; *static* `org.glassfish.jersey.ext.spring6`, `org.aspectj.runtime`. | `opens` `web.jersey.json`, `web.jersey.proxy` `→ jakarta.xml.bind`; `web.jersey.spring`, `web.jersey.proxy.spring`, `web.jersey.cors.spring` `→ spring.core, spring.beans`; `web.jersey.aop`, `web.jersey.page` `→ org.glassfish.hk2.locator, org.glassfish.hk2.utilities, org.glassfish.jersey.core.server` (§5.5). Also `jakarta.annotation` (found by `jdeps`). POM: the two `--add-exports` compiler arguments for Jersey internals (§5.5). `WEB.adoc`: consumer rules from §5.5. |
| `org.smallmind.web.jwt` | `org.smallmind.nutsnbolts`, `org.smallmind.web.json.scaffold`, `org.jose4j`, `tools.jackson.databind` | `WEB.adoc` |

**Wave D outcome (2026-10-05).** Descriptors committed for all four modules. Tests: `web/json/doppelganger` 83, `web/jersey` 135, `web/jwt` 53, all green (`testbench/condition` has no tests). A full repository `install -DskipTests` is green, every wave jar describes as a named module, and `jdeps --check` differs from the descriptors only in the deliberate ways noted under Wave A. The remaining `[exports]` lint warnings name automatic modules (Spring, HttpComponents, `org.jose4j`, `com.rabbitmq.client`, `com.google.auto.service`) or the static `org.aspectj.runtime`, and the chapters tell consumers to require those. Findings:

- **`web/json/doppelganger`.** `auto-service` (the processor, already on `annotationProcessorPaths`) moved to `provided`, and `auto-service-annotations` became optional to match `requires static com.google.auto.service`, so neither reaches consumers any more. The descriptor does **not** require `jakarta.annotation`, because no processor class uses it. Generated code needs it, and the consumer declares it (`requires static jakarta.annotation;`, as in §5.3). Lint asked for `requires transitive` on `java.compiler`, `jakarta.xml.bind` (`NullXmlAdapter` extends `XmlAdapter`), and `org.smallmind.nutsnbolts`.
- **Doppelganger module-path spot check** (scratch modular consumer, not committed):
  - A named consumer module does not get the processor from the module path, even with `-proc:full`; no views are generated. It works from `--processor-path` and from `--processor-module-path`, where `javac` finds it through the new `provides`.
  - Generated views compile with `requires org.smallmind.web.json.scaffold; requires static jakarta.annotation; requires static org.smallmind.web.json.doppelganger;`. Without `jakarta.annotation` the error is `package jakarta.annotation is not visible`.
  - At run time `org.smallmind.web.json.doppelganger` is not in the graph, and a `JsonCodec` round trip of a generated view works when the package is opened to `tools.jackson.databind`.
  - **Decided (2026-10-05, owner): optional.** `claxon/registry` and `bayeux/oumuamua/server/impl` now declare `web-json-doppelganger` as `<optional>true</optional>`, so their Wave E/G descriptors use `requires static org.smallmind.web.json.doppelganger` (plus `requires static jakarta.annotation`, §5.3). `bayeux/oumuamua/server/impl` had been receiving `web-json-scaffold` (`JsonCodec` in `OumuamuaServer`) and `jakarta.xml.bind-api` (its three `XmlAdapter`s) only through Doppelganger, so both became direct compile dependencies (§3.3), and `jakarta.annotation-api` became a direct optional one. A scratch consumer's resolved runtime set lost exactly `web-json-doppelganger` and `jakarta.validation-api` (both modules) and `jakarta.annotation-api` (impl); neither module uses Validation, and `@Generated` has source retention. Tests: `claxon/registry` 209, `bayeux/oumuamua/server/impl` 320, all green.
  - Separately, on JDK 25 a non-modular build with the processor only on the classpath also generates nothing unless `-proc:full` is set (JDK 23 change). `DOPPELGANGER.adoc` claimed otherwise and was corrected.
  - Noticed, not fixed: the chapter's Quick Start names the generated classes `UserOut`/`UserCreateIn`, but the processor emits `UserOutView`/`UserCreateInView`; and "No `equals`, `hashCode`" under Limitations is wrong, because generated views do override both. That is a documentation fix outside this lift.
- **`testbench/condition`.** `docker-java` was swapped for `docker-java-api`, as Wave C recommended. Nothing declares `docker-java` any more, so its managed entry and Jersey-transport exclusion were dropped from the root POM, and the `TESTBENCH.adoc` installation note, troubleshooting entry, and defaults row were rewritten or removed. `amqp-client` 5.28 brings Netty; `netty-codec-marshalling` and `netty-codec-protobuf` require modules nobody supplies, so `jdeps` (which resolves every module) must run with Netty filtered off the path. This is a lead, not a blocker (§5.5 "How to check"). `RabbitMQAvailableTestCondition` exposes `com.rabbitmq.client.Address` (automatic, so documented, not re-exported).
- **`web/jersey`, differences from the table, all verified:**
  - No `tools.jackson.core` and no `org.glassfish.jersey.core.client`; neither is referenced.
  - No `requires static org.glassfish.jersey.ext.spring6`: `jersey-spring6` is a runtime-scoped optional dependency that no class references, which is the `shiro-spring` exception from Wave A.
  - The only JAXB-annotated type is `aop.Envelope`, so `web.jersey.json` and `web.jersey.proxy` are not opened. `web.jersey.aop` is opened to `jakarta.xml.bind` and `tools.jackson.databind` in addition to the §5.5 HK2 targets.
  - Lint added `requires transitive jakarta.annotation` (`@Priority` on `JsonProvider`).
  - POM: direct `jakarta.annotation-api`, `httpclient5`, and `httpcore5` dependencies (§3.3); the two §5.5 `--add-exports` on the plugin-level `compilerArgs`; and the §3.6 `jdk.httpserver` `default-testCompile` arguments.
  - Expected warnings: `[module] module not found` for `org.glassfish.hk2.locator`/`utilities`, which are not on the compile path, and Maven's `Required filename-based automodules detected: [osgi-resource-locator-3.0.0.jar]`, because `jersey-common` declares `requires static osgi.resource.locator`. Both are harmless and upstream.
- **`web/jersey` module-path spot check** (the §5.5 harness rebuilt against the real descriptor, booting `web/jetty` through Spring with `jersey-hk2` minus `aopalliance-repackaged`; not committed):
  - Every request was served: a resource with a private `@Context` field, `JsonProvider` writing a consumer DTO, `@ResourceMethod(Envelope.class)` with `@EntityParam` parameters, and `PageRangeResponseExtension`/`ThrowableExceptionExtension` registered.
  - Without the two `--add-exports`: `IllegalAccessError … cannot access class org.glassfish.jersey.innate.inject.InternalBinder`. Without `jersey-hk2`: `IllegalStateException: InjectionManagerFactory not found.` (now documented for classpath use too, as §7 asked).
  - The run needed `--add-modules ALL-MODULE-PATH`, because `web/jetty` was still automatic and could not pull the explicit Jetty modules into the graph. **Resolved in Wave E:** with `web/jetty`'s descriptor the same harness passes with only `--add-modules org.apache.commons.logging,jakarta.el,org.glassfish.expressly`.
- **`web/jwt`.** `org.jose4j` is an automatic module (its manifest declares the name), so it is a plain `requires`, not the table's bold candidate. `tools.jackson.databind` is not referenced directly, so it is not required; it is read through scaffold's `requires transitive`. The POM's `jackson-databind` dependency is left alone, as with Afterburner in Wave C.

### Wave E — depth 4

| Module | `requires` | `opens` / `provides` / notes |
|---|---|---|
| `org.smallmind.claxon.registry` | `org.smallmind.nutsnbolts`, `org.smallmind.scribe.pen`, `org.smallmind.web.json.scaffold`, `org.smallmind.web.json.doppelganger` (compile dependency today; check whether generated code needs it at runtime or only the processor does), `jakarta.annotation`, `jakarta.xml.bind`, `HdrHistogram` (unnamed; the capitalized derived name is legal), `spring.beans`, `tools.jackson.databind`; *static* `org.aspectj.runtime` | `opens org.smallmind.claxon.registry;` (Spring XML) and the remaining registry packages `→ spring.core, spring.beans`. `CLAXON.adoc`. |
| `org.smallmind.testbench.groundwater` | `org.smallmind.testbench.condition`, `org.smallmind.testbench.docker`, `org.testng` | `TESTBENCH.adoc` |
| `org.smallmind.web.grizzly` | `org.smallmind.nutsnbolts`, `org.smallmind.scribe.pen`, `org.smallmind.web.jersey`, `jakarta.servlet`, `jakarta.websocket`, `jakarta.websocket.client`, `jakarta.xml.bind`, `org.glassfish.grizzly`, `org.glassfish.grizzly.http`, `org.glassfish.grizzly.http.server`, `org.glassfish.grizzly.servlet`, `org.glassfish.grizzly.http2`, `grizzly.http.server.jaxws` (unnamed), `org.glassfish.jersey.container.servlet`, `org.glassfish.jersey.core.server`, `org.glassfish.tyrus.core`, `org.glassfish.tyrus.server`, `org.glassfish.tyrus.spi`, `org.glassfish.tyrus.container.grizzly.client`, `org.glassfish.tyrus.container.grizzly.server`, `spring.beans`, `spring.context`, `spring.web` | `opens org.smallmind.web.grizzly;` (Spring XML). No HK2 `opens` needed (Jersey does not reflect into it); documented as classpath-only until the Metro blocker clears (§5.5). `WEB.adoc`. |
| `org.smallmind.web.jetty` | `org.smallmind.nutsnbolts`, `org.smallmind.scribe.pen`, `org.smallmind.web.jersey`, `jakarta.servlet`, `jakarta.websocket`, `jakarta.websocket.client`, `jakarta.xml.ws`, `org.eclipse.jetty.http`, `org.eclipse.jetty.http.spi`, `org.eclipse.jetty.server`, `org.eclipse.jetty.util`, `org.eclipse.jetty.ee10.servlet`, `org.eclipse.jetty.ee10.websocket.jakarta.server`, `org.glassfish.jersey.container.servlet`, `org.glassfish.jersey.core.server`, `spring.beans`, `spring.context`, `spring.web`, **`jdk.httpserver`** | No `opens` needed (verified, §5.5). `WEB.adoc` |

**Wave E outcome (2026-10-05).** Descriptors committed for all four modules. Tests: `claxon/registry` 209, `web/grizzly` 101, `web/jetty` 63, all green (`testbench/groundwater` has no tests). A full repository `install -DskipTests` is green, every wave jar describes as a named module, and `jdeps --check` differs from the descriptors only in the deliberate ways (no `transitive` on automatic modules; `static` for the optional Doppelganger, AspectJ, and `jakarta.annotation`). The remaining `[exports]` lint warnings name automatic modules (Spring, `HdrHistogram`, `org.testng`) or static ones (`org.aspectj.runtime`, and `jakarta.annotation` for `@Generated` on generated public views). Findings:

- **`claxon/registry`:**
  - Uses `requires static` for `org.smallmind.web.json.doppelganger`, as decided in Wave D. `jdeps` lists it as a plain `requires`, because the runtime-retention `@Doppelganger` stays in the class files, but nothing loads it.
  - Its own code never imports `jakarta.annotation`; only the source-retention `@Generated` in the generated views does. So the descriptor uses `requires static jakarta.annotation`, and the POM's `jakarta.annotation-api` became optional, the same reasoning as for Doppelganger. No module downstream of registry imports `jakarta.annotation`.
  - `HdrHistogram` is a filename-derived automatic name, exposed through `HistogramTime`. It is documented, not re-exported.
  - `opens`: the root package unconditionally (`claxon.xml`), `json` to `jakarta.xml.bind, tools.jackson.databind` (the generated `*InView` classes `JsonCodec` reads), and `spring` to `spring.core, spring.beans`. The other generated views (`PercentileInView`, `WindowInView`) live in the root package and are covered by its unconditional open.
  - Module-path spot check: a scratch module requiring only `org.smallmind.claxon.registry` sees `claxon.xml` through `ClassLoader.getResource` and parses a histogram definition through `HistogramParser` with no extra flags.
- **`web/grizzly` and `web/jetty`:** both tests use `java.net.http`, so each gained the §3.6 `default-testCompile` arguments (`--add-modules java.net.http`, `--add-reads <module>=java.net.http`).
- **`web/grizzly`:**
  - Its descriptor compiles in Maven despite the Metro split. A direct `javac` with the whole compile path on `--module-path` fails with `reads package jakarta.xml.ws from both webservices.api.osgi and jakarta.xml.ws`, so lint and `jdeps` for it run with `webservices-api-osgi` filtered out.
  - The §5.5 check still reported it `blocked`, and `WEB.adoc` first documented it as classpath-only. That was reversed the same day; see the follow-up below.
  - `jakarta.xml.bind` is a plain `requires`, not the table's bold candidate; `jdeps` and lint find it in no exported signature.
  - `tyrus-container-grizzly-client` is a compile dependency that no class references, so the descriptor does not name it. It was left in the POM.
  - `grizzly.http.server.jaxws` is a filename-derived automatic name.
- **`web/jetty`:**
  - `jdk.httpserver` is a plain `requires`, not transitive: `JettyInitializingBean` uses `com.sun.net.httpserver.HttpContext` internally, but no exported signature shows it.
  - No `opens`, as the §5.5 spike predicted.
  - The §5.5 harness passes as noted under Wave D, and §5.5's check reports `web/jetty` and `claxon/http` `clean`.
- **`testbench/groundwater`:** re-exports `org.smallmind.testbench.docker`, because `AbstractGroundwaterTest(DockerApplication...)` exposes it. `org.testng` is automatic, and a test that extends the class requires it itself.
- **Docs:** `CLAXON.adoc` gained a "Module path" subsection and the §5.7 `aspectjrt` sentence under "AOP weaving". `WEB.adoc` gained "Module path" subsections for `web-grizzly` and `web-jetty` (the `web-grizzly` one rewritten by the follow-up below), and its `web-jersey` container paragraph points at both. `TESTBENCH.adoc` now lists every testbench artifact as explicit.

**Wave E follow-up: `web/grizzly` on the module path (2026-10-05, owner request: Grizzly is the preferred container).**

- **Metro.**
  - Every package in Metro's `webservices-api-osgi` (11 of them) is also in `jakarta.xml.ws-api` plus `jakarta.xml.soap-api`, so excluding it alone clears `--validate-modules`.
  - On a module path, however, Metro's `webservices-osgi` (an automatic module) reflects into `jaxb-osgi` (explicit, `com.sun.xml.bind.osgi`), which opens its internals only to `com.sun.xml.ws`. That fails with `InaccessibleObjectException … does not "opens org.glassfish.jaxb.core.v2.model.nav" to module webservices.osgi`. With that opened, the bundle still cannot pull Metro's explicit support modules (`gmbal`, …) into the graph.
  - So `web/grizzly` now excludes both OSGi bundles (`org.glassfish.metro:webservices-osgi`, `com.sun.xml.bind:jaxb-osgi`) from `grizzly-http-server-jaxws` and depends on `com.sun.xml.ws:jaxws-rt`, the `provided` dependency Grizzly's add-on compiles against. The root's unused `sun.xml.ws.version`/`sun.xml.bind.version` moved from 4.0.3/4.0.6 to 4.0.5/4.0.9 to match that pairing.
  - For a consumer, the runtime set changes as follows. Removed: `webservices-osgi`, `webservices-api-osgi`, `webservices-extra-jdk-packages`, `jaxb-osgi`, `xmlsec`, `commons-codec`. Added: `jaxws-rt`, `jaxb-impl`, `jaxb-core`, `saaj-impl`, `streambuffer`, `stax-ex`, `FastInfoset`.
  - `jaxws-rt` is plain JAX-WS: the WS-* (WSIT) extensions the full Metro bundle carried are gone. Nothing in the repository uses them, and `WEB.adoc` says so.
- **HTTP/2 add-on (existing bug, found while doing this).** `GrizzlyInitializingBean` always registers `Http2AddOn`, which needs `org.glassfish.grizzly.npn`, but `grizzly-http2` declares `grizzly-npn-api` `provided` (and `requires static`). Consumers therefore failed at startup with `NoClassDefFoundError: org/glassfish/grizzly/npn/AlpnServerNegotiator`. Tests hid it, because the test-scoped `grizzly-npn-bootstrap` 2.0.0 (a JDK 8-era jar that also patches `sun.security.ssl`) carries the same classes.
  - Fix: `web/grizzly` depends on `grizzly-npn-api` 2.0.1 (an explicit module, the version Grizzly 5.0.2 builds against) at compile scope, and its descriptor `requires org.glassfish.grizzly.npn`, so the module path resolves it too. `jdeps` calls that `requires` unused; it is deliberate, like `testbench/foundation`'s resource-driven ones.
  - `grizzly-npn-bootstrap` was dropped from `web/grizzly`'s tests and replaced by `grizzly-npn-api` in the root `dependencyManagement` (`grizzly.npn.version` 2.0.0 → 2.0.1).
- **Consumer rules for SOAP on a module path** (verified, recorded in `WEB.adoc`):
  - Export the `@ServicePath` service package unconditionally. Metro's `Trampoline` invokes service methods from the unnamed module; a qualified `opens` to `com.sun.xml.ws` is neither needed nor sufficient.
  - Pass `--add-reads com.sun.xml.ws=grizzly.http.server.jaxws`. Metro builds method handles over Grizzly's `JaxwsConnection`, which needs that read edge, and a library cannot add it on another module's behalf.
- **Verification.**
  - `web/grizzly` tests: 101 green.
  - `bayeux/oumuamua/server/impl` (whose tests run on `web-grizzly` via `oumuamua.xml`): 320 green.
  - Full repository `install -DskipTests` green.
  - The §5.5 check now reports `web/grizzly` `clean`.
  - A scratch harness that boots `GrizzlyInitializingBean` through Spring with a `@ServicePath` `@WebService` and a Jersey resource gave WSDL 200, SOAP 200 (`echo:hi`), and REST 200 (`{"text":…}`) on both the classpath and the module path.
  - Tyrus WebSocket endpoints have not been exercised on a module path.

### Wave F — depth 5

| Module | `requires` | `opens` / `provides` / notes |
|---|---|---|
| `org.smallmind.quorum` | `org.smallmind.nutsnbolts`, `org.smallmind.scribe.pen`, `org.smallmind.claxon.registry`, `java.management`, `java.naming` | `QUORUM.adoc` |
| `org.smallmind.bayeux.oumuamua.server.spi` | `org.smallmind.nutsnbolts`, `org.smallmind.scribe.pen`, **`org.smallmind.bayeux.oumuamua.server.api`**, `jakarta.servlet`; *static*: `org.smallmind.claxon.registry`, `org.smallmind.kafka.utility`, `org.smallmind.web.json.scaffold`, `jakarta.websocket`, `jakarta.websocket.client`, `kafka.clients`, `spring.beans`, `tools.jackson.core`, `tools.jackson.databind` | Nine optional dependencies — the §3.4 warning belongs prominently in `BAYEUX.adoc`. |
| `org.smallmind.claxon.emitter.aws` | `org.smallmind.claxon.registry`, `spring.beans`, `software.amazon.awssdk.auth`, `software.amazon.awssdk.awscore`, `software.amazon.awssdk.regions`, `software.amazon.awssdk.services.cloudwatch` | all six emitters: *static* `org.aspectj.runtime` only if `jdeps` shows woven references (the inherited plugin runs but there are no aspects to weave in) |
| `org.smallmind.claxon.emitter.datadog` | `org.smallmind.claxon.registry`, `java.dogstatsd.client` (unnamed), `spring.beans` | |
| `org.smallmind.claxon.emitter.jmx` | `org.smallmind.claxon.registry`, `java.management` | |
| `org.smallmind.claxon.emitter.message` | `org.smallmind.claxon.registry`, `org.smallmind.scribe.pen`, `spring.beans` | |
| `org.smallmind.claxon.emitter.noop` | `org.smallmind.claxon.registry`, `spring.beans` | |
| `org.smallmind.claxon.emitter.prometheus` | `org.smallmind.claxon.registry`, `org.smallmind.nutsnbolts` | |
| `org.smallmind.claxon.exotic` | `org.smallmind.claxon.registry`, `java.management`, probably `jdk.management` (verify the `com.sun.management` import) | |
| `org.smallmind.claxon.http` | `org.smallmind.claxon.registry`, `jakarta.ws.rs` | One JAX-RS resource; no `opens` needed (verified, §5.5). All of wave F's claxon modules: `CLAXON.adoc`. |

**Wave F outcome (2026-10-05).** Descriptors committed for all ten modules. Tests: `quorum` 208, `bayeux/oumuamua/server/spi` 529, `claxon/emitter/jmx` 14, `claxon/emitter/message` 11, `claxon/emitter/noop` 5, `claxon/emitter/prometheus` 11, `claxon/http` 6, `claxon/exotic` 6, all green (`claxon/emitter/aws` and `claxon/emitter/datadog` have no tests). A full repository `install -DskipTests` is green, every wave jar describes as a named module, and `jdeps --check` differs from the descriptors only in the deliberate ways. The only `[exports]` lint warnings name automatic modules (Spring, the AWS SDK) or `spi`'s static ones. Findings:

- **No POM changes were needed.** No module ships resources or services files. None references `org.aspectj.runtime` (the inherited Claxon AspectJ plugin weaves nothing), so no emitter has the table's conditional `requires static`.
- **`bayeux/oumuamua/server/spi`:**
  - Eight `requires static` modules: `jakarta.websocket`, `jakarta.websocket.client`, `kafka.clients`, `org.smallmind.claxon.registry`, `org.smallmind.kafka.utility`, `org.smallmind.web.json.scaffold`, `tools.jackson.core`, `tools.jackson.databind`.
  - Lint flags `kafka-utility` (`KafkaBackbone`) and WebSocket (`WebsocketConfiguration`, `WebsocketConfigurator`) types in exported signatures; they stay non-transitive per the Wave A rule.
  - The optional `spring-beans` is referenced by no class, so it is not in the descriptor (the Wave A `shiro-spring` exception, here with no resource either). It was left in the POM.
  - No `opens`: the codecs convert *consumer* types, which the consumer opens to `tools.jackson.databind` per the scaffold rule.
  - **§3.4 verified on a module path.** A module requiring only `spi` and building `new OrthodoxCodec(new JaxbDeserializer<>())` compiles, then fails with `NoClassDefFoundError: org/smallmind/web/json/scaffold/util/JsonCodec` although `web-json-scaffold` is on the path. Adding `requires org.smallmind.web.json.scaffold` fixes it. `BAYEUX.adoc` carries this as a `[WARNING]` plus a feature-to-module table.
- **`quorum`:** re-exports `java.management`, `java.naming`, and `nutsnbolts`; no `opens`. Module-path spot check: naming `JavaURLContextFactory` as `INITIAL_CONTEXT_FACTORY` reaches Quorum's code from `java.naming` exactly as on the classpath.
  - **Pre-existing documentation bug:** `QUORUM.adoc`'s documented wiring, `Context.URL_PKG_PREFIXES = "org.smallmind.quorum.namespace"`, never reaches Quorum on either path (`NoInitialContextException`). JNDI resolves `<prefix>.java.javaURLContextFactory`, and no such class exists. **Fixed in the chapter (2026-10-05, owner):** `QUORUM.adoc` now documents `Context.INITIAL_CONTEXT_FACTORY = JavaURLContextFactory`, verified through `new InitialContext(env)` against the embedded LDAP server from Quorum's tests.
- **Claxon emitters and add-ons:**
  - Every one re-exports `org.smallmind.claxon.registry`.
  - The Spring factory-bean packages are opened to `spring.core, spring.beans`: the `aws` root package, and the `spring` subpackages of `datadog`, `message`, and `noop`.
  - `claxon/exotic` requires `jdk.management` (`OSFacts` uses `com.sun.management`).
  - `java.dogstatsd.client` and `kafka.clients` are filename-derived names; the AWS SDK jars declare `Automatic-Module-Name`.

### Wave G — depth 6

| Module | `requires` | `opens` / `provides` / notes |
|---|---|---|
| `org.smallmind.persistence` | `org.smallmind.nutsnbolts`, `org.smallmind.scribe.pen`, `java.sql`, `java.logging`, `java.transaction.xa`; *static*: `org.smallmind.claxon.registry`, `org.smallmind.memcached.utility`, `org.smallmind.mongodb.throng`, `org.smallmind.quorum`, `org.smallmind.testbench.condition`, `com.querydsl.core`, `com.querydsl.jpa`, `jakarta.persistence`, `org.hibernate.orm.core`, `org.mongodb.bson`, `org.mongodb.driver.core`, `org.mongodb.driver.sync.client`, `spring.beans`, `spring.context`, `spring.core`, `spring.jcl`, `spring.orm`, `org.aspectj.runtime`. `querydsl-apt` is a processor — `provided`/processor path, not `requires`. | `opens org.smallmind.persistence;` (Spring XML). `opens` `→ spring.core, spring.beans` for `persistence.orm`, `persistence.orm.spring`, `persistence.orm.spring.jpa`, `persistence.orm.spring.throng`, `persistence.sql.pool.spring`, `persistence.cache.memcached.spring`. `opens` entity-bearing packages `→ org.hibernate.orm.core` (3 files with JPA annotations; find with `grep -l '@Entity\|@MappedSuperclass'`). `hibernate-enhance-maven-plugin` unaffected. `PERSISTENCE.adoc`: consumers open entity packages to `org.hibernate.orm.core`; the §3.4 warning for every one of the 14 optional modules. |
| `org.smallmind.bayeux.oumuamua.server.impl` | `org.smallmind.nutsnbolts`, `org.smallmind.scribe.pen`, `org.smallmind.bayeux.oumuamua.server.api`, `org.smallmind.bayeux.oumuamua.server.spi`, `org.smallmind.web.json.doppelganger` (see registry note), `jakarta.servlet`; *static* `jakarta.websocket.client` | `BAYEUX.adoc` |
| `org.smallmind.phalanx` | `org.smallmind.nutsnbolts`, `org.smallmind.scribe.pen`, `org.smallmind.claxon.registry`, `org.smallmind.quorum`, `org.smallmind.web.jersey`, `org.smallmind.web.json.scaffold`, **`jakarta.messaging`**, `jakarta.ws.rs`, `jakarta.xml.bind`, `spring.beans`, `tools.jackson.core`, `tools.jackson.databind`, `java.management`, `java.naming`; *static*: `org.smallmind.kafka.utility`, `com.rabbitmq.client`, `kafka.clients` | `opens org.smallmind.phalanx.wire;` (Spring XML); `opens org.smallmind.phalanx.wire.signal to jakarta.xml.bind, tools.jackson.databind;`; `phalanx.wire.spring`, `phalanx.wire.transport.amqp.rabbitmq.spring` `→ spring.core, spring.beans`. `PHALANX.adoc`. |

**Wave G outcome (2026-10-05).** Descriptors committed for all three modules. Tests: `persistence` 611, `bayeux/oumuamua/server/impl` 320, `phalanx` 162, all green. A full repository `install -DskipTests` is green, every wave jar describes as a named module, and `jdeps --check` differs from the descriptors only in the deliberate ways. The only `[exports]` lint warnings name optional (static) or automatic modules, plus one pre-existing `[removal]` deprecation (`PersistenceUnitTransactionType`). Findings:

- **`persistence`, `spring-jcl` → `commons-logging`.**
  - The POM declared `spring-jcl` **6.2.15** (optional) next to Spring 7.0.3. Spring 7 dropped `spring-jcl`, and its `spring-core` depends on `commons-logging` 1.3 instead, so both jars carried `org.apache.commons.logging`. That split made `jdeps` fail, and a module-path compile would read the package from two modules.
  - The only reference was `EntitySeekingEntityManagerFactoryBean` using the `logger` it inherits from Spring's `AbstractEntityManagerFactoryBean`.
  - `spring-jcl` was replaced by an optional `commons-logging` (`requires static org.apache.commons.logging`). The root's `spring-jcl` managed entry and `spring.jcl.version` were removed; nothing else used them. Optional, so consumers see no change.
- **`persistence`, descriptor.**
  - 18 `requires static` entries, matching the optional POM dependencies; `querydsl-apt` (processor-only) is not named.
  - `opens`: the root package unconditionally (Spring XML); the six Spring packages to `spring.core, spring.beans`; `orm.jpa` to `org.hibernate.orm.core` (`JPADurable`'s `@MappedSuperclass` private fields); `orm.throng` to `org.smallmind.mongodb.throng, org.smallmind.nutsnbolts` (`ThrongDurable`'s private `@Id`, per Wave B).
  - `jakarta.transaction-api` (via Hibernate) requires `jakarta.interceptor` and `jakarta.cdi`, which are not present; `persistence` uses only `javax.transaction.xa` (`java.transaction.xa`), so it is irrelevant, but `jdeps` runs must drop that jar from the path.
- **`phalanx`.**
  - Its POM declared `spring-context` at `test` scope, which hid the compile-scope `spring-context` that `web-jersey` brings, so `org.smallmind.web.jersey`'s `requires spring.context` was unresolvable on `phalanx`'s compile path. The declaration was removed: tests still get it through `web-jersey`, and consumers never saw test scope.
  - No `phalanx` class (main or test) imports `web-jersey` or `tools.jackson.core`, so neither is in the descriptor. **Removed from the POM (2026-10-05, owner request).**
    - `jackson-core` still arrives through `jackson-databind`.
    - Dropping `web-jersey` means 28 jars no longer reach `phalanx` consumers transitively: the Jersey stack, Hibernate Validator, the Servlet API, HttpComponents, `web-http`, `spring-context`/`spring-web`/`spring-aop`, and their support jars. `PHALANX.adoc` carries a NOTE.
    - One test uses `ClassPathXmlApplicationContext`, so the test-scoped `spring-context` removed earlier in this wave is back; nothing compile-scoped supplies it now. `phalanx` tests: 162 green. No module depends on `phalanx`.
  - `PHALANX.adoc` "Dependencies" attributed `Fault`/`FaultWrappingException` to `web-jersey` (they are in `web-json-scaffold`) and listed `scribe-ink-indigenous` as a compile dependency (it is not). **Fixed (2026-10-05, owner request).**
- **`bayeux/oumuamua/server/impl`:**
  - `requires static` `org.smallmind.web.json.doppelganger` and `jakarta.annotation` (Wave D decision), and `jakarta.websocket.client`.
  - `opens` its root package to `jakarta.xml.bind, tools.jackson.databind` for the generated `OumuamuaConfigurationOutView`.
  - Its tests use `java.net.http`, so the §3.6 `default-testCompile` arguments were added.
- **Module-path spot check** (scratch module requiring all three plus `web-json-scaffold`):
  - `hibernate.xml`, `mongo.xml`, and `mock-wire.xml` are visible through `ClassLoader.getResource`.
  - `JsonCodec` writes `OumuamuaConfigurationOutView`.
  - `JsonSignalCodec` round-trips a `ResultSignal`.
  - **Not yet exercised on a module path:** Hibernate against `JPADurable`/consumer entities, and `phalanx` proxies invoking consumer service implementations. Both belong in Stage 4.
- **Pre-existing, not fixed:** `persistence`'s shipped `schema-hibernate.xml` (documented in `PERSISTENCE.adoc`, "schema-hibernate.xml — Hibernate (Session API) Plus Schema Migration") names four classes in `org.smallmind.persistence.orm.spring.hibernate`, a package that does not exist (`AnnotationSeekingBeanFactoryPostProcessor`, `EntitySeekingSessionFactoryBean`, `FileSeekingBeanFactoryPostProcessor`, `FileSeekingFactoryBean`). Loading it can only fail. **Fixed (2026-10-05, owner request).**
  - The whole Hibernate Session-API layer it targeted (`HibernateProxySession`, `HibernateDao`, the `orm.spring.hibernate` factory beans) was removed in 2024 ("jakarta nmespce"). The file also had a literal backslash in a class name (`liquibase.spring.\SpringLiquibase`) and misspelled pool keys (`….poo`).
  - It was rebuilt on the current JPA stack: the same pool and Liquibase layout, `EntitySeekingEntityManagerFactoryBean` (key `schema`, Hibernate provider), and `JPAProxySession`. The four Hibernate properties removed in Hibernate 6 and the `hibernate.search.*` keys were dropped, and the entity manager factory has `depends-on="schemaLiquibase"`.
  - Liquibase runs on its own `DriverManagerDataSource` (see the pool bug below).
  - Verified end to end in a scratch app on in-memory H2: the shipped file imported unmodified, Liquibase migrated, Hibernate `validate` passed, the entity was found through a `@SessionSource("schema")` DAO, and a row persisted and read back through the pooled data source.
  - `PERSISTENCE.adoc`'s section was rewritten (defined beans, properties, DAO binding, the per-application-context prerequisite), and the stale `HibernateProxySession` examples in `PERSISTENCE.adoc` ("Spring Wiring For Boundaries") and `LIQUIBASE.adoc` now use `JPAProxySession`.
  - **Moved to `smallmind-liquibase` (2026-10-05, owner decision).** A file inside `persistence` could not properly reference `org.smallmind.liquibase.spring.SpringLiquibase`, because `smallmind-liquibase` depends on `persistence`, not the reverse.
    - The file now ships as `classpath:org/smallmind/liquibase/spring/schema-hibernate.xml`. `liquibase` gained optional `smallmind-quorum` and `spring-orm` so every class it names resolves.
    - Its chapter section moved from `PERSISTENCE.adoc` to `LIQUIBASE.adoc` (`[[schema-hibernate]]`), with the dependencies an application adds; `PERSISTENCE.adoc` points there. The H2 end-to-end check passes against the moved file.
    - **Same problem in `schedule/quartz`. Fixed (2026-10-05, owner request).** `quartz-liquibase.xml` (imported unconditionally by `quartz.xml`) names `SpringLiquibase` and `persistence`'s `DriverManagerDataSource`, but `schedule/quartz` declared neither module.
      - Unlike `persistence`, `schedule/quartz` can depend on `liquibase` without a cycle, so it now declares `smallmind-liquibase` and `smallmind-persistence` as optional, resource-only dependencies. The descriptor is unchanged (the Wave A `shiro-spring` exception); tests: 58 green.
      - `SCHEDULE.adoc` now says applications using `quartz.xml`/`quartz-liquibase.xml` declare both (and require both modules on a module path).
      - It also corrected the chapter's resource paths. The files load from `classpath:org/smallmind/schedule/quartz/…`, not `classpath:spring/org/smallmind/…`, because `spring/` and `liquibase/` are resource roots.
- **Pre-existing pool bugs, found while fixing the file. Fixed (2026-10-05, owner request).**
  - `AbstractPooledConnection.invoke` fired `connectionErrorOccurred` for every `SQLException` from a `Connection` method, so `PooledConnectionComponentInstance` destroyed healthy connections on ordinary errors (Liquibase's first-run `SELECT COUNT(*) FROM DATABASECHANGELOGLOCK` → "table not found" → connection killed → `The object is already closed`).
  - It also rethrew everything as the checked `PooledConnectionException`, which the proxy surfaced as `UndeclaredThrowableException`, so callers never saw an `SQLException`.
  - Now errors are rethrown unchanged (as `PooledPreparedStatement` already did). The event fires only for JDBC connection exceptions, SQLState class `08`, or when `actualConnection.isValid(...)` returns false or throws within a new validity timeout.
  - `int validityTimeoutSeconds` (passed straight to `Connection.isValid`; non-positive rejected) is threaded through `AbstractPooledConnection` → `DataSourcePooledConnection`/`XADataSourcePooledConnection` → `PooledConnectionFactory` → `OmnivorousConnectionPoolDataSource` → `DriverManagerComponentInstanceFactory`/`DataSourceComponentInstanceFactory` → `PooledConnectionComponentPoolFactory` → `PooledDataSourceFactory` → `PooledDataSourceFactoryBean` (`validityTimeoutSeconds` property, checked in `afterPropertiesSet`) and `DynamicPooledDataSourceInitializingBean` (required `jdbc.validity_timeout_seconds.<pool>`).
  - **No defaults (owner decision):** every signature takes `maxStatements` and `validityTimeoutSeconds` explicitly. The overloads that omitted either were removed, an intended API break. `schema-hibernate.xml` gained `${jdbc.max_statements.pool}` and `${jdbc.validity_timeout_seconds.pool}`. The unused `creationMilliseconds` field went with the old exception message; `PooledConnectionException` remains.
  - Tests: the old wrapping test was replaced by seven cases (statement-level error keeps the connection; SQLState `08`; connection-exception type; `isValid` false; `isValid` throws; the timeout reaches `isValid`; non-positive timeout rejected). `persistence` 617 green.
  - The H2 scenario with `SpringLiquibase` on the pooled data source now passes. `PERSISTENCE.adoc` ("Connection Pool") documents the rule, the timeout, where to set it, and a NOTE on the changed exception type.

### Wave H — depth 7

| Module | `requires` | `opens` / `provides` / notes |
|---|---|---|
| `org.smallmind.liquibase` | `org.smallmind.nutsnbolts`, `org.smallmind.persistence`, `liquibase.core`, `java.sql`, `java.logging`; *static* `spring.beans` | **No** `provides` (§3.5). `opens org.smallmind.liquibase.spring;` unconditionally (`schema-hibernate.xml`, moved here in Wave G). The optional `smallmind-quorum` and `spring-orm` are used only by that file, so they are not in the descriptor (the Wave A `shiro-spring` exception). `LIQUIBASE.adoc`: add the modular-consumer subclass paragraph under "How Liquibase discovers the bridge". |
| `org.smallmind.web.json.query` | `org.smallmind.nutsnbolts`, `org.smallmind.persistence`, **`org.smallmind.web.json.scaffold`**, **`jakarta.persistence`**, `jakarta.validation`, **`jakarta.xml.bind`**, `tools.jackson.core`, **`tools.jackson.databind`**; *static* `com.querydsl.core`, `org.smallmind.mongodb.throng` | 25 JAXB-annotated files across all five packages: `opens` each `→ jakarta.xml.bind, tools.jackson.databind`. `WEB.adoc`. |

### Wave I — depth 8

| Module | `requires` | `opens` / `provides` / notes |
|---|---|---|
| `org.smallmind.batch.spring` | `org.smallmind.nutsnbolts`, `org.smallmind.scribe.pen`, `org.smallmind.batch.base`, `org.smallmind.liquibase`, `spring.beans`, `spring.context`, `spring.core`, `spring.batch.core`, `java.sql` | `opens org.smallmind.batch.spring;` (Spring XML + Liquibase changelog). `BATCH.adoc`. |

### 6.1 Descriptor sketch (shape, not final content)

For calibration — `scribe/pen`, because it exercises `uses`, `requires static`, Spring `opens`, and an unnamed optional module in one small file:

```java
module org.smallmind.scribe.pen {

  requires transitive org.smallmind.nutsnbolts;

  requires static jakarta.activation;
  requires static jakarta.xml.bind;
  requires static jackson.dataformat.msgpack;
  requires static org.aspectj.runtime;
  requires static software.amazon.awssdk.auth;
  requires static software.amazon.awssdk.awscore;
  requires static software.amazon.awssdk.regions;
  requires static software.amazon.awssdk.services.cloudwatchlogs;
  requires static spring.beans;
  requires static syslog4j;
  requires static tools.jackson.core;
  requires static tools.jackson.databind;

  exports org.smallmind.scribe.pen;
  exports org.smallmind.scribe.pen.adapter;
  exports org.smallmind.scribe.pen.fluentbit;
  exports org.smallmind.scribe.pen.probe;
  exports org.smallmind.scribe.pen.spring;
  exports org.smallmind.scribe.pen.spring.plan;

  opens org.smallmind.scribe.pen to spring.core, spring.beans;
  opens org.smallmind.scribe.pen.spring to spring.core, spring.beans;
  opens org.smallmind.scribe.pen.spring.plan to spring.core, spring.beans;

  uses org.smallmind.scribe.pen.adapter.LoggingBlueprint;
}
```

The package list above is illustrative — take it from the tree. Place `module-info.java` at `src/main/java/module-info.java` with the same copyright prologue as every other source file (`CODE_STYLE.md`). Order clauses `requires` → `requires static` → `exports` → `opens` → `uses`/`provides`, alphabetical within each group; one blank line between groups. No comments inside the descriptor — the chapter explains the why.

## 7. Stage 3 — Documentation Pass

Done per wave, but listed here so nothing is forgotten:

- Every chapter: module name; `requires` line in the quick-start; which dependencies are `requires static` and the §3.4 "present but unresolved" warning; which packages are `opens` and why; what the **consumer** must `opens` (to `org.smallmind.nutsnbolts`, `org.smallmind.mongodb.throng`, `org.hibernate.orm.core`, `org.quartz`, HK2); troubleshooting entries for unnamed-module names and third-party split packages that affect the chapter's dependencies.
- `README.adoc`: module index with names; a short repository-wide section on module-path vs classpath usage (all-or-nothing, automatic modules, `--add-modules` for optional modules), linked from each chapter rather than repeated.
- `DOPPELGANGER.adoc`: the `requires` a consumer needs for generated code.
- `LIQUIBASE.adoc`: the subclass-and-`provides` paragraph (§3.5).
- `TESTBENCH.adoc`: tests on the classpath are the supported configuration.
- `WEB.adoc`: the §5.5 consumer rules (resource and entity `opens`, `jersey-hk2` with `aopalliance-repackaged` excluded, the two `--add-exports`, `--add-modules` for explicit modules behind automatic ones); `jersey-spring6` is classpath-only, with the blocker and the re-check procedure in troubleshooting (`web-grizzly` was too, until the Wave E follow-up). Also document `jersey-hk2` as a required consumer dependency for classpath use — it is missing today. `CLAXON.adoc`: `claxon-http` needs no `opens`; its host follows the `WEB.adoc` rules.
- `CODE_STYLE.md`: a short "Module descriptors" subsection — file location, prologue, clause order, the `exports`-everything rule, `requires static` ≡ optional, no `transitive` on automatic modules.
- `DESIGN_PHILOSOPHY.md`: one paragraph — the descriptor is part of the module contract; `requires static` is how optional integrations are expressed; `opens` is granted to named frameworks, never `open module`.

Follow `ASCIIDOC_DOC_AUTHORING_GUIDE.md` for where these land within each chapter's existing section flow.

## 8. Stage 4 — Module-Path Verification

Tests run on the classpath (§3.6), so the module path needs a dedicated check once all waves are in:

1. **`jdeps --check`** on every jar with its full dependency module path (`dependency:build-classpath` as in §5.4). It reports unused `requires`, missing `requires transitive`, and unresolvable names. Fix until clean.
2. **Resolution smoke test.** A throwaway `main` (not committed, or committed under `testbench` only if the owner wants it) that runs with `--module-path <all smallmind jars + deps> --add-modules ALL-MODULE-PATH` and touches: `LoggingBlueprintFactory` (service lookup through `uses`/`provides` with `scribe-ink-indigenous` present), `EphemeralFileSystemProvider` via `FileSystems.newFileSystem`, a Spring `ClassPathXmlApplicationContext` over `classpath:org/smallmind/persistence/hibernate.xml` (encapsulated-resource open), one JAXB round-trip of a `web/json/scaffold` type, one `ScribeSLF4JServiceProvider` lookup via `LoggerFactory`. Include the web stack by re-running the §5.5 spike harness (Jetty, `web/jersey` providers, `claxon/http`) against the real descriptors; include `web/grizzly` with a SOAP endpoint and `--add-reads com.sun.xml.ws=grizzly.http.server.jaxws` (Wave E follow-up), and exercise a Tyrus WebSocket endpoint, which no harness has covered yet.
3. **Negative check.** Run the same with `hibernate-core` on the module path but **without** `--add-modules org.hibernate.orm.core` and confirm the failure is the one documented in §3.4 — then confirm the chapter text matches the actual exception.
4. **Classpath regression.** The full `mvn install` test suite is this check; nothing should have changed for classpath consumers.

## 9. Verification Checklist (per wave and at the end)

- [ ] `mvn -pl <wave modules> -am install` green, tests included.
- [ ] `-Xlint:exports,module` output reviewed; every `requires transitive` added has a signature that justifies it.
- [ ] `jar --describe-module --file <jar>` shows the §4.2 name, **not** `automatic`, for every wave module; still `automatic` for the §3.7 set.
- [ ] `jdeps --check <jar>` clean.
- [ ] Every `<optional>true</optional>` in the POM has a matching `requires static`, and vice versa; every `requires` names an artifact present in the POM (directly, not transitively).
- [ ] Every `META-INF/services` file has a matching `provides`; every `ServiceLoader.load` has a matching `uses`.
- [ ] Every Spring XML / `.properties` / changelog in a package directory has an unconditional `opens` for that package.
- [ ] Chapter updated; `README.adoc` index updated.
- [ ] No file outside the touched modules reformatted.

## 10. Rollback

Each stage is independently reversible:

- Stage 2/3: delete the wave's `module-info.java` files and revert the chapter edits. Jars drop back to automatic modules under the Stage 0 names — consumers who wrote `requires org.smallmind.x;` keep compiling.
- Stage 1: revert the three POM edits (`useModulePath`, `compilerArgs`, AspectJ `excludes`) and the two `package-info.java` files.
- Stage 0: never roll back. Removing an `Automatic-Module-Name` after it has been published breaks every consumer who used it.

## 11. Out Of Scope

- Narrowing `exports` or introducing an `internal` package convention.
- `jlink`/`jpackage` images, multi-release jars, `open module` declarations.
- Splitting `nutsnbolts` (46 packages, 19 optional dependencies) into smaller modules — tempting once the descriptor makes the coupling visible, but a separate design conversation under `DESIGN_PHILOSOPHY.md`.
- Running the test suite on the module path.
- Making the four Maven plugins or the two Maven-API consumers (§3.7) into named modules.
- Changing which dependencies are optional (except the `aspectjrt` question in §5.7, which this document only asks).
