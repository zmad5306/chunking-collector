package dev.zachmaddox.chunking;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.BinaryOperator;

import static dev.zachmaddox.chunking.Chunking.RemainderPolicy.*;
import static org.junit.jupiter.api.Assertions.*;

class FixedSizeChunkingCollectorImplTest {

    @Test
    @DisplayName("FixedSizeChunkingCollectorImpl combiner covers all branches and characteristics")
    void fixedSizeCollectorInternalPathsCovered() {
        FixedSizeChunkingCollectorImpl<Integer, List<Integer>> collector =
                new FixedSizeChunkingCollectorImpl<>(3,
                        Chunking.RemainderPolicy.INCLUDE_PARTIAL,
                        ArrayList::new);

        BinaryOperator<List<Integer>> combiner =
                collector.combiner();

        List<Integer> left = Arrays.asList(1, 2);
        List<Integer> right = Arrays.asList(3, 4);
        List<Integer> empty = Collections.emptyList();

        assertEquals(Arrays.asList(1, 2, 3, 4), combiner.apply(left, right),
                "combiner should concatenate non-empty lists");
        assertEquals(right, combiner.apply(empty, right),
                "combiner should return right when left is empty");
        assertEquals(left, combiner.apply(left, empty),
                "combiner should return left when right is empty");
        assertSame(empty, combiner.apply(empty, empty),
                "combiner should return the empty list when both inputs are empty");
        assertTrue(collector.characteristics().isEmpty(),
                "FixedSizeChunkingCollectorImpl should declare no Collector characteristics");
    }

    @Test
    void fixedSizeFinisher_coversDropRemainderPolicy() {
        List<List<Integer>> finished = getLists();

        assertEquals(
                1,
                finished.size(),
                "Expected DROP_PARTIAL to drop the trailing partial chunk and keep only one full chunk, but got: " + finished
        );
        assertEquals(
                Arrays.asList(1, 2, 3),
                finished.get(0),
                "Expected the remaining chunk to be [1, 2, 3], but was: " + finished.get(0)
        );
    }

    private static List<List<Integer>> getLists() {
        IntFunction<List<Integer>> listFactory = ArrayList::new;

        FixedSizeChunkingCollectorImpl<Integer, List<Integer>> collector =
                new FixedSizeChunkingCollectorImpl<>(3, DROP_PARTIAL, listFactory);

        Function<List<Integer>, List<List<Integer>>> finisher = collector.finisher();
        List<Integer> input = Arrays.asList(1, 2, 3, 4, 5);

        return finisher.apply(input);
    }

    @Test
    void fixedSizeFinisher_includePartial_includesTrailingPartialChunk() {
        FixedSizeChunkingCollectorImpl<Integer, List<Integer>> collector =
                new FixedSizeChunkingCollectorImpl<>(
                        3,
                        Chunking.RemainderPolicy.INCLUDE_PARTIAL,
                        ArrayList::new
                );

        Function<List<Integer>, List<List<Integer>>> finisher = collector.finisher();

        List<Integer> input = Arrays.asList(1, 2, 3, 4, 5);

        List<List<Integer>> chunks = finisher.apply(input);

        assertEquals(
                2,
                chunks.size(),
                "Expected INCLUDE_PARTIAL to produce 2 chunks (one full, one partial), but got: " + chunks
        );

        assertEquals(
                Arrays.asList(1, 2, 3),
                chunks.get(0),
                "First chunk should be the full [1, 2, 3], but was: " + chunks.get(0)
        );

        assertEquals(
                Arrays.asList(4, 5),
                chunks.get(1),
                "Second chunk should be the trailing partial [4, 5], but was: " + chunks.get(1)
        );
    }

    @Test
    void fixedSizeFinisher_dropPartial_dropsAllWhenOnlyPartialChunkExists() {
        FixedSizeChunkingCollectorImpl<Integer, List<Integer>> collector =
                new FixedSizeChunkingCollectorImpl<>(
                        5,
                        Chunking.RemainderPolicy.DROP_PARTIAL,
                        ArrayList::new
                );

        Function<List<Integer>, List<List<Integer>>> finisher = collector.finisher();

        List<Integer> input = Arrays.asList(1, 2, 3);

        List<List<Integer>> chunks = finisher.apply(input);

        assertNotNull(
                chunks,
                "Resulting chunks list should not be null even when all elements are dropped"
        );
        assertTrue(
                chunks.isEmpty(),
                "Expected DROP_PARTIAL to drop the only partial chunk and return an empty list, but got: " + chunks
        );
    }

