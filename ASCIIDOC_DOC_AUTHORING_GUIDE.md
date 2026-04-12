# AsciiDoc Authoring Guide (Smallmind)

This guide describes the documentation pattern used by `README.adoc` and the root-level module chapters in this repository. Use it when creating or revising `.adoc` content so the result reads like the existing Smallmind guides instead of a generic product manual.

## What Smallmind Documentation Must Do

A Smallmind guide should help a reader answer concrete integration questions quickly:

- which artifact or module do I need?
- what do I wire myself versus what the library owns?
- what defaults, lifecycle rules, and failure modes matter?
- what optional dependencies or runtime providers must be present?
- where do I start if I am new, and where do I jump if I already know the problem?

The root guides are operational documents, not marketing pages. They describe module boundaries, integration choices, and runtime consequences.

## Document Model

Smallmind documentation has two layers.

1. `README.adoc` is the assembled book.
   - It owns the book attributes, preface, design-priority framing, module index table, and `include::...[]` list.
   - It should stay focused on repository-wide orientation, not detailed module reference material.

2. Each root-level module `.adoc` file is an include-safe chapter.
   - It should read cleanly on its own.
   - It should also read cleanly when included into `README.adoc`.
   - It is the main source of truth for module intent, artifact selection, lifecycle, defaults, troubleshooting, and verification guidance.

That means a root-level module chapter should keep exactly one top-level title, avoid book-level attributes, and use explicit section anchors for navigation.

## House Pattern For Root-Level Module Chapters

Every current root-level module guide follows this opening shape:

```adoc
[[module, Module]]
= Module

[partintro]
Module is Smallmind's ...

Audience: ...
Versioning: examples use `${smallmind.version}` ...
```

Preserve these rules:

- Start with a module anchor and one `= Title`.
- Use `[partintro]` for the opening summary.
- Keep the opening factual and boundary-oriented: what the module is, what it does well, and what it deliberately leaves to the caller.
- Include both `Audience:` and `Versioning:` lines. All current root-level module guides do.
- Do not add `:doctype:`, `:toc:`, or similar book-level attributes to module chapters.

The opening can be one or two paragraphs, and it may include a short bullet list of stable rules when that clarifies the model, as `SCRIBE.adoc` does.

## Common Section Flow

Not every module uses every section, but the current repository has a strong common structure. Most successful Smallmind module guides contain many of these sections in roughly this order:

1. Navigation and orientation
   - `Quick Navigation`
   - `Reading Paths`
   - `At A Glance`

2. Adoption and fit
   - `Why Use ...`
   - `Project Layout`, `Choose A Module`, `Choosing The Right Surface`, or similar module-selection guidance
   - `Installation`
   - `Quick Start`, `First Run In ...`, or `Example Catalog`

3. Reference material
   - `API By Use Case`
   - `API Index`
   - core types, contracts, configuration references, ownership models, transport matrices, or package references

4. Operational guidance
   - lifecycle and threading sections
   - error handling or error model sections
   - `Operational Notes`
   - `Behavioral Edge Cases`
   - `Troubleshooting`
   - `Failure Signatures`
   - `Minimal Checklist` and `Verification Checklist`

5. Appendices or closing material when warranted
   - `Defaults Reference`
   - `See Also`

Do not force every chapter into a rigid template. Use the sections that match the module's actual surface. Omit filler.

## Repeating Section Patterns Observed In The Repository

The root guides use a few section types consistently enough that they are part of the house style.

### Quick Navigation

This is usually a bullet list of cross references near the top of the chapter.

```adoc
== Quick Navigation

* <<module-at-a-glance,At A Glance>>
* <<module-installation,Installation>>
* <<module-quick-start,Quick Start>>
* <<module-troubleshooting,Troubleshooting>>
```

Use it when the chapter is large enough that readers need a section picker.

### Reading Paths

`Reading Paths` is not filler. It tells different audiences what to read first.

Common patterns:

- a first-time adopter path
- a library-author path versus an application-author path
- a fast path for readers who already know the problem and need exact behavior

Write these as ordered lists or short bullet lists with cross references.

### At A Glance

`At A Glance` is usually a table. It should help the reader choose the correct starting point, module, package family, or integration surface in seconds.

Typical uses in the current docs:

- problem-to-section tables
- artifact or family comparison tables
- matrices showing when to reach for a package or feature

### Installation

Installation sections in Smallmind docs are explicit about dependency shape.

Cover the facts the reader needs:

- exact artifact ids
- when reusable libraries should depend only on an API artifact
- when applications must add one concrete runtime integration
- optional dependency matrices
- common classpath mistakes or runtime requirements

Prefer real Maven snippets using `${smallmind.version}`.

### Quick Start And Example Sections

Examples should be intentionally small, realistic entry points into the API.

