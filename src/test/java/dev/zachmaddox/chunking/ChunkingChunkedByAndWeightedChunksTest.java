package dev.zachmaddox.chunking;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class ChunkingChunkedByAndWeightedChunksTest {

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
        assertEquals(Arrays.asList(1, 3), chunks.get(0));
        assertEquals(Arrays.asList(2, 4, 6), chunks.get(1));
        assertEquals(Arrays.asList(7, 9), chunks.get(2));
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
                    .collect(Chunking.chunkedBy((prev, curr) -> true));

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
    @DisplayName("chunkedBy on empty stream returns empty list")
    void chunkedByEmptyStreamProducesEmptyList() {
        List<List<Integer>> result =
                Stream.<Integer>empty().collect(Chunking.chunkedBy(Integer::equals));

        assertTrue(result.isEmpty(), "chunkedBy on an empty stream should yield an empty list");
    }

    @Test
    @DisplayName("weightedChunks groups by max weight")
    void weightedChunksBasic() {
        List<Integer> input = Arrays.asList(1, 2, 3, 4, 5);

        List<List<Integer>> chunks = input.stream()
                .collect(Chunking.weightedChunks(5, i -> i));

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
                    .collect(Chunking.weightedChunks(10, v -> 1L));

            List<Integer> flattened = chunks.stream()
                    .flatMap(List::stream)
                    .collect(Collectors.toList());

            assertEquals(input, flattened);
        }
    }

    @Test
    @DisplayName("weightedChunks handles all elements overweight with no trailing current chunk")
    void weightedChunksAllOverweightNoTrailingChunk() {
        List<Integer> input = Arrays.asList(100, 200);

        List<List<Integer>> chunks = input.stream()
                .collect(Chunking.weightedChunks(10, i -> i));

        assertEquals(
                Arrays.asList(Collections.singletonList(100), Collections.singletonList(200)),
                chunks,
                "each overweight element should form its own chunk with no trailing partial chunk");
    }

    @Test
    @DisplayName("weightedChunks on empty stream returns empty list")
    void weightedChunksEmptyStreamProducesEmptyList() {
        List<List<Integer>> result =
                Stream.<Integer>empty().collect(Chunking.weightedChunks(10, i -> 1L));

        assertTrue(result.isEmpty(), "weightedChunks on an empty stream should yield an empty list");
    }

    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("chunkedBy combiner merges accumulator lists and handles empties")
    void boundaryCollectorCombinerCoversAllBranches() {
        java.util.stream.Collector<Integer, ?, List<List<Integer>>> collector =
                Chunking.chunkedBy((prev, curr) -> prev <= curr);

        BinaryOperator<List<Integer>> combiner =
                (BinaryOperator<List<Integer>>) collector.combiner();

        List<Integer> left = Arrays.asList(1, 2);
        List<Integer> right = Arrays.asList(3, 4);

        assertEquals(Arrays.asList(1, 2, 3, 4), combiner.apply(left, right),
                "combiner should merge non-empty left and right");
        assertEquals(right, combiner.apply(Collections.emptyList(), right),
                "combiner should return right when left is empty");
        assertEquals(left, combiner.apply(left, Collections.emptyList()),
                "combiner should return left when right is empty");
    }

    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("weightedChunks combiner merges accumulator lists and handles empties")
    void weightedCollectorCombinerCoversAllBranches() {
        java.util.stream.Collector<Integer, ?, List<List<Integer>>> collector =
                Chunking.weightedChunks(10, i -> i);

        BinaryOperator<List<Integer>> combiner =
                (BinaryOperator<List<Integer>>) collector.combiner();

        List<Integer> left = Arrays.asList(1, 2);
        List<Integer> right = Arrays.asList(3, 4);

        assertEquals(Arrays.asList(1, 2, 3, 4), combiner.apply(left, right),
                "weighted combiner should merge non-empty left and right");
        assertEquals(right, combiner.apply(Collections.emptyList(), right),
                "weighted combiner should return right when left is empty");
        assertEquals(left, combiner.apply(left, Collections.emptyList()),
                "weighted combiner should return left when right is empty");
    }
}
