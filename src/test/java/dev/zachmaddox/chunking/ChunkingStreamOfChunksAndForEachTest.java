package dev.zachmaddox.chunking;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static dev.zachmaddox.chunking.Chunking.RemainderPolicy.*;
import static org.junit.jupiter.api.Assertions.*;

class ChunkingStreamOfChunksAndForEachTest {

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

        try (Stream<List<Integer>> chunkStream = Chunking.streamOfChunks(stream, 2, Chunking.RemainderPolicy.DROP_PARTIAL)) {
            chunks = chunkStream.collect(Collectors.toList());
        }

        assertEquals(2, chunks.size());
        assertEquals(Arrays.asList(1, 2), chunks.get(0));
        assertEquals(Arrays.asList(3, 4), chunks.get(1));
    }

    @Test
    @DisplayName("streamOfChunks with PAD_WITH_NULLS pads trailing chunk")
    void streamOfChunksPadWithNulls() {
        Stream<Integer> stream = IntStream.rangeClosed(1, 5).boxed();
        List<List<Integer>> chunks;

        try (Stream<List<Integer>> chunkStream = Chunking.streamOfChunks(stream, 2, PAD_WITH_NULLS)) {
            chunks = chunkStream.collect(Collectors.toList());
        }

        assertEquals(3, chunks.size());
        assertEquals(Arrays.asList(1, 2), chunks.get(0));
        assertEquals(Arrays.asList(3, 4), chunks.get(1));
        assertEquals(Arrays.asList(5, null), chunks.get(2));
    }

    @Test
    @DisplayName("streamOfChunks with ERROR_IF_PARTIAL throws on trailing chunk")
    void streamOfChunksErrorIfPartialThrows() {
        try (Stream<Integer> s = IntStream.rangeClosed(1, 5).boxed()) {
            assertThrows(IllegalStateException.class,
                    () -> Chunking.streamOfChunks(s, 2, ERROR_IF_PARTIAL).forEach(chunk -> {}));
        }
    }

    @Test
    @DisplayName("forEachChunk delegates to streamOfChunks and visits all chunks")
    void forEachChunkProcessesAllChunks() {
        Stream<Integer> stream = IntStream.rangeClosed(1, 5).boxed();
        List<List<Integer>> seen = new ArrayList<>();

        Chunking.forEachChunk(stream, 2, seen::add);

        assertEquals(3, seen.size());
        assertEquals(Arrays.asList(1, 2), seen.get(0));
        assertEquals(Arrays.asList(3, 4), seen.get(1));
        assertEquals(Collections.singletonList(5), seen.get(2));
    }

    @Test
    @DisplayName("forEachChunk with DROP_PARTIAL does not deliver trailing partial chunk")
    void forEachChunkHonorsDropPartial() {
        Stream<Integer> stream = IntStream.rangeClosed(1, 5).boxed();
        List<List<Integer>> seen = new ArrayList<>();

        Chunking.forEachChunk(stream, 2, DROP_PARTIAL, seen::add);

        assertEquals(2, seen.size(), "Trailing partial chunk should be dropped");
        assertEquals(Arrays.asList(1, 2), seen.get(0));
        assertEquals(Arrays.asList(3, 4), seen.get(1));
    }

    @Test
    @DisplayName("forEachChunk with ERROR_IF_PARTIAL throws on trailing chunk")
    void forEachChunkErrorIfPartialThrows() {
        Stream<Integer> stream = IntStream.rangeClosed(1, 5).boxed();

        assertThrows(IllegalStateException.class,
                () -> Chunking.forEachChunk(stream, 2, ERROR_IF_PARTIAL, chunk -> {
                    // no-op
                }));
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
}
