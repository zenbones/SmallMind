# Code Style

This document describes both the design philosophy and the concrete code-formatting conventions of a conservative, explicit Java library codebase. It is intended for humans and LLMs producing code for this kind of project.

## One-Line Summary

Explicit Java glue code: narrow libraries that help applications integrate transports, persistence, logging, scheduling, packaging, caching, and related runtime seams without taking ownership of the surrounding system.

## Stable Design Priorities

- modules stay narrow enough to adopt selectively
- configuration, lifecycle, and runtime policy stay application-owned
- dependencies remain optional where practical
- wrappers stay thin over third-party APIs
- operational behavior is treated as part of the public contract

This style is not trying to win by framework ownership, annotation magic, or hidden conventions.

## Module Shape

Modules are organized as explicit layers rather than one umbrella abstraction.

Typical patterns:

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

Code should behave well inside somebody else's runtime.

- Do not assume ownership of startup, shutdown, logging backend, transport stack, container, or deployment layout unless the module is explicitly about that concern.
- Keep resource ownership obvious. If the caller must start, stop, close, destroy, or replace something, make that visible in the API.
- Keep background work explicit. Pollers, monitors, queues, and asynchronous wrappers should have clear lifecycle semantics.
- Fail fast on invalid wiring, contradictory configuration, missing providers, or impossible runtime combinations when the module already follows that pattern.
- Avoid static magic unless the existing package already depends on it and the behavior is well understood.

## API Design

### Make The Contract Readable

- Use descriptive names, even when they are long.
- Prefer explicit constructor arguments over hidden configuration.
- Use overloads when they improve ergonomics without obscuring behavior.
- Use bounded generics when the bounds express a real domain rule.
- Keep default behavior easy to discover in constructors, fields, or clearly named helpers.
- Make null handling, fallback rules, and replacement semantics deliberate and obvious.

### Keep Abstractions Honest

- Add interfaces when the code needs a stable seam or replaceable behavior.
- Add base classes only when they remove meaningful duplication or standardize important behavior.
- Keep adapters, codecs, factory beans, listeners, and helper types thin.
- Do not hide important third-party semantics behind a private abstraction layer that mostly renames things.

### Preserve Existing Boundaries

If a module already distinguishes between reusable-library usage and application wiring, preserve that distinction.

- Libraries may depend only on an API artifact while applications choose a runtime implementation.
- Spring support may exist, but direct wiring remains first-class.
- Optional integrations are opt-in and should stay that way.

### Static Factories And Builders

- Prefer descriptive verb-based names for static factory methods (`from`, `create`, `open`, or a domain-specific name) over generic `of` or `newInstance`, unless the surrounding module already uses the shorter form.
- When a type needs a builder, make the builder a peer class rather than a nested inner class, unless nesting genuinely aids discoverability.
- Builders return `this` from configuration methods and terminate in a `build()` method (or a named action method when the builder is a one-shot driver).
- Keep the direct constructor or direct factory reachable; a builder is an ergonomic surface, not the only way in.

## Concrete Formatting Conventions

### File Header

- Every source file starts with a block-comment license/copyright header.
- The header sits at line 1, above the `package` statement.
- Do not strip, reorder, or "modernize" the header. Preserve it verbatim when editing existing files and reproduce it when creating new files in the same module.

### Package And Imports

- Package names are all lowercase, dot-separated, and mirror the directory hierarchy.
- Imports are **always explicit** — never use star (`*`) imports, even for many symbols from the same package.
- Imports are sorted alphabetically within three groups, with a single blank line between each group:
  1. `java.*`
  2. `javax.*`
  3. Everything else (third-party and in-project)
- Do not add a blank line between `import` and `import static` beyond the grouping above; keep static imports minimal.

### Indentation And Whitespace

- **Indent with 2 spaces.** Never tabs. Never 4 spaces.
- Every nesting level adds exactly 2 spaces.
- No trailing whitespace at end of line.
- End files with a newline.
- Use blank lines liberally inside method bodies to separate logical phases (declarations, main work, return). A blank line immediately after the opening brace of a method body, separating local declarations from the work, is typical.

