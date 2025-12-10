package dev.zachmaddox.chunking;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class ChunkingTest {

    @Test
    @DisplayName("Chunking an empty list yields an empty list of chunks")
    void emptyInputYieldsEmptyChunks() {
        List<Integer> input = Collections.emptyList();
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

        // Flatten and compare order/content with original input
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

    // ----- Chunking convenience methods tests -----

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
        Set<Integer> set = new LinkedHashSet<Integer>();
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
        Iterable<Integer> iterable = new Iterable<Integer>() {
            @Override
            public java.util.Iterator<Integer> iterator() {
                return backing.iterator();
            }
        };

        List<List<Integer>> result = Chunking.chunk(iterable, 4);

        assertEquals(2, result.size());
        assertEquals(Arrays.asList(1, 2, 3, 4), result.get(0));
        assertEquals(Arrays.asList(5, 6), result.get(1));
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
        List<Integer> input = new java.util.ArrayList<Integer>(Arrays.asList(1, 2, 3, 4));
        List<List<Integer>> chunks = input.stream()
                .collect(Chunking.toChunks(2));

        // Modify first chunk
        chunks.get(0).set(0, 99);

        // Other chunk unchanged
        assertEquals(Arrays.asList(3, 4), chunks.get(1));

        // Original list unchanged in position 0
        assertEquals(Arrays.asList(1, 2, 3, 4), input);
    }

    @Test
    @DisplayName("chunk(Stream, ...) closes the stream")
    void chunkStreamClosesStream() {
        final boolean[] closed = { false };

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

}
