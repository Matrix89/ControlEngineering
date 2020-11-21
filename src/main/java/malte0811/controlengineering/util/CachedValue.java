package malte0811.controlengineering.util;

import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Supplier;

public class CachedValue<Key, Value> implements Supplier<Value> {
    private final Supplier<Key> getKey;
    private final Function<Key, Value> create;
    private final BiPredicate<Key, Key> equivalent;

    private Key lastKey;
    private Value lastValue;

    public CachedValue(Supplier<Key> getKey, Function<Key, Value> create) {
        this(getKey, create, Objects::equals);
    }

    public CachedValue(Supplier<Key> getKey, Function<Key, Value> create, BiPredicate<Key, Key> equivalent) {
        this.getKey = getKey;
        this.create = create;
        this.equivalent = equivalent;
    }

    public Value get() {
        final Key currentKey = getKey.get();
        if (!equivalent.test(currentKey, lastKey)) {
            this.lastKey = currentKey;
            this.lastValue = create.apply(currentKey);
        }
        return this.lastValue;
    }
}