### Braces

- K&R style: opening brace on the same line as the declaration or control keyword; closing brace on its own line aligned with the opening statement.
- Method bodies, class bodies, `if`, `else`, `for`, `while`, `do`, `switch`, `try`, `catch`, and `finally` all follow this rule.
- `else`, `catch`, and `finally` appear on the same line as the preceding closing brace: `} else {`, `} catch (...) {`, `} finally {`.

### Space-Before-Parenthesis (Distinctive)

This is a deliberate house-style quirk. Apply it consistently:

- **Control keywords: one space before `(`.** `if (x)`, `for (...)`, `while (...)`, `switch (...)`, `catch (...)`, `return (x)`, `throw (...)` when followed by a parenthesized expression.
- **Method and constructor declarations: one space before `(`.** Example: `public String encrypt (InputStream stream, String password)`. This applies to every declaration, including no-arg methods: `public int hashCode ()`.
- **Method and constructor invocations: NO space before `(`.** Example: `encrypt(stream, password)`, `new StringBuilder()`, `list.add(x)`.
- Empty argument lists follow the same rule: declaration `foo ()`, invocation `foo()`.

### Other Spacing

- One space on each side of binary operators: `=`, `+`, `-`, `*`, `/`, `%`, `==`, `!=`, `<`, `<=`, `>`, `>=`, `&&`, `||`, `&`, `|`, `^`, `<<`, `>>`, `instanceof`.
- One space on each side of `?` and `:` in ternary expressions.
- No space between a unary operator and its operand: `!flag`, `-count`, `++i`.
- No space after `(` or before `)`.
- No space before `,` or `;`; one space after.
- No space before `.` in member access or method chains.
- Cast expressions: no space between the cast and the operand: `(String)object`.

### Line Wrapping

- Prefer keeping a declaration on one line when practical; line length is not strictly capped but readability rules.
- When a method signature spans lines, put the `throws` clause (and only the `throws` clause) on a continuation line indented **2 spaces** from the method's first line, with the opening brace on that continuation line after `throws ...`. Example: `public static String encrypt (...)` on one line, `  throws IOException, VaultCodecException {` on the next.
- When an argument list itself must wrap, continuation lines indent by **4 spaces** (two indent units) from the statement's starting column.
- When a long boolean condition must wrap, break before the `&&` or `||` and align the continuation readably.

### Annotations

- Place each annotation on its own line, above the annotated member. Do not trail them on the same line as the signature.
- When multiple annotations apply, stack them vertically with no blank lines between them.
- Javadoc precedes the first annotation.
- The only routine exception is parameter or type-use annotations, which appear inline with the thing they annotate (for example `@Nullable String name`).

### Method Body Structure

- Declare local variables at the top of the method, followed by a blank line before the first statement that acts on them. This matches the existing house preference for a visible separation between setup and work.
- Use guard clauses and early returns for edge cases; keep the happy path at the end of the method.
- Separate logical phases inside a longer body with blank lines — setup, main work, cleanup, return.

## Naming Conventions

- **Classes, interfaces, enums, annotations:** `PascalCase`. No `I` prefix for interfaces. No `E` prefix for enums. No `Impl` suffix unless it genuinely disambiguates a single implementation of a well-known interface.
- **Abstract base classes:** prefix with `Abstract` (e.g., `AbstractFooProcessor`).
- **Methods:** `camelCase`. Accessors use `get`/`set`; boolean queries use `is` or `has`; actions use verb phrases.
- **Instance fields:** `camelCase`, almost always `private`, no `m_` or `_` prefix.
- **Static final constants:** `ALL_CAPS_WITH_UNDERSCORES`.
- **Local variables and parameters:** `camelCase`, descriptive. Avoid one-letter names except for tight loop counters or stream lambda parameters.
- **Generic type parameters:** single capital letter (`T`, `E`, `K`, `V`) or a short, descriptive capitalized name when multiple parameters need disambiguation (`<I extends Serializable & Comparable<I>, D extends AbstractDurable<I, D>>`).
- **Packages:** all lowercase, dot-separated, mirroring directory layout. No underscores or camelCase.
- **Interface suffixes:** use `-Listener` for callback interfaces, `-Handler` for strategy or processing interfaces, `-Factory` for factory interfaces. Reserve `-Service` for names that genuinely describe a service surface rather than using it as a generic suffix.

