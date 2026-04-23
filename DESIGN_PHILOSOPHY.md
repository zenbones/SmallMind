# Design Philosophy

This guide describes how to design frameworks, annotations, aspects, and integration
surfaces in a style that favors narrow contracts, explicit wiring, caller-owned lifecycle,
and thin adapters over third-party APIs. It is a companion to any general code-style
guide: this document is about shape and design decisions, not formatting or naming.

Drop this file into a project where you want code generators and contributors to produce
frameworks that compose cleanly rather than frameworks that try to own the runtime.

## Core Principles

- **Narrow contracts.** A framework answers one question well. If it is answering two
  questions, it is two frameworks sharing a name.
- **Explicit over implicit.** A reader should be able to trace behavior from a line of
  code to the thing that produces it, without following auto-configuration, classpath
  scanning, or annotation-magic chains.
- **Caller-owned lifecycle.** Startup, shutdown, resource ownership, and replacement
  policy belong to the application unless the framework is explicitly about that concern.
- **Thin adapters, not meta-frameworks.** When integrating with a container, transport,
  or external system, adapt — do not wrap and rename.
- **Fail fast, fail loud.** Bad wiring should surface at startup with a clear message,
  not as silent drift at request time.
- **A direct path must always exist.** Whatever convenience a framework offers —
  annotations, builders, DI glue — the programmatic direct path stays first-class.

## Framework Design

A framework in this style is a small, focused mechanism that answers one integration
question cleanly. It is not a platform, not a container, not an application skeleton.

### Separate API, SPI, Implementation, And Integration

Structure the framework as layers with distinct responsibilities:

- **API / contract** — the interfaces, markers, and value types callers depend on. Small
  and stable.
- **SPI / extension** — the interfaces an implementor satisfies to plug in. Sometimes the
  same types as the API; sometimes a separate surface.
- **Runtime / implementations** — one or more concrete implementations, each a separate
  module or package when possible.
- **Integration adapters** — container glue, factory beans, XML schema support, assembly
  helpers. Live in a clearly labeled subpackage so consumers can see they are optional.

A library using the framework should be able to depend on only the API module.
Applications select and depend on runtime implementations explicitly.

### Prefer Many Narrow Frameworks Over One Umbrella

If several related questions share a problem space but each deserves its own vocabulary
(for example, rate limiting, load balancing, pooling, and naming all live in "resource
management"), ship them as sibling sub-frameworks that share a package root rather than
as one framework with modes. Shared vocabulary hides divergent contracts.

### Pluggability Through Composition, Not Discovery

- Applications wire implementations by direct construction, factory, or explicit name
  lookup.
- `ServiceLoader`, classpath scanning, and auto-configuration are not first-class
  extension mechanisms in this style. They are acceptable only when the framework is
  explicitly in that business, and the behavior is documented.
- A framework that routes by name (for example, a registry of named emitters) binds
  those names at assembly time, not at runtime from ambient state.

### Configuration By Setters And Constructors

- Make configuration visible in the type system: constructor parameters, setters, or
  builder methods.
- Avoid configuration schemes whose inputs come from annotations, system properties, or
  environment variables unless the framework is explicitly a bridge to those surfaces.
- Document every parameter's default in the constructor or field initializer, not in
  prose elsewhere.

### Registries

When a framework genuinely needs a registry — for loggers, contexts, metric meters, or
other well-known names — prefer a static class with explicit `register` / `unregister`
and deterministic lookup.

- Do not hide the registry behind auto-discovery.
- Do not mutate the registry from static initializers of unrelated classes.
- Do not let the registry become a global mutable scratch space; keep it to the named
  surface it was designed for.

## Annotation Design

An annotation's job is to declare intent in a location where a separate tool (aspect,
annotation processor, reflection-based utility) can find it. Annotations do not carry
behavior; they carry labels.

### When To Introduce An Annotation

Introduce an annotation when:

- A separate tool needs to locate the annotated elements (an aspect, a processor, a
  validator).
- The declarative form is a genuine improvement over a programmatic call at that site.
- The semantics of the label are obvious from the name.

Do not introduce an annotation when:

- The effect could be achieved by a method call at the same site with equal or better
  clarity.
- The annotation would only save a few characters.
- The annotation's behavior depends on ambient configuration the reader cannot see.

### Annotation Shape

- Keep parameter sets small. Annotations with many parameters are usually doing too much.
- Parameters should be primitives, strings, class literals, enums, or simple nested
  annotations. Avoid parameter types whose semantics depend on external state.
- Choose retention and target deliberately. A `RUNTIME` retention is a runtime cost;
  pick `SOURCE` or `CLASS` when that is enough.

### Markers, Metadata, Behavior

- **Marker annotations** are legitimate: they say "this element participates in some
  concern." The concern is implemented elsewhere.
- **Metadata annotations** carry parameters describing shape: expected types, validation
  bounds, format strings. The reader carries those parameters out.
- **Behavior annotations** — annotations that promise to do something on their own — are
  almost always a mistake in this style. If the annotation implies behavior, the behavior
  lives in a named aspect, processor, or utility that the reader can trace to.

### Always Provide A Non-Annotation Path

Every declarative annotation should have a programmatic equivalent. A caller who does
not want to pull in the annotation processor, the aspect weaver, or the reflective
scanner must have a direct call that produces the same effect.

## Aspect Design

Aspects are justified for cross-cutting concerns that cannot be expressed as ordinary
interface methods or wrappers without significant duplication: field injection,
declarative transactions, instrumentation, logging advice.

### Tightly Scoped Pointcut

- The pointcut binds a specific annotation on a specific kind of join point (a method
  execution, a field access, a constructor).
