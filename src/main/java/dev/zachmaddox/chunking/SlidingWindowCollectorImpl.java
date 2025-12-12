package dev.zachmaddox.chunking;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

/**
 * Collector that builds sliding windows of fixed size with a fixed step.
 */
final class SlidingWindowCollectorImpl<T>
        implements Collector<T, List<T>, List<List<T>>> {

    private final int windowSize;
    private final int step;

    SlidingWindowCollectorImpl(int windowSize, int step) {
        if (windowSize < 1) {
            throw new IllegalArgumentException("windowSize must be greater than zero.");
        }
        if (step < 1) {
            throw new IllegalArgumentException("step must be greater than zero.");
        }
        this.windowSize = windowSize;
        this.step = step;
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
        return ListCombiners.mergingLists();
    }

    @Override
    public Function<List<T>, List<List<T>>> finisher() {
        return allElements -> {
            int size = allElements.size();
            if (size < windowSize) {
                return Collections.emptyList();
            }
            int estimatedCount = 1 + Math.max(0, (size - windowSize) / step);
            List<List<T>> windows = new ArrayList<>(estimatedCount);

            for (int start = 0; start + windowSize <= size; start += step) {
                List<T> window = new ArrayList<>(windowSize);
                for (int i = start; i < start + windowSize; i++) {
                    window.add(allElements.get(i));
                }
                windows.add(window);
            }

            return windows;
        };
    }

    @Override
    public Set<Characteristics> characteristics() {
        return Collections.emptySet();
    }
}
