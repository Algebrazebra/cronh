# AGENTS.md

## Project overview

`cronh` is a Scala 3 library for defining and rendering human-readable cron
schedules. Its v1 target is the five-field Vixie cron baseline. The public API
is a fluent DSL layered over a typed domain model and dialect-aware rendering.

## Toolchain and commands

- Scala: 3.3.7
- sbt: 1.12.11
- CI Java: Temurin 11
- Formatter: scalafmt, configured in `.scalafmt.conf`

Run commands from the repository root:

```sh
sbt test                         # compile and run the test suite
sbt "testOnly fully.qualified.TestName" # run a focused test
sbt scalafmtCheckAll             # check formatting
sbt scalafmtAll                  # apply formatting
sbt ci                           # formatting + CI-mode compile + CI-mode tests
```

`sbt ci` mirrors the GitHub Actions build steps. Use it before handing off any
non-trivial change. For a small, isolated change, run the relevant focused test
and formatting check at minimum.

## Repository layout

- `src/main/scala/cronh/domain/` — cron ADT, fields, terms, and validated field
  types.
- `src/main/scala/cronh/dsl/` — fluent scheduling DSL, literals, and aliases.
- `src/main/scala/cronh/render/` — cron dialects and human-readable rendering.
- `src/test/scala/cronh/` — MUnit unit tests and ScalaCheck properties, mirroring
  production packages.
- `examples/` — compile-checked usage examples.
- `build.sbt` and `project/` — build, plugins, and CI task definition.
- `README.md` — public usage and supported-scope documentation.
- `DESIGN.md` — design background; validate any implementation detail against
  current source and tests before relying on it.

## Implementation conventions

- Keep the core dependency-free; production dependencies should need a strong
  justification.
- Use Scala 3 idioms already present in the codebase (`enum`, extension methods,
  givens, opaque/validated domain types where applicable).
- Preserve the separation between the domain model, DSL, and rendering. Do not
  leak a renderer's dialect-specific encoding into the domain model.
- Preserve domain validation and compile-time safety. Add tests for valid bounds,
  invalid bounds, and type/DSL constraints when changing a field type, macro, or
  fluent API.
- Cron day-of-month and day-of-week combine with Vixie cron's OR semantics. The
  DSL should make a combination explicit rather than silently changing meaning.
- Keep the DSL readable from broad date selection to time selection and avoid
  APIs that make an incomplete or ambiguous schedule appear complete.
- Preserve deterministic field/render ordering. Normalization is explicit; do
  not silently normalize user input unless the existing API explicitly does so.
- Treat `CronDialect` as the owner of dialect-specific rendering, especially
  weekday numeric encodings.

## Tests and quality checks

- Use `munit.FunSuite` for example- and assertion-driven tests.
- Use `munit.ScalaCheckSuite` and the shared generators in
  `cronh.domain.Generators` for algebraic/domain properties.
- Cover renderer changes with exact cron-string assertions and relevant edge
  cases (notably ranges and weekday encoding).
- Add or update compile-checked examples when changing user-facing DSL syntax.
- Run `scalafmtAll` only when formatting changes are intended; otherwise prefer
  `scalafmtCheckAll` so unrelated formatting churn is visible.

## Change hygiene

- Keep changes scoped and do not overwrite unrelated working-tree edits.
- Update `README.md` when public behavior, supported cron features, or examples
  change.
- Do not manually edit generated GitHub Actions workflows unless the task is
  specifically about generated output; configure them through `build.sbt`.