- The pointcut binds the annotation instance and the `this` reference directly, so the
  advice does not need reflection to locate either.
- Pointcuts should not use wildcard class or package matches as their primary condition.
  That is a discovery pattern, and aspects should not do discovery.

### One Concern Per Aspect

- Keep advice small and focused. `@Around` for "replace or wrap the call";
  `@Before` / `@AfterReturning` / `@AfterThrowing` when the phases are actually different
  concerns.
- Do not build a general-purpose aspect that switches on annotation parameters to do
  different things. Split it.

### No Fallback For "Aspect Not Woven"

- If the aspect is not woven, the deployment is broken. Surface it at build or startup,
  not as a subtle runtime degradation.
- Do not add runtime checks that silently no-op when the aspect is missing. That hides
  the misconfiguration and makes the failure mode harder to diagnose.

### Aspects Do Not Discover

- The annotation points the aspect at its join points. The aspect operates on what the
  pointcut already gave it.
- If the aspect needs to scan fields, resolve names, or walk type hierarchies, that
  logic belongs in a separate utility the aspect calls into — not in the advice body.

## Integration With Dependency Injection Containers

A dependency-injection container (Spring, Guice, CDI, or similar) is an optional
assembly layer. The framework does not take a dependency on the container; the container
is merely one way to wire the framework.

### Adapter Pattern

- Put all container-specific classes (factory beans, configuration adapters, XML schema
  handlers) in a clearly labeled subpackage — for example, `.spring`, `.guice`, `.cdi`.
- The core framework imports nothing from the container.
- A consumer who does not use the container can depend on the core module and never
  pull in the adapter module.

### Factory Bean Pattern (Spring Example)

When providing a Spring factory bean:

- Implement `FactoryBean<T>` and `InitializingBean`.
- Expose configuration via Java Bean setters.
- Build the product in `afterPropertiesSet()` and hold it in a private field.
- Return it from `getObject()`.
- Declare `getObjectType()` and `isSingleton()` explicitly.

The same shape applies, adapted, to other containers.

### Direct-Programmatic Wiring Stays First-Class

- The factory bean must not be the only way to construct the product. The constructor
  or builder it delegates to is public and usable directly.
- Documentation covers the direct path before or alongside the container path.

## Exception Design

A custom exception hierarchy earns its keep through shape, not naming. A base class
that renames the JDK hierarchy without adding value is noise.

### What Custom Exception Bases Should Add

- A printf-style constructor (`String format, Object... args`) that applies
  `String.format` at construction. This is a real ergonomic improvement at the throw
  site.
- Preserved causes: always carry a `Throwable cause` through when wrapping.
- Optional structured fields for error codes, categories, or context when those are
  genuinely part of the contract.

### Checked And Unchecked Bases

- Provide both a checked and an unchecked base when the framework has both kinds of
  failure.
- Use the checked base for failures the caller is expected to handle; use the unchecked
  base for contract violations or programming errors.

### Do Not Replace The JDK Hierarchy

- Do not introduce parallel hierarchies that rename `IOException`,
  `IllegalArgumentException`, or `IllegalStateException` without structural additions.
- Do not wrap every exception; wrap only when the wrap adds contract value.

## Failure Modes

### Fail Fast

- Missing providers, contradictory configuration, and impossible runtime combinations
  should throw at startup, not at the first request.
- Throw from constructors, `afterPropertiesSet()`, or an explicit `start()` method.
  Prefer a named exception over a bare `IllegalStateException`.

### Fail Loud

- A named, specific exception with a descriptive message beats a silent fallback every
  time.
- When a fallback is genuinely the right behavior, log it at a visible level and make
  the fallback path part of the documented contract — not an implementation secret.

### No Hidden Retries

- Anything the framework retries on the caller's behalf must be an explicit, documented
  policy — configurable and visible in the API.
- Do not add "just one quiet retry" to smooth over flaky dependencies; the caller needs
  to know.

## Anti-Patterns This Philosophy Rejects

- Auto-configuration that wires beans from the classpath without the application opting
  in by name.
- Annotation-driven behavior where the code path cannot be traced from the annotation
  back to a named processor or aspect.
- Mega-frameworks that own multiple unrelated concerns under one package.
- Framework code that assumes a container is present at runtime.
- Extending JDK hierarchies purely to rename them.
- Hidden worker threads, hidden timers, hidden retries, hidden fallbacks.
- Optional features implemented as mandatory dependencies in the core module.
- Registries that mutate from static initializers in unrelated classes.

## Design Checklist For Code Generators

When designing a new framework, annotation, or aspect in this style, proceed in this
order:

1. Write the narrowest possible contract — the interface or type that describes the one
   thing the framework does.
2. Add the programmatic direct path: constructor, builder, or factory that produces a
   working implementation without any declarative layer.
3. Add the declarative convenience only after the direct path is in place. Annotations
   mark intent; aspects or processors implement it.
4. Put container-specific glue in a labeled subpackage. The core depends on nothing from
   the container.
5. Decide and document the exception shape: checked base, unchecked base, message
   format, cause preservation.
6. Decide and document the lifecycle: who constructs, who closes, who replaces, who
   owns worker threads.
7. Fail fast on misconfiguration, fail loud on runtime errors, do not silently retry or
   fall back.
8. Verify that every declarative convenience has a non-declarative equivalent and that
   the two produce the same effect.
9. Document the contract, defaults, and failure modes in the same place, as part of
   shipping the framework.

A framework, annotation, or aspect that survives this checklist will compose cleanly
with others and will not grow into something that owns more of the runtime than its
name suggests.