- Use actual Smallmind class names and artifact names.
- Show explicit wiring rather than hidden auto-configuration.
- Keep examples close to how the code is really meant to be entered.
- Prefer several focused examples over one oversized showcase.

### Operational Sections

The existing module guides document operational behavior aggressively. Include these sections when the code supports them:

- lifecycle and shutdown ownership
- threading and concurrency model
- resource ownership and replacement rules
- exact defaults from constructors or fields
- misconfiguration symptoms and failure signatures
- checklists for production or verification work

If the module has surprising behavior, document it where the reader will look for it, not only in Javadocs.

## Writing Priorities

Use the documentation to make these points explicit when they exist in the code:

- the smallest layer a user can adopt
- the difference between library usage and application usage
- direct-programmatic wiring versus Spring or XML assembly helpers
- optional integrations and what they unlock
- runtime providers or singleton choices that must be unique on the classpath
- ownership of startup, shutdown, close, destroy, or replacement operations
- threading, queueing, background-worker, or polling behavior
- defaults and fallback behavior
- exact exceptions, rejection semantics, or failure messages when they matter
- non-goals and scope boundaries that prevent wrong assumptions

Describe non-goals only when they prevent a realistic integration mistake or wrong expectation.
Keep that material factual, brief, and secondary to the module's positive contract.
Do not define a chapter primarily by what the module is not, and do not use negative or overstated contrast language when a straightforward description of what the module does is enough.

Smallmind docs are strongest when they explain what exists and, where necessary, the specific boundaries that keep readers from making the wrong integration assumptions.

## AsciiDoc Conventions To Preserve

Follow the conventions already established in the root guides.

- Use explicit anchors: `[[module-section, Section Title]]` when the section is referenced from navigation lists or `README.adoc`.
- Use cross references instead of raw URLs: `<<anchor,label>>`.
- Use `Title Case` for headings.
- Prefer `==`, `===`, and `====`; go deeper only when the material genuinely needs it.
- Use source blocks with explicit languages such as `java`, `xml`, `json`, `properties`, or `text`.
- Use tables with explicit column specs and `options="header"` when the table has a header row.
- Prefer `[IMPORTANT]` and `[NOTE]` admonitions for real constraints or caveats. They are common in the current docs. `TIP` is not part of the current house style.
- Use `[appendix]` sparingly, only when the material is truly appendix-style. The repository uses it very rarely.

Keep examples minimal but concrete. If the code uses `LoggerManager`, `MavenRepository`, `BatchJobFactory`, `ThrongClient`, or similar types, use those exact names rather than generic placeholders.

## README-Specific Guidance

When editing `README.adoc`:

- preserve the book attributes and `[preface]` structure
- keep the repository-wide sections factual and short
- keep the `Module Index` table in sync with the included chapters
- keep the `include::...[]` list aligned with the root-level chapter files
- avoid repeating full module reference material that already belongs in a chapter

`README.adoc` is the front door and assembly shell, not the place for large amounts of per-module operational detail.

## What To Verify In Source Before Finalizing

1. Confirm public type names, method names, and configuration parameter names in code.
2. Confirm artifact ids, module boundaries, and optional dependencies in POM files.
3. Confirm defaults in constructors, field initialization, helper beans, or configuration classes.
4. Confirm lifecycle and ownership statements in the concrete implementation.
5. Confirm threading or polling statements in the real runtime code.
6. Confirm troubleshooting advice against actual failure conditions or error messages.
7. Confirm every quick-navigation and README cross reference still points at a real anchor.

Prefer source-backed statements over inferred behavior.

## Workflow That Fits This Repository

1. Read the current chapter and map its anchors and major headings.
2. Compare it against the matching module source and `pom.xml` files.
3. Identify stale artifact names, missing defaults, weak examples, and unclear ownership boundaries.
4. Patch in small passes so section names and anchors remain stable where possible.
5. Review the rendered flow mentally as both a standalone chapter and an included README chapter.
6. Finish with a verification pass focused on defaults, lifecycle, troubleshooting, and navigation links.

## Anti-Patterns To Avoid

- generic framework-style prose that ignores Smallmind's explicit integration model
- writing around the code instead of documenting what the code actually does
- marketing language where the existing docs are factual and operational
- defining a module mainly by what it is not, or using negative, dismissive, or exaggerated contrast language
- vague statements about convenience that hide library/application ownership boundaries
- oversized examples that bury the entry point
- copying a section layout that does not fit the module's real API or runtime behavior
- adding sections that add scan time but not usable information

## Definition Of Done

A Smallmind `.adoc` update is complete when:

- a reader can choose the right artifact or module surface quickly
- ownership, defaults, lifecycle, and operational constraints are explicit
- examples match the actual code and artifact names
- troubleshooting and edge cases address the module's real failure modes
- section anchors, quick navigation, and README references are still correct
- the tone remains concrete, restrained, and consistent with the existing root-level guides
