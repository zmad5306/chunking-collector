package dev.zachmaddox.chunking;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.Spliterator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class ChunkingSlidingWindowsAndSpliteratorTest {

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
    @DisplayName("streamOfSlidingWindows produces full overlapping windows")
    void streamOfSlidingWindowsBasic() {
        Stream<Integer> stream = IntStream.rangeClosed(1, 4).boxed();
        List<List<Integer>> windows;
        try (Stream<List<Integer>> windowStream = Chunking.streamOfSlidingWindows(stream, 3)) {
            windows = windowStream.collect(Collectors.toList());
        }

        assertEquals(2, windows.size());
        assertEquals(Arrays.asList(1, 2, 3), windows.get(0));
        assertEquals(Arrays.asList(2, 3, 4), windows.get(1));
    }

    @Test
    @DisplayName("streamOfSlidingWindows respects step parameter")
    void streamOfSlidingWindowsWithStep() {
        Stream<Integer> stream = IntStream.rangeClosed(1, 6).boxed();
        List<List<Integer>> windows;
        try (Stream<List<Integer>> windowStream = Chunking.streamOfSlidingWindows(stream, 3, 2)) {
            windows = windowStream.collect(Collectors.toList());
        }

        assertEquals(2, windows.size());
        assertEquals(Arrays.asList(1, 2, 3), windows.get(0));
        assertEquals(Arrays.asList(3, 4, 5), windows.get(1));
    }

    @Test
    void streamOfSlidingWindows_throwsOnNonPositiveWindowSize() {
        Stream<Integer> source = Stream.of(1, 2, 3);
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> Chunking.streamOfSlidingWindows(source, 0, 1),
                "Expected IllegalArgumentException when windowSize <= 0, but no exception was thrown"
        );
        assertNotNull(ex, "Exception should not be null");
    }

    @Test
    void streamOfSlidingWindows_throwsOnNonPositiveStep() {
        Stream<Integer> source = Stream.of(1, 2, 3);
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> Chunking.streamOfSlidingWindows(source, 2, 0),
                "Expected IllegalArgumentException when step <= 0, but no exception was thrown"
        );
        assertNotNull(ex, "Exception should not be null");
    }

    @Test
    void streamOfSlidingWindows_allowsStepGreaterThanWindowSize() {
        Stream<Integer> source = Stream.of(1, 2, 3, 4);

        List<List<Integer>> windows = Chunking
                .streamOfSlidingWindows(source, 2, 3)
                .collect(Collectors.toList());

        assertEquals(
                2,
                windows.size(),
                "Expected two windows when windowSize=2 and step=3 over [1,2,3,4], but got: " + windows
        );
        assertEquals(
                Arrays.asList(1, 2),
                windows.get(0),
                "First window should contain [1, 2], but was: " + windows.get(0)
        );
        assertEquals(
                Arrays.asList(3, 4),
                windows.get(1),
                "Second window should contain [3, 4], but was: " + windows.get(1)
        );
    }

    @Test
    void slidingWindows_emptyStream() {
        List<List<Integer>> windows =
                Chunking.streamOfSlidingWindows(Stream.<Integer>empty(), 3, 1)
                        .collect(Collectors.toList());

        assertTrue(
                windows.isEmpty(),
                "Expected no windows for an empty stream, but found: " + windows
        );
    }

    @Test
    void slidingWindows_windowLargerThanStream() {
        List<List<Integer>> windows =
                Chunking.streamOfSlidingWindows(Stream.of(1, 2), 3, 1)
                        .collect(Collectors.toList());

        assertTrue(
                windows.isEmpty(),
                "Expected no windows when stream has fewer elements than windowSize, but found: " + windows
        );
    }

    @Test
    void slidingWindows_estimateSize_beforeAndAfterConsumption() {
        Stream<Integer> source = IntStream.rangeClosed(1, 10).boxed();
        Spliterator<List<Integer>> spliterator =
                Chunking.streamOfSlidingWindows(source, 3, 2).spliterator();

        long sizeBefore = spliterator.estimateSize();
        assertTrue(
                sizeBefore > 0,
                "Expected estimateSize() to be positive before consuming windows, but was: " + sizeBefore
        );

        List<List<Integer>> windows = new ArrayList<>();
        spliterator.forEachRemaining(windows::add);

        long sizeAfter = spliterator.estimateSize();
        assertEquals(
                0,
                sizeAfter,
                "Expected estimateSize() to be 0 after spliterator exhaustion, but was: " + sizeAfter
        );
        assertFalse(
                windows.isEmpty(),
                "Expected windows to be produced before exhaustion, but the collected result was empty"
        );
    }

    @Test
    void streamOfSlidingWindows_tryAdvanceAfterCompletionReturnsFalse() {
        Stream<Integer> source = Stream.of(1, 2, 3, 4);

        Spliterator<List<Integer>> spliterator =
                Chunking.streamOfSlidingWindows(source, 2, 1).spliterator();

        List<List<Integer>> windows = new ArrayList<>();
        //noinspection StatementWithEmptyBody
        while (spliterator.tryAdvance(windows::add)) {
            // consume all windows
        }

        boolean advancedAfterCompletion = spliterator.tryAdvance(windows::add);

        assertFalse(
                advancedAfterCompletion,
                "Expected tryAdvance to return false after the spliterator has completed"
        );
        assertEquals(
                Arrays.asList(Arrays.asList(1, 2), Arrays.asList(2, 3), Arrays.asList(3, 4)),
                windows,
                "Windows produced before completion did not match expected sliding windows"
        );
    }

    @Test
    void streamOfSlidingWindows_estimateSizeReturnsZeroWhenBaseSizeLessThanWindow() {
        List<Integer> source = Arrays.asList(1, 2);
        int windowSize = 3;
        int step = 1;

        Spliterator<List<Integer>> spliterator =
                Chunking.streamOfSlidingWindows(source.stream(), windowSize, step)
                        .spliterator();

        long estimated = spliterator.estimateSize();

        assertEquals(
                0L,
                estimated,
                "Expected estimateSize() to return 0 when baseSize < windowSize, but was: " + estimated
        );

        List<List<Integer>> windows = new ArrayList<>();
        //noinspection StatementWithEmptyBody
        while (spliterator.tryAdvance(windows::add)) {
            // consume all windows
        }

        assertTrue(
                windows.isEmpty(),
                "Expected no windows when the source size is smaller than the window size, but got: " + windows
        );
    }

    @Test
    @DisplayName("streamOfSlidingWindows spliterator estimates finite size correctly")
    void streamOfSlidingWindowsSpliteratorFiniteEstimate() {
        try (Stream<List<Integer>> windows =
                     Chunking.streamOfSlidingWindows(IntStream.rangeClosed(1, 10).boxed(), 3, 1)) {

            Spliterator<List<Integer>> spliterator = windows.spliterator();

            assertNull(spliterator.trySplit(), "sliding windows spliterator should not support splitting");
            assertEquals(8L, spliterator.estimateSize(),
                    "Expected 10 - 3 + 1 windows for finite stream");
        }
    }

    @Test
    @DisplayName("streamOfSlidingWindows spliterator reports unknown size for unbounded streams")
    void streamOfSlidingWindowsSpliteratorUnknownEstimate() {
        Stream<Integer> infinite = Stream.generate(() -> 1);

        try (Stream<List<Integer>> windows =
                     Chunking.streamOfSlidingWindows(infinite, 3, 1)) {

            Spliterator<List<Integer>> spliterator = windows.spliterator();

            assertNull(spliterator.trySplit(), "sliding windows spliterator should not support splitting");
            assertEquals(Long.MAX_VALUE, spliterator.estimateSize(),
                    "Expected unknown size for unbounded base stream");
        }
    }
}
