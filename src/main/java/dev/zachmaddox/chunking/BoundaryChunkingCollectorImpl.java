package dev.zachmaddox.chunking;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

/**
 * Collector that groups elements into chunks based on a boundary predicate.
 */
final class BoundaryChunkingCollectorImpl<T>
        implements Collector<T, List<T>, List<List<T>>> {

    private final BiPredicate<? super T, ? super T> sameGroup;

    BoundaryChunkingCollectorImpl(BiPredicate<? super T, ? super T> sameGroup) {
        this.sameGroup = Objects.requireNonNull(sameGroup, "sameGroup must not be null");
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
            if (size == 0) {
                return Collections.emptyList();
            }

            List<List<T>> result = new ArrayList<>();
            List<T> current = new ArrayList<>();
            T previous = null;

            for (T value : allElements) {
                if (current.isEmpty()) {
                    current.add(value);
                } else {
                    if (sameGroup.test(previous, value)) {
                        current.add(value);
                    } else {
                        result.add(current);
                        current = new ArrayList<>();
                        current.add(value);
                    }
                }
                previous = value;
            }

            result.add(current);

            return result;
        };
    }

    @Override
    public Set<Characteristics> characteristics() {
        return Collections.emptySet();
    }
}
