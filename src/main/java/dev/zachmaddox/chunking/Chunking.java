package dev.zachmaddox.chunking;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Spliterator;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.ToLongFunction;
import java.util.stream.Collector;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Utility class for chunking streams and iterables into lists.
 *
 * <p>All chunking methods preserve the encounter order of the input.
 * In particular, collectors such as {@link #chunkedBy(BiPredicate)} and
 * {@link #weightedChunks(long, ToLongFunction)} define their semantics
 * in terms of adjacent elements in the encounter order. Supplying an
 * unordered or non-deterministic stream may therefore produce
 * non-deterministic chunk boundaries.</p>
 */
public final class Chunking {

    private Chunking() {
        // utility class
    }

    /**
     * Defines how to handle the trailing remainder when the input size
     * is not evenly divisible by the chunk size.
     */
    public enum RemainderPolicy {
        /**
         * Include the final partial chunk (default behavior).
         */
        INCLUDE_PARTIAL,

        /**
         * Drop the final chunk if it is smaller than {@code chunkSize}.
         */
        DROP_PARTIAL
    }

    /**
     * Returns a {@link Collector} that groups elements into chunks of {@code chunkSize},
     * including the final partial chunk if any.
     *
     * <p>Works with any stream source (lists, sets, arrays via {@code Arrays.stream}, etc.).</p>
     *
     * @param <T>       the element type of the stream
     * @param chunkSize the maximum number of elements in each chunk; must be greater than zero
     * @return a collector that accumulates elements into a {@code List<List<T>>}
     * @throws IllegalArgumentException if {@code chunkSize} is less than {@code 1}
     */
    public static <T> Collector<T, ?, List<List<T>>> toChunks(int chunkSize) {
        return new FixedSizeChunkingCollectorImpl<>(
                chunkSize,
                RemainderPolicy.INCLUDE_PARTIAL,
                ArrayList::new
        );
    }

    /**
     * Returns a {@link Collector} that groups elements into chunks of {@code chunkSize},
     * using the provided {@link RemainderPolicy}.
     *
     * @param <T>             the element type of the stream
     * @param chunkSize       the maximum number of elements in each chunk; must be greater than zero
     * @param remainderPolicy how to handle the final partial chunk; must not be {@code null}
     * @return a collector that accumulates elements into a {@code List<List<T>>}
     * @throws IllegalArgumentException if {@code chunkSize} is less than {@code 1}
     * @throws NullPointerException     if {@code remainderPolicy} is {@code null}
     */
    public static <T> Collector<T, ?, List<List<T>>> toChunks(
            int chunkSize,
            RemainderPolicy remainderPolicy
    ) {
        Objects.requireNonNull(remainderPolicy, "remainderPolicy must not be null");
        return new FixedSizeChunkingCollectorImpl<>(
                chunkSize,
                remainderPolicy,
                ArrayList::new
        );
    }

    /**
     * Returns a {@link Collector} that groups elements into chunks of {@code chunkSize},
     * using a custom list implementation for each chunk.
     *
     * <p>The {@code chunkFactory} is called once per chunk, with the intended size
     * of that chunk (number of elements). Implementations may ignore the hint.</p>
     *
     * <p>The final partial chunk is included (equivalent to {@link RemainderPolicy#INCLUDE_PARTIAL}).</p>
     *
     * @param <T>          the element type
     * @param <C>          the list implementation used for chunks
     * @param chunkSize    the maximum number of elements in each chunk; must be greater than zero
     * @param chunkFactory factory used to create each chunk list; must not be {@code null}
     * @return a collector that accumulates elements into a {@code List<C>}
     * @throws IllegalArgumentException if {@code chunkSize} is less than {@code 1}
     * @throws NullPointerException     if {@code chunkFactory} is {@code null}
     */
    public static <T, C extends List<T>> Collector<T, ?, List<C>> toChunks(
            int chunkSize,
            IntFunction<C> chunkFactory
    ) {
        Objects.requireNonNull(chunkFactory, "chunkFactory must not be null");
        return new FixedSizeChunkingCollectorImpl<>(
                chunkSize,
                RemainderPolicy.INCLUDE_PARTIAL,
                chunkFactory
        );
    }

    /**
     * Returns a {@link Collector} that groups elements into chunks of {@code chunkSize},
     * using both a custom {@link RemainderPolicy} and a custom chunk list implementation.
     *
     * @param <T>             the element type
     * @param <C>             the list implementation used for chunks
     * @param chunkSize       the maximum number of elements in each chunk; must be greater than zero
     * @param remainderPolicy how to handle the final partial chunk; must not be {@code null}
     * @param chunkFactory    factory used to create each chunk list; must not be {@code null}
     * @return a collector that accumulates elements into a {@code List<C>}
     * @throws IllegalArgumentException if {@code chunkSize} is less than {@code 1}
     * @throws NullPointerException     if {@code remainderPolicy} or {@code chunkFactory} is {@code null}
     */
    public static <T, C extends List<T>> Collector<T, ?, List<C>> toChunks(
            int chunkSize,
            RemainderPolicy remainderPolicy,
            IntFunction<C> chunkFactory
    ) {
        Objects.requireNonNull(remainderPolicy, "remainderPolicy must not be null");
        Objects.requireNonNull(chunkFactory, "chunkFactory must not be null");
        return new FixedSizeChunkingCollectorImpl<>(
                chunkSize,
                remainderPolicy,
                chunkFactory
        );
    }

    /**
     * Returns a {@link Stream} of fixed-size chunks from the given source stream.
     *
     * <p>The returned stream is lazy and consumes the source stream as it is traversed.
     * Closing the returned stream will also close the source stream.</p>
     *
     * <p>The final partial chunk is included.</p>
     *
     * @param <T>       element type
     * @param source    the source stream; must not be {@code null}
     * @param chunkSize the maximum number of elements in each chunk; must be greater than zero
     * @return a stream of chunks as {@code List<T>}
     * @throws NullPointerException     if {@code source} is {@code null}
     * @throws IllegalArgumentException if {@code chunkSize} is less than {@code 1}
     */
    public static <T> Stream<List<T>> streamOfChunks(Stream<T> source, int chunkSize) {
        return streamOfChunks(source, chunkSize, RemainderPolicy.INCLUDE_PARTIAL);
    }

    /**
     * Returns a {@link Stream} of fixed-size chunks from the given source stream,
     * using the given {@link RemainderPolicy}.
     *
     * <p>The returned stream is lazy and consumes the source stream as it is traversed.
     * Closing the returned stream will also close the source stream.</p>
     *
     * @param <T>             element type
     * @param source          the source stream; must not be {@code null}
     * @param chunkSize       the maximum number of elements in each chunk; must be greater than zero
     * @param remainderPolicy how to handle the final partial chunk; must not be {@code null}
     * @return a stream of chunks as {@code List<T>}
     * @throws NullPointerException     if {@code source} or {@code remainderPolicy} is {@code null}
     * @throws IllegalArgumentException if {@code chunkSize} is less than {@code 1}
     */
    public static <T> Stream<List<T>> streamOfChunks(
            Stream<T> source,
            int chunkSize,
            RemainderPolicy remainderPolicy
    ) {
        Objects.requireNonNull(source, "source must not be null");
        Objects.requireNonNull(remainderPolicy, "remainderPolicy must not be null");
        if (chunkSize < 1) {
            throw new IllegalArgumentException("Chunk size must be greater than zero.");
        }

        Spliterator<T> baseSpliterator = source.spliterator();
        Spliterator<List<T>> chunkSpliterator = new Spliterator<List<T>>() {
            @Override
            public boolean tryAdvance(Consumer<? super List<T>> action) {
                List<T> chunk = new ArrayList<>(chunkSize);
                while (chunk.size() < chunkSize && baseSpliterator.tryAdvance(chunk::add)) {
                    // keep filling chunk
                }

                if (chunk.isEmpty()) {
                    return false;
                }

                if (chunk.size() < chunkSize && remainderPolicy == RemainderPolicy.DROP_PARTIAL) {
                    // We reached the end with an incomplete chunk; drop it.
                    return false;
                }

                action.accept(chunk);
                return true;
            }

            @Override
            public Spliterator<List<T>> trySplit() {
                // no splitting; keep implementation simple
                return null;
            }

            @Override
            public long estimateSize() {
                return baseSpliterator.estimateSize();
            }

            @Override
            public int characteristics() {
                // Preserve ORDERED; we don't claim other characteristics.
                return baseSpliterator.characteristics() & Spliterator.ORDERED;
            }
        };

        Stream<List<T>> chunkStream = StreamSupport.stream(chunkSpliterator, false);
        return chunkStream.onClose(source::close);
    }

    /**
     * Returns a {@link Collector} that produces sliding windows of size {@code windowSize}
     * with step {@code step}.
     *
     * <p>Only full windows are produced. For example:</p>
     * <pre>
     * [1,2,3,4] with windowSize=3, step=1 → [[1,2,3],[2,3,4]]
     * </pre>
     * <p>This collector respects the stream's encounter order and only makes sense
     * for ordered streams; using an unordered stream will yield arbitrary windows.</p>
     *
     * @param <T>        element type
     * @param windowSize the size of each window; must be greater than zero
     * @param step       the step between window starting positions; must be greater than zero
     * @return a collector that accumulates elements into sliding windows
     * @throws IllegalArgumentException if {@code windowSize} or {@code step} is less than {@code 1}
     */
    public static <T> Collector<T, ?, List<List<T>>> slidingWindows(int windowSize, int step) {
        return new SlidingWindowCollectorImpl<>(windowSize, step);
    }

    /**
     * Returns a {@link Collector} that groups elements into chunks based on a boundary predicate.
     *
     * <p>The first element always starts a new chunk. For each subsequent element {@code current},
     * the predicate is called with {@code (previous, current)}:
     * <ul>
     *     <li>If it returns {@code true}, {@code current} is added to the current chunk.</li>
     *     <li>If it returns {@code false}, a new chunk is started with {@code current}.</li>
     * </ul>
     *
     * <p><strong>Ordering note:</strong> this collector defines its semantics in terms of
     * adjacent elements in the stream's <em>encounter order</em>. If you supply an
     * unordered or non-deterministic stream (for example, a parallel stream without
     * forcing {@code .sequential()}), the resulting chunk boundaries may be
     * non-deterministic.</p>
     *
     * @param <T>       element type
     * @param sameGroup predicate that determines whether two adjacent elements belong
     *                  to the same chunk; must not be {@code null}
     * @return a collector that accumulates elements into boundary-based chunks
     * @throws NullPointerException if {@code sameGroup} is {@code null}
     */
    public static <T> Collector<T, ?, List<List<T>>> chunkedBy(
            BiPredicate<? super T, ? super T> sameGroup
    ) {
        Objects.requireNonNull(sameGroup, "sameGroup must not be null");
        return new BoundaryChunkingCollectorImpl<>(sameGroup);
    }

    /**
     * Returns a {@link Collector} that groups elements into chunks such that the sum of
     * weights (as provided by {@code weigher}) within each chunk is &lt;= {@code maxWeight}.
     *
     * <p>The semantics are:
     * <ul>
     *     <li>Elements are processed in encounter order.</li>
     *     <li>If adding an element would exceed {@code maxWeight}, a new chunk is started.</li>
     *     <li>If a single element's weight is greater than {@code maxWeight}, it forms a chunk
     *         by itself.</li>
     *     <li>Negative weights are not allowed.</li>
     * </ul>
     *
     * <p><strong>Ordering note:</strong> chunk composition is based purely on the
     * stream's encounter order and the running sum of weights. If you collect from an
     * unordered or non-deterministic stream, the grouping of elements into chunks
     * may vary between runs.</p>
     *
     * @param <T>       element type
     * @param maxWeight maximum total weight per chunk; must be greater than zero
     * @param weigher   function that computes the weight of each element; must not be {@code null}
     * @return a collector that accumulates elements into weighted chunks
     * @throws IllegalArgumentException if {@code maxWeight} is less than {@code 1}
     * @throws NullPointerException     if {@code weigher} is {@code null}
     */
    public static <T> Collector<T, ?, List<List<T>>> weightedChunks(
            long maxWeight,
            ToLongFunction<? super T> weigher
    ) {
        Objects.requireNonNull(weigher, "weigher must not be null");
        return new WeightedChunkingCollectorImpl<>(maxWeight, weigher);
    }

    /**
     * Chunks an {@link Iterable} into a list of lists, using its iteration order.
     *
     * <p>The final partial chunk is included.</p>
     *
     * @param <T>       the element type
     * @param iterable  the source iterable; must not be {@code null}
     * @param chunkSize the maximum number of elements in each chunk; must be greater than zero
     * @return a list of chunks; possibly empty if the iterable has no elements
     * @throws NullPointerException     if {@code iterable} is {@code null}
     * @throws IllegalArgumentException if {@code chunkSize} is less than {@code 1}
     */
    public static <T> List<List<T>> chunk(Iterable<T> iterable, int chunkSize) {
        Objects.requireNonNull(iterable, "iterable must not be null");
        return StreamSupport.stream(iterable.spliterator(), false)
                .collect(toChunks(chunkSize));
    }

    /**
     * Chunks a {@link Collection} into a list of lists, using its iteration order.
     *
     * <p>The final partial chunk is included.</p>
     *
     * @param <T>        the element type
     * @param collection the source collection; must not be {@code null}
     * @param chunkSize  the maximum number of elements in each chunk; must be greater than zero
     * @return a list of chunks; possibly empty if the collection is empty
     * @throws NullPointerException     if {@code collection} is {@code null}
     * @throws IllegalArgumentException if {@code chunkSize} is less than {@code 1}
     */
    public static <T> List<List<T>> chunk(Collection<T> collection, int chunkSize) {
        Objects.requireNonNull(collection, "collection must not be null");
        return collection.stream().collect(toChunks(chunkSize));
    }

    /**
     * Chunks a {@link Stream} into a list of lists and closes the stream.
     *
     * <p>The final partial chunk is included.</p>
     *
     * @param <T>       the element type
     * @param stream    the source stream; must not be {@code null}
     * @param chunkSize the maximum number of elements in each chunk; must be greater than zero
     * @return a list of chunks; possibly empty if the stream has no elements
     * @throws NullPointerException     if {@code stream} is {@code null}
     * @throws IllegalArgumentException if {@code chunkSize} is less than {@code 1}
     */
    public static <T> List<List<T>> chunk(Stream<T> stream, int chunkSize) {
        Objects.requireNonNull(stream, "stream must not be null");
        try (Stream<T> s = stream) {
            return s.collect(toChunks(chunkSize));
        }
    }

    /**
     * Varargs convenience method: chunks an array of elements.
     *
     * <p>The final partial chunk is included.</p>
     *
     * @param <T>       the element type
     * @param chunkSize the maximum number of elements in each chunk; must be greater than zero
     * @param elements  the elements to chunk; must not be {@code null}
     * @return a list of chunks; possibly empty if no elements are provided
     * @throws NullPointerException     if {@code elements} is {@code null}
     * @throws IllegalArgumentException if {@code chunkSize} is less than {@code 1}
     */
    @SafeVarargs
    public static <T> List<List<T>> chunk(int chunkSize, T... elements) {
        Objects.requireNonNull(elements, "elements must not be null");
        List<T> list = new ArrayList<>(elements.length);
        Collections.addAll(list, elements);
        return chunk(list, chunkSize);
    }

    /**
     * Chunks an {@link IntStream} into {@code List<List<Integer>>} and closes it.
     *
     * <p>The final partial chunk is included.</p>
     *
     * @param stream    the source int stream; must not be {@code null}
     * @param chunkSize the maximum number of elements in each chunk; must be greater than zero
     * @return a list of chunks; possibly empty if the stream has no elements
     * @throws NullPointerException     if {@code stream} is {@code null}
     * @throws IllegalArgumentException if {@code chunkSize} is less than {@code 1}
     */
    public static List<List<Integer>> chunk(IntStream stream, int chunkSize) {
        Objects.requireNonNull(stream, "stream must not be null");
        try (IntStream s = stream) {
            return s.boxed().collect(toChunks(chunkSize));
        }
    }

    /**
     * Chunks a {@link LongStream} into {@code List<List<Long>>} and closes it.
     *
     * <p>The final partial chunk is included.</p>
     *
     * @param stream    the source long stream; must not be {@code null}
     * @param chunkSize the maximum number of elements in each chunk; must be greater than zero
     * @return a list of chunks; possibly empty if the stream has no elements
     * @throws NullPointerException     if {@code stream} is {@code null}
     * @throws IllegalArgumentException if {@code chunkSize} is less than {@code 1}
     */
    public static List<List<Long>> chunk(LongStream stream, int chunkSize) {
        Objects.requireNonNull(stream, "stream must not be null");
        try (LongStream s = stream) {
            return s.boxed().collect(toChunks(chunkSize));
        }
    }

    /**
     * Chunks a {@link DoubleStream} into {@code List<List<Double>>} and closes it.
     *
     * <p>The final partial chunk is included.</p>
     *
     * @param stream    the source double stream; must not be {@code null}
     * @param chunkSize the maximum number of elements in each chunk; must be greater than zero
     * @return a list of chunks; possibly empty if the stream has no elements
     * @throws NullPointerException     if {@code stream} is {@code null}
     * @throws IllegalArgumentException if {@code chunkSize} is less than {@code 1}
     */
    public static List<List<Double>> chunk(DoubleStream stream, int chunkSize) {
        Objects.requireNonNull(stream, "stream must not be null");
        try (DoubleStream s = stream) {
            return s.boxed().collect(toChunks(chunkSize));
        }
    }

    /**
     * Convenience collector for {@link IntStream}s.
     *
     * @param chunkSize the maximum number of elements in each chunk; must be greater than zero
     * @return a collector that accumulates {@link Integer} values into fixed-size chunks
     * @throws IllegalArgumentException if {@code chunkSize} is less than {@code 1}
     */
    public static Collector<Integer, ?, List<List<Integer>>> toIntChunks(int chunkSize) {
        return toChunks(chunkSize);
    }

    /**
     * Convenience collector for {@link LongStream}s.
     *
     * @param chunkSize the maximum number of elements in each chunk; must be greater than zero
     * @return a collector that accumulates {@link Long} values into fixed-size chunks
     * @throws IllegalArgumentException if {@code chunkSize} is less than {@code 1}
     */
    public static Collector<Long, ?, List<List<Long>>> toLongChunks(int chunkSize) {
        return toChunks(chunkSize);
    }

    /**
     * Convenience collector for {@link DoubleStream}s.
     *
     * @param chunkSize the maximum number of elements in each chunk; must be greater than zero
     * @return a collector that accumulates {@link Double} values into fixed-size chunks
     * @throws IllegalArgumentException if {@code chunkSize} is less than {@code 1}
     */
    public static Collector<Double, ?, List<List<Double>>> toDoubleChunks(int chunkSize) {
        return toChunks(chunkSize);
    }
}
