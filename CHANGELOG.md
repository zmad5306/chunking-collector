# 🧾 Changelog

All notable changes to **Chunking Collector** will be documented in this file.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [1.1.0] - 2025-12-10
### Added
- New collectors and stream helpers:
    - `RemainderPolicy` enum (`INCLUDE_PARTIAL`, `DROP_PARTIAL`)
    - Custom chunk factory via `toChunks(int, IntFunction<C>)`
    - `streamOfChunks(Stream<T>, …)` for lazy chunk streaming
    - `slidingWindows(int, int)` for overlapping windows
    - `chunkedBy(BiPredicate)` for boundary-based grouping
    - `weightedChunks(long, ToLongFunction)` for weighted batching
    - Primitive stream helpers for `IntStream`, `LongStream`, and `DoubleStream`

### Documentation
- Added advanced examples to the README
- Clarified ordering semantics and parallel stream behavior
- Added detailed Javadoc for boundary-based and weighted collectors

### Testing
- Comprehensive new test suite covering all permutations, edge cases, and randomized verification

---

## [1.0.0] - 2025-12-10
### Added
- Initial release:
    - Core `toChunks(int)` collector
    - Convenience methods for `Collection`, `Iterable`, `Stream`, and varargs
    - Order-preserving chunking with full test coverage
