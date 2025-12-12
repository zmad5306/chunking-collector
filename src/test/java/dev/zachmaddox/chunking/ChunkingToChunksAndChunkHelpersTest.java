package dev.zachmaddox.chunking;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.*;
import static org.junit.jupiter.api.Assertions.*;

class ChunkingToChunksAndChunkHelpersTest {

    @Test
    @DisplayName("Chunking an empty list yields an empty list of chunks")
    void emptyInputYieldsEmptyChunks() {
        List<Integer> input = Collections.emptyList();
        @SuppressWarnings("RedundantOperationOnEmptyContainer")
        List<List<Integer>> result = input.stream()
                .collect(Chunking.toChunks(3));

        assertNotNull(result);
        assertTrue(result.isEmpty(), "Expected no chunks for empty input");
    }

    @Test
    @DisplayName("Chunk size of 1 produces singleton chunks")
    void chunkSizeOneProducesSingletons() {
        List<Integer> input = Arrays.asList(1, 2, 3);
        List<List<Integer>> result = input.stream()
                .collect(Chunking.toChunks(1));

        assertEquals(3, result.size());
        assertEquals(Collections.singletonList(1), result.get(0));
        assertEquals(Collections.singletonList(2), result.get(1));
        assertEquals(Collections.singletonList(3), result.get(2));
    }

    @Test
    @DisplayName("Input size divisible by chunk size")
    void exactDivision() {
        List<Integer> input = Arrays.asList(1, 2, 3, 4, 5, 6);
        List<List<Integer>> result = input.stream()
                .collect(Chunking.toChunks(2));

        assertEquals(3, result.size());
        assertEquals(Arrays.asList(1, 2), result.get(0));
        assertEquals(Arrays.asList(3, 4), result.get(1));
        assertEquals(Arrays.asList(5, 6), result.get(2));
    }

    @Test
    @DisplayName("Input size not divisible by chunk size")
    void nonExactDivision() {
        List<Integer> input = Arrays.asList(1, 2, 3, 4, 5);
        List<List<Integer>> result = input.stream()
                .collect(Chunking.toChunks(2));

        assertEquals(3, result.size());
        assertEquals(Arrays.asList(1, 2), result.get(0));
        assertEquals(Arrays.asList(3, 4), result.get(1));
        assertEquals(Collections.singletonList(5), result.get(2));
    }

    @Test
    @DisplayName("Chunk size larger than list size produces a single chunk")
    void chunkSizeLargerThanInput() {
        List<Integer> input = Arrays.asList(1, 2, 3);
        List<List<Integer>> result = input.stream()
                .collect(Chunking.toChunks(10));

        assertEquals(1, result.size());
        assertEquals(input, result.get(0));
    }

    @Test
    @DisplayName("Collector preserves encounter order")
    void preservesOrder() {
        List<Integer> input = Arrays.asList(10, 20, 30, 40, 50);
        List<List<Integer>> result = input.stream()
                .collect(Chunking.toChunks(2));

        assertEquals(Arrays.asList(10, 20), result.get(0));
        assertEquals(Arrays.asList(30, 40), result.get(1));
        assertEquals(Collections.singletonList(50), result.get(2));
    }

    @Test
    @DisplayName("Collector works with parallel streams")
    void worksWithParallelStream() {
        List<Integer> input = IntStream.rangeClosed(1, 1000)
                .boxed()
                .collect(Collectors.toList());

        List<List<Integer>> result = input.parallelStream()
                .collect(Chunking.toChunks(128));

        List<Integer> flattened = result.stream()
                .flatMap(List::stream)
                .collect(Collectors.toList());

        assertEquals(input, flattened, "Parallel stream should preserve element order and content");
    }

