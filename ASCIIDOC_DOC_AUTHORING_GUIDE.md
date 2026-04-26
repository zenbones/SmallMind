# AsciiDoc Authoring Guide

This guide describes the documentation pattern that produces the `.adoc` files in this repository.
Use it when creating or revising `.adoc` content so the result reads like the existing guides
instead of a generic product manual.

Drop this file into any project and customize the project-specific examples in
[House Pattern For Root-Level Documents](#house-pattern-for-root-level-documents) and
[Installation](#installation). Everything else applies as-is.

## What Good Documentation Does

A guide for this project should help a reader answer concrete integration questions quickly:

- which artifact or module do I need?
- what do I wire or configure myself versus what the library owns?
- what defaults, lifecycle rules, and failure modes matter?
- what optional dependencies or runtime requirements must be present?
- where do I start if I am new, and where do I jump if I already know the problem?

Documentation should be operational, not marketing. It should describe API boundaries,
integration choices, and runtime consequences — not the project's ambitions or general virtues.

## Document Model

A documentation file can take one of two basic shapes: a standalone article that owns its
own book-level attributes, or an include-safe chapter that defers those attributes to an
assembling `README.adoc`. The standalone form has a book-style variant for long,
reference-heavy documents.

### Standalone Article

A standalone article owns its own `:doctype:`, `:toc:`, and other book-level attributes.
It renders independently as a complete reference document.

Use this form when:

- the repository has no assembled `README.adoc`
- the document covers a module large enough to deserve its own rendering context
- the audience is expected to navigate to the file directly

### Include-Safe Chapter

An include-safe chapter keeps exactly one top-level title (`= Title`), omits book-level
attributes, and uses explicit section anchors for navigation. It should read cleanly on its
own and also cleanly when included into `README.adoc`.

Use this form when:

- the repository uses `README.adoc` as an assembled book that `include::...[]` individual chapters
- the document is one of several modules sharing a common navigation structure

### README.adoc

`README.adoc` is the repository's front door. Two shapes are valid; pick whichever fits the
project and be consistent.

**Front-door index.** A short file that orients the reader and links out to per-module
documents. Keep the attribute set minimal (`:toc:`, `:toclevels:`, `:icons: font`), write
one or two paragraphs of project-level context, then list the modules as
`link:FILE.adoc[Label] — short description` bullets. The module files stand alone and render
independently. This shape suits repositories where readers navigate to a specific module
rather than reading the project end-to-end.

**Assembled book.** A file that owns book-level attributes, a preface, a module index
table, and an `include::...[]` list that pulls in each chapter. Chapter files must be
include-safe (see the next section). This shape suits repositories where readers benefit
from a single rendered document covering the whole project.

When editing `README.adoc`, regardless of shape:

- keep the module list (bullets or table) in sync with the actual files
- if using `include::...[]`, keep the include list aligned with the chapter files
- avoid repeating reference material that belongs in a dedicated chapter
- make it clear whether the reader is choosing a parent artifact, a specific module, or one
  of several integration surfaces

`README.adoc` is the front door, not the place for large amounts of per-module operational
detail.

## House Pattern For Root-Level Documents

Whether you write a standalone article or an include-safe chapter, the opening should follow
a consistent shape.

### Standalone Article

```adoc
= Module Name: Short Subtitle
:doctype: article
:toc: left
:toclevels: 3
:sectanchors:
:sectlinks:
:source-highlighter: rouge
:icons: font

== The Problem This Solves

...prose that names the concrete problem before introducing the solution...
```

The opening section should state the problem the module solves in concrete terms before
introducing the solution. Name the forces that drive the problem, then state what the
module does about them.

`:sectanchors:` and `:sectlinks:` make each heading independently linkable, which readers
need as soon as navigation lists and cross references exist. Add `:sectnums:` only when
numbered sections aid navigation (typically long, reference-heavy documents); most modules
read better without them.

### Book-Style Article

A variant of the standalone form, used when a single document covers a comprehensive
reference surface and benefits from numbered sections and a formal abstract.

```adoc
= Module Name: Short Subtitle
Author or Project Name
:doctype: book
:toc: left
:toclevels: 3
:sectnums:
:sectnumlevels: 3
:source-highlighter: rouge
:icons: font

[abstract]
== Abstract

...one or two paragraphs naming the problem surface and the module's answer to it...
```

Use this form when the document is long enough that numbered sections help the reader
orient, and the opening deserves a formal abstract rather than a terse problem statement.
Everything else — section flow, conventions, examples — follows the same rules as a
standalone article.

### Include-Safe Chapter

```adoc
[[module-anchor, Module Name]]
= Module Name

[partintro]
Module Name is the library's ...

Audience: ...
Versioning: examples use `${project.version}` or a clearly defined version placeholder
```

Preserve these rules for include-safe chapters:

- Start with a document anchor and one `= Title`.
- Use `[partintro]` for the opening summary.
- Keep the opening factual and boundary-oriented: what the module is, what it does well, and
  what it deliberately leaves to the caller.
- Include both `Audience:` and `Versioning:` lines.
- Do not add `:doctype:`, `:toc:`, or similar book-level attributes.

The opening can be one or two paragraphs and may include a short bullet list of stable
invariants when that clarifies the design.

## Common Section Flow

Not every document needs every section. Use the sections that match the actual API surface.
Omit filler.

1. Navigation and orientation
   - `Quick Navigation` (for long documents)
   - `Reading Paths` (for documents with distinct audiences)
   - `At A Glance`

2. Problem and fit
   - `The Problem This Solves` or `Why Use ...`
   - `Conceptual Model`
   - artifact or module selection guidance (`Choose The Right Artifact`, `Project Layout`, etc.)
   - `Installation`
   - `Quick Start`, `Usage Examples`, or `Example Catalog`

3. Reference material
   - `API Reference`
   - core types, contracts, parameters, configuration references
   - ownership models, transport matrices, or package references

4. Operational guidance
   - `Behavioral Reference` (edge cases, invariants, semantic details)
   - lifecycle and threading sections
   - error handling or error model sections
   - `Operational Notes`
   - `Troubleshooting`
   - `Failure Signatures`
   - `Limitations and Known Issues`
   - `Minimal Checklist` and `Verification Checklist`

5. Closing material when warranted
   - `Dependency Notes`
   - `Defaults Reference`
   - `See Also`

Do not force every document into a rigid template. Use the sections that match the module's
actual surface. Omit filler.

## Repeating Section Patterns

A few section types are worth using consistently across documents.

### Quick Navigation

Use `Quick Navigation` when the document is large enough that readers need a section picker.
Write it as a bullet list of cross references near the top.

```adoc
== Quick Navigation

* <<module-at-a-glance,At A Glance>>
* <<module-installation,Installation>>
* <<module-quick-start,Quick Start>>
* <<module-troubleshooting,Troubleshooting>>
```

### Reading Paths

`Reading Paths` is not filler. It tells different audiences what to read first.

Common patterns:

- a first-time adopter path
- a library-author path versus an application-author path
- a fast path for readers who already know the problem and need exact behavior

Write these as ordered lists or short bullet lists with cross references.

### At A Glance

`At A Glance` should help the reader choose the correct starting point in seconds.
It is usually a table.

Typical uses:

- problem-to-section tables
- artifact or type-family comparison tables
- matrices showing when to reach for one integration surface versus another

Common column-weight patterns:

- `[cols="1,3", options="header"]` — label and description
- `[cols="2,3", options="header"]` — comparison of two aligned things
- `[cols="1,1,1,3", options="header"]` — parameter / type / default / description

Always use `options="header"` when the table has a header row.

### Configuration Reference

A configuration reference table documents every tunable parameter in one place. Distinct
from `At A Glance`: the latter orients, the former is the authoritative parameter list.

```adoc
[cols="1,1,1,3", options="header"]
|===
| Parameter | Type | Default | Description

| `maxConnections`   | `int`     | `16`     | Upper bound on concurrent connections.
| `idleTimeout`      | `Duration`| `30s`    | Time an idle connection waits before closing.
| `strictMode`       | `boolean` | `false`  | Reject requests that fail optional validation.
|===
```

Keep the defaults column honest: copy from the constructor, field initializer, or config
class — do not invent or round.

### Conceptual Model

Use a `Conceptual Model` section when the design has non-obvious invariants or vocabulary
the rest of the document depends on. Write in prose. Name key concepts in bold on first use.
Use definition lists for terms that need precise technical definitions.

```adoc
Source repository::
    The _canonical_ repository where shared content is authored and maintained.

Target repository::
    A _mirror_ repository that receives updates propagated from the source.
```

### Installation

Installation sections should be explicit about dependency shape. Cover:

- exact artifact ids with group and version
- when a reusable library should depend only on an API artifact
- when an application must add a concrete runtime integration
- optional dependency matrices
- common classpath mistakes or runtime requirements

Prefer real Maven snippets with actual coordinates. Use a version placeholder consistently
(`${project.version}` or a documented attribute).

### Quick Start And Example Sections

Examples should be intentionally small, realistic entry points into the API.

- Use actual class names, method names, and artifact names from the repository.
- Show explicit wiring rather than hidden auto-configuration.
- Prefer several focused examples over one oversized showcase.
- Use callouts (`<1>`, `<2>`) to annotate significant lines without interrupting the code.
- Give a code block a title with a leading `.Title` line when the block would otherwise
  be hard to distinguish from neighboring snippets.

```adoc
.Canonical mirror, skipping the `local` subtree
[source,java]
----
SomeUtility.process(
    Paths.get("/opt/repos/source"),  <1>
    Paths.get("/opt/repos/target"),  <2>
    "local"                          <3>
);
----
<1> The root of the canonical source tree.
<2> The root of the mirror target tree.
<3> A directory name that is skipped at every level of the tree.
```

Write callout annotations as full sentences: capitalize the first word and end with a
period. Describe the significance of the line, not a restatement of what it literally does.

### Behavioral Reference

Use a `Behavioral Reference` section (or named subsections within it) to document edge cases,
invariants, and semantic details that readers will look for by name. Subsections like
`Idempotency`, `Error Handling`, `Skip Semantics`, and `Output Format` are examples of
behavioral facts worth naming explicitly.

Write each behavioral subsection as:

1. a statement of the invariant or rule
2. the mechanism that enforces it
3. the practical implication the reader should plan for

### Operational Sections

Document operational behavior aggressively when the code supports it:

- lifecycle and shutdown ownership
- threading and concurrency model
- resource ownership and replacement rules
- exact defaults from constructors or fields
- misconfiguration symptoms and failure signatures
- checklists for verification work

If a module has surprising behavior, document it where the reader will look for it, not only
in Javadocs.

### Limitations and Known Issues

Use a `Limitations and Known Issues` section for deliberate design constraints and known
defects. Write each entry as a definition list item: the limitation name as the term, a
factual explanation as the body.

```adoc
== Limitations and Known Issues

No deletion::
    Files removed from the source are not removed from the target. This is intentional:
    the utility propagates additions and updates, not removals.

No dry-run mode::
    There is no preview option. Idempotency makes a real run harmless as a preview.
```

### See Also

`See Also` goes at the end of the document. Write it as a flat bullet list that mixes
internal cross references, cross-file references, and external URLs as needed. No
categorization or grouping unless the list is long enough to genuinely need it.

```adoc
== See Also

* <<behavioral-reference,Behavioral Reference>>
* xref:OTHER.adoc[Other Module] — the companion module that consumes this output
* https://example.com/spec[Upstream specification]
```

Keep entries short. A trailing em dash and one-clause gloss is fine when the label alone
does not make the relevance obvious.

## Writing Priorities

Use the documentation to make these points explicit when they exist in the code:

- the smallest artifact or layer a user can adopt
- the difference between library usage and application usage
- direct-programmatic wiring versus Spring, XML, or assembly helpers
- optional integrations and what they unlock
- runtime providers or singleton constraints that must be unique on the classpath
- ownership of startup, shutdown, close, destroy, or replacement operations
- threading, queueing, background-worker, or polling behavior
- exact defaults from constructors or fields
- exact exceptions, rejection semantics, or failure messages when they matter
- non-goals and scope boundaries that prevent wrong integration assumptions

Describe non-goals only when they prevent a realistic integration mistake. Keep that material
factual, brief, and secondary to the positive contract. Do not define a document mainly by
what the library is not, and do not use negative, dismissive, or exaggerated contrast language
when a straightforward description of what the module does is enough.

## AsciiDoc Conventions To Preserve

### Anchors

- Always use the long form: `[[id]]` for a bare anchor, `[[id, Label]]` for the top-level
  chapter anchor of an include-safe document. Do not use the short form `[#id]`.
- Write anchor ids in lowercase kebab-case (`[[behavioral-reference]]`, not `[[BehavioralReference]]`
  or `[[behavioral_reference]]`).
- Add an explicit anchor whenever a section is referenced from navigation lists, a table
  of contents built by hand, another document, or `README.adoc`.

### Cross References

- Same-document references: `<<anchor,Label>>`. Always include the label — a bare
  `<<anchor>>` relies on AsciiDoc's heuristic and reads poorly when rendered out of context.
- Cross-file references: `xref:FILE.adoc[Label]` for references that should render as a
  link in the current document, or `link:FILE.adoc[Label]` in plain navigation lists such as
  a README index.
- Prefer cross references over raw URLs for anything local to the repository.

### Headings and Sectioning

- Use `Title Case` for headings.
- Prefer `==`, `===`, `====`; go deeper only when the material genuinely needs it.
- Use `[appendix]` sparingly, only when the material is truly appendix-style.

### Source Blocks

- Use explicit languages: `java`, `xml`, `json`, `shell`, `properties`, `text`, etc.
- Use the comma-no-space form: `[source,java]`.
- Delimit with `----`.
- Prefix a code block with a `.Title` line when the block is one of several similar snippets
  and the reader needs a label to distinguish them.
- Use source callouts (`<1>`, `<2>`) to annotate significant lines without interrupting the
  code. Write callout text as a full sentence, capitalized and ending with a period.

### Tables

- Always supply explicit column specs: `[cols="1,3", options="header"]`.
- Use `options="header"` when the table has a header row.
- See `At A Glance` and `Configuration Reference` for common column-weight patterns.

### Admonitions

- `[NOTE]` — edge cases, behavioral caveats, supplementary context the reader should be
  aware of but will not usually hit first.
- `[IMPORTANT]` — failure modes, thread-safety constraints, and gotchas the caller must
  handle correctly to avoid incorrect behavior.
- `[TIP]` — genuinely useful shortcuts, not padding. Most documents do not need any.
- `[WARNING]` and `[CAUTION]` — reserve for operational hazards (data loss, irreversible
  state changes). If a `[NOTE]` or `[IMPORTANT]` suffices, use that instead.

### Definition Lists

- Use `term::` for parameters, options, error types, configuration keys, limitations, or
  any terms with precise technical meaning. Indent the body below the term.
- A definition-list body can span several paragraphs when the explanation needs it;
  indent each continuation paragraph to match the first.

### Bold and Inline Emphasis

- Use bold (`*term*`) when introducing a key concept by name for the first time.
- Use backticks for code identifiers, class names, method names, and literal values.

### Bullet Lists

- Use capitalized, period-terminated sentences for items that are complete thoughts.
- Use lowercase, un-punctuated fragments for short label-style items.
- Be consistent within a single list. Do not mix sentences and fragments in one list.

### Examples

- Keep examples minimal but concrete. Use real type names, method names, artifact ids, and
  package names from the repository — not generic placeholders.
- Use a documented version placeholder consistently (for example `${project.version}` in
  Maven snippets, or a dedicated document attribute). Do not scatter literal version strings
  through examples.

## What To Verify In Source Before Finalizing

1. Confirm public type names, method names, and configuration parameter names in code.
2. Confirm artifact ids, module boundaries, and optional dependencies in build files.
3. Confirm defaults in constructors, field initialization, helper beans, or configuration classes.
4. Confirm lifecycle and ownership statements in the concrete implementation.
5. Confirm threading, polling, or concurrency statements in the real runtime code.
6. Confirm troubleshooting advice against actual failure conditions or error messages.
7. Confirm every quick-navigation and README cross reference still points at a real anchor.

Prefer source-backed statements over inferred behavior.

## Workflow

1. Read the existing document (if any) and map its anchors and major headings.
2. Read the relevant source files and build files to identify the actual API surface.
3. Identify stale artifact names, missing defaults, weak examples, and unclear ownership
   boundaries.
4. Patch in small passes so section names and anchors remain stable where possible.
5. Review the rendered flow mentally as a standalone reference for someone consuming the
   library for the first time.
6. If the file is include-safe, also review it as an included chapter.
7. Finish with a verification pass focused on ownership, defaults, lifecycle,
   troubleshooting, and navigation links.

## Anti-Patterns To Avoid

- generic framework-style prose that ignores this project's specific integration model
- writing around the code instead of documenting what the code actually does
- marketing language where the style should be factual and operational
- trite or clichéd phrasing that sounds generated rather than considered ("every service eventually", "out of the box", "at its core", etc.)
- defining a module mainly by what it is not, or using negative, dismissive, or exaggerated
  contrast language
- vague statements about convenience that hide ownership boundaries
- oversized examples that bury the entry point
- copying a section layout that does not fit the module's real API surface
- adding sections that add scan time but not usable information
- book-level attributes in include-safe chapter files

## Definition Of Done

A `.adoc` update is complete when:

- a reader can identify the right artifact or integration surface quickly
- ownership, defaults, lifecycle, and operational constraints are explicit
- examples match the actual code, artifact names, and build coordinates
- troubleshooting and edge cases address the module's real failure modes
- section anchors, quick navigation, and README references are correct
- the tone remains concrete, restrained, and consistent with the repository's established style
