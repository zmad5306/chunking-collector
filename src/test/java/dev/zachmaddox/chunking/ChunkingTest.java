package dev.zachmaddox.chunking;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import static dev.zachmaddox.chunking.Chunking.RemainderPolicy;
import static org.junit.jupiter.api.Assertions.*;

class ChunkingTest {

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

    @Test
    @DisplayName("DROP_PARTIAL policy drops the trailing incomplete chunk")
    void remainderPolicyDropPartial() {
        List<Integer> input = Arrays.asList(1, 2, 3, 4, 5);
        List<List<Integer>> result = input.stream()
                .collect(Chunking.toChunks(2, RemainderPolicy.DROP_PARTIAL));

        assertEquals(2, result.size(), "Trailing chunk of size 1 should be dropped");
        assertEquals(Arrays.asList(1, 2), result.get(0));
        assertEquals(Arrays.asList(3, 4), result.get(1));
    }

    @Test
    @DisplayName("INCLUDE_PARTIAL policy matches original behavior")
    void remainderPolicyIncludePartialMatchesOriginal() {
        List<Integer> input = Arrays.asList(1, 2, 3, 4, 5);

        List<List<Integer>> original = input.stream()
                .collect(Chunking.toChunks(2));

        List<List<Integer>> explicitInclude = input.stream()
                .collect(Chunking.toChunks(2, RemainderPolicy.INCLUDE_PARTIAL));

        assertEquals(original, explicitInclude);
    }

    @Test
    @DisplayName("RemainderPolicy null is rejected")
    void remainderPolicyNullRejected() {
        assertThrows(NullPointerException.class, () -> Chunking.toChunks(3, (RemainderPolicy) null));
    }

    @Test
    @DisplayName("Randomized round-trip with DROP_PARTIAL: flattened prefix equals original prefix")
    void randomizedDropPartialRoundTrip() {
        for (int i = 0; i < 20; i++) {
            int length = ThreadLocalRandom.current().nextInt(0, 200);
            int chunkSize = ThreadLocalRandom.current().nextInt(1, 20);

            List<Integer> input = IntStream.range(0, length)
                    .boxed()
                    .collect(Collectors.toList());

            List<List<Integer>> chunks = input.stream()
                    .collect(Chunking.toChunks(chunkSize, RemainderPolicy.DROP_PARTIAL));

            List<Integer> flattened = chunks.stream()
                    .flatMap(List::stream)
                    .collect(Collectors.toList());

            int expectedSize = (length / chunkSize) * chunkSize;
            assertEquals(expectedSize, flattened.size(),
                    "Flattened size should be largest multiple of chunkSize <= length");

            assertEquals(input.subList(0, expectedSize), flattened,
                    "Flattened content should equal prefix of original");
        }
    }

    static class TrackingList<T> extends ArrayList<T> {
        final int requestedSize;

        TrackingList(int requestedSize) {
            super(Math.max(0, requestedSize));
            this.requestedSize = requestedSize;
        }
    }

    @Test
    @DisplayName("Custom chunkFactory is used for each chunk")
    void customChunkFactoryUsed() {
        List<Integer> input = IntStream.rangeClosed(1, 5)
                .boxed()
                .collect(Collectors.toList());

        List<TrackingList<Integer>> chunks = input.stream()
                .collect(Chunking.toChunks(2, TrackingList::new));

        assertEquals(3, chunks.size());
        assertInstanceOf(TrackingList.class, chunks.get(0));
        assertInstanceOf(TrackingList.class, chunks.get(1));
        assertInstanceOf(TrackingList.class, chunks.get(2));

        assertEquals(2, chunks.get(0).requestedSize);
        assertEquals(2, chunks.get(1).requestedSize);
        assertEquals(1, chunks.get(2).requestedSize);
    }

    @Test
    @DisplayName("Custom chunkFactory with DROP_PARTIAL never called for incomplete chunk")
    void customChunkFactoryDropPartialSkipsIncompleteChunk() {
        List<Integer> input = Arrays.asList(1, 2, 3, 4, 5);

        List<Integer> requestedSizes = new ArrayList<>();
        List<List<Integer>> chunks = input.stream()
                .collect(Chunking.toChunks(2,
                        RemainderPolicy.DROP_PARTIAL,
                        size -> {
                            requestedSizes.add(size);
                            return new ArrayList<>(size);
                        }));

        assertEquals(Arrays.asList(2, 2), requestedSizes, "Only full chunks should be requested");
        assertEquals(2, chunks.size());
    }

    @Test
    @DisplayName("Custom chunkFactory null is rejected")
    void customChunkFactoryNullRejected() {
        assertThrows(NullPointerException.class, () -> Chunking.toChunks(3, (java.util.function.IntFunction<List<Integer>>) null));
        assertThrows(NullPointerException.class, () -> Chunking.toChunks(3, RemainderPolicy.INCLUDE_PARTIAL, null));
    }

