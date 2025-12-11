# 🧩 Chunking Collector

*A lightweight, zero-dependency Java library for splitting streams and collections into fixed-size chunks.*

[![Maven Central](https://img.shields.io/maven-central/v/dev.zachmaddox/chunking-collector.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/dev.zachmaddox/chunking-collector/overview)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-8%2B-orange.svg)](https://openjdk.org/)

---

## ✨ Overview

`chunking-collector` provides a simple, elegant way to **batch, paginate, or segment** data from any Java stream or collection into smaller lists — without introducing external dependencies.

It’s implemented as a `Collector`, so it fits naturally into the Java Stream API and works seamlessly with `List`, `Set`, `Iterable`, arrays, or any other stream source.

---

## 📦 Installation

### Using Maven

Add this dependency to your project’s `pom.xml`:

```xml
<dependency>
  <groupId>dev.zachmaddox</groupId>
  <artifactId>chunking-collector</artifactId>
  <version>1.0.0</version>
</dependency>
```

### Using Gradle

```groovy
implementation 'dev.zachmaddox:chunking-collector:1.0.0'
```

---

## 🚀 Quick Start

### ✅ Collecting Stream Elements into Chunks

```java
import dev.zachmaddox.chunking.Chunking;
import java.util.List;
import java.util.stream.IntStream;

public class Example {
    public static void main(String[] args) {
        List<List<Integer>> chunks = IntStream.rangeClosed(1, 10)
            .boxed()
            .collect(Chunking.toChunks(3));

        // Output: [[1, 2, 3], [4, 5, 6], [7, 8, 9], [10]]
        System.out.println(chunks);
    }
}
```

---

## 🧠 Features

* 🔹 **Pure Java 8+, no dependencies**
* 🔹 Works with **Streams, Collections, Iterables, Sets, and arrays**
* 🔹 Preserves **element order**
* 🔹 Compatible with **parallel streams**
* 🔹 Handles **null elements** gracefully
* 🔹 Prevents **empty or zero-size chunks**
* 🔹 Provides **type-safe helper methods** for convenience

---

## 🧹 API Overview

### 🧱 Collector Method

```java
Collector<T, ?, List<List<T>>> Chunking.toChunks(int chunkSize)
```

* Use directly in a stream pipeline.
* Works for any stream type (`Stream<T>`, `IntStream.boxed()`, etc.).
* Throws `IllegalArgumentException` for `chunkSize <= 0`.

### 🧤 Convenience Methods

```java
// From a Collection
List<List<T>> Chunking.chunk(Collection<T> collection, int chunkSize);

// From an Iterable
List<List<T>> Chunking.chunk(Iterable<T> iterable, int chunkSize);

// From a Stream (auto-closes)
List<List<T>> Chunking.chunk(Stream<T> stream, int chunkSize);

// From an array (varargs)
List<List<T>> Chunking.chunk(int chunkSize, T... elements);
```

---

## 🧮 Example Use Cases

### 1. Batch Processing

```java
Chunking.chunk(records, 100)
    .forEach(batch -> processBatch(batch));
```

### 2. Database Paging

```java
var pages = results.stream()
    .collect(Chunking.toChunks(500));

for (List<Record> page : pages) {
    saveAll(page);
}
```

### 3. Parallel Workloads

```java
Chunking.chunk(items, 10)
    .parallelStream()
    .forEach(this::processChunk);
```

---

## 🧩 Design Philosophy

The implementation avoids mutable state or shared accumulators beyond what `Collector` provides.
All sublists returned are **copies**, ensuring isolation between chunks and immutability of the source data.

---

## 🛠️ Requirements

* **Java 8+**
* **Maven 3.6+**

No other runtime dependencies.

---

## ⚡ Advanced Chunking Features

The `Chunking` class now supports several advanced strategies beyond fixed-size chunking.

### 1. Remainder Policy

Control how trailing partial chunks are handled:

```java
List<List<Integer>> chunks = IntStream.rangeClosed(1, 10)
    .boxed()
    .collect(Chunking.toChunks(3, Chunking.RemainderPolicy.DROP_PARTIAL));
```

* `INCLUDE_PARTIAL` (default) – keep the last incomplete chunk
* `DROP_PARTIAL` – discard it

---

### 2. Custom Chunk Factory

Choose the list implementation created for each chunk:

```java
List<List<Integer>> chunks = numbers.stream()
    .collect(Chunking.toChunks(5, ArrayList::new));
```

Use this to pre-size or provide your own `List` type.

---

### 3. Streaming Chunks Lazily

Process chunks as a **stream of lists** instead of collecting them all:

```java
try (Stream<List<Integer>> chunkStream =
         Chunking.streamOfChunks(IntStream.range(0, 20).boxed(), 4)) {
    chunkStream.forEach(System.out::println);
}
```

Automatically closes the underlying stream.

---

### 4. Sliding Windows

Create overlapping windows:

```java
List<List<Integer>> windows = IntStream.rangeClosed(1, 5)
    .boxed()
    .collect(Chunking.slidingWindows(3, 1));
// → [[1,2,3],[2,3,4],[3,4,5]]
```

---

### 5. Boundary-Based Chunking

Start a new chunk when a boundary predicate is false:

```java
List<List<Integer>> groups = numbers.stream()
    .collect(Chunking.chunkedBy(Integer::equals));
// Groups consecutive equal elements
```

> **Ordering Note:** Boundary-based chunking relies on the encounter order of the input stream. If the stream is unordered (such as a parallel stream without `.sequential()`), chunk boundaries may not be predictable. Always use an ordered or sequential stream when grouping by adjacency.

---

### 6. Weighted Chunking

Group elements until a maximum “weight” threshold is reached:

```java
List<List<String>> chunks = items.stream()
    .collect(Chunking.weightedChunks(100, String::length));
```

Each chunk’s total weight ≤ 100.

> **Ordering Note:** Weighted chunking also depends on encounter order. Elements are added to chunks sequentially based on their appearance in the stream. If you use an unordered stream, group boundaries will be nondeterministic.

---

### 7. Primitive Stream Helpers

Convenience methods for primitive streams:

```java
List<List<Integer>> ints = Chunking.chunk(IntStream.range(0, 10), 3);
List<List<Long>> longs = Chunking.chunk(LongStream.of(1,2,3,4), 2);
List<List<Double>> doubles = Chunking.chunk(DoubleStream.of(1.0,2.0,3.0), 2);
```

---

## ✅ Summary of Advanced APIs

| Category          | Method / Feature                                | Description                                          |
| ----------------- | ----------------------------------------------- | ---------------------------------------------------- |
| Remainder Policy  | `toChunks(int, RemainderPolicy)`                | Include or drop trailing chunk                       |
| Custom Factory    | `toChunks(int, IntFunction<C>)`                 | Custom list type per chunk                           |
| Lazy Stream       | `streamOfChunks(Stream<T>, …)`                  | Lazily stream chunks                                 |
| Sliding Windows   | `slidingWindows(int, int)`                      | Overlapping fixed-size windows                       |
| Boundary Chunking | `chunkedBy(BiPredicate)`                        | Group by boundary condition (ordered input required) |
| Weighted Chunks   | `weightedChunks(long, ToLongFunction)`          | Max total weight per chunk (ordered input required)  |
| Primitive Helpers | `chunk(IntStream/LongStream/DoubleStream, int)` | Convenience wrappers                                 |
