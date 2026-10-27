# Scope Upgrade: Migrating From Thread-Locals To `ScopedValue`

> **Status (decision recorded).** The end state of this work is **coexistence, chosen by the client**, not replacement. `PerApplicationContext` and `PerApplicationScope` both stay, bridged for library lookups by the neutral `*Manager` classes (§6.2); `ContextFactory` and `ScopeFactory` both stay, with `WireContext` / `WireScope` as their wire-level counterparts (§8). Nothing in the `context` package is to be deprecated or removed. Sections below that describe cutting over, deprecating, or deleting the thread-local types are retained as the history of how the scope side was built; where they conflict with this note, this note wins.

This document began as a work order for moving the repository off the two `InheritableThreadLocal`-backed utilities in `smallmind-nutsnbolts` — `context.PerApplicationContext` and `context.ContextFactory` — onto their `ScopedValue`-backed counterparts in `org.smallmind.nutsnbolts.scope`, which exist, are tested, and are documented in `NUTSNBOLTS.adoc` (`pkg-scope`). Per the status note above, that move was not taken; the sections below record what was built and decided along the way, and the passages marked **History** describe the plan as it stood before the coexistence decision.

Read `AGENTS.md`, `CODE_STYLE.md`, and the `pkg-scope` section of `NUTSNBOLTS.adoc` before starting. Everything below assumes that style: explicit wiring, caller-owned lifecycle, no hidden inheritance, documentation kept in sync with code.

Line numbers cited here were accurate when this document was written and will drift. Re-grep before editing.

---

## 1. Why This Migration Exists

Both thread-local utilities share the same defects:

- **Set-and-leave.** A binding installed on a thread stays there until something replaces it. On a pooled thread (servlet container, executor) that means stale application data or a forgotten `popContext` outlives the work that installed it.
- **Implicit inheritance.** `InheritableThreadLocal` copies bindings into every `new Thread` created from a bound thread. That is convenient and invisible, which is exactly the combination `DESIGN_PHILOSOPHY.md` warns against — nobody can tell from a call site which pools depend on it.
- **Virtual-thread cost.** Every virtual thread copies the inheritable map at creation.

`ScopedValue` (final in JDK 25; the build targets `release 25`) fixes all three by construction. A binding exists only for the dynamic extent of a `run`/`call` and is never inherited by threads the operation starts. The price is that every binding must be expressed as "run this operation inside the scope" and every thread that needs the binding must be handed it explicitly.

## 2. What Already Exists

Package `org.smallmind.nutsnbolts.scope` (module `nutsnbolts`):

| Type | Replaces | Notes |
|---|---|---|
| `Scope` | `context.Context` | Marker, `Serializable`, concrete class is the key |
| `ScopeFactory` | `context.ContextFactory` | `callInScope(s)`, `runInScope(s)`, `getScope`, `filterScopesOn`, `snapshot` |
| `ScopeSnapshot` | `InheritableThreadLocal` inheritance | Immutable capture; `run`/`call` on any thread |
| `ScopeLifecycleAware` | `context.ContextLifecycleAware` | `beforeEnter` / `afterExit` around the bound operation; the old hook fires only on explicit `pushContext` / `popContext` |
| `ExpectedScopes` | `context.ExpectedContexts` | Same shape |
| `ScopeException` | `context.ContextException` | Same constructors |
| `PerApplicationScope` | `context.PerApplicationContext` | Instance owns the shared map; `run`/`call` bind it (since 2026-10-26 the `ScopedValue` holds the instance itself, not just its map); static `setPerApplicationData`/`getPerApplicationData`/`setPerApplicationDataIfAbsent`, `generateCarrier`, `wrapThreadFactory`, `wrapExecutor` (the context type now has all three propagation statics too); static `getBound`/`resolve` (2026-10-26; rows below) |
| `PerApplicationScope.ApplicationCarrier` | `PerApplicationContext.ContextCarrier` | `run`/`call` instead of `prepareThread` |
| `PerApplicationScope.isBound()` (static) | `PerApplicationContext.isBound()` (static, added to the old type alongside) | `true` when any scope is bound on the calling thread; the bridging managers (§6.2) test both accessors to decide which registries to write |
| `PerApplicationScope.isBoundOnCurrentThread()` | — | `true` when this instance's map is the one bound on the calling thread; ~~the Grizzly/Jetty beans use it to reject a refresh bound to a different scope~~ (2026-10-26: that rejection now comes from `resolve`, and no main-code caller remains) |
| `PerApplicationScope.getBound()` (static) | — | Added 2026-10-26. The scope instance bound on the calling thread, or `null`. This is the accessor §6.1 item 3 once ruled out; the dated note there records why the rule was reversed. In Spring XML, `<bean class="…PerApplicationScope" factory-method="getBound"/>` declares the instance enclosing the refresh as a bean (`NUTSNBOLTS.adoc` `per-application-scope-bean`; `PerApplicationScopeBeanTest` pins the bound and unbound outcomes) |
| `PerApplicationScope.resolve(handed)` (static) | — | Added 2026-10-26. The one rule every consumer that accepts a scope applies, exactly once, on the thread that constructs or starts it: a handed scope is returned when nothing is bound (a real override) or when the bound scope is that same instance; a handed scope that differs from the bound one throws `ScopeException`; `null` returns the bound scope, or `null` when nothing is bound. The consumer stores the result and reads only that |
| `MissingPerApplicationScopeException` | `lang.MissingPerApplicationContextException` | Same constructor |

`PerApplicationScope` reuses `lang.PerApplicationDataManager` as its key type on purpose, so managers do not need an `implements` change.

Tests: `nutsnbolts/src/test/java/org/smallmind/nutsnbolts/scope/ScopeFactoryTest.java` and `PerApplicationScopeTest.java`.

The old types stay; several callers (`WebContextLoaderListener`, the Grizzly/Jetty beans) were revised to work alongside the scope types — see §6.4. Nothing is deprecated.

## 3. Semantic Differences You Must Design Around

Internalize these before touching a call site.

### 3.1 No push/pop, no install

There is no `set`. `pushContext(x)` … `popContext(X.class)` becomes `callInScope(x, () -> …)`. `new PerApplicationContext()` (which bound the constructing thread as a side effect) and `prepareThread()` become `perApplicationScope.run(() -> …)`. Anything that binds in one method and expects a different method to unbind cannot be ported as-is; it must be restructured so the whole bound extent is one lambda.

### 3.2 Nothing is inherited

A thread started inside a bound operation sees **no** binding. This is true for `new Thread`, `ExecutorService`, `CompletableFuture`, `ScheduledExecutorService`, `ForkJoinPool`, container-managed pools, and JMS/Kafka/RabbitMQ consumer threads. The only exceptions are `StructuredTaskScope` subtasks, and that API is still preview in JDK 25 — do not depend on it.

Propagation is explicit:

- `ScopeFactory.snapshot()` → `ScopeSnapshot.run/call`
- `PerApplicationScope.generateCarrier()` → `ApplicationCarrier.run/call`
- `PerApplicationScope.wrapThreadFactory(factory)` → every thread the wrapped factory creates runs its whole body inside the carrier

### 3.3 `PerApplicationScope`: the binding is scoped, the map is not

Each `PerApplicationScope` instance owns one `ConcurrentHashMap`. `run`/`call` bind a *reference* to it. Data written inside one `run` is visible inside every later or concurrent `run` on the same instance and through every carrier taken from it, including data written *after* the carrier was captured. This is what allows Spring-startup registration (`Instrument.register`, `OrmDaoContextManager.register`, …) to be seen by request threads later. Do not "fix" this into an immutable snapshot; it is the contract.

### 3.4 Fail-fast on unbound threads

`PerApplicationScope.generateCarrier()` and `wrapThreadFactory()` throw `MissingPerApplicationScopeException` when called on an unbound thread. `PerApplicationContext.generateCarrier()` silently returned a carrier around `null`. Any code that captured a carrier speculatively ("save whatever is there so I can restore it") will now throw; the test reset pattern in §7.2 is the main instance.

### 3.5 `ScopedValue.CallableOp` carries one checked exception type