    @Test
    @DisplayName("streamOfChunks includes trailing partial chunk by default")
    void streamOfChunksIncludePartial() {
        Stream<Integer> stream = IntStream.rangeClosed(1, 5).boxed();
        List<List<Integer>> chunks;

        try (Stream<List<Integer>> chunkStream = Chunking.streamOfChunks(stream, 2)) {
            chunks = chunkStream.collect(Collectors.toList());
        }

        assertEquals(3, chunks.size());
        assertEquals(Arrays.asList(1, 2), chunks.get(0));
        assertEquals(Arrays.asList(3, 4), chunks.get(1));
        assertEquals(Collections.singletonList(5), chunks.get(2));
    }

    @Test
    @DisplayName("streamOfChunks with DROP_PARTIAL drops trailing chunk")
    void streamOfChunksDropPartial() {
        Stream<Integer> stream = IntStream.rangeClosed(1, 5).boxed();
        List<List<Integer>> chunks;

        try (Stream<List<Integer>> chunkStream = Chunking.streamOfChunks(stream, 2, RemainderPolicy.DROP_PARTIAL)) {
            chunks = chunkStream.collect(Collectors.toList());
        }

        assertEquals(2, chunks.size());
        assertEquals(Arrays.asList(1, 2), chunks.get(0));
        assertEquals(Arrays.asList(3, 4), chunks.get(1));
    }

    @Test
    @DisplayName("streamOfChunks closes underlying stream")
    void streamOfChunksClosesUnderlying() {
        final boolean[] closed = {false};

        Stream<Integer> source = IntStream.range(0, 10).boxed()
                .onClose(() -> closed[0] = true);

        try (Stream<List<Integer>> chunkStream = Chunking.streamOfChunks(source, 3)) {
            assertFalse(closed[0], "Stream should not be closed before chunkStream is closed");

            long chunkCount = chunkStream.count();
            assertEquals(4L, chunkCount, "Expected 4 chunks for 10 elements with chunk size 3");
        }

        assertTrue(closed[0], "Closing chunkStream should close source stream");
    }

    @Test
    @DisplayName("streamOfChunks rejects invalid arguments")
    void streamOfChunksRejectsInvalidArguments() {
        assertThrows(NullPointerException.class, () -> Chunking.streamOfChunks(null, 3));
        assertThrows(IllegalArgumentException.class,
                () -> Chunking.streamOfChunks(Stream.of(1, 2, 3), 0));
        assertThrows(NullPointerException.class,
                () -> Chunking.streamOfChunks(Stream.of(1, 2, 3), 2, null));
    }

    @Test
    @DisplayName("slidingWindows produces full overlapping windows")
    void slidingWindowsBasic() {
        List<Integer> input = Arrays.asList(1, 2, 3, 4);
        List<List<Integer>> windows = input.stream()
                .collect(Chunking.slidingWindows(3, 1));

        assertEquals(2, windows.size());
        assertEquals(Arrays.asList(1, 2, 3), windows.get(0));
        assertEquals(Arrays.asList(2, 3, 4), windows.get(1));
    }

    @Test
    @DisplayName("slidingWindows with step > 1 skips starts appropriately")
    void slidingWindowsStepGreaterThanOne() {
        List<Integer> input = Arrays.asList(1, 2, 3, 4, 5, 6);
        List<List<Integer>> windows = input.stream()
                .collect(Chunking.slidingWindows(3, 2));

        assertEquals(2, windows.size());
        assertEquals(Arrays.asList(1, 2, 3), windows.get(0));
        assertEquals(Arrays.asList(3, 4, 5), windows.get(1));
    }

    @Test
    @DisplayName("slidingWindows with windowSize larger than input yields no windows")
    void slidingWindowsLargerThanInput() {
        List<Integer> input = Arrays.asList(1, 2, 3);
        List<List<Integer>> windows = input.stream()
                .collect(Chunking.slidingWindows(4, 1));

        assertTrue(windows.isEmpty());
    }

    @Test
    @DisplayName("slidingWindows rejects invalid windowSize and step")
    void slidingWindowsRejectsInvalid() {
        assertThrows(IllegalArgumentException.class,
                () -> Chunking.slidingWindows(0, 1));
        assertThrows(IllegalArgumentException.class,
                () -> Chunking.slidingWindows(1, 0));
    }

    @Test
    @DisplayName("chunkedBy groups adjacent equal elements")
    void chunkedByAdjacentEquals() {
        List<Integer> input = Arrays.asList(1, 1, 2, 2, 2, 3, 1, 1);

        List<List<Integer>> chunks = input.stream()
                .collect(Chunking.chunkedBy(Integer::equals));

        assertEquals(4, chunks.size());
        assertEquals(Arrays.asList(1, 1), chunks.get(0));
        assertEquals(Arrays.asList(2, 2, 2), chunks.get(1));
        assertEquals(Collections.singletonList(3), chunks.get(2));
        assertEquals(Arrays.asList(1, 1), chunks.get(3));
    }

