package dev.zachmaddox.chunking;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Utility class for chunking streams and iterables into fixed-size lists.
 */
public final class Chunking {

    private Chunking() {
        // utility class
    }

    /**
     * Returns a {@link Collector} that groups elements into chunks of {@code chunkSize}.
     *
     * <p>Works with any stream source (lists, sets, arrays via {@code Arrays.stream}, etc.).</p>
     */
    public static <T> Collector<T, ?, List<List<T>>> toChunks(int chunkSize) {
        return new ChunkingCollectorImpl<>(chunkSize);
    }

    /**
     * Chunks an {@link Iterable} into a list of lists, using its iteration order.
     */
    public static <T> List<List<T>> chunk(Iterable<T> iterable, int chunkSize) {
        Objects.requireNonNull(iterable, "iterable must not be null");
        return StreamSupport.stream(iterable.spliterator(), false)
                .collect(toChunks(chunkSize));
    }

    /**
     * Chunks a {@link Collection} into a list of lists, using its iteration order.
     */
    public static <T> List<List<T>> chunk(Collection<T> collection, int chunkSize) {
        Objects.requireNonNull(collection, "collection must not be null");
        return collection.stream().collect(toChunks(chunkSize));
    }

    /**
     * Chunks a {@link Stream} into a list of lists and closes the stream.
     */
    public static <T> List<List<T>> chunk(Stream<T> stream, int chunkSize) {
        Objects.requireNonNull(stream, "stream must not be null");
        try (Stream<T> s = stream) {
            return s.collect(toChunks(chunkSize));
        }
    }

    /**
     * Varargs convenience method: chunk an array of elements.
     */
    @SafeVarargs
    public static <T> List<List<T>> chunk(int chunkSize, T... elements) {
        Objects.requireNonNull(elements, "elements must not be null");
        List<T> list = new ArrayList<>(elements.length);
        Collections.addAll(list, elements);
        return chunk(list, chunkSize);
    }

    /**
     * Internal collector implementation. Users never see this type directly.
     */
    private static final class ChunkingCollectorImpl<T>
            implements Collector<T, List<T>, List<List<T>>> {

        private final int chunkSize;

        ChunkingCollectorImpl(int chunkSize) {
            if (chunkSize < 1) {
                throw new IllegalArgumentException("Chunk size must be greater than zero.");
            }
            this.chunkSize = chunkSize;
        }

        @Override
        public Supplier<List<T>> supplier() {
            return ArrayList::new;
        }

        @Override
        public BiConsumer<List<T>, T> accumulator() {
            return List::add;
        }

        @Override
        public BinaryOperator<List<T>> combiner() {
            return (left, right) -> {
                if (left.isEmpty()) {
                    return right;
                }
                if (right.isEmpty()) {
                    return left;
                }
                List<T> combined = new ArrayList<>(left.size() + right.size());
                combined.addAll(left);
                combined.addAll(right);
                return combined;
            };
        }

        @Override
        public Function<List<T>, List<List<T>>> finisher() {
            return allElements -> {
                if (allElements.isEmpty()) {
                    return Collections.emptyList();
                }

                int size = allElements.size();
                List<List<T>> chunks = new ArrayList<>((size + chunkSize - 1) / chunkSize);

                for (int start = 0; start < size; start += chunkSize) {
                    int end = Math.min(start + chunkSize, size);
                    chunks.add(new ArrayList<>(allElements.subList(start, end)));
                }

                return chunks;
            };
        }

        @Override
        public Set<Characteristics> characteristics() {
            // Order matters and finisher is non-identity, so no characteristics.
            return Collections.emptySet();
        }
    }
}