```java
interface CallableOp<T, X extends Throwable> { T call() throws X; }
```

A body that throws two unrelated checked types (`IOException` and `ServletException`, say) cannot be typed precisely. See §5.1 for the pattern.

---

## 4. Inventory

### 4.1 `PerApplicationContext` — production code

**Entry points (things that bind a thread today):** (line numbers removed; see the classes)

| File | Today |
|---|---|
| `nutsnbolts/.../context/web/PerApplicationContextFilter.java` | `init` uses the instance supplied to the constructor or, for the no-arg forms, the context published under the `PerApplicationContext.class.getName()` servlet-context attribute, and throws `MissingPerApplicationContextException` otherwise; it never creates one. `doFilter` calls `prepareThread()` with no `finally` |
| `nutsnbolts/.../spring/web/WebContextLoaderListener.java` | `contextInitialized` attaches the already-published context's map to the deploy thread (`prepareThread()`) when the attribute is present; otherwise does `new PerApplicationContext()` (adopting any map already attached, or creating one) and publishes it. Either way `super.contextInitialized` then refreshes Spring on that bound thread |
| `spark/tanukisoft/integration/.../AbstractWrapperListener.java` | **`extends PerApplicationContext`** — constructing the listener binds the Tanuki main thread; everything the wrapper starts inherits (2026-10-26: the `PerApplicationScope` the listener also owns is opt-in — `spark.per.application.scope=true` in `wrapper.conf`, which the mojo writes from its `perApplicationScope` parameter — and off by default, so `getPerApplicationScope()` returns `null` and `start`/`stop`/`controlEvent` run with the context binding alone; decided because the context flavor is something an application opts into by declaring its bean, and the scope needed an equivalent switch) |
| `web/grizzly/.../GrizzlyInitializingBean.java` | Registers `new PerApplicationContextFilter(...)` on the JAX-RS path (`REQUEST` + `ASYNC`, async-supported) only when a `PerApplicationContext` is attached to the refresh thread. `setPerApplicationScope(PerApplicationScope)` registers a `PerApplicationScopeFilter` on `/*` *in addition*, and `onApplicationEvent` throws `ScopeException` when the refresh thread is bound to a different scope |
| `web/jetty/.../JettyInitializingBean.java` | Same rules as Grizzly |
| `testbench/foundation/.../foundation.xml` | `<bean id="perApplicationContext" class="org.smallmind.nutsnbolts.context.PerApplicationContext"/>` — bean construction binds the Spring bootstrap thread |

**Scope-side entry points that already exist (added as prep, side by side with the above; nothing uses them by default):**

| File | What it does |
|---|---|
| `nutsnbolts/.../scope/web/PerApplicationScopeFilter.java` | Binds a `PerApplicationScope` for the extent of `chain.doFilter` (§5.1 Option A). Takes the scope in its constructor; the no-arg form reads servlet-context attribute `PerApplicationScope.class.getName()` in `init` ~~and throws `MissingPerApplicationScopeException` if none is published~~ (2026-10-26: `init` passes the constructor scope, else the published attribute, through `PerApplicationScope.resolve`, so with neither it adopts the scope bound on the initializing thread; `MissingPerApplicationScopeException` only when all three are absent, `ScopeException` when the constructor or published scope differs from the bound one). Never creates a scope. Same `suppressConnectionClosedException` rule as the old filter. |
| `nutsnbolts/.../spring/web/PerApplicationScopeContextLoaderListener.java` | Runs `super.contextInitialized` and `super.contextDestroyed` inside `scope.run(...)`, publishes the scope under the attribute above. No-arg reuses a published scope ~~or creates one~~ (2026-10-26: else adopts the scope bound on the initializing thread, else creates one); the one-arg form publishes the given scope and throws `ScopeException` if a different one is already published (2026-10-26: or, through `PerApplicationScope.resolve`, if the constructor or published scope differs from the scope bound on the initializing thread). |

**Managers (implement `PerApplicationDataManager`, call the static set/get):**

> **Superseded.** The paragraph and table below describe the prep state before the bridging managers (§6.2). Library code no longer reads the context flavor; it reads the neutral `*Manager` bridges, and the two maps *are* bridged for lookups. Retained for history.

