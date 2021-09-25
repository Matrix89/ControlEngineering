package malte0811.controlengineering.blocks.shapes;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

@FunctionalInterface
public interface FromBlockFunction<T> {
    T apply(BlockState state, BlockGetter world, BlockPos pos);

    static <T extends Comparable<T>> FromBlockFunction<T> getProperty(Property<T> prop) {
        return (state, w, p) -> state.getValue(prop);
    }

    static <T> FromBlockFunction<T> either(
            FromBlockFunction<Boolean> useSecond, FromBlockFunction<T> first, FromBlockFunction<T> second
    ) {
        return switchOn(useSecond, ImmutableMap.of(false, first, true, second));
    }

    static <T, T2 extends Comparable<T2>>
    FromBlockFunction<T> switchOnProperty(Property<T2> prop, Map<T2, FromBlockFunction<T>> subFunctions) {
        return switchOn((state, world, pos) -> state.getValue(prop), subFunctions);
    }

    static <T, T2 extends Comparable<T2>>
    FromBlockFunction<T> switchOn(FromBlockFunction<T2> prop, Map<T2, FromBlockFunction<T>> subFunctions) {
        return (state, world, pos) -> subFunctions.get(prop.apply(state, world, pos))
                .apply(state, world, pos);
    }

    static <T> FromBlockFunction<T> constant(T value) {
        return (s, w, p) -> value;
    }
}
