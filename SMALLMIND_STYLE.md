# Smallmind Style

This document describes the coding and design style reflected in the Smallmind codebase and its root-level module guides. It is intended for humans and LLMs producing code for this repository.

## One-Line Summary

Smallmind is explicit Java glue code: narrow libraries that help applications integrate transports, persistence, logging, scheduling, packaging, caching, and related runtime seams without taking ownership of the surrounding system.

## Stable Design Priorities

The repository-wide docs make these priorities explicit. Preserve them.

- modules stay narrow enough to adopt selectively
- configuration, lifecycle, and runtime policy stay application-owned
- dependencies remain optional where practical
- wrappers stay thin over third-party APIs
- operational behavior is treated as part of the public contract

Smallmind is not trying to win by framework ownership, annotation magic, or hidden conventions.

## Module Shape

Smallmind modules are usually organized as explicit layers rather than one umbrella abstraction.

Common patterns in the repository:

- a core API or contract module
- optional runtime integrations or bridges
- Spring or XML assembly helpers that sit beside, not above, the direct API
- thin support modules for plugins, transports, emitters, codecs, or packaging goals

When extending a module:

- preserve clear boundaries between API, SPI, helper, and integration layers
- choose the smallest surface that fits the task
- do not collapse optional integrations into a mandatory dependency path
- do not turn an assembly convenience into the only supported way to use the module

## Library-Safe Behavior

Smallmind code should behave well inside somebody else's runtime.

- Do not assume ownership of startup, shutdown, logging backend, transport stack, container, or deployment layout unless the module is explicitly about that concern.
- Keep resource ownership obvious. If the caller must start, stop, close, destroy, or replace something, make that visible in the API.
- Keep background work explicit. Pollers, monitors, queues, and asynchronous wrappers should have clear lifecycle semantics.
- Fail fast on invalid wiring, contradictory configuration, missing providers, or impossible runtime combinations when the module already follows that pattern.
- Avoid static magic unless the existing package already depends on it and the behavior is well understood.

## API Design

### Make The Contract Readable

Public APIs should explain themselves from the signature and the type names.

- Use descriptive names, even when they are long.
- Prefer explicit constructor arguments over hidden configuration.
- Use overloads when they improve ergonomics without obscuring behavior.
- Use bounded generics when the bounds express a real domain rule.
- Keep default behavior easy to discover in constructors, fields, or clearly named helpers.
- Make null handling, fallback rules, and replacement semantics deliberate and obvious.

### Keep Abstractions Honest

Smallmind does use abstractions, but usually because they preserve decoupling or integration seams.

- Add interfaces when the code needs a stable seam or replaceable behavior.
- Add base classes only when they remove meaningful duplication or standardize important behavior.
- Keep adapters, codecs, factory beans, listeners, and helper types thin.
- Do not hide important third-party semantics behind a private abstraction layer that mostly renames things.

### Preserve Existing Boundaries

If a module already distinguishes between reusable-library usage and application wiring, preserve that distinction.

Examples from the current docs:

- libraries may depend only on an API artifact while applications choose a runtime implementation
- Spring support may exist, but direct wiring remains first-class
- optional integrations are opt-in and should stay that way

## Implementation Style

### Match Local Code First

Smallmind spans old and new code. The build targets a modern JDK, but the house style is still conservative and explicit.

- Match the local file and package style before introducing any new pattern.
- Do not rewrite code into a more fashionable idiom unless the module already uses that idiom nearby.
- Preserve existing copyright headers and file prologues.
- Preserve local formatting, including spaces before parentheses where the file uses them.
- Use explicit imports only.

### Prefer Straightforward Control Flow

- Favor imperative logic, named locals, and readable branching.
- Use streams or functional chains only when they genuinely make the code clearer.
- Accept a small amount of repetition if it keeps the control flow obvious.
- Keep synchronization, queue handling, and concurrency mechanics direct and reviewable.

### Use Ordinary Java Constructs

- Prefer ordinary classes and interfaces over records, sealed types, or Lombok-generated structure unless the surrounding module already uses them.
- Static utility classes are fine when the behavior is truly stateless.
- Helper abstractions should reduce real coupling or duplication, not just save a few lines.

## Exceptions, Concurrency, And Lifecycle

- Follow the exception vocabulary already present in the package.
- Preserve the module's checked-versus-runtime boundary.
- Do not swallow failures.
- Wrap exceptions only when the new type or message adds real contract value.
- Make thread ownership, polling behavior, queue limits, shutdown rules, and replacement semantics explicit in code and docs when they matter.
- Avoid hidden retries, silent fallbacks, or invisible worker threads.

## Dependency And Integration Style

- Keep dependencies optional where practical.
- Do not move integration-specific code into the core module without a strong reason.
- Treat Spring XML, factory beans, and other container helpers as optional assembly surfaces, not mandatory framework ownership.
- Keep direct-programmatic usage viable even when a Spring helper exists.
- Avoid component-scanning, auto-registration, and auto-configuration habits unless the module already depends on that model.
- Preserve third-party types in signatures when those types carry important integration meaning.

## Documentation Style

Smallmind documentation is part of the engineering style. The root-level `.adoc` guides are consistently explicit about ownership, defaults, failure modes, and operational consequences.

When code changes affect public behavior:

- update the matching `.adoc` chapter
- document defaults from code, not from memory
- keep library-versus-application guidance explicit
- describe lifecycle, threading, and troubleshooting details when they are part of the contract
- use real artifact names and `${smallmind.version}` in dependency examples

Keep documentation factual, restrained, and source-backed.

## What Makes Smallmind Distinct

Compared with a typical modern Java framework codebase, Smallmind is:

- more module-oriented than platform-oriented
- more explicit about who owns runtime policy
- more comfortable with direct wiring and Spring XML as peers
- less interested in annotation-driven magic
- more willing to use verbose names when they improve clarity
- more likely to expose a narrow adapter than to build an internal meta-framework

## Instructions For LLMs

When generating code for this repository:

1. Read the matching root-level `.adoc` guide and nearby code before writing anything.
2. Match the local formatting, naming, and exception style exactly.
3. Preserve narrow module boundaries and optional integrations.
4. Prefer explicit APIs, direct wiring, and thin adapters.
5. Keep lifecycle, ownership, defaults, and failure behavior visible.
6. Do not introduce framework-style machinery unless the task clearly requires it.
7. Update the corresponding `.adoc` chapter if the public contract changes.

## Short Prompt Template

Use this when prompting another model:

"Work in the established Smallmind style. This is explicit Java glue code: narrow modules, caller-owned lifecycle and configuration, optional integrations, thin adapters over third-party APIs, conservative abstractions, descriptive names, and operationally precise documentation. Match local formatting exactly, preserve direct wiring alongside any Spring helpers, and keep defaults, ownership, and failure modes visible in the code and docs."