**Prep done:** every implementor was renamed to `*ContextManager` (a deliberate, one-time compile break for client code, so that the mechanism is visible in the name and the cut-over cannot silently change a client's thread-minting contract), and each has a `*ScopeManager` twin with the same static API resolved through `PerApplicationScope`. `Instrument` keeps its name and delegates to `ClaxonRegistryContextManager`. "Either flavor" is a **migration staging device**, not a permanent feature: library-internal readers stay on the context flavor until their module is cut over, and nothing bridges the two maps.

| Context flavor (what library code reads today) | Scope twin | Module | Data shape | Read from (main code) |
|---|---|---|---|---|
| `SpringPropertyAccessorContextManager` | `SpringPropertyAccessorScopeManager` | nutsnbolts | single value | `DynamicPooledDataSourceInitializingBean`, `DynamicClassNameTemplateInitializingBean` (Spring init — startup thread) |
| `ClaxonRegistryContextManager` (via the `Instrument` façade) | `ClaxonRegistryScopeManager` | claxon/registry | single `ClaxonRegistry` | `Instrument.with` from `InstrumentedAspect`, `Instrumentation`, `CacheAsAspect`, `CacheCoherentAspect`, `TimerAspect`, `InstrumentedLatencyListener`, and **all phalanx transports** (`RabbitMQRequestTransport`, `RequestMessageRouter`, `ResponseMessageRouter`, `InvocationWorker` ×2, `JmsRequestTransport`, `JmsResponseTransport`, `RequestListener`, `ResponseListener`) |
| `OrmDaoContextManager` | `OrmDaoScopeManager` | persistence | lazily-created nested `ConcurrentHashMap` | `RepositoryAspect`, `ByKeyRoster`, `ByKeySingularVector` |
| `SessionContextManager` | `SessionScopeManager` | persistence | lazily-created nested map | written by `ProxySession.register()`; read by application code |
| `DataSourceContextManager` | `DataSourceScopeManager` | persistence | lazily-created nested map | pooled data source lookup |
| `WireContextManager` (name unchanged; it already names `WireContext`) | `WireScopeManager` | phalanx | lazily-created nested map | `WireContextXmlAdapter` — runs on **`WorkManager` worker threads** (or mock dispatch threads) during deserialization |
| `PoolContextManager` | `PoolScopeManager` | quorum | single `Pool` | `Pool.java` |
| `JsonEntityResourceProxyContextManager` | `JsonEntityResourceProxyScopeManager` | web/jersey | lazily-created nested map | proxy lookup |
| `PolymorphicAttributeContextManager` | `PolymorphicAttributeScopeManager` (neither flavor has a static initializer any more; both `get`s fall back to the adapter default when nothing is stored) | web/json/scaffold | single `String` | `AttributedPolymorphicXmlAdapter` — JAXB marshal/unmarshal, any thread |

The managers themselves are done. The **read-from** column is now served by the bridging managers (§6.2 decision) — except `WireContextManager` / `WireScopeManager`, which have no bridge (§8) and are read directly by their own adapters — so a thread listed there needs *some* binding whose registry was written: the scope the refresh ran in, or an inherited context from a refresh that carried one.

### 4.2 `PerApplicationContext` — tests (≈45 classes)

Every test that does `new PerApplicationContext()` in `@BeforeClass`/`@BeforeMethod` to make the test thread ready. Notable patterns:

- `claxon/.../InstrumentedAspectTest`, `ClaxonSpringAssemblyTest`: `priorContext = generateCarrier(); new PerApplicationContext(); … priorContext.prepareThread()` (save/restore).
- `phalanx/.../WorkerPoolTest`: comment says worker threads forked later inherit the context — ~~that inheritance is exactly what disappears~~ (**History.** The inheritance stays; the test now covers a worker bound to a scope and one started with `null`.)
- `persistence/.../OrmDaoManagerTest`: comment notes the map is shared across test classes on the same thread and installs a fresh one per method.
- `nutsnbolts/.../PerApplicationContextTest`: `runIsolated` runs each body on a fresh thread to escape inherited state.

Full list: `grep -rln --include=*.java PerApplicationContext . | grep /src/test/`.

### 4.3 `PerApplicationContext` — documentation

`CLAXON.adoc` (143, 1118), `PERSISTENCE.adoc` (1531), `QUORUM.adoc` (91, 134, 140), `SPARK.adoc` (1920), `TESTBENCH.adoc` (386, 395, 519), `NUTSNBOLTS.adoc` (`pkg-scope` already contrasts the two).

### 4.4 `ContextFactory` — production code

> **History.** The "Replacement" column and the `implements Scope` switch below were the conversion plan; per the §8 decision none of it was done. `ContextFactory` and `ScopeFactory` both stay, and every call site below still uses `ContextFactory`.

| File | Call | Replacement (not taken; §8) |
|---|---|---|
| `phalanx/.../wire/transport/MethodInvoker.java` 155-179 | `pushContext` loop / `invoke` / `popContext` loop in `finally` | `ScopeFactory.callInScopes(contexts, () -> …)` |
| `phalanx/.../wire/WireInvocationHandler.java` 174 | `filterContextsOn(method, WireContext.class)` | `filterScopesOn(method, WireContext.class)` |
| `persistence/.../sql/pool/context/ContextualPooledDataSource.java` 86 | `getContext(PooledDataSourceContext.class)` | `getScope(...)` |
| `persistence/.../sql/pool/context/ContextualPooledXADataSource.java` 83 | same | same |
| `file/jailed/.../ContextSensitiveRootedPathTranslator.java` 124 | `getContext(RootedFileSystemContext.class)` | `getScope(...)` |

**`Context` implementors** (would have switched to `implements Scope`; they stay `Context` implementors per §8): `phalanx/.../wire/signal/WireContext` (abstract base; `ProtoWireContext` extends it), `persistence/.../PooledDataSourceContext`, `file/jailed/.../RootedFileSystemContext`.

**`ExpectedContexts` users:** only `phalanx/src/test/.../WireTestingService.java:42`.

### 4.5 `ContextFactory` — tests

`nutsnbolts/.../context/ContextFactoryTest`, `file/jailed/.../ContextSensitiveRootedPathTranslatorTest`, `persistence/.../ContextualPooledDataSourceTest`, `ContextualPooledXADataSourceTest`, `phalanx/.../MockWireTest`, `AbstractWireTransportContractTest`, `MethodInvokerTest`, `WireInvocationHandlerTest`, `WireTestingService`, `WireTestingServiceImpl`.

### 4.6 `ContextFactory` — documentation

`PHALANX.adoc` (36, 65, 104-107, 163, 630, 682-694, 770-773, 869), `FILE.adoc` (278, 1036-1041, 1137-1176, 1347-1363), `NUTSNBOLTS.adoc` (`pkg-context`, Behavioral Reference "Context inheritance", the `[NOTE]` pointing at `pkg-scope`).

---

## 5. The Two Hard Problems

Decide these before writing code. Record the decisions in the commit message and in `NUTSNBOLTS.adoc`.

### 5.1 The `CallableOp` single-exception-type wrinkle (`doFilter`)

`PerApplicationContextFilter.doFilter` must call `chain.doFilter(request, response)`, which throws `IOException` **and** `ServletException`. `PerApplicationScope.call` takes `CallableOp<T, X>` with one `X`. Options, in order of preference:

**Option A — widen to `Exception` and split the rethrow (recommended).**

```java
@Override
public void doFilter (ServletRequest request, ServletResponse response, FilterChain chain)
  throws IOException, ServletException {

  try {
    perApplicationScope.<Void, Exception>call(() -> {
      chain.doFilter(request, response);

      return null;
    });
  } catch (IOException ioException) {
    if ((!suppressConnectionClosedException) || (!"Connection is closed".equals(ioException.getMessage()))) {
      throw ioException;
    }
  } catch (ServletException servletException) {
    throw servletException;
  } catch (RuntimeException runtimeException) {
    throw runtimeException;
  } catch (Exception exception) {
    throw new ServletException(exception);
  }
}
```

The final `catch (Exception)` is unreachable in practice (the body can only throw the three types above) but the compiler needs it because `X = Exception`. Wrapping in `ServletException` is the honest fallback for a filter. Keep the existing `suppressConnectionClosedException` behavior exactly as it is today.

**Status:** implemented as written in `scope/web/PerApplicationScopeFilter.java`, with `PerApplicationScopeFilterTest` covering every branch.

**Option B — add a two-exception overload to `PerApplicationScope`.**

```java
public interface TwoExceptionOp<T, X1 extends Throwable, X2 extends Throwable> { T call () throws X1, X2; }
public <T, X1 extends Throwable, X2 extends Throwable> T call (TwoExceptionOp<? extends T, X1, X2> op) throws X1, X2
```

Implementable by catching `Throwable` inside a `CallableOp<T, RuntimeException>` and re-throwing via an unchecked-cast helper. Rejected by default: it introduces a sneaky-throw helper into `nutsnbolts` for one call site. Revisit only if a second two-exception body appears.

**Option C — use `run(Runnable)` and tunnel checked exceptions in a holder.** Strictly worse than A; do not.

The same wrinkle appears anywhere else a bound body throws two checked types. `MethodInvoker.remoteInvocation` does not have it — its body throws `IllegalAccessException` and `InvocationTargetException`, but the method already declares `throws Exception`, so `X = Exception` is natural there.

### 5.2 The mandatory-pool-wrapping decision

> **History.** The framing below ("fails with `MissingPerApplicationScopeException` on first lookup", "a missed pool fails loudly and early") predates the bridging managers. Under the bridge an un-wrapped thread falls back to the `PerApplicationContext` it inherited; with no binding at all a lookup throws `MissingPerApplicationContextException`, or, for Claxon, records nothing. ~~The loud signal is now the WARN that `ComponentPool.startup()` and `WorkManager.startUp` log when started scope-less on a thread bound to a `PerApplicationScope` with no context attached.~~ (2026-10-26: there is no WARN; an owner started with `null` on a bound thread adopts that scope — see the amendment to the decision below and §6.1 item 3.) The "Decision taken (pool pass)" at the end of this section is what was built.

With `PerApplicationContext`, any executor a Spring bean creates during startup inherits the application map for free. With `PerApplicationScope`, **every thread that reaches a manager in §4.1 must have been bound explicitly**, or it fails with `MissingPerApplicationScopeException` on first lookup. There are three ways to live with that.

**Option 1 — Mandatory explicit wrapping (recommended).**

Every pool whose threads resolve per-application data is built from `PerApplicationScope.wrapThreadFactory(...)`, or its tasks are submitted through an `ApplicationCarrier`. A missed pool fails loudly and early. This is the option consistent with `DESIGN_PHILOSOPHY.md` ("no hidden worker ownership") and is what the `pkg-scope` docs already promise.

Cost: an audit (§6.3) and a `ThreadFactory` change in each affected module. Benefit: the set of application-aware threads is greppable.

**Option 2 — Hybrid fallback inside `PerApplicationScope`.**

`requireBoundMap()` falls back to a process-wide default `PerApplicationScope` (set once by the application) when nothing is bound on the thread. Single-application JVMs — the overwhelmingly common case — need no wrapping at all. Multi-application containers must still wrap, and a missed pool silently reads the wrong application's data instead of failing.

Rejected by default: it reintroduces hidden static state and turns a loud failure into a quiet wrong answer. If it is ever adopted, make it opt-in (`PerApplicationScope.installDefault(scope)`), never automatic.

**Option 3 — Keep `PerApplicationContext` for pools, use `PerApplicationScope` for requests.**

Two sources of truth, each manager consults both. Rejected as a *pool* strategy: it doubles every manager's lookup logic and never lets the old type go. (The manager pass in §6.2 later adopted a bridging read for a different reason — supporting clients that bind both during a transition — without changing the pool decision below: pools are still wrapped explicitly, and the bridge never substitutes for wrapping.)

**Decision to record:** Option 1 unless a specific module makes wrapping infeasible, in which case document that module's exception in its `.adoc` chapter.

**Decision taken (pool pass):** Option 1, delivered as an optional `PerApplicationScope` collaborator rather than a `ThreadFactory` seam. Every pool or consumer owner takes a nullable `PerApplicationScope` (constructor overload, fluent setter, or configuration property) and runs each worker body and third-party callback through `scope.run(...)`. Chosen over the seam because (a) the RabbitMQ and JMS callbacks have no thread of ours to decorate, so a `ThreadFactory` cannot reach them while `scope.run` can; (b) the instance method binds from any thread, so construction need not happen on a bound thread; (c) it reuses the `perApplicationScope` vocabulary already on the Grizzly/Jetty/Tanuki entry points; and (d) the expectation is visible in each owner's signature rather than left to the client to remember. ~~`null` means "start with no scope, carrying only the inherited `PerApplicationContext`" — today's behavior — so nothing breaks, since the readers go through the bridge.~~ **Amended 2026-10-26.** `null` means "the root already chosen". Each owner passes what it was handed through `PerApplicationScope.resolve` exactly once — on the constructing thread when the scope is a constructor argument held in a final field (`WorkManager`, the request transports, the mock router/queue/topic, `KafkaBackbone`, `OumuamuaServer`), or on the starting thread when it is setter-injected (`ComponentPinManager.startup` for `ComponentPool`, `onApplicationEvent` for the Grizzly/Jetty beans) — and stores the result, which is the only value its threads and callbacks read. A `null` resolves to the scope bound on that thread, so an owner built during a refresh that `AbstractWrapperListener` or `PerApplicationScopeContextLoaderListener` bound runs inside that scope without being handed anything; with nothing bound either it resolves to `null` and runs unwrapped, carrying only the inherited `PerApplicationContext`, with no WARN. A handed scope on an unbound thread is a real override and is used; a handed scope that differs from the bound one throws `ScopeException` at construction or start. Resolution is never lazy and never happens on a worker thread. The WARN that stood in for this — phalanx's `BindingAdvisory` helper and the inline equivalents in quorum's `ComponentPinManager.startup` and the two web beans' `onApplicationEvent` — was removed with it. There are no scope-less overloads: every constructor or static factory that an owner exposes takes the scope (`WorkManager` has two constructors, both taking it; `ComponentPool` uses a fluent setter instead), so each construction site — Java or Spring XML — states its choice. ~~No change to `nutsnbolts`.~~ (2026-10-26: `nutsnbolts` gained `getBound` / `resolve`; §6.1 item 3.)

---

## 6. Migration Steps — `PerApplicationContext` → `PerApplicationScope`

Work module by module; each step should leave the build green. Suggested order follows the dependency graph: `nutsnbolts` → `quorum` / `claxon` / `persistence` / `phalanx` → `web/*` → `spark` / `testbench`.

### 6.1 Prerequisites (nutsnbolts)

1. Confirm `mvn -q -pl nutsnbolts test -Dtest='ScopeFactoryTest,PerApplicationScopeTest'` is green before touching anything.
2. Decide §5.1 (default: Option A) and §5.2 (default: Option 1).
3. ~~Do **not** add a `PerApplicationScope` no-arg static "current scope" accessor to make the migration easier. The whole point is that the instance is owned by the application assembly. (During prep this item also ruled out `isBound()` and any dual-read seam; §6.2's **decision taken (manager pass)** supersedes that — see there for why. The no-accessor rule stands.)~~

   **Reversed 2026-10-26.** The instance *is* owned by the application assembly — but that ownership is exercised once, when the application chooses its root. Starting from `AbstractWrapperListener` or from `PerApplicationScopeContextLoaderListener` *is* the choice, and the `ScopedValue` binding on the refresh thread is that choice made visible. Requiring every pool, transport, and web bean to be handed the same instance again, by hand, only repeated a decision already taken, and the owners that were not handed it could do nothing better than log a WARN and run unbound beside the scope they belonged in. So `null` now means "the root already chosen", not "no scope". `PerApplicationScope` binds the instance itself in its `ScopedValue` (previously only the map) and exposes two statics: `getBound()`, the scope bound on the current thread or `null`; and `resolve(handed)`, the one rule every consumer applies — handed > bound > `null`. A handed scope is returned when nothing is bound (a real override, respected) or when the bound scope is that same instance; a handed scope that differs from the bound one throws `ScopeException`; `null` returns the bound scope, or `null` when nothing is bound. Each consumer calls it exactly once, on the thread that constructs or starts it, and stores the result — never lazily, never on a worker thread, because a later thread may carry a different binding or none. The web entry points follow the same precedence: `PerApplicationScopeContextLoaderListener` settles on the constructor scope, else the published servlet-context attribute, else the bound scope, else a new instance; `PerApplicationScopeFilter.init` on the constructor scope, else the attribute, else the bound scope; both throw `ScopeException` through `resolve` when the chosen scope differs from the bound one. This is not the Option 2 fallback rejected in §5.2: there is no process-wide default, nothing is installed on a thread, and a thread with no binding still resolves to `null` and still fails loudly on lookup. Under the Tanuki wrapper the root choice is itself opt-in (2026-10-26): `AbstractWrapperListener` creates and binds its scope only when `spark.per.application.scope=true` in `wrapper.conf` (the mojo's `perApplicationScope` parameter), default off, because the context is opt-in by declaring its bean and the scope needed an equivalent switch; with the flag off nothing is bound around the refresh, so every `null` there resolves to `null` and the application runs with its inherited `PerApplicationContext` alone.

### 6.2 Managers — bridge the readers (per module)

The twins already exist (§4.1). Do **not** change the `*ContextManager` classes to use `PerApplicationScope`; that is exactly the silent cut-over the rename was done to prevent.

**Decision taken (manager pass).** Rather than flipping each reader straight to its `*ScopeManager` twin, every couplet gained a third, neutral `*Manager` (`OrmDaoManager`, `SessionManager`, `DataSourceManager`, `PoolManager`, `ClaxonRegistryManager`, `SpringPropertyAccessorManager`, `JsonEntityResourceProxyManager`, `PolymorphicAttributeManager`) and every library-internal reader and writer in the read-from column was switched to it. The neutral manager is a **bridge**, not a selector: `register` writes to every registry bound on the calling thread (`PerApplicationScope.isBound()` → scope twin; `PerApplicationContext.isBound()` → context twin), and lookups read the scope twin first, falling back to the context twin when the scope holds nothing and a context is attached. A thread with neither bound delegates to the context twin and fails with `MissingPerApplicationContextException` — except `ClaxonRegistryManager.getRegistry()`, which returns `null` there so that `Instrument.with` degrades to an `UnpluggedInstrumentation` and metrics never fail business code (its `register` still throws). Both `isBound()` accessors were added for this. Reasons: (a) a client may legitimately be in any of three states — context only, scope only, or both — for the length of its own migration, and the Tanuki wrapper, the stacked Grizzly/Jetty filters, and the testbench foundation all bind both on the refresh thread; (b) an earlier pure-selector version ("scope if bound, else context") sent every refresh-time registration to the scope map whenever both were bound, so an un-wrapped pool that inherited only the context read an empty map and missed *silently* — the exact failure this document set out to prevent, produced by the mechanism meant to prevent it; (c) the bridge turns that into a hit while still failing loudly on a thread with no binding at all. The `*ContextManager` twins are therefore **not** deprecated and will not be; they stay as the context-side store the bridge writes to. The pool-wrapping audit (§6.3) is unchanged and still required: the bridge resolves registrations across bindings, it does not create a binding where there is none. `WireContextManager` / `WireScopeManager` have no neutral manager because they store different types (`WireContext` vs. `WireScope`); see §8.

> **History.** The bullets below were the per-module plan before the bridging decision and are superseded by it: readers go through the neutral `*Manager`, not the `*ScopeManager` twin, and nothing is deprecated. The only library code that calls a flavor directly is phalanx's `WireContextXmlAdapter` / `WireContextInitializingBean` (context) and `WireScopeXmlAdapter` / `WireScopeInitializingBean` (scope), by design (§8).

- ~~Switch every library-internal writer and reader in the module's **read-from** column from `*ContextManager` to `*ScopeManager` (`ORMDao.register()`, `ProxySession.register()`, `RepositoryAspect`, `ByKeyRoster`, `WireContextXmlAdapter`, `WireContextInitializingBean`, `AttributedPolymorphicXmlAdapter`, `PolymorphicAttributeNameInitializingBean`, `JsonEntityResourceProxyFactory`, `PropertyPlaceholderConfigurer`, the dynamic initializing beans, `Pool.register()`, and `Instrument`, which flips its delegate to `ClaxonRegistryScopeManager`).
- Do the pool-wrapping audit for that module (§6.3) in the same change, so the module's tests prove the wiring.
- ~~Deprecate the module's `*ContextManager` with `@Deprecated(forRemoval = true)` and an `@see` to the twin; keep it one release as a client-facing shim.~~ Not planned; see the status note.
- Update the module's chapter: the twin becomes the documented entry point; the "library code resolves through the context flavor" sentence goes away.
- `PerApplicationDataManager` stays as-is (its Javadoc should mention both types until `PerApplicationContext` is removed).

The lazily-created nested-map pattern (`if ((map = get(...)) == null) map = setPerApplicationDataIfAbsent(..., new ConcurrentHashMap<>())`) formerly raced on the first registration (two threads could each create a map, the second overwriting the first); both bindings now expose `setPerApplicationDataIfAbsent`, and the ten lazily created registries go through it, so concurrent first registrations share one map. (The session managers' former `ApplicationContextAware` reset hook was removed as a vestige; none of them is a Spring bean.)

### 6.3 Pool-wrapping audit (Option 1)

> **History.** The procedure and "known suspects" below are the audit plan as written for `wrapThreadFactory` / carriers; it was superseded by the "Decision taken (pool pass)" in §5.2 (a nullable `PerApplicationScope` collaborator on each owner). The "Audit closed (pool pass)" paragraph further down records what was actually done.

Goal: every thread in the **read-from** column of §4.1 is bound.

Procedure per module:

1. `grep -rn --include=*.java "new Thread(\|Executors\.\|ThreadFactory\|ThreadPoolExecutor\|ScheduledThreadPoolExecutor\|ForkJoinPool\|CompletableFuture\.\(run\|supply\)Async" <module>/src/main` — list every thread source.
2. For each, determine whether code running on those threads reaches a manager (directly, via an aspect, or via a JAXB adapter). If unsure, assume yes.
3. Wrap: build the pool from `PerApplicationScope.wrapThreadFactory(existingFactory)`, called from a bound thread (typically the Spring `afterPropertiesSet` / constructor running inside the application's `run`). If the pool is created by a third-party library that owns its `ThreadFactory` (JMS providers, Kafka consumers, RabbitMQ), instead capture `ApplicationCarrier carrier = PerApplicationScope.generateCarrier()` at wiring time and run each callback body through `carrier.run(...)` / `carrier.call(...)`.
4. Add a test that runs the module's worker path on a thread created *outside* any scope and asserts it does **not** see application data, then the same path through the wrapped factory and asserts it does.

Known suspects, from §4.1:

- **phalanx** — transport consumer threads deserialize `WireContext` via `WireContextXmlAdapter` → `WireContextManager.getContextClass`, and every transport records Claxon metrics via `Instrument.with`. `WorkerPool` / `WorkManager` (see `WorkerPoolTest`'s comment) records metrics too. Every JMS `MessageListener`, Kafka poll loop, and RabbitMQ `Consumer` callback needs a carrier.
- **claxon/registry** — the registry's own collection/emission worker: check whether it calls `Instrument.*` or only holds the registry by reference. If by reference, no wrapping needed; document that.
- **quorum** — `ComponentPool` deconstruction / lease-timeout threads, if they reach `PoolContextManager.getPool`.
- **persistence** — cache-praxis vectors and rosters call `OrmDaoManager.get` (bridge); if any run on a background refresh thread, wrap it.
- **web/jersey, web/json/scaffold** — JAXB adapters run on whatever thread marshals. Request threads are covered by the filter; anything that marshals off-request (async responders, scheduled pushes) needs a carrier.

**Audit closed (pool pass).** Every `src/main` thread source was listed and traced to the managers it reaches. Mechanism per the §5.2 decision taken: a nullable `PerApplicationScope` on each owner, ~~`null` = unbound~~ (amended 2026-10-26: each owner resolves what it is handed exactly once, through `PerApplicationScope.resolve`, on its constructing or starting thread; `null` adopts the scope bound there and is unbound only when nothing is bound; a handed scope that differs from the bound one throws `ScopeException`. Pass-through collaborators — `KafkaMessageIngester`, `RequestMessageRouter` / `ResponseMessageRouter`, `RequestListener` / `ResponseListener`, `DeconstructionQueue`, and the persistence factories and beans that forward to `ComponentPool` — receive the owner's resolved scope and do not resolve again. Phalanx's `BindingAdvisory` and the inline WARNs in quorum and the web beans were removed.)

Bound when given a scope (or, since 2026-10-26, when constructed or started inside one):

- **phalanx** — `WorkManager` (four-argument constructor; the three `*ResponseTransport`s pass theirs through), `KafkaMessageIngester` (both transports' ingesters), `RequestMessageRouter` / `ResponseMessageRouter` (`handleDelivery` body moved to `handleBoundDelivery`, run through `scope.run` on the RabbitMQ client's consumer thread), `RequestListener` / `ResponseListener` (`onMessage` → `onBoundMessage`, same pattern on the JMS provider's delivery thread), `MockQueue` / `MockTopic` / `MockMessageRouter`. Every transport constructor gained a trailing `PerApplicationScope` parameter (before the `servers` varargs on Kafka); the shipped `mock-wire.xml` and `rabbitmq-wire.xml` originally declared a private `perApplicationScope` bean (never bound around any refresh, so always empty); they now `ref` `org.smallmind.spark.perApplicationScope`, the name the Tanuki wrapper registers, and `MockWireTest` registers a test-owned instance under that name (2026-10-26: the shipped files pass `<null/>` instead and adopt the scope bound around the refresh, and `MockWireTest` refreshes inside its test-owned scope without registering a bean; `MockQueue` / `MockTopic` no longer resolve — `MockMessageRouter` resolves once and hands the result down, mirroring a transport and its routers and listeners). Tests: `WorkerPoolTest` (bound / unbound worker), `MockScopeBindingTest` (service invoked over the mock transport reports its own thread's binding).
- **quorum** — `ComponentPool.setPerApplicationScope` (fluent, before `startup()`); `DeconstructionQueue.startup(scope)` wraps the ignition thread; `ComponentPinManager` wraps the virtual creator thread. Test: `ComponentPoolScopeTest` (component records the reaping thread's binding on `close`).
- **persistence** — `perApplicationScope` property on `PooledDataSourceFactoryBean` and `DynamicPooledDataSourceInitializingBean`, forwarded through the `PerApplicationScope` parameter added to `PooledDataSourceFactory.createPooledDataSource` and `PooledConnectionComponentPoolFactory.constructComponentPool` (replacing the previous signatures — no scope-less overload remains, so context-only callers pass `null`; documented in `PERSISTENCE.adoc`) to every `ComponentPool`. No persistence thread of its own; cache-praxis readers run on the caller's thread.
- **bayeux** — `OumuamuaConfiguration.perApplicationScope`; `OumuamuaServer` wraps the default executor (now `newThreadPerTaskExecutor` over a wrapped virtual-thread factory) and both idle timers, and exposes `getPerApplicationScope()` / `runBound(Runnable)`; `AsyncOumuamuaServlet` builds its executor in `init` with the same wrapping and runs the read-listener's `onReceipt` through `runBound`; `OumuamuaServlet` does the same for its synchronous `onReceipt`; `WebSocketEndpoint` runs `onDelivery` through `runBound`; `KafkaBackbone` replaced its constructor with one taking a `PerApplicationScope` (before the `servers` varargs) that wraps its consumer threads. A caller-supplied `executorService` is used as given and documented as the caller's to wrap. Test: `OumuamuaServerTest` (bound default executor, unbound without a scope, supplied executor untouched).

The `// TODO: Per Application Scope Entry Point` markers that once tagged every constructor, setter, and entry method accepting a `PerApplicationScope` (32 sites) were removed once the entry points were documented in their chapters; `grep -rn "PerApplicationScope perApplicationScope" --include=*.java */src/main` lists the signatures instead. **Decision (B5): no scope-less overloads are being added back.** Each construction site states its choice explicitly; a context-only client passes `null` (`<null/>` in XML), and each chapter says so where the parameter is introduced.

Deliberately unwrapped, verified to resolve no per-application data: `ClaxonRegistry`'s `claxon-collection` thread (holds the registry by reference; only `Instrument.register(this)` at construction), the Kafka transports' and `KafkaBackbone`'s producer executors (`producer.send` only), `RabbitMQConnectionManager`'s retry thread (reconnects only), and the `spark-wrapper-signal` keep-alive (§6.4). Caller-dependent and left to the caller: `BatchJobExecutorFactory`'s pool and `SleuthThreadPool` run only caller code.

### 6.4 Entry points

**Prep status.** The filter and listener below now exist as separate, side-by-side types (`scope/web/PerApplicationScopeFilter`, `spring/web/PerApplicationScopeContextLoaderListener`) rather than as in-place rewrites, and the Grizzly/Jetty beans accept a `perApplicationScope` property that registers the scope filter alongside the context filter, which is registered only when a `PerApplicationContext` is attached to the refresh thread (the two filters touch unrelated storage; with the bridging managers a request thread that carries both reads the scope first and falls back to the context, so stacking them is harmless as long as the refresh thread that registered also carried both, or only the context). The context filter and `WebContextLoaderListener` remain the context-flavor defaults, revised as the §4.1 table describes (the filter never creates a context; the listener attaches an already-published one on refresh and on destroy).

**Entry-point pass (done, side by side with the context flavor):**

- Grizzly/Jetty: when `perApplicationScope` is set, the scope filter is mapped on `/*` of every `WebApplicationOption` context for `REQUEST` + `ASYNC` dispatches with async support enabled (the context filter stays on `restPath/*`, also `REQUEST` + `ASYNC` with async support, and is registered only when a context is attached to the refresh thread). The bean throws `ScopeException` from `onApplicationEvent` when the refresh thread is bound to a scope other than the configured one (since 2026-10-26 through `PerApplicationScope.resolve`, which also makes an unset `perApplicationScope` adopt the scope bound on the refresh thread; §6.1 item 3 — under the Tanuki wrapper that scope is bound only when `spark.per.application.scope` is on, so with the flag off an unset property resolves to `null` and no scope filter is registered). JAX-WS, which both containers serve outside the servlet filter chain, is bound by `web/grizzly/PerApplicationScopeHttpHandler` (a delegating `HttpHandler` around `JaxwsHandler`) and `web/jetty/PerApplicationScopeHttpFilter` (a `com.sun.net.httpserver.Filter` on each `HttpContext`). Not bound, by design and documented in `WEB.adoc`: `AsyncContext.start`, `ReadListener`/`WriteListener` callbacks, the WebSocket handshake (Tyrus upgrades outside the filter chain on Grizzly; Jetty's upgrade filter precedes ours) and endpoint callbacks, `AsyncProvider` services, and Jetty endpoints given their own `Executor` — those hold the scope instance and `run`/`call` their bodies.
- `web/jersey/spring/ExposedPerApplicationScopeContextLoaderListener` is the scope twin of `ExposedWebContextLoaderListener`.
- `AbstractWrapperListener` keeps `extends PerApplicationContext` and additionally owns a `PerApplicationScope` (`getPerApplicationScope()`), running `start`/`stop`/`controlEvent` inside it ~~always~~ (2026-10-26: only when `spark.per.application.scope=true` in `wrapper.conf` — the mojo's `perApplicationScope` parameter — the default being off; `isPerApplicationScopeEnabled()` reads the property, is overridable, and is consulted once, on the first `getPerApplicationScope()`, which returns `null` when the scope is off). The `spark-wrapper-signal` executor is deliberately **not** wrapped (it never resolves per-application data). `SpringContextWrapperListener.startup` ~~always~~ refreshes inside that scope when the listener has one (binding it itself when the caller is not already inside it) and, once when the context is first loaded, adds a `BeanFactoryPostProcessor` that registers the scope as the singleton bean `org.smallmind.spark.perApplicationScope` on every refresh, so XML can `ref` it into the web beans (2026-10-26: without the scope it loads, refreshes, and closes under the context binding alone and registers no scope bean, so a `ref` to that name fails the refresh (Spring's `BeanCreationException`, caused by `NoSuchBeanDefinitionException`) and a `factory-method="getBound"` bean is `null`, as `PerApplicationScopeBeanTest` pins for an unbound refresh); a `shutdown()`→`startup()` restart in one JVM works only for contexts that support a second refresh (`ClassPathXmlApplicationContext`); `ClassPathXmlSpringContextWrapperListener` now builds its context unrefreshed.
- Testbench: `foundation.xml` is unchanged (a scope bean created during refresh cannot be the one bound around it); `TESTBENCH.adoc` documents the test-owned-scope pattern instead.

What remained for the cut-over, and what was decided instead:

- ~~migrate the managers (§6.2)~~ done via the bridging managers; the bootstrap binders (`AbstractWrapperListener` — the scope flavor only under its `spark.per.application.scope` flag, §6.1 item 3 — and testbench) keep both flavors on purpose, since a refresh that binds both populates both maps;
- ~~stop registering `PerApplicationContextFilter` in the Grizzly/Jetty beans (and make `perApplicationScope` required)~~ not planned; instead the beans register the context filter only when a `PerApplicationContext` is attached to the refresh thread, and `PerApplicationContextFilter.init` now fails fast (`MissingPerApplicationContextException`) rather than creating an empty context when none is supplied or published — the same rule the scope filter has always had;
- ~~deprecate `PerApplicationContextFilter`, `WebContextLoaderListener`, and `ExposedWebContextLoaderListener` per §6.5~~ not planned (status note).

Decisions recorded while building the prep: the no-arg scope filter fails fast in `init` instead of creating a scope (a filter-created map is silently empty); a scope passed to the filter's constructor ignores the servlet-context attribute; the listener throws `ScopeException` when handed a scope that differs from one already published; the Grizzly/Jetty beans do **not** wrap `deploy`/`server.start()` in `scope.run` — binding the refresh thread is the bootstrap's job and wrapping there would only mask its absence. Decisions recorded in the entry-point pass: whole-context `/*` mapping rather than a URL-pattern property (least to forget; the bean owns the policy once a scope is set); thin JAX-WS wrappers rather than documenting SOAP as unbound; Tanuki side by side rather than cut over; with the bridging managers (§6.2) a refresh that binds both registers into both, so Tanuki's inherited context keeps un-wrapped threads working while wrapped threads use the scope.

> **History.** Everything from here to the end of §6.4 is the original in-place rewrite plan for the context-flavor entry points. None of it was applied: `PerApplicationContextFilter` and `WebContextLoaderListener` keep their context semantics (the listener now attaches an already-published map to the refresh thread, or constructs one adopting any map on the thread), `AbstractWrapperListener` keeps `extends PerApplicationContext`, and `foundation.xml` is unchanged. The scope-flavor types were added side by side instead, as recorded above.

**`PerApplicationContextFilter`** (`nutsnbolts/.../context/web`):

- Field type `PerApplicationContext` → `PerApplicationScope`. Servlet-context attribute key: keep `PerApplicationContext.class.getName()` **only** during a transition when both listener and filter must interoperate; otherwise switch to `PerApplicationScope.class.getName()` and change it in the listener in the same commit.
- `doFilter` per §5.1 Option A.
- Consider renaming to `PerApplicationScopeFilter` and leaving a thin `PerApplicationContextFilter` subclass for one release; or rename outright and update the two web registrations (§4.1). Renaming outright is simpler and the class is internal to this repository's web stack.
- Behavior change to document: the binding now ends when `chain.doFilter` returns. Async servlet processing (`AsyncContext.start`, `startAsync` + dispatch) is **not** bound. If any web module uses async dispatch, its completion path needs an `ApplicationCarrier`.

**`WebContextLoaderListener`** (`nutsnbolts/.../spring/web`):

```java
@Override
public void contextInitialized (ServletContextEvent servletContextEvent) {

  PerApplicationScope perApplicationScope;

  if ((perApplicationScope = (PerApplicationScope)servletContextEvent.getServletContext().getAttribute(PerApplicationScope.class.getName())) == null) {
    servletContextEvent.getServletContext().setAttribute(PerApplicationScope.class.getName(), perApplicationScope = new PerApplicationScope());
  }

  perApplicationScope.run(() -> super.contextInitialized(servletContextEvent));
}

@Override
public void contextDestroyed (ServletContextEvent servletContextEvent) {

  PerApplicationScope perApplicationScope;

  if ((perApplicationScope = (PerApplicationScope)servletContextEvent.getServletContext().getAttribute(PerApplicationScope.class.getName())) != null) {
    perApplicationScope.run(() -> super.contextDestroyed(servletContextEvent));
  } else {
    super.contextDestroyed(servletContextEvent);
  }
}
```

`super.method()` inside a lambda is legal and resolves against the enclosing instance. Spring's refresh runs inside `run`, so bean initializers that call `*.register(...)` are bound. Any executor those beans create must be wrapped (§6.3).

**`AbstractWrapperListener`** (`spark/tanukisoft/integration`):

- Stop extending `PerApplicationContext`. Hold `private final PerApplicationScope perApplicationScope = new PerApplicationScope();`.
- `start(String[])` (line 160) is the boot path: it calls the abstract `startup(args)` and creates a `ScheduledExecutorService` named `spark-wrapper-signal` from an inline `ThreadFactory` (lines 162-164). Wrap the body of `start` in `perApplicationScope.run(() -> { … })`, and build that executor's factory through `PerApplicationScope.wrapThreadFactory(...)` from inside the bound body. `stop(int)` (line 207) and `controlEvent(int)` (line 137) likewise if they reach managers — `shutdown()` almost certainly does.
- Every other thread the wrapper or its subclasses start (Spring context, main loop, shutdown hook) must be bound via `wrapThreadFactory` or a carrier — today they inherit.
- Update `SPARK.adoc:1920`, which currently describes `PerApplicationContext` as the base class.

**Grizzly / Jetty initializing beans**: only the filter class name changes if the filter is renamed. Also check whether either bean creates the server's worker pool itself; if a filter binds per-request, container threads need nothing else.

**`testbench/foundation/foundation.xml`**: a bean definition cannot express "run the rest of the context inside a scope". Options:

- Replace the bean with a `PerApplicationScope` bean **and** a `BeanFactoryPostProcessor`/`ApplicationContextInitializer` in `testbench` that wraps refresh — heavy.
- ~~Simpler and honest: keep a `PerApplicationScope` bean for injection~~ (superseded: no scope bean is declared, see `TESTBENCH.adoc`; the test registers its own instance as the singleton `org.smallmind.spark.perApplicationScope`), and have the testbench base test class (or a TestNG `@BeforeClass`) call `scope.run(...)` around context creation. Update `TESTBENCH.adoc:386-395` to describe the new pattern instead of `new PerApplicationContext()` in `@BeforeClass`.

### 6.5 `PerApplicationContext` itself

**Not planned.** Per the status note at the top, `PerApplicationContext` stays as a supported binding model; the bridging managers read it. The steps below are retained for history only.

After all callers are gone:

1. Mark `PerApplicationContext`, `ContextCarrier`, `WrappingThreadFactory`, and `MissingPerApplicationContextException` `@Deprecated(forRemoval = true)` with `@see` links to the scope types. Keep them for one release.
2. Update `PerApplicationDataManager`'s Javadoc to name `PerApplicationScope`.
3. In the following release, delete them and `PerApplicationContextTest`.

---

## 7. Tests

> **History.** §7.1–7.4 were written for the replacement plan. The tests on the context flavor stay as they are (`new PerApplicationContext()` in setup remains a supported pattern), the Claxon save/restore pattern in `InstrumentedAspectTest` / `ClaxonSpringAssemblyTest` stays, and `WorkerPoolTest` keeps its inherited-context setup alongside the bound/unbound worker cases. The "Done" items in §7.4 are what exists.

### 7.1 General pattern

Every `new PerApplicationContext();` in a `@BeforeClass`/`@BeforeMethod` becomes one of:

- **Wrap the test body.** `scope.run(() -> { … })` inside each test method, or a small helper `inScope(Runnable)`. Verbose but explicit; prefer it when a class has few tests.
- **TestNG `IHookable`.** Implement `IHookable.run(IHookCallBack callBack, ITestResult result)` on the test class (or an abstract base) as `perApplicationScope.run(callBack::runTestMethod)`. Every test method then runs bound without per-method boilerplate. `@BeforeMethod`/`@AfterMethod` are **not** inside the hook, so setup that calls `register(...)` must also be moved into the hook or into the test body. Recommended for `persistence`, `phalanx`, and `quorum`, which have many such classes.

Either way, data registered in setup must be registered inside the same scope instance the test body runs in. Because the map is shared per instance, one `PerApplicationScope` per test class (a field) is the natural unit.

### 7.2 Save/restore pattern (Claxon)

```java
priorContext = PerApplicationContext.generateCarrier();
new PerApplicationContext();
…
priorContext.prepareThread();
```

Delete it. With scopes there is nothing to restore: the test's own `run` ends when the body ends. Replace with a per-class `PerApplicationScope` and the `IHookable` pattern.

### 7.3 Inheritance-dependent tests

`WorkerPoolTest` (phalanx) relies on forked workers inheriting the context. After the migration it must build the pool from `PerApplicationScope.wrapThreadFactory` — which is also the production fix, so the test now proves the production wiring.

`PerApplicationContextTest.runIsolated` exists only to dodge inherited state and goes away with the class.

### 7.4 What to add

- One test per wrapped pool (§6.3 step 4).
- ~~A filter test proving the binding is gone after `doFilter` returns and that `suppressConnectionClosedException` still works through the `call` wrapper.~~ Done: `nutsnbolts/.../scope/web/PerApplicationScopeFilterTest`.
- ~~A `WebContextLoaderListener` test proving a bean's `afterPropertiesSet` sees the scope.~~ Done for the scope listener: `nutsnbolts/.../spring/web/PerApplicationScopeContextLoaderListenerTest` (also proves `destroy` is bound and the calling thread is unbound afterwards).
- Done: `GrizzlyPerApplicationScopeBootTest` and `JettyPerApplicationScopeBootTest` boot a server with and without `perApplicationScope` and probe which binding the request thread sees.
- Done for the context listener: `nutsnbolts/.../spring/web/WebContextLoaderListenerTest`.
- Done: `nutsnbolts/.../context/web/PerApplicationContextFilterTest` (expanded).
- Done: `phalanx/.../wire/transport/mock/MockWireScopeRoundTripTest` and `MockReceiverBindingMismatchTest`.
- Done: `quorum/.../pool/complex/ComponentPoolScopeTest`.
- Done: `spark/tanukisoft/integration/.../spring/SpringContextWrapperListenerTest` and `ClassPathXmlSpringContextWrapperListenerTest`.
- Done: `bayeux/.../server/impl/OumuamuaServletTest`, `AsyncOumuamuaServletTest`, and `bayeux/.../server/spi/backbone/kafka/KafkaBackboneTest`.

---

## 8. Migration Steps — `ContextFactory` → `ScopeFactory`

**Decision taken: no migration. Both stay; the client decides.** `ContextFactory` / `Context` / `WireContext` and `ScopeFactory` / `Scope` / `WireScope` are parallel, supported mechanisms. phalanx carries both on the wire (`InvocationSignal` has independent context and scope arrays; `MethodInvoker` pushes/pops contexts and `callInScopes` scopes; `WireInvocationHandler` collects both), registers both (`WireContextInitializingBean` / `WireScopeInitializingBean`), and decodes each through its own registry (`WireContextManager` needs a `PerApplicationContext` on the worker thread, `WireScopeManager` the transport's `PerApplicationScope`; a receiver lacking the binding an element needs drops the message — see `PHALANX.adoc`). `PooledDataSourceContext` and `RootedFileSystemContext` remain `Context` implementors read through `ContextFactory`; a scope-flavored twin of either would be an additive feature, not a conversion. There is no neutral `Wire*Manager` because the two registries store different types. The subsections below describe what a conversion would have involved and are retained for history only.

Smaller and independent of Part 6; can be done first or second.

### 8.1 Type changes

- `WireContext implements Context` → `implements Scope` (phalanx). `ProtoWireContext` follows automatically.
- `PooledDataSourceContext implements Context` → `implements Scope` (persistence).
- `RootedFileSystemContext implements Context` → `implements Scope` (file/jailed).
- `WireTestingService`: `@ExpectedContexts(UnknownContext.class)` → `@ExpectedScopes(UnknownContext.class)`; `UnknownContext` implements `Scope`.

Renaming the classes themselves (`WireContext` → `WireScope`, etc.) is **out of scope**: `WireContext` is on the wire (`WireContextXmlAdapter`, `WireContextManager` handles) and part of phalanx's public vocabulary.

### 8.2 `MethodInvoker.remoteInvocation`

```java
public Object remoteInvocation (WireContext[] contexts, Function function, Object... arguments)
  throws Exception {

  Methodology methodology;

  if ((methodology = methodMap.get(function)) == null) {
    throw new MissingInvocationException("No method(%s) available in service interface(%s)", function.getName(), serviceInterface.getName());
  }

  return ScopeFactory.<Object, Exception>callInScopes(contexts, () -> {
    try {
      return methodology.getMethod().invoke(targetObject, arguments);
    } catch (InvocationTargetException invocationTargetException) {
      if ((invocationTargetException.getCause() != null) && (invocationTargetException.getCause() instanceof Exception)) {
        throw (Exception)invocationTargetException.getCause();
      } else {
        throw invocationTargetException;
      }
    }
  });
}
```

Notes:

- `callInScopes` treats a `null` or empty array as "run unbound", matching today's guard.
- Today's loop skips `null` **elements**; `callInScopes` throws `ScopeException` on them. Decide: either filter nulls into a fresh array before the call (preserves behavior), or let it throw (a null wire context is a transport bug). Preserving behavior is the conservative choice; document whichever you pick in `PHALANX.adoc`.
- `WireContext` subclasses may now implement `scope.ScopeLifecycleAware` to get `beforeEnter`/`afterExit` around the invocation. Mention this in `PHALANX.adoc` as new capability.
- Update the method's Javadoc (it currently describes push/pop).

### 8.3 `WireInvocationHandler.invoke`

```java
Scope[] filteredScopes = ScopeFactory.filterScopesOn(method, WireContext.class);
```

Then cast to `WireContext[]` as today. `ScopeException` replaces `ContextException` in the `@throws` documentation.

### 8.4 Readers

`ContextualPooledDataSource`, `ContextualPooledXADataSource`, `ContextSensitiveRootedPathTranslator`: `ContextFactory.getContext(X.class)` → `ScopeFactory.getScope(X.class)`. Same null semantics.

### 8.5 Whoever pushes

The readers above only work if *something* entered the scope. Today that is:

- phalanx: `MethodInvoker` on the service side (§8.2) — covered.
- persistence `PooledDataSourceContext`, file/jailed `RootedFileSystemContext`: pushed by **application code** following the examples in `PERSISTENCE.adoc` / `FILE.adoc`. Those examples must change from push/`finally`-pop to `ScopeFactory.callInScope(ctx, () -> …)`, and the prose about inheritance into child threads must be replaced with `ScopeSnapshot` guidance.

### 8.6 Tests

- `ContextualPooledDataSourceTest`, `ContextualPooledXADataSourceTest`, `ContextSensitiveRootedPathTranslatorTest`, `MockWireTest`, `AbstractWireTransportContractTest`, `WireInvocationHandlerTest`: `pushContext` in setup / `popContext` in teardown → wrap the body in `ScopeFactory.runInScope(ctx, () -> …)`. The `IHookable` trick from §7.1 works here too if a class has many tests.
- `MethodInvokerTest`: verify the invoked method sees the scope and that it is gone afterwards, including when the target throws.
- `ContextFactoryTest` is deleted with `ContextFactory` (§8.8).

### 8.7 Documentation

- `PHALANX.adoc`: every `ContextFactory.pushContext`/`popContext` example → `ScopeFactory.callInScope`; lines 630 and 770-773 describe thread-local behavior and inheritance — rewrite around dynamic extent and `ScopeSnapshot`; line 869 dependency note names `Context`/`ContextFactory` → `Scope`/`ScopeFactory`.
- `FILE.adoc`: same for 278, 1036-1041, 1137-1176, 1347-1363.
- `NUTSNBOLTS.adoc`: once `ContextFactory` is deprecated, move the `[NOTE]` in `pkg-context` to say so; when removed, delete `pkg-context`, the "Context inheritance" behavioral subsection, the At A Glance row, the Quick Navigation entry, and decrement the subpackage count in the overview (currently twenty-nine).

### 8.8 `ContextFactory` itself

**Not planned** (see the decision at the top of §8).

Same two-release deprecation as §6.5: `Context`, `ContextFactory`, `ContextException`, `ExpectedContexts`, `context.ContextLifecycleAware` → `@Deprecated(forRemoval = true)` with `@see`, then delete the `context` package.

---

## 9. Verification Checklist

> **History.** Written for the replacement plan. Under the coexistence decision: context references in `src` are permanent, not shims; an un-wrapped thread that inherited a context resolves through the bridge (and one with no binding fails with `MissingPerApplicationContextException`, or records nothing for Claxon); and the chapters deliberately document context inheritance. The live checks are the per-module `mvn -q -pl <module> test`, the bridging-manager tests (`*ManagerTest` in each module), and the chapter rules in `NUTSNBOLTS.adoc` `per-application-registries`.

Per module, in dependency order:

- [ ] `mvn -q -pl <module> test` green.
- [ ] `grep -rn "PerApplicationContext\|nutsnbolts\.context\." <module>/src` returns only deprecated-shim references (or nothing, after removal).
- [ ] Every pool in the module that reaches a manager is built from `wrapThreadFactory` or runs tasks through a carrier, and a test proves an unwrapped thread fails with `MissingPerApplicationScopeException`.
- [ ] The module's `.adoc` chapter names the new types, shows `run`/`call` or `callInScope` in examples, and no longer promises thread inheritance.

Repository-wide, at the end:

- [ ] Full build: `mvn -q test` (the `integration` group is enabled in the root surefire config; expect it to run).
- [ ] `grep -rn "InheritableThreadLocal" --include=*.java .` — only hits should be in deprecated shims or unrelated code you have consciously left alone.
- [ ] `NUTSNBOLTS.adoc` `pkg-scope` "Why it exists" paragraph still describes `ContextFactory`/`PerApplicationContext` accurately for whatever state they are in (present, deprecated, removed).
- [ ] `README.adoc` module index still matches the chapter files.

---

## 10. Rollback

> **History.** The "do not build a bridge" rule below was reversed by the §6.2 decision; the bridge exists and is the supported way library code reads both maps. Nothing is scheduled for removal, so there is no rollback phase.

Each phase is independently revertible because the old types remain. If a module's pool audit proves impractical mid-migration, that module can stay on `PerApplicationContext` temporarily **only** if it does not share a JVM with modules already on `PerApplicationScope` that expect to see the same data — ~~the two maps are unrelated and nothing bridges them. Do not build a bridge; finish the module instead.~~

---

## 11. Out Of Scope

- `StructuredTaskScope` adoption (preview in JDK 25).
- Renaming `WireContext` or any on-the-wire vocabulary.
- Reworking the lazily-created nested-map pattern in the managers.
- Consolidating `ScopeSnapshot` and `ApplicationCarrier` into one carrier type. They capture different things (immutable map vs. shared map) and should stay distinct.