## Class Structure

Within a class, order members as:

1. Static final constants
2. Static fields (including `ThreadLocal`, initializers)
3. Instance fields (all private, grouped logically)
4. Constructors (no-arg first, then by argument count; `this(...)` / `super(...)` delegation where useful)
5. Public API methods (interface-contract methods, accessors, domain methods)
6. Overridden methods (`@Override`)
7. Private helper methods
8. Nested / inner types

Other rules:

- Instance fields are `private`. Avoid `public` and `protected` fields.
- Declare every instance field `final` unless it is explicitly mutable state that changes after construction. Non-final fields should be the visible exception, not the default.
- Use `@Override` on every method that overrides a supertype method, including interface implementations.
- Do not use `final` on parameters or local variables as a matter of habit — leave it off unless the surrounding code already uses it.
- Do not use `this.` unless required for disambiguation (for example, a setter or constructor assigning a parameter to a field of the same name).
- Prefer straightforward `public` / `private` visibility. Reach for `protected` only when a real subclass contract requires it.
- On `Serializable` types, prefer not to declare `serialVersionUID`. The default auto-computed id rejects deserialization when the class's serialized shape changes — that rejection is usually the safety mechanism you want. Declare an explicit `serialVersionUID` only when you have a specific reason to accept structural changes across versions, and record why.

## Control Flow And Idioms

- Favor imperative logic, named locals, and readable branching. Accept a small amount of repetition if it keeps control flow obvious.
- Use `switch` statements with explicit `case` labels and an always-present `default` branch. Traditional `switch` is preferred over newer expression/pattern forms unless the surrounding module already uses them.
- Use lambdas and method references where they clearly improve readability (for example, `ThreadLocal.withInitial(HashSet::new)`), but do not rewrite imperative code into streams for style alone.
- Streams are acceptable for transformation pipelines that read more clearly than loops, but the default is imperative code.
- Prefer `try`-with-resources for anything implementing `AutoCloseable`.
- Use multi-catch (`catch (A | B exception)`) when the handling is identical.

## Null, Optionals, And Collections

- Handle null directly: `if (x == null)`, `x != null ? x.foo() : fallback`.
- Do not use `Optional` in library APIs or internal code unless the surrounding module already does so.
- Do not introduce `@Nullable` / `@NonNull` annotations unless the module already depends on them.
- Document nullability in Javadoc where it matters. Make fallback and replacement semantics explicit in names and docs.
- Prefer ordinary collection types (`List`, `Set`, `Map`) over exotic wrappers in public signatures.

## Exceptions And Lifecycle

- Follow the exception vocabulary already present in the package — do not invent parallel hierarchies.
- Preserve the module's checked-vs-runtime boundary.
- Do not swallow failures. If an exception must be wrapped, use a type or message that adds real contract value; otherwise rethrow.
- Where a custom exception base exists with a `String format, Object... args` constructor, use it: `throw new FormattedFooException("Unknown id(%s)", id)`.
- Make thread ownership, polling behavior, queue limits, shutdown rules, and replacement semantics explicit in code and docs when they matter.
- Avoid hidden retries, silent fallbacks, or invisible worker threads.

## Concurrency And Resources

### Thread Creation

- When code creates a `Thread` directly, give it a descriptive name via `setName(...)` and set `setDaemon(true)` or `setDaemon(false)` explicitly. An unnamed thread is much harder to attribute in logs and thread dumps.
- Prefer named patterns that identify both the owning module and the role, for example `"claxon-emitter"` or `"phalanx-worker-" + index`.
- When using a `ThreadFactory` or an executor, supply a factory that applies the same naming and daemon conventions. Do not rely on the default factory for long-lived workers.

### Closeable And AutoCloseable