    @Test
    @DisplayName("Collector handles null elements")
    void handlesNullElements() {
        List<Integer> input = Arrays.asList(1, null, 3, null);
        List<List<Integer>> result = input.stream()
                .collect(Chunking.toChunks(2));

        assertEquals(2, result.size());
        assertEquals(Arrays.asList(1, null), result.get(0));
        assertEquals(Arrays.asList(3, null), result.get(1));
    }

    @Test
    @DisplayName("Collector rejects non-positive chunk sizes")
    void rejectsInvalidChunkSizes() {
        assertThrows(IllegalArgumentException.class, () -> Chunking.toChunks(0));
        assertThrows(IllegalArgumentException.class, () -> Chunking.toChunks(-1));
    }

    @Test
    @DisplayName("Chunking a Collection uses iteration order")
    void chunkCollection() {
        List<String> input = Arrays.asList("a", "b", "c", "d", "e");
        List<List<String>> result = Chunking.chunk(input, 2);

        assertEquals(3, result.size());
        assertEquals(Arrays.asList("a", "b"), result.get(0));
        assertEquals(Arrays.asList("c", "d"), result.get(1));
        assertEquals(Collections.singletonList("e"), result.get(2));
    }

    @Test
    @DisplayName("Chunking a Set respects set iteration order")
    void chunkSet() {
        Set<Integer> set = new LinkedHashSet<>();
        set.add(5);
        set.add(10);
        set.add(15);
        set.add(20);

        List<List<Integer>> result = Chunking.chunk(set, 3);

        assertEquals(2, result.size());
        assertEquals(Arrays.asList(5, 10, 15), result.get(0));
        assertEquals(Collections.singletonList(20), result.get(1));
    }

    @Test
    @DisplayName("Chunking an Iterable works")
    void chunkIterable() {
        final List<Integer> backing = Arrays.asList(1, 2, 3, 4, 5, 6);
        Iterable<Integer> iterable = backing::iterator;

        List<List<Integer>> result = Chunking.chunk(iterable, 4);

        assertEquals(2, result.size());
        assertEquals(Arrays.asList(1, 2, 3, 4), result.get(0));
        assertEquals(Arrays.asList(5, 6), result.get(1));
    }

    @Test
    @DisplayName("Chunking a Spliterator works")
    void chunkSpliterator() {
        List<Integer> backing = Arrays.asList(1, 2, 3, 4, 5);
        List<List<Integer>> result = Chunking.chunk(backing.spliterator(), 2);

        assertEquals(3, result.size());
        assertEquals(Arrays.asList(1, 2), result.get(0));
        assertEquals(Arrays.asList(3, 4), result.get(1));
        assertEquals(Collections.singletonList(5), result.get(2));
    }

    @Test
    @DisplayName("Chunking a Stream directly")
    void chunkStream() {
        Stream<String> stream = Stream.of("x", "y", "z");
        List<List<String>> result = Chunking.chunk(stream, 2);

        assertEquals(2, result.size());
        assertEquals(Arrays.asList("x", "y"), result.get(0));
        assertEquals(Collections.singletonList("z"), result.get(1));
    }

    @Test
    @DisplayName("Chunking arrays via varargs helper")
    void chunkArrayVarargs() {
        List<List<Integer>> result = Chunking.chunk(2, 1, 2, 3);

        assertEquals(2, result.size());
        assertEquals(Arrays.asList(1, 2), result.get(0));
        assertEquals(Collections.singletonList(3), result.get(1));
    }

    @Test
    @DisplayName("Chunking rejects null inputs")
    void chunkingRejectsNullInputs() {
        assertThrows(NullPointerException.class, () -> Chunking.chunk((List<Integer>) null, 2));
        assertThrows(NullPointerException.class, () -> Chunking.chunk((Iterable<Integer>) null, 2));
        assertThrows(NullPointerException.class, () -> Chunking.chunk((Spliterator<Integer>) null, 2));
        assertThrows(NullPointerException.class, () -> Chunking.chunk((Stream<Integer>) null, 2));
        assertThrows(NullPointerException.class, () -> Chunking.chunk(2, (Integer[]) null));
    }

