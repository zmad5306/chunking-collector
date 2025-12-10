# 🧩 Chunking Collector

*A lightweight, zero-dependency Java library for splitting streams and collections into fixed-size chunks.*

[![Build](https://img.shields.io/badge/build-passing-brightgreen?style=flat-square)]()
[![License](https://img.shields.io/badge/license-MIT-blue.svg?style=flat-square)]()
[![Java](https://img.shields.io/badge/Java-8%2B-orange?style=flat-square)]()

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
  <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### Using Gradle

```groovy
implementation 'dev.zachmaddox:chunking-collector:1.0.0-SNAPSHOT'
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

## 🤪 Testing

This project includes **extensive JUnit 5 tests** covering:

* Edge cases (empty inputs, invalid sizes)
* Parallel stream consistency
* Order and null preservation
* Helper methods (`Collection`, `Iterable`, `Stream`, `array`)
* Randomized round-trip validation
* Stream closing behavior
* No empty chunk guarantee

To run tests:

```bash
mvn clean test
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

## 📄 License

MIT License © 2025 [Zach Maddox](https://github.com/zmad5306)

You’re free to use, modify, and distribute this library for personal or commercial purposes.

---

## 🌟 Contributing

Contributions are welcome!
Feel free to open issues or pull requests if you have improvements or additional test scenarios.

---

## 👨‍💻 Project Structure

```
chunking-collector/
├── pom.xml
├── src/
│   ├── main/java/dev/zachmaddox/chunking/
│   │   └── Chunking.java
│   └── test/java/dev/zachmaddox/chunking/
│       └── ChunkingTest.java
└── README.md
```

---

## 🚢 Releasing a New Version

The release process is fully automated via **GitHub Actions** — no manual Sonatype uploads required.

### 🔖 1. Update Version

Edit `pom.xml` and bump the version:

```xml
<version>1.1.0</version>
```

Commit the change:

```bash
git add pom.xml
git commit -m "chore(release): bump version to 1.1.0"
```

### 📇 2. Tag the Release

Create an annotated tag:

```bash
git tag -a v1.1.0 -m "Release v1.1.0"
```

Push it to GitHub:

```bash
git push origin main --tags
```

### ⚙️ 3. GitHub Actions Builds & Publishes

Once you push the tag:

* The `CI & Release` workflow runs automatically.
* It builds, tests, signs, and deploys artifacts to **Sonatype OSSRH**.
* A GitHub Release (`v1.1.0`) is created with the JAR attached.

### ☁️ 4. Finalize on Maven Central

After the workflow succeeds:

1. Log in at [https://central.sonatype.com](https://central.sonatype.com)
2. Go to **Publishing → Deployments**
3. Find your new version (`chunking-collector 1.1.0`)
4. Click **Publish** — that’s it! 🎉

Your new version will appear on Maven Central within a few minutes.

---

>
