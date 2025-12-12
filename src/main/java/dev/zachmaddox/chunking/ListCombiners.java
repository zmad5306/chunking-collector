package dev.zachmaddox.chunking;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BinaryOperator;

/**
 * Internal combiners for list-based collectors.
 *
 * <p>Package-private on purpose: this is an implementation detail shared by
 * multiple collector implementations and is not part of the public API.</p>
 */
final class ListCombiners {

    private ListCombiners() {
        // utility class
    }

    static <T> BinaryOperator<List<T>> mergingLists() {
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
}
