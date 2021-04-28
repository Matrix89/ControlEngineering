package malte0811.controlengineering.logic.cells.impl;

import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleMaps;
import malte0811.controlengineering.logic.cells.LeafcellType;
import malte0811.controlengineering.logic.cells.Pin;
import malte0811.controlengineering.logic.cells.PinDirection;
import malte0811.controlengineering.logic.cells.SignalType;

public class DelayCell extends LeafcellType<Double> {
    public DelayCell(SignalType type, int numTubes) {
        super(
                ImmutableMap.of(DEFAULT_IN_NAME, new Pin(type, PinDirection.INPUT)),
                ImmutableMap.of(DEFAULT_OUT_NAME, new Pin(type, PinDirection.DELAYED_OUTPUT)),
                0D, Codec.DOUBLE, numTubes
        );
    }

    @Override
    public Double nextState(Object2DoubleMap<String> inputSignals, Double currentState) {
        return inputSignals.getDouble(DEFAULT_IN_NAME);
    }

    @Override
    public Object2DoubleMap<String> getOutputSignals(Object2DoubleMap<String> inputSignals, Double oldState) {
        return Object2DoubleMaps.singleton(DEFAULT_OUT_NAME, oldState);
    }
}