    @Test
    @DisplayName("Randomized round-trip test: flatten(chunks(input)) == input")
    void randomizedRoundTrip() {
        for (int i = 0; i < 20; i++) {
            int length = ThreadLocalRandom.current().nextInt(0, 200);
            int chunkSize = ThreadLocalRandom.current().nextInt(1, 20);

            List<Integer> input = IntStream.range(0, length)
                    .boxed()
                    .collect(Collectors.toList());

            List<List<Integer>> chunks = input.stream()
                    .collect(Chunking.toChunks(chunkSize));

            List<Integer> flattened = chunks.stream()
                    .flatMap(List::stream)
                    .collect(Collectors.toList());

            assertEquals(input, flattened,
                    "Round-trip failed for length=" + length + ", chunkSize=" + chunkSize);
        }
    }

    @Test
    @DisplayName("No empty chunks are produced")
    void noEmptyChunks() {
        List<Integer> input = IntStream.rangeClosed(1, 10)
                .boxed()
                .collect(Collectors.toList());

        List<List<Integer>> chunks = input.stream()
                .collect(Chunking.toChunks(3));

        assertFalse(chunks.isEmpty());
        for (List<Integer> chunk : chunks) {
            assertFalse(chunk.isEmpty(), "No chunk should be empty");
        }
    }

    @Test
    @DisplayName("Chunking empty Collection/Iterable/Stream/array yields empty chunks")
    void chunkHelpersEmptyInputs() {
        List<Integer> emptyList = Collections.emptyList();

        assertTrue(Chunking.chunk(emptyList, 3).isEmpty());
        assertTrue(Chunking.chunk((Iterable<Integer>) emptyList, 3).isEmpty());
        assertTrue(Chunking.chunk(Stream.<Integer>empty(), 3).isEmpty());
        assertTrue(Chunking.chunk(3, new Integer[0]).isEmpty());
    }

    @Test
    @DisplayName("Varargs helper with no elements returns empty list")
    void chunkVarargsNoElements() {
        List<List<Integer>> chunks = Chunking.chunk(5 /* chunkSize */, new Integer[0]);
        assertTrue(chunks.isEmpty());
    }

    @Test
    @DisplayName("Chunks are independent copies (no aliasing between chunks)")
    void chunksAreIndependent() {
        List<Integer> input = new ArrayList<>(Arrays.asList(1, 2, 3, 4));
        List<List<Integer>> chunks = input.stream()
                .collect(Chunking.toChunks(2));

        chunks.get(0).set(0, 99);

        assertEquals(Arrays.asList(3, 4), chunks.get(1));
        assertEquals(Arrays.asList(1, 2, 3, 4), input);
    }

    @Test
    @DisplayName("chunk(Stream, ...) closes the stream")
    void chunkStreamClosesStream() {
        final boolean[] closed = {false};

        Stream<Integer> stream = IntStream.range(0, 10).boxed()
                .onClose(() -> closed[0] = true);

        List<List<Integer>> result = Chunking.chunk(stream, 3);
        assertFalse(result.isEmpty());
        assertTrue(closed[0], "Stream should have been closed by chunk(Stream, ...)");
    }

    @Test
    @DisplayName("Very large chunk size behaves like one big chunk")
    void veryLargeChunkSize() {
        List<Integer> input = IntStream.rangeClosed(1, 100)
                .boxed()
                .collect(Collectors.toList());

        List<List<Integer>> chunks = input.stream()
                .collect(Chunking.toChunks(Integer.MAX_VALUE));

        assertEquals(1, chunks.size());
        assertEquals(input, chunks.get(0));
    }

    @Test
    @DisplayName("Collector characteristics are empty")
    void collectorCharacteristics() {
        Collector<Integer, ?, List<List<Integer>>> collector = Chunking.toChunks(3);
        assertTrue(collector.characteristics().isEmpty());
    }