    @Test
    @DisplayName("chunkedBy can group by parity")
    void chunkedByParity() {
        List<Integer> input = Arrays.asList(1, 3, 2, 4, 6, 7, 9);
        List<List<Integer>> chunks = input.stream()
                .collect(Chunking.chunkedBy((prev, curr) -> prev % 2 == curr % 2));

        assertEquals(3, chunks.size());
        assertEquals(Arrays.asList(1, 3), chunks.get(0));        // odd, odd
        assertEquals(Arrays.asList(2, 4, 6), chunks.get(1));     // even, even, even
        assertEquals(Arrays.asList(7, 9), chunks.get(2));        // odd, odd
    }

    @Test
    @DisplayName("chunkedBy preserves order and concatenation equals original")
    void chunkedByRoundTrip() {
        for (int i = 0; i < 20; i++) {
            int length = ThreadLocalRandom.current().nextInt(0, 200);
            List<Integer> input = IntStream.range(0, length)
                    .boxed()
                    .collect(Collectors.toList());

            List<List<Integer>> chunks = input.stream()
                    .collect(Chunking.chunkedBy((prev, curr) -> true)); // all in one chunk

            List<Integer> flattened = chunks.stream()
                    .flatMap(List::stream)
                    .collect(Collectors.toList());

            assertEquals(input, flattened);
        }
    }

    @Test
    @DisplayName("chunkedBy rejects null predicate")
    void chunkedByNullRejected() {
        assertThrows(NullPointerException.class,
                () -> Chunking.chunkedBy(null));
    }

    @Test
    @DisplayName("weightedChunks groups by max weight")
    void weightedChunksBasic() {
        List<Integer> input = Arrays.asList(1, 2, 3, 4, 5);

        List<List<Integer>> chunks = input.stream()
                .collect(Chunking.weightedChunks(5, i -> i));

        // Best-effort grouping in order:
        // [1,2] (weight 3), [3] (weight 3 > remaining 2), [4] (weight 4), [5] (weight 5)
        assertEquals(Arrays.asList(
                        Arrays.asList(1, 2),
                        Collections.singletonList(3),
                        Collections.singletonList(4),
                        Collections.singletonList(5)
                ),
                chunks);
    }

    @Test
    @DisplayName("weightedChunks uses single chunk for elements under max weight")
    void weightedChunksSingleChunk() {
        List<Integer> input = Arrays.asList(1, 1, 1);

        List<List<Integer>> chunks = input.stream()
                .collect(Chunking.weightedChunks(10, i -> i));

        assertEquals(1, chunks.size());
        assertEquals(input, chunks.get(0));
    }

    @Test
    @DisplayName("weightedChunks puts overweight element alone")
    void weightedChunksOverweightElementAlone() {
        List<Integer> input = Arrays.asList(1, 100, 2);

        List<List<Integer>> chunks = input.stream()
                .collect(Chunking.weightedChunks(10, i -> i));

        assertEquals(3, chunks.size());
        assertEquals(Collections.singletonList(1), chunks.get(0));
        assertEquals(Collections.singletonList(100), chunks.get(1));
        assertEquals(Collections.singletonList(2), chunks.get(2));
    }

    @Test
    @DisplayName("weightedChunks rejects non-positive maxWeight")
    void weightedChunksRejectsInvalidMaxWeight() {
        assertThrows(IllegalArgumentException.class,
                () -> Chunking.weightedChunks(0, i -> 1L));
    }

    @Test
    @DisplayName("weightedChunks rejects negative weights")
    void weightedChunksRejectsNegativeWeight() {
        List<Integer> input = Arrays.asList(1, 2, 3);
        assertThrows(IllegalArgumentException.class,
                () -> input.stream().collect(Chunking.weightedChunks(10, i -> i == 2 ? -1L : 1L)));
    }

    @Test
    @DisplayName("weightedChunks preserves order and concatenation equals original")
    void weightedChunksRoundTrip() {
        for (int i = 0; i < 20; i++) {
            int length = ThreadLocalRandom.current().nextInt(0, 200);
            List<Integer> input = IntStream.range(0, length)
                    .boxed()
                    .collect(Collectors.toList());

            List<List<Integer>> chunks = input.stream()
                    .collect(Chunking.weightedChunks(10, v -> 1L)); // weight=1 for each

            List<Integer> flattened = chunks.stream()
                    .flatMap(List::stream)
                    .collect(Collectors.toList());

            assertEquals(input, flattened);
        }
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
        List<Integer> input = new ArrayList<>(Arrays.asList(1, 2, 3, 4));
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
