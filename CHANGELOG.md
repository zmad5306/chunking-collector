# 🧾 Changelog

All notable changes to **Chunking Collector** will be documented in this file.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [1.1.3] - 2025-12-11
### Added
- GitHub Releases now include **Maven Central links** and **copy-paste dependency snippets** for Maven, Gradle (Groovy), and Gradle (Kotlin) to make adoption easier.

### Changed
- Enhanced release automation: CI workflow appends dependency instructions to the extracted changelog section when generating GitHub Releases.

### Internal
- Release workflow improvements only — no changes to library code or public API.
- Behavior remains identical to 1.1.2; this is a documentation/automation enhancement release.

---

## [1.1.2] - 2025-12-11
### Added
- Automated release notes generation: GitHub Releases now pull the **latest section of `CHANGELOG.md`** and use it as the official release body.

### Changed
- Updated CI workflow to replace hard-coded “Automated release…” text with accurate changelog-based notes.
- Ensures all future releases consistently include detailed change descriptions.

### Internal
- No library code changes; behavior and API remain identical.
- Release pipeline now produces cleaner, more informative GitHub Releases.

---

## [1.1.1] - 2025-12-11

### Changed

* Refactored internal implementation to improve maintainability:

    * Extracted formerly nested private classes into separate **package-private top-level classes**:

        * `FixedSizeChunkingCollectorImpl`
        * `SlidingWindowCollectorImpl`
        * `BoundaryChunkingCollectorImpl`
        * `WeightedChunkingCollectorImpl`
    * Greatly reduced the size and complexity of `Chunking.java` while keeping all APIs unchanged.

### Internal

* No behavioral changes; all existing unit tests continue to pass.
* Public API and Javadoc output remain unchanged since new classes are not public.

---

## [1.1.0] - 2025-12-10

### Added

* New collectors and stream helpers:

    * `RemainderPolicy` enum (`INCLUDE_PARTIAL`, `DROP_PARTIAL`)
    * Custom chunk factory via `toChunks(int, IntFunction<C>)`
    * `streamOfChunks(Stream<T>, …)` for lazy chunk streaming
    * `slidingWindows(int, int)` for overlapping windows
    * `chunkedBy(BiPredicate)` for boundary-based grouping
    * `weightedChunks(long, ToLongFunction)` for weighted batching
    * Primitive stream helpers for `IntStream`, `LongStream`, and `DoubleStream`

### Documentation

* Added advanced examples to the README
* Clarified ordering semantics and parallel stream behavior
* Added detailed Javadoc for boundary-based and weighted collectors

### Testing

* Comprehensive new test suite covering all permutations, edge cases, and randomized verification

---

## [1.0.0] - 2025-12-10

### Added

* Initial release:

    * Core `toChunks(int)` collector
    * Convenience methods for `Collection`, `Iterable`, `Stream`, and varargs
    * Order-preserving chunking with full test coverage
