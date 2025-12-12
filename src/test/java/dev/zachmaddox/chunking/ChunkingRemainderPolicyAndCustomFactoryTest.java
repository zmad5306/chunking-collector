package dev.zachmaddox.chunking;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.IntFunction;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static dev.zachmaddox.chunking.Chunking.RemainderPolicy.*;
import static org.junit.jupiter.api.Assertions.*;

class ChunkingRemainderPolicyAndCustomFactoryTest {

    @Test
    @DisplayName("DROP_PARTIAL policy drops the trailing incomplete chunk")
    void remainderPolicyDropPartial() {
        List<Integer> input = Arrays.asList(1, 2, 3, 4, 5);
        List<List<Integer>> result = input.stream()
                .collect(Chunking.toChunks(2, Chunking.RemainderPolicy.DROP_PARTIAL));

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
                .collect(Chunking.toChunks(2, Chunking.RemainderPolicy.INCLUDE_PARTIAL));

        assertEquals(original, explicitInclude);
    }

    @Test
    @DisplayName("RemainderPolicy null is rejected")
    void remainderPolicyNullRejected() {
        assertThrows(NullPointerException.class, () -> Chunking.toChunks(3, (Chunking.RemainderPolicy) null));
    }

    @Test
    @DisplayName("RemainderPolicy.PAD_WITH_NULLS pads the final chunk to full size")
    void padWithNullsPadsFinalChunk() {
        List<Integer> input = Arrays.asList(1, 2, 3, 4, 5);

        List<List<Integer>> chunks = input.stream()
                .collect(Chunking.toChunks(4, PAD_WITH_NULLS));

        assertEquals(2, chunks.size(), "Expected two chunks (one full, one padded)");

        List<Integer> first = chunks.get(0);
        List<Integer> second = chunks.get(1);

        assertEquals(Arrays.asList(1, 2, 3, 4), first);
        assertEquals(4, second.size(), "Padded chunk should have full chunkSize");
        assertEquals(Arrays.asList(5, null, null, null), second);
    }

    @Test
    @DisplayName("RemainderPolicy.ERROR_IF_PARTIAL throws on trailing partial chunk")
    void errorIfPartialThrowsOnRemainder() {
        List<Integer> input = Arrays.asList(1, 2, 3);

        assertThrows(IllegalStateException.class,
                () -> input.stream().collect(Chunking.toChunks(2, ERROR_IF_PARTIAL)));
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
                    .collect(Chunking.toChunks(chunkSize, Chunking.RemainderPolicy.DROP_PARTIAL));

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
                        Chunking.RemainderPolicy.DROP_PARTIAL,
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
        assertThrows(NullPointerException.class, () -> Chunking.toChunks(3, (IntFunction<List<Integer>>) null));
        assertThrows(NullPointerException.class, () -> Chunking.toChunks(3, Chunking.RemainderPolicy.INCLUDE_PARTIAL, null));
    }

    @Test
    @DisplayName("Custom outer collection type via toChunks overload")
    void customOuterCollectionType() {
        Collector<Integer, ?, LinkedHashSet<List<Integer>>> collector =
                Chunking.toChunks(
                        2,
                        INCLUDE_PARTIAL,
                        ArrayList::new,
                        LinkedHashSet::new
                );

        LinkedHashSet<List<Integer>> result = IntStream.rangeClosed(1, 4)
                .boxed()
                .collect(collector);

        assertEquals(2, result.size());
        assertTrue(result.contains(Arrays.asList(1, 2)));
        assertTrue(result.contains(Arrays.asList(3, 4)));
    }
}