- Implementations of `close()` should be idempotent: calling `close()` a second time must be safe and a no-op.
- Guard with a `closed` flag. Use a plain `boolean` with the class's existing synchronization, or an `AtomicBoolean` when the class is otherwise lock-free — pick one pattern and stay consistent with surrounding code.
- Document whether the caller owns the resource's lifecycle or whether the component closes itself. If a component spawns owned resources, close them in the component's own `close()`.

### Logger Acquisition

- Use a single logging abstraction per module and acquire loggers consistently. Do not mix logger styles (static field in one class, manager lookup in another) within the same module.
- Whatever acquisition pattern the project chose — `private static final Logger LOGGER = ...`, or a centralized `LoggerManager.getLogger(Class<?>)` — keep it uniform. New code should match the surrounding files.
- Parameterize log messages rather than concatenating when the logger supports it.

## Javadoc And Comments

- Public classes and public methods should have Javadoc.
- Javadoc uses the conventional `/**` on its own line, leading `*` on each line, and `@param` / `@return` / `@throws` tags with concise descriptions. Sentences end in periods.
- Use inline block tags like `{@code ...}` and `{@link ...}` for symbols, code tokens, and cross-references.
- Inline comments are sparse and used only where the *why* is non-obvious: subtle invariants, workarounds, behavior that would surprise a reader. Don't explain *what* well-named code already says.

## Dependency And Integration Style

- Keep dependencies optional where practical.
- Do not move integration-specific code into a core module without a strong reason.
- Treat Spring XML, factory beans, and container helpers as optional assembly surfaces, not mandatory framework ownership.
- Keep direct-programmatic usage viable even when a Spring helper exists.
- When implementing a Spring `FactoryBean<T>`, also implement `InitializingBean`, build the instance in `afterPropertiesSet()`, hold it in a private field, and return it from `getObject()`. Implement `getObjectType()` and `isSingleton()` explicitly.
- Avoid component-scanning, auto-registration, and auto-configuration habits unless the module already depends on that model.
- Preserve third-party types in signatures when those types carry important integration meaning.

## Documentation Style

When code changes affect public behavior:

- update the matching documentation chapter
- document defaults from code, not from memory
- keep library-versus-application guidance explicit
- describe lifecycle, threading, and troubleshooting details when they are part of the contract
- use real artifact coordinates and version variables in dependency examples

Keep documentation factual, restrained, and source-backed.

## What Makes This Style Distinct

Compared with a typical modern Java framework codebase, this style is:

- more module-oriented than platform-oriented
- more explicit about who owns runtime policy
- more comfortable with direct wiring and Spring XML as peers
- less interested in annotation-driven magic
- more willing to use verbose names when they improve clarity
- more likely to expose a narrow adapter than to build an internal meta-framework
- deliberately quirky in formatting (space before `(` in declarations, not invocations; 2-space indent)

## Instructions For LLMs

When generating code for a project following this style:

1. Read the matching documentation and nearby code before writing anything.
2. Match the local formatting exactly, especially the space-before-parenthesis rule for declarations vs. invocations, the 2-space indent, and the import grouping.
3. Preserve the full license header on every file.
4. Preserve narrow module boundaries and optional integrations.
5. Prefer explicit APIs, direct wiring, and thin adapters.
6. Keep lifecycle, ownership, defaults, and failure behavior visible.
7. Do not introduce framework-style machinery unless the task clearly requires it.
8. Update the corresponding documentation if the public contract changes.

## Short Prompt Template

Use this when prompting another model:

"Work in the established style of this codebase. This is explicit Java glue code: narrow modules, caller-owned lifecycle and configuration, optional integrations, thin adapters over third-party APIs, conservative abstractions, descriptive names, and operationally precise documentation. Formatting rules: 2-space indent, K&R braces, explicit imports grouped java/javax/other with alphabetical order, one space before `(` in method and constructor declarations and after control keywords, no space before `(` in invocations or `new`. Preserve file license headers verbatim. Private fields with no prefixes, `Abstract` prefix for base classes, no `I` prefix on interfaces, `@Override` everywhere applicable, try-with-resources, direct null checks (no `Optional`). Match local formatting exactly, preserve direct wiring alongside any Spring helpers, and keep defaults, ownership, and failure modes visible in the code and docs."
