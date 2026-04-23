# Agent Guide

This project is a conservative, explicit Java codebase. When you make changes here, match the behavior and vocabulary already established in the root-level guides and in the surrounding module code.

Drop this file into any project that follows the same style. It applies as-is; no per-project customization is required.

## Primary Goal

Implement explicit Java glue code that can be embedded safely into other people's systems.

This project favors:

- narrow module surfaces
- caller-owned lifecycle, configuration, and runtime policy
- optional integrations instead of hard-wired stacks
- thin adapters over third-party APIs
- operationally precise contracts covering defaults, failure modes, and ownership

This project is not trying to become an application framework, auto-configuration layer, or container policy engine.

## Source Of Truth

When a task touches a module, read in this order:

1. the matching root-level `.adoc` chapter for that module
2. `README.adoc` for repository-wide design priorities and the assembled book structure
3. `CODE_STYLE.md` for repository-wide coding conventions, formatting, and concrete style rules
4. `DESIGN_PHILOSOPHY.md` for the shape of new frameworks, annotations, aspects, and integration surfaces
5. the relevant `pom.xml` files for artifact boundaries, optional dependencies, and packaging intent
6. nearby source in the same package and module

If the task creates or revises root-level `.adoc` content, also read `ASCIIDOC_DOC_AUTHORING_GUIDE.md` before drafting or restructuring the chapter.

Useful anchors:

- `README.adoc` — repository-wide goals, module index, book structure.
- `CODE_STYLE.md` — repository-wide code, API, formatting, naming, concurrency, and exception conventions. Consult this whenever you write or edit Java.
- `DESIGN_PHILOSOPHY.md` — higher-level shape rules for designing frameworks, annotations, aspects, DI-container adapters, and pluggable SPIs. Consult this whenever you add a new framework, annotation, aspect, or extension point.
- `ASCIIDOC_DOC_AUTHORING_GUIDE.md` — root-level `.adoc` chapter structure, navigation patterns, and AsciiDoc house style. Consult this whenever you write or edit `.adoc` content.
- root-level module `.adoc` files — canonical module contracts, usage paths, defaults, lifecycle, troubleshooting, and verification guidance.
- representative modules for local style — sample three to five modules at random from the repository and read a few files in each to calibrate to the local style before making changes.

If the docs and code appear to disagree, verify the code and then bring the documentation back into sync.

## Repository Intent

The root-level guides make a few project-wide rules explicit. Preserve them.

- Modules are meant to be adopted selectively. Prefer the smallest surface that solves the problem.
- Libraries and applications often depend on different artifacts. Keep that distinction visible in code and docs.
- Dependency-injection and container support is an assembly convenience, not the only supported integration model.
- Runtime ownership stays with the caller unless the module is explicitly about lifecycle management.
- Operational detail is part of the contract. Defaults, threading, shutdown expectations, failure signatures, and classpath rules are not incidental.

## Design Rules

See `DESIGN_PHILOSOPHY.md` for the full set of rules governing new frameworks, annotations, aspects, DI-container adapters, and pluggable SPIs. The points below summarize the expectations that apply to any change in this repository.

- Prefer small, orthogonal types over broad feature bundles.
- Favor interfaces and focused adapters where the module wants replaceable behavior or a stable seam.
- Keep constructors and method signatures explicit about collaborators, ownership, and required configuration.
- Preserve public APIs unless the task explicitly requires an API break.
- Keep wrappers thin. Do not hide third-party behavior behind a private framework vocabulary unless that vocabulary already exists in the module.
- Choose additive helpers only when they reduce real duplication or coupling across a module boundary.
- If a module guide describes an exact runtime rule, such as choosing one endpoint implementation or wiring an explicit helper bean, keep that rule enforceable and obvious.

## Coding Style

See `CODE_STYLE.md` for the authoritative, detailed conventions — formatting, naming, annotations, class structure, concurrency, exceptions, and the distinctive space-before-parenthesis rule. The points below summarize the expectations that apply to any change in this repository.

- Match local formatting exactly.
- Preserve the established spacing style, including spaces before parentheses in declarations and control flow where the surrounding code does so.
- Preserve existing copyright headers and file prologues.
- Use explicit imports. Never use wildcard imports.
- Use descriptive names, even when they are long.
- Prefer straightforward imperative control flow over stream-heavy or functional rewrites.
- Avoid records, sealed types, Lombok, annotation-driven code generation, or other convenience features unless the surrounding module already uses them.
- Do not modernize code just because the build targets a recent JDK. This project's style is intentionally conservative and explicit.
- Use comments sparingly. Prefer clear code and focused Javadocs on public contracts.
- Do not refactor unrelated code while making a focused fix.

## Public API And Integration Guidance

- Public APIs should read clearly from the signature alone.
- Use overloads when they improve ergonomics without hiding behavior.
- Make defaults visible in constructors, fields, or clearly named helper methods.
- Keep null-handling, fallback behavior, and ownership rules explicit.
- Do not replace direct wiring with hidden registration, service scanning, or container-coupled assumptions unless the module already depends on that model.
- Preserve both direct-programmatic and container-based assembly paths when the module docs describe both.
- Keep optional dependencies optional in both code and build metadata whenever practical.

## Exceptions, Threads, And Lifecycle

- Follow the exception vocabulary already used by the package.
- Do not swallow failures.
- Wrap exceptions only when the new type or message adds contract value.
- Fail early on invalid wiring, missing providers, or contradictory configuration when that matches the module's existing behavior.
- Background work should have explicit start, stop, close, or listener semantics. Do not introduce hidden worker ownership.
- Resource ownership must stay obvious: if a caller is expected to close, destroy, or replace something, keep that visible in the API and docs.

## Documentation Duties

See `ASCIIDOC_DOC_AUTHORING_GUIDE.md` for the full authoring conventions — document model, section flow, navigation patterns, anchors, code-block style, and the repeating section templates. The points below summarize the documentation expectations that apply to any change here.

If you change public behavior, update the matching root-level `.adoc` chapter.

Documentation in this repository is expected to cover:

- module and artifact selection
- library versus application dependency guidance
- optional dependency boundaries
- quick-start wiring that matches the actual code
- defaults taken from constructors, fields, or configuration classes
- lifecycle, threading, shutdown, and ownership rules
- troubleshooting and common failure conditions

When editing root-level `.adoc` files, preserve the chapter model documented in `ASCIIDOC_DOC_AUTHORING_GUIDE.md`: anchor, single title, `[partintro]` or abstract, audience and versioning lines, anchored navigation sections, and concrete operational guidance.

## Verification

- Run focused tests for the affected module when possible.
- Verify documentation changes against code, POM files, and packaged resources instead of inferring behavior.
- Check artifact names, section anchors, defaults, and failure messages before treating the work as complete.
- Do not broaden verification into unrelated modules unless the build or the change genuinely requires it.

## Things To Avoid

- framework-style auto-configuration
- hidden static state unless the module already depends on it
- hidden background thread ownership
- broad internal abstractions that mostly serve this repository's own indirection
- clever one-liners that reduce readability
- broad refactors performed as part of a small change
- reformatting files outside the touched area
- documentation that markets the module instead of explaining real behavior
- promising behavior in docs that was not checked in source

## Prompting Shortcut

If you need to summarize the style for another LLM, use this:

"Write code in this project's style: explicit Java glue code, narrow module surfaces, caller-owned lifecycle and configuration, optional integrations, thin adapters over third-party APIs, descriptive naming, conservative abstraction, and operationally precise documentation. Match local formatting exactly, preserve direct wiring alongside any container-based helpers, and keep defaults, failure modes, and ownership visible in the API."
