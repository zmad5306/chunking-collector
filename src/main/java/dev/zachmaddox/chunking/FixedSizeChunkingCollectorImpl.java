package dev.zachmaddox.chunking;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.stream.Collector;

/**
 * Fixed-size chunking collector with {@link Chunking.RemainderPolicy} and custom chunk factory.
 */
final class FixedSizeChunkingCollectorImpl<T, C extends List<T>>
        implements Collector<T, List<T>, List<C>> {

    private final int chunkSize;
    private final Chunking.RemainderPolicy remainderPolicy;
    private final IntFunction<C> chunkFactory;

    FixedSizeChunkingCollectorImpl(
            int chunkSize,
            Chunking.RemainderPolicy remainderPolicy,
            IntFunction<C> chunkFactory
    ) {
        if (chunkSize < 1) {
            throw new IllegalArgumentException("Chunk size must be greater than zero.");
        }
        this.chunkSize = chunkSize;
        this.remainderPolicy = Objects.requireNonNull(remainderPolicy, "remainderPolicy must not be null");
        this.chunkFactory = Objects.requireNonNull(chunkFactory, "chunkFactory must not be null");
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
    public Function<List<T>, List<C>> finisher() {
        return allElements -> {
            int size = allElements.size();
            if (size == 0) {
                return Collections.emptyList();
            }

            int fullChunkCount = size / chunkSize;
            boolean hasRemainder = (size % chunkSize) != 0;
            int estimatedChunks = fullChunkCount +
                    ((hasRemainder && remainderPolicy == Chunking.RemainderPolicy.INCLUDE_PARTIAL) ? 1 : 0);

            List<C> chunks = new ArrayList<>(estimatedChunks);

            int start = 0;
            while (start < size) {
                int end = Math.min(start + chunkSize, size);
                int length = end - start;

                if (length < chunkSize && remainderPolicy == Chunking.RemainderPolicy.DROP_PARTIAL) {
                    break;
                }

                C chunk = chunkFactory.apply(length);
                for (int i = start; i < end; i++) {
                    chunk.add(allElements.get(i));
                }
                chunks.add(chunk);

                start += chunkSize;
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
