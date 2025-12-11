# 🧩 Chunking Collector

*A lightweight, zero-dependency Java library for splitting streams and collections into fixed-size chunks.*

[![Maven Central](https://img.shields.io/maven-central/v/dev.zachmaddox/chunking-collector.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/dev.zachmaddox/chunking-collector/overview)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](./LICENSE.md)
[![Java](https://img.shields.io/badge/Java-8%2B-orange.svg)](https://openjdk.org/)
[![JavaDoc](https://img.shields.io/badge/docs-JavaDoc-blue.svg)](https://zmad5306.github.io/chunking-collector/latest/)

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
  <version>1.1.0</version>
</dependency>
```

### Using Gradle

```groovy
implementation 'dev.zachmaddox:chunking-collector:1.1.0'
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

### 🧤 Convenience Methods

```java
List<List<T>> Chunking.chunk(Collection<T> collection, int chunkSize);
List<List<T>> Chunking.chunk(Iterable<T> iterable, int chunkSize);
List<List<T>> Chunking.chunk(Stream<T> stream, int chunkSize);
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
```

### 3. Parallel Workloads

```java
Chunking.chunk(items, 10)
    .parallelStream()
    .forEach(this::processChunk);
```

---

# 🌱 Real-World Examples

## 🔥 Splitting Huge Predicate Lists to Avoid Parameter Limits

Many JDBC drivers impose limits on the number of parameters allowed in an SQL query. Chunking helps keep each query under this limit.

```java
List<UUID> ids = loadIds();

NamedParameterJdbcTemplate named = new NamedParameterJdbcTemplate(jdbcTemplate);

Chunking.chunk(ids, 500)
    .parallelStream()
    .map(chunk -> named.query(
        "SELECT * FROM users WHERE id IN (:ids)",
        Map.of("ids", chunk),
        (rs, rowNum) -> mapRow(rs)
    ))
    .flatMap(List::stream)
    .toList();
```

---

## 🌿 Spring Examples

### Spring MVC Controller

```java
@GetMapping("/users/batched")
public ResponseEntity<List<List<UserDto>>> getUsersBatched() {
    List<UserDto> users = userService.findAll();

    List<List<UserDto>> batches = users.stream()
        .collect(Chunking.toChunks(250));

    return ResponseEntity.ok(batches);
}
```

### Batch Writes

```java
List<Order> orders = orderRepository.findAll();

Chunking.chunk(orders, 100)
    .forEach(orderRepository::saveAll);
```

---

## ⚛️ Reactor / WebFlux Example

```java
Flux<Order> orders = orderService.findAll();

orders
    .buffer(200)
    .flatMap(orderClient::sendBatch)
    .then()
    .subscribe();
```

---

## ⚡ Advanced Chunking Features

### Remainder Policy

```java
.collect(Chunking.toChunks(3, Chunking.RemainderPolicy.DROP_PARTIAL));
```

### Custom Chunk Factory

```java
.collect(Chunking.toChunks(5, ArrayList::new));
```

### Lazy Chunk Streaming

```java
try (Stream<List<Integer>> chunkStream =
         Chunking.streamOfChunks(IntStream.range(0, 20).boxed(), 4)) {
    chunkStream.forEach(System.out::println);
}
```

### Sliding Windows

```java
.collect(Chunking.slidingWindows(3, 1));
```

### Boundary-Based Chunking

```java
.collect(Chunking.chunkedBy(Integer::equals));
```

### Weighted Chunking

```java
.collect(Chunking.weightedChunks(100, String::length));
```

### Primitive Stream Helpers

```java
Chunking.chunk(IntStream.range(0, 10), 3);
```

---

## 📚 Documentation

**JavaDoc:** [https://zmad5306.github.io/chunking-collector/latest/](https://zmad5306.github.io/chunking-collector/latest/)

**Versions:** [https://zmad5306.github.io/chunking-collector/](https://zmad5306.github.io/chunking-collector/)
