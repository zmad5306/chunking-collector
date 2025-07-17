import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ChunkingCollector<T> implements Collector<T, List<T>, List<List<T>>> {

    private final int chunkSize;

    public ChunkingCollector(int chunkSize) {
        if (chunkSize < 1) throw new IllegalArgumentException("Chunk size must be greater than zero.");
        this.chunkSize = chunkSize;
    }

    @Override
    public Supplier<List<T>> supplier() {
        return ArrayList::new;
    }

    @Override
    public BiConsumer<List<T>, T> accumulator() {
        return (ts, t) -> ts.add(t);
    }

    @Override
    public BinaryOperator<List<T>> combiner() {
        return (ts, ts2) -> Stream.concat(ts.stream(), ts2.stream()).collect(Collectors.toList());
    }

    @Override
    public Function<List<T>, List<List<T>>> finisher() {
        return ts -> {
            List<List<T>> chunks = new ArrayList<>();
            List<T> chunk = new ArrayList<>();
            for (int n = 0; n < ts.size(); n++) {
                if ((n > 0 && n % chunkSize == 0) || n == ts.size()) {
                    chunks.add(chunk);
                    chunk = new ArrayList<>();
                }
                chunk.add(ts.get(n));
            }
            chunks.add(chunk);
            return chunks;
        };
    }

    @Override
    public Set<Collector.Characteristics> characteristics() {
        Set<Characteristics> characteristics = new HashSet<>();
        characteristics.add(Characteristics.UNORDERED);
        return characteristics;
    }

}