    @Test
    @DisplayName("chunk(IntStream, ...) works and closes stream")
    void chunkIntStreamHelper() {
        final boolean[] closed = {false};
        IntStream stream = IntStream.range(0, 10)
                .onClose(() -> closed[0] = true);

        List<List<Integer>> chunks = Chunking.chunk(stream, 3);

        assertEquals(Arrays.asList(
                        Arrays.asList(0, 1, 2),
                        Arrays.asList(3, 4, 5),
                        Arrays.asList(6, 7, 8),
                        Collections.singletonList(9)
                ),
                chunks);

        assertTrue(closed[0], "IntStream should be closed");
    }

    @Test
    @DisplayName("chunk(LongStream, ...) works")
    void chunkLongStreamHelper() {
        LongStream stream = LongStream.of(1L, 2L, 3L, 4L);
        List<List<Long>> chunks = Chunking.chunk(stream, 3);

        assertEquals(2, chunks.size());
        assertEquals(Arrays.asList(1L, 2L, 3L), chunks.get(0));
        assertEquals(Collections.singletonList(4L), chunks.get(1));
    }

    @Test
    @DisplayName("chunk(DoubleStream, ...) works")
    void chunkDoubleStreamHelper() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0, 3.0);
        List<List<Double>> chunks = Chunking.chunk(stream, 2);

        assertEquals(2, chunks.size());
        assertEquals(Arrays.asList(1.0, 2.0), chunks.get(0));
        assertEquals(Collections.singletonList(3.0), chunks.get(1));
    }

    @Test
    @DisplayName("chunk(DoubleStream, ...) chunks and closes the primitive stream")
    void chunkDoubleStreamCoversWrapper() {
        final boolean[] closed = {false};

        DoubleStream stream = DoubleStream.of(1.0, 2.0, 3.0, 4.0, 5.0)
                .onClose(() -> closed[0] = true);

        List<List<Double>> chunks = Chunking.chunk(stream, 2);

        assertEquals(Arrays.asList(
                        Arrays.asList(1.0, 2.0),
                        Arrays.asList(3.0, 4.0),
                        Collections.singletonList(5.0)),
                chunks,
                "chunk(DoubleStream, ...) should chunk the stream like other helpers");
        assertTrue(closed[0], "chunk(DoubleStream, ...) should close the underlying stream");
    }

    @Test
    @DisplayName("Primitive collector helpers delegate correctly")
    void primitiveCollectorHelpers() {
        List<Integer> ints = IntStream.rangeClosed(1, 5)
                .boxed()
                .collect(Collectors.toList());
        List<List<Integer>> intChunks = ints.stream().collect(Chunking.toIntChunks(2));

        assertEquals(Arrays.asList(
                Arrays.asList(1, 2),
                Arrays.asList(3, 4),
                Collections.singletonList(5)
        ), intChunks);

        List<Long> longs = Arrays.asList(1L, 2L, 3L);
        List<List<Long>> longChunks = longs.stream().collect(Chunking.toLongChunks(2));
        assertEquals(Arrays.asList(
                Arrays.asList(1L, 2L),
                Collections.singletonList(3L)
        ), longChunks);

        List<Double> doubles = Arrays.asList(1.0, 2.0, 3.0);
        List<List<Double>> doubleChunks = doubles.stream().collect(Chunking.toDoubleChunks(2));
        assertEquals(Arrays.asList(
                Arrays.asList(1.0, 2.0),
                Collections.singletonList(3.0)
        ), doubleChunks);
    }

    @Test
    @DisplayName("Primitive helpers reject null streams")
    void primitiveHelpersRejectNull() {
        assertThrows(NullPointerException.class, () -> Chunking.chunk((IntStream) null, 3));
        assertThrows(NullPointerException.class, () -> Chunking.chunk((LongStream) null, 3));
        assertThrows(NullPointerException.class, () -> Chunking.chunk((DoubleStream) null, 3));
    }
}
