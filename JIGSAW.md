# Jigsaw: Giving Every Artifact A Module Descriptor

This document is a work order for a later session (or several). It describes how to take the 59 artifacts in this repository from "plain jars with no module identity" to "explicitly named Java Platform Module System (JPMS) modules with `module-info.class`", in stages that each leave the build green and the published artifacts usable.

Read `AGENTS.md`, `CODE_STYLE.md`, and `DESIGN_PHILOSOPHY.md` before starting. Everything below assumes that style: explicit wiring, caller-owned lifecycle, optional integrations kept optional, and documentation kept in sync with code. A module descriptor is a *public contract*; treat every `exports`, `opens`, `requires static`, and `provides` as API.

Line numbers and version strings cited here were accurate when this document was written (`7.4.0-SNAPSHOT`, Maven 3.9.14, JDK 25, `maven-compiler-plugin` 3.14.1, `maven-surefire-plugin` 3.5.4, AspectJ 1.9.25.1 via `dev.aspectj:aspectj-maven-plugin` 1.14.1) and will drift. Re-verify before editing.

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
| 1 | `ansible`, `scribe/pen`, `memcached/utility`, `mongodb/throng`, `bayeux/oumuamua/server/api`, `file/ephemeral`, `file/jailed`, `mongodb/utility`, `web/http`, `schedule/base`, `sleuth/runner`, `spark/tanukisoft/integration`, `testbench/foundation`, `testbench/style`, `spark/singularity/mojo`†, `spark/tanukisoft/mojo`† |
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
| `persistence` | `spring/…/persistence/{hibernate,mongo,schema-hibernate}.xml` | `org.smallmind.persistence` | yes |
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
| Throng (this project's MongoDB mapper) | `mongodb/throng` reads consumer classes annotated with *its own* `@Entity`/`@Embedded`/`@Id` | consumers open entity packages **to `org.smallmind.mongodb.throng`** — document in `MONGODB.adoc` |
| `nutsnbolts.reflection` (bean utilities, `setAccessible`) | reflects over caller-supplied classes | consumers open the relevant packages **to `org.smallmind.nutsnbolts`** — document in `NUTSNBOLTS.adoc` |
| Quartz | instantiates job classes | consumers open job packages to `org.quartz` |
| Jersey / HK2 | `web/jersey`, `web/grizzly`, `web/jetty`, `claxon/http` resource and provider classes | HK2 instantiates resources reflectively; on a module path resource packages typically need `opens … to org.glassfish.hk2.locator, org.glassfish.hk2.utilities` or must be exported. **Spike in Stage 1 (§5.5).** |
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
- **Split packages among third-party jars** that could matter to a consumer who assembles a module path: `jakarta.mail-api` vs `com.sun.mail:jakarta.mail` (test-only here), `commons-logging` vs `spring-jcl` (`scribe/apache` requires the former, `persistence` optionally the latter — no single module requires both, but a consumer might), `grizzly-http-servlet-server` vs the individual Grizzly jars (test-only in `bayeux/…/impl`), and the Maven core jars among themselves (plugin modules only). None affects the descriptors in this repository; all belong in the troubleshooting sections of the affected chapters.

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

The shipped jars are modular; the tests exercise them as classpath consumers, which is also the dominant real-world consumer. Module-path verification is done once, explicitly, in §8 — not in every module's unit tests.

### 3.7 Modules that stay automatic-only

These get a Stage 0 `Automatic-Module-Name` and **never** a descriptor:

- The four `maven-plugin` artifacts (`license`, `spark/singularity/mojo`, `spark/tanukisoft/mojo`, `web/schema`). Maven loads plugins through Plexus class realms on a classpath; a descriptor gains nothing and the Maven core jars they compile against split packages among themselves (§2.8).
- `artifact/maven` and `sleuth/maven/surefire`: same reason — they consume Maven/Surefire internals that are unnamed jars with split packages.
- Candidates to leave automatic by choice: `spark/tanukisoft/integration` (its only external dependency is Tanuki `wrapper`, an unnamed jar whose derived name `wrapper` is as fragile as names get). The plan below gives it a descriptor with `requires wrapper;` and flags the fragility; if that is unpalatable, demote it to this list.

### 3.8 Resource-only packages and encapsulated resources

- Packages that contain resources consumers load by `classpath:` path (§2.3) get an **unconditional** `opens`. Spring would need the open anyway, and relocating the files would break documented paths.
- javac refuses `opens`/`exports` of a package that contains no compiled class ("package is empty or does not exist"). The two legitimate resource-only packages therefore each gain a `package-info.java` (with the standard copyright prologue and a one-line Javadoc saying the package exists to carry resources). Affected: `org.smallmind.nutsnbolts.examples`, `org.smallmind.testbench.foundation`.
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

### 4.1 Build change

Root `pom.xml`:

1. Add `<maven.jar.plugin.version>` to `<properties>` (pin a current 3.4.x; check Maven Central).
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
| `spark/tanukisoft/integration` | `spark-tanukisoft-integration` | `org.smallmind.spark.tanukisoft.integration` | yes (B), see §3.7 |
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
for j in $(find . -path '*/target/*.jar' -not -name '*-sources.jar' -not -name '*-tests.jar'); do
  printf '%-70s ' "$j"; jar --describe-module --file "$j" | head -1
done
```

Every line must show `<name>@7.4.0-SNAPSHOT automatic`; a literal `${jigsaw.module.name}` or a filename-derived name means a POM was missed.

### 4.4 Documentation

- `README.adoc` module index: add the module name beside each artifact.
- Each chapter's artifact-selection section gains one line: "Module name (for `requires`): `org.smallmind.…`". Follow `ASCIIDOC_DOC_AUTHORING_GUIDE.md` for placement; do not add a new top-level section just for this.

## 5. Stage 1 — Build Plumbing And Spikes (no descriptors committed)

### 5.1 Surefire

Root `pluginManagement`, `maven-surefire-plugin` `<configuration>`: add `<useModulePath>false</useModulePath>` next to the existing `useSystemClassLoader=false`. Run the full test suite; nothing should change (no descriptors exist yet). This isolates any surefire surprises from the descriptor work.

### 5.2 Compiler lint

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

### 5.4 `jdeps` dry run

For each module after a full install, generate a candidate descriptor and keep it as scratch input for Stage 2 (do not commit `jdeps` output):

```sh
mvn -q dependency:build-classpath -Dmdep.outputFile=target/cp.txt -pl <module>
jdeps --multi-release 25 --module-path "$(cat <module>/target/cp.txt)" \
      --generate-module-info <module>/target/jdeps <module>/target/<artifact>.jar
```

`jdeps` names the module after the jar; rename in your head. Its `requires` list is the true owner-of-package set (§3.3) and will include things not in the POM. Its `requires transitive` guesses are usually too generous; filter with `-Xlint:exports` instead.

### 5.5 Jersey/HK2 spike

On a scratch branch, give `web/jersey` and `web/grizzly` draft descriptors from `jdeps`, then run `web/grizzly`'s existing server tests **on the module path** once (temporarily `useModulePath=true` for that module) to learn exactly which `opens` HK2 demands for resource and provider classes. Record the required targets; they go into `web/jersey`, `web/grizzly`, `web/jetty`, `claxon/http`, and into `WEB.adoc` as consumer guidance for resource classes. Then restore `useModulePath=false`.

### 5.6 Resource-only packages

- Add `package-info.java` to `nutsnbolts/src/main/java/org/smallmind/nutsnbolts/examples/` and `testbench/foundation/src/main/java/org/smallmind/testbench/foundation/` (creating `src/main/java` for the latter; it is currently resources-only). Standard copyright prologue; Javadoc: "Carrier package for the resources shipped under this path; it contains no classes."

### 5.7 Decide the `aspectjrt` optionality question

Root POM: `aspectjrt` is `compile` + `optional=true` for all 59 modules. Woven modules (`nutsnbolts`, `persistence`, `web/jersey`, `claxon/registry`) will throw `NoClassDefFoundError` at runtime without it, which the chapters presumably document as "add `aspectjrt` if you use the aspects". The descriptor will say `requires static org.aspectj.runtime;` to mirror the POM. If the owner prefers honesty over mirroring, make `aspectjrt` non-optional in the woven modules' POMs and use plain `requires` — but that is a dependency-graph change for consumers and belongs in its own commit.

## 6. Stage 2 — Descriptors, Wave By Wave

Work leaf-first. Each wave: write the descriptors, build with `mvn -pl <modules> -am install` (tests on), run `-Xlint` clean, run `jar --describe-module` on each jar, update the chapter, commit. A later wave can `requires` an earlier wave's module; nothing in an earlier wave is touched again except to add a `requires transitive` discovered by lint.

Conventions in the tables: **bold** = `requires transitive` candidate (confirm with lint); *italic* = `requires static`; `→ X` after `opens` = `opens … to X`.

### Wave A — depth 0

| Module | `requires` (JDK / third-party) | `opens` / `provides` / notes |
|---|---|---|
| `org.smallmind.nutsnbolts` | `java.compiler`, `java.management`, `java.naming`, `java.rmi`, `java.xml`; all third-party are *static*: `org.apache.commons.net` (verify name), `jakarta.activation`, `jakarta.mail`, `jakarta.servlet`, `jakarta.validation`, `jakarta.xml.bind`, `jakarta.xml.soap`, `jakarta.xml.ws`, `org.apache.shiro.spring` (+ `org.apache.shiro.core` — `jdeps` will show it), `org.bouncycastle.provider`, `org.bouncycastle.pkix`, `freemarker`, `org.objectweb.asm`, `org.objectweb.asm.util`, `spring.beans`, `spring.context`, `spring.core`, `spring.web`, `org.yaml.snakeyaml`, `org.aspectj.runtime` | `opens org.smallmind.nutsnbolts.examples;` (unconditional, after §5.6). `opens` to `spring.core, spring.beans` for `nutsnbolts.spring`, `nutsnbolts.spring.jmx`, `nutsnbolts.spring.property`, `nutsnbolts.spring.remote`, `nutsnbolts.spring.web`, `nutsnbolts.security.spring`, `nutsnbolts.namespace.shiro.realm.spring`, `nutsnbolts.resource`. `opens org.smallmind.nutsnbolts.json to jakarta.xml.bind;`. Many exported signatures will reference optional types (e.g. `jakarta.servlet` in `lang.web`) — use `requires static transitive` where lint demands it. Chapter: `NUTSNBOLTS.adoc` — add the "consumers must open packages to `org.smallmind.nutsnbolts` for `reflection.*` utilities" rule and the §3.4 warning. 46 packages: generate the `exports` list from the directory tree, do not type it. |
| `org.smallmind.batch.base` | none | trivial. `BATCH.adoc`. |
| `org.smallmind.spark.singularity.boot` | none | The boot launcher builds its own class loader for nested jars at runtime; everything it loads runs in the unnamed module and is unaffected. `SPARK.adoc`. |

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
| `org.smallmind.spark.tanukisoft.integration` | `org.smallmind.nutsnbolts`, `spring.context`, `wrapper` (unnamed, fragile — §3.7) | `SPARK.adoc` troubleshooting: the Tanuki jar's derived name. |
| `org.smallmind.testbench.foundation` | `org.smallmind.nutsnbolts` (declared; no code uses it — consider dropping the POM dependency) | `opens org.smallmind.testbench.foundation;` after §5.6; **no** `exports`. `TESTBENCH.adoc`. |
| `org.smallmind.testbench.style` | `org.smallmind.nutsnbolts`, `java.xml`, `jdk.jdi` | `opens org.smallmind.testbench.style;` only if the XSLT is loaded through a `ClassLoader` (§3.8). `TESTBENCH.adoc`. |

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

### Wave D — depth 3

| Module | `requires` | `opens` / `provides` / notes |
|---|---|---|
| `org.smallmind.web.json.doppelganger` | `org.smallmind.nutsnbolts`, `org.smallmind.web.json.scaffold`, `java.compiler`, `jakarta.annotation`, `jakarta.validation`, `jakarta.xml.bind`, `tools.jackson.databind`; *static* `com.google.auto.service` (annotations only). `auto-service` (the processor) belongs on the processor path, not in `requires` — change its POM scope to `provided` if it is currently `compile`. | `provides javax.annotation.processing.Processor with org.smallmind.web.json.doppelganger.DoppelgangerAnnotationProcessor;`. Generated code in **consumer** modules references scaffold, Jackson, JAXB types — `DOPPELGANGER.adoc` must list the `requires` a consuming module needs. |
| `org.smallmind.testbench.condition` | `org.smallmind.nutsnbolts`, `org.smallmind.testbench.docker`, `com.github.dockerjava`, `com.rabbitmq.client` | `TESTBENCH.adoc` |
| `org.smallmind.web.jersey` | `org.smallmind.nutsnbolts`, `org.smallmind.scribe.pen`, `org.smallmind.web.http`, **`org.smallmind.web.json.scaffold`**, **`jakarta.inject`**, `jakarta.servlet`, **`jakarta.validation`**, **`jakarta.ws.rs`**, **`jakarta.xml.bind`**, `org.glassfish.jersey.core.client`, `org.glassfish.jersey.core.common`, **`org.glassfish.jersey.core.server`**, `org.glassfish.jersey.ext.bean.validation`, `org.glassfish.jersey.media.multipart`, `org.hibernate.validator`, `spring.beans`, `spring.context`, `spring.web`, `tools.jackson.core`; *static* `org.glassfish.jersey.ext.spring6`, `org.aspectj.runtime`. | `opens` `web.jersey.json`, `web.jersey.proxy` `→ jakarta.xml.bind`; `web.jersey.spring`, `web.jersey.proxy.spring`, `web.jersey.cors.spring` `→ spring.core, spring.beans`; HK2 targets per §5.5. `WEB.adoc`. |
| `org.smallmind.web.jwt` | `org.smallmind.nutsnbolts`, `org.smallmind.web.json.scaffold`, `org.jose4j`, `tools.jackson.databind` | `WEB.adoc` |

### Wave E — depth 4

| Module | `requires` | `opens` / `provides` / notes |
|---|---|---|
| `org.smallmind.claxon.registry` | `org.smallmind.nutsnbolts`, `org.smallmind.scribe.pen`, `org.smallmind.web.json.scaffold`, `org.smallmind.web.json.doppelganger` (compile dependency today; check whether generated code needs it at runtime or only the processor does), `jakarta.annotation`, `jakarta.xml.bind`, `HdrHistogram` (unnamed; the capitalized derived name is legal), `spring.beans`, `tools.jackson.databind`; *static* `org.aspectj.runtime` | `opens org.smallmind.claxon.registry;` (Spring XML) and the remaining registry packages `→ spring.core, spring.beans`. `CLAXON.adoc`. |
| `org.smallmind.testbench.groundwater` | `org.smallmind.testbench.condition`, `org.smallmind.testbench.docker`, `org.testng` | `TESTBENCH.adoc` |
| `org.smallmind.web.grizzly` | `org.smallmind.nutsnbolts`, `org.smallmind.scribe.pen`, `org.smallmind.web.jersey`, `jakarta.servlet`, `jakarta.websocket`, `jakarta.websocket.client`, `jakarta.xml.bind`, `org.glassfish.grizzly`, `org.glassfish.grizzly.http`, `org.glassfish.grizzly.http.server`, `org.glassfish.grizzly.servlet`, `org.glassfish.grizzly.http2`, `grizzly.http.server.jaxws` (unnamed), `org.glassfish.jersey.container.servlet`, `org.glassfish.jersey.core.server`, `org.glassfish.tyrus.core`, `org.glassfish.tyrus.server`, `org.glassfish.tyrus.spi`, `org.glassfish.tyrus.container.grizzly.client`, `org.glassfish.tyrus.container.grizzly.server`, `spring.beans`, `spring.context`, `spring.web` | `opens org.smallmind.web.grizzly;` (Spring XML). HK2 targets per §5.5. `WEB.adoc`. |
| `org.smallmind.web.jetty` | `org.smallmind.nutsnbolts`, `org.smallmind.scribe.pen`, `org.smallmind.web.jersey`, `jakarta.servlet`, `jakarta.websocket`, `jakarta.websocket.client`, `jakarta.xml.ws`, `org.eclipse.jetty.http`, `org.eclipse.jetty.http.spi`, `org.eclipse.jetty.server`, `org.eclipse.jetty.util`, `org.eclipse.jetty.ee10.servlet`, `org.eclipse.jetty.ee10.websocket.jakarta.server`, `org.glassfish.jersey.container.servlet`, `org.glassfish.jersey.core.server`, `spring.beans`, `spring.context`, `spring.web`, **`jdk.httpserver`** | `WEB.adoc` |

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
| `org.smallmind.claxon.http` | `org.smallmind.claxon.registry`, `jakarta.ws.rs` | One JAX-RS resource; HK2 `opens` per §5.5. All of wave F's claxon modules: `CLAXON.adoc`. |

### Wave G — depth 6

| Module | `requires` | `opens` / `provides` / notes |
|---|---|---|
| `org.smallmind.persistence` | `org.smallmind.nutsnbolts`, `org.smallmind.scribe.pen`, `java.sql`, `java.logging`, `java.transaction.xa`; *static*: `org.smallmind.claxon.registry`, `org.smallmind.memcached.utility`, `org.smallmind.mongodb.throng`, `org.smallmind.quorum`, `org.smallmind.testbench.condition`, `com.querydsl.core`, `com.querydsl.jpa`, `jakarta.persistence`, `org.hibernate.orm.core`, `org.mongodb.bson`, `org.mongodb.driver.core`, `org.mongodb.driver.sync.client`, `spring.beans`, `spring.context`, `spring.core`, `spring.jcl`, `spring.orm`, `org.aspectj.runtime`. `querydsl-apt` is a processor — `provided`/processor path, not `requires`. | `opens org.smallmind.persistence;` (Spring XML). `opens` `→ spring.core, spring.beans` for `persistence.orm`, `persistence.orm.spring`, `persistence.orm.spring.jpa`, `persistence.orm.spring.throng`, `persistence.sql.pool.spring`, `persistence.cache.memcached.spring`. `opens` entity-bearing packages `→ org.hibernate.orm.core` (3 files with JPA annotations; find with `grep -l '@Entity\|@MappedSuperclass'`). `hibernate-enhance-maven-plugin` unaffected. `PERSISTENCE.adoc`: consumers open entity packages to `org.hibernate.orm.core`; the §3.4 warning for every one of the 14 optional modules. |
| `org.smallmind.bayeux.oumuamua.server.impl` | `org.smallmind.nutsnbolts`, `org.smallmind.scribe.pen`, `org.smallmind.bayeux.oumuamua.server.api`, `org.smallmind.bayeux.oumuamua.server.spi`, `org.smallmind.web.json.doppelganger` (see registry note), `jakarta.servlet`; *static* `jakarta.websocket.client` | `BAYEUX.adoc` |
| `org.smallmind.phalanx` | `org.smallmind.nutsnbolts`, `org.smallmind.scribe.pen`, `org.smallmind.claxon.registry`, `org.smallmind.quorum`, `org.smallmind.web.jersey`, `org.smallmind.web.json.scaffold`, **`jakarta.messaging`**, `jakarta.ws.rs`, `jakarta.xml.bind`, `spring.beans`, `tools.jackson.core`, `tools.jackson.databind`, `java.management`, `java.naming`; *static*: `org.smallmind.kafka.utility`, `com.rabbitmq.client`, `kafka.clients` | `opens org.smallmind.phalanx.wire;` (Spring XML); `opens org.smallmind.phalanx.wire.signal to jakarta.xml.bind, tools.jackson.databind;`; `phalanx.wire.spring`, `phalanx.wire.transport.amqp.rabbitmq.spring` `→ spring.core, spring.beans`. `PHALANX.adoc`. |

### Wave H — depth 7

| Module | `requires` | `opens` / `provides` / notes |
|---|---|---|
| `org.smallmind.liquibase` | `org.smallmind.nutsnbolts`, `org.smallmind.persistence`, `liquibase.core`, `java.sql`, `java.logging`; *static* `spring.beans` | **No** `provides` (§3.5). `LIQUIBASE.adoc`: add the modular-consumer subclass paragraph under "How Liquibase discovers the bridge". |
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
- `CODE_STYLE.md`: a short "Module descriptors" subsection — file location, prologue, clause order, the `exports`-everything rule, `requires static` ≡ optional, no `transitive` on automatic modules.
- `DESIGN_PHILOSOPHY.md`: one paragraph — the descriptor is part of the module contract; `requires static` is how optional integrations are expressed; `opens` is granted to named frameworks, never `open module`.

Follow `ASCIIDOC_DOC_AUTHORING_GUIDE.md` for where these land within each chapter's existing section flow.

## 8. Stage 4 — Module-Path Verification

Tests run on the classpath (§3.6), so the module path needs a dedicated check once all waves are in:

1. **`jdeps --check`** on every jar with its full dependency module path (`dependency:build-classpath` as in §5.4). It reports unused `requires`, missing `requires transitive`, and unresolvable names. Fix until clean.
2. **Resolution smoke test.** A throwaway `main` (not committed, or committed under `testbench` only if the owner wants it) that runs with `--module-path <all smallmind jars + deps> --add-modules ALL-MODULE-PATH` and touches: `LoggingBlueprintFactory` (service lookup through `uses`/`provides` with `scribe-ink-indigenous` present), `EphemeralFileSystemProvider` via `FileSystems.newFileSystem`, a Spring `ClassPathXmlApplicationContext` over `classpath:org/smallmind/persistence/hibernate.xml` (encapsulated-resource open), one JAXB round-trip of a `web/json/scaffold` type, one `ScribeSLF4JServiceProvider` lookup via `LoggerFactory`.
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