    @Test
    void fixedSizeFinisher_includePartial_handlesRemainder() {
        FixedSizeChunkingCollectorImpl<Integer, List<Integer>> collector =
                new FixedSizeChunkingCollectorImpl<>(
                        3,
                        Chunking.RemainderPolicy.INCLUDE_PARTIAL,
                        ArrayList::new
                );

        Function<List<Integer>, List<List<Integer>>> finisher = collector.finisher();

        List<Integer> input = Arrays.asList(1, 2, 3, 4, 5);

        List<List<Integer>> chunks = finisher.apply(input);

        assertNotNull(
                chunks,
                "Chunks should not be null when INCLUDE_PARTIAL is used with a remainder"
        );
        assertEquals(
                2,
                chunks.size(),
                "Expected one full chunk and one partial chunk for INCLUDE_PARTIAL with remainder"
        );
        assertEquals(
                Arrays.asList(1, 2, 3),
                chunks.get(0),
                "First chunk should be the first full chunk"
        );
        assertEquals(
                Arrays.asList(4, 5),
                chunks.get(1),
                "Second chunk should be the trailing partial chunk"
        );
    }

    @Test
    void fixedSizeFinisher_dropPartial_dropsSinglePartialChunk() {
        FixedSizeChunkingCollectorImpl<Integer, List<Integer>> collector =
                new FixedSizeChunkingCollectorImpl<>(
                        5,
                        Chunking.RemainderPolicy.DROP_PARTIAL,
                        ArrayList::new
                );

        Function<List<Integer>, List<List<Integer>>> finisher = collector.finisher();

        List<Integer> input = Arrays.asList(1, 2, 3);

        List<List<Integer>> chunks = finisher.apply(input);

        assertNotNull(
                chunks,
                "Resulting chunks list should not be null even when all elements are dropped"
        );
        assertTrue(
                chunks.isEmpty(),
                "Expected DROP_PARTIAL to drop the only partial chunk and return an empty list, but got: " + chunks
        );
    }

    @Test
    void toChunks_padWithNulls_padsTrailingPartialChunk() {
        List<Integer> input = Arrays.asList(1, 2, 3, 4, 5);

        List<List<Integer>> chunks = input.stream()
                .collect(Chunking.toChunks(
                        3,
                        Chunking.RemainderPolicy.PAD_WITH_NULLS
                ));

        assertNotNull(
                chunks,
                "Expected non-null chunks list for PAD_WITH_NULLS remainder policy"
        );
        assertEquals(
                2,
                chunks.size(),
                "Expected 2 chunks (one full, one padded partial), but got: " + chunks
        );

        List<Integer> first = chunks.get(0);
        assertEquals(
                Arrays.asList(1, 2, 3),
                first,
                "First chunk should be the full chunk [1, 2, 3], but was: " + first
        );

        List<Integer> second = chunks.get(1);
        assertEquals(
                3,
                second.size(),
                "Expected padded chunk to have size 3, but was: " + second.size()
        );
        assertEquals(
                Integer.valueOf(4),
                second.get(0),
                "Expected first element of padded chunk to be 4, but was: " + second.get(0)
        );
        assertEquals(
                Integer.valueOf(5),
                second.get(1),
                "Expected second element of padded chunk to be 5, but was: " + second.get(1)
        );
        assertNull(
                second.get(2),
                "Expected third element of padded chunk to be null (padding), but was: " + second.get(2)
        );
    }

    @Test
    void fixedSizeFinisher_errorIfPartial_throwsWhenInputNotEvenlyDivisible() {
        FixedSizeChunkingCollectorImpl<Integer, List<Integer>> collector =
                new FixedSizeChunkingCollectorImpl<>(
                        3,
                        Chunking.RemainderPolicy.ERROR_IF_PARTIAL,
                        ArrayList::new
                );

        final Function<List<Integer>, List<List<Integer>>> finisher = collector.finisher();

        final List<Integer> input = Arrays.asList(1, 2, 3, 4);

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> finisher.apply(input),
                "Expected IllegalStateException when input size is not evenly divisible by chunkSize for ERROR_IF_PARTIAL"
        );

        assertTrue(
                ex.getMessage().contains("chunkSize=3"),
                "Expected exception message to mention chunkSize=3, but was: " + ex.getMessage()
        );
    }
}
