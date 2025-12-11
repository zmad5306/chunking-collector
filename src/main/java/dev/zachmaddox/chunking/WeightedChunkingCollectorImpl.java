package dev.zachmaddox.chunking;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.ToLongFunction;
import java.util.stream.Collector;

/**
 * Collector that groups elements into chunks with a maximum total weight.
 */
final class WeightedChunkingCollectorImpl<T>
        implements Collector<T, List<T>, List<List<T>>> {

    private final long maxWeight;
    private final ToLongFunction<? super T> weigher;

    WeightedChunkingCollectorImpl(long maxWeight, ToLongFunction<? super T> weigher) {
        if (maxWeight < 1L) {
            throw new IllegalArgumentException("maxWeight must be greater than zero.");
        }
        this.maxWeight = maxWeight;
        this.weigher = Objects.requireNonNull(weigher, "weigher must not be null");
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
            int size = allElements.size();
            if (size == 0) {
                return Collections.emptyList();
            }

            List<List<T>> result = new ArrayList<>();
            List<T> currentChunk = new ArrayList<>();
            long currentWeight = 0L;

            for (int i = 0; i < size; i++) {
                T value = allElements.get(i);
                long w = weigher.applyAsLong(value);
                if (w < 0L) {
                    throw new IllegalArgumentException("Element weight must not be negative.");
                }

                // If this element itself is heavier than maxWeight, it forms its own chunk.
                if (w > maxWeight) {
                    if (!currentChunk.isEmpty()) {
                        result.add(currentChunk);
                        currentChunk = new ArrayList<>();
                        currentWeight = 0L;
                    }
                    List<T> single = new ArrayList<>(1);
                    single.add(value);
                    result.add(single);
                    continue;
                }

                if (currentChunk.isEmpty()) {
                    currentChunk.add(value);
                    currentWeight = w;
                } else if (currentWeight + w <= maxWeight) {
                    currentChunk.add(value);
                    currentWeight += w;
                } else {
                    result.add(currentChunk);
                    currentChunk = new ArrayList<>();
                    currentChunk.add(value);
                    currentWeight = w;
                }
            }

            if (!currentChunk.isEmpty()) {
                result.add(currentChunk);
            }

            return result;
        };
    }

    @Override
    public Set<Characteristics> characteristics() {
        return Collections.emptySet();
    }
}
