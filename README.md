# chunking-collector

Splits a collection into chunks.

## usage

```
List<Long> data = new ArrayList();
// add a bunch of objects to data
List<List<Long>> chunked = data.stream().collect(new ChunkingCollector<>(5)); // 5 is the chunk size
```
