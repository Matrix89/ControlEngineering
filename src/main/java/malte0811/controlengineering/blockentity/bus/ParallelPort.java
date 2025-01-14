package malte0811.controlengineering.blockentity.bus;

import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.bytes.ByteList;
import malte0811.controlengineering.bus.BusLine;
import malte0811.controlengineering.bus.BusSignalRef;
import malte0811.controlengineering.bus.BusState;
import malte0811.controlengineering.gui.CEContainers;
import malte0811.controlengineering.gui.remapper.AbstractRemapperMenu;
import malte0811.controlengineering.util.BitUtils;
import malte0811.controlengineering.util.mycodec.MyCodec;
import malte0811.controlengineering.util.mycodec.MyCodecs;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class ParallelPort {
    private static final BusSignalRef DEFAULT_CLOCK_LINE = new BusSignalRef(0, Byte.SIZE);
    private static final MyCodec<@Nullable BusSignalRef> NULLABLE_SIGNAL_CODEC = MyCodecs.nullable(BusSignalRef.CODEC);

    private final ByteList transmitQueue;
    private boolean sendingFirst;
    private byte currentInput;
    private boolean lastTriggerHigh;
    private boolean triggerHigh;
    private List<@Nullable BusSignalRef> dataLines = makeDefaultDataLines();
    @Nullable
    private BusSignalRef clockLine = DEFAULT_CLOCK_LINE;

    public ParallelPort() {
        transmitQueue = new ByteArrayList();
    }

    public boolean tickTX() {
        boolean updateRS = false;
        if (sendingFirst && !transmitQueue.isEmpty()) {
            transmitQueue.removeByte(0);
            sendingFirst = false;
            updateRS = true;
        } else if (!transmitQueue.isEmpty()) {
            sendingFirst = true;
            updateRS = true;
        }
        return updateRS;
    }

    public Optional<Byte> tickRX() {
        Optional<Byte> result;
        if (triggerHigh && !lastTriggerHigh) {
            result = Optional.of(currentInput);
        } else {
            result = Optional.empty();
        }
        lastTriggerHigh = triggerHigh;
        return result;
    }

    public BusState getOutputState() {
        if (!sendingFirst || transmitQueue.isEmpty()) {
            return BusState.EMPTY;
        }
        byte toSend = transmitQueue.getByte(0);
        BusState totalState = set(BusState.EMPTY, clockLine);
        for (int i = 0; i < Byte.SIZE; ++i) {
            if (BitUtils.getBit(toSend, i)) {
                totalState = set(totalState, dataLines.get(i));
            }
        }
        return totalState;
    }

    private BusState set(BusState in, @Nullable BusSignalRef toSet) {
        if (toSet != null) {
            return in.with(toSet, BusLine.MAX_VALID_VALUE);
        } else {
            return in;
        }
    }

    public void onBusStateChange(BusState inputState) {
        triggerHigh = clockLine != null && inputState.getSignal(clockLine) != 0;
        currentInput = 0;
        for (int i = 0; i < Byte.SIZE; ++i) {
            var line = dataLines.get(i);
            if (line != null && inputState.getSignal(line) != 0) {
                currentInput |= 1 << i;
            }
        }
    }

    public void readNBT(CompoundTag nbt) {
        transmitQueue.setElements(nbt.getByteArray("remotePrintQueue"));
        sendingFirst = nbt.getBoolean("sendingFirst");
        currentInput = nbt.getByte("currentInput");
        triggerHigh = nbt.getBoolean("triggerHigh");
        lastTriggerHigh = nbt.getBoolean("lastTriggerHigh");
        dataLines = MyCodecs.list(NULLABLE_SIGNAL_CODEC).fromNBT(nbt.get("dataLines"));
        if (dataLines == null || dataLines.size() != Byte.SIZE) {
            dataLines = makeDefaultDataLines();
        }
        clockLine = NULLABLE_SIGNAL_CODEC.fromNBT(nbt.get("clockLine"), () -> DEFAULT_CLOCK_LINE);
    }

    public CompoundTag toNBT() {
        CompoundTag result = new CompoundTag();
        result.putByteArray("remotePrintQueue", transmitQueue.toByteArray());
        result.putBoolean("sendingFirst", sendingFirst);
        result.putByte("currentInput", currentInput);
        result.putBoolean("triggerHigh", triggerHigh);
        result.putBoolean("lastTriggerHigh", lastTriggerHigh);
        result.put("dataLines", MyCodecs.list(NULLABLE_SIGNAL_CODEC).toNBT(dataLines));
        result.put("clockLine", NULLABLE_SIGNAL_CODEC.toNBT(clockLine));
        return result;
    }

    public void queueChar(byte out) {
        transmitQueue.add(out);
    }

    public void queueStringWithParity(String message) {
        for (byte b : message.getBytes()) {
            queueChar(BitUtils.fixParity(b));
        }
    }

    public int[] getIndicesForRemapping() {
        int[] indices = new int[Byte.SIZE + 1];
        for (int i = 0; i < Byte.SIZE; ++i) {
            indices[i] = indexFromNullable(dataLines.get(i));
        }
        indices[Byte.SIZE] = indexFromNullable(clockLine);
        return indices;
    }

    public void setIndicesFromRemapping(int[] indices) {
        for (int i = 0; i < Byte.SIZE; ++i) {
            dataLines.set(i, BusSignalRef.fromIndex(indices[i]));
        }
        clockLine = BusSignalRef.fromIndex(indices[Byte.SIZE]);
    }

    public Function<UseOnContext, InteractionResult> makeRemapInteraction(BlockEntity be) {
        return ctx -> {
            if (ctx.getPlayer() instanceof ServerPlayer player) {
                NetworkHooks.openGui(player, CEContainers.PORT_REMAPPER.provider(be, this));
            }
            return InteractionResult.SUCCESS;
        };
    }

    private int indexFromNullable(@Nullable BusSignalRef ref) {
        return ref != null ? ref.index() : AbstractRemapperMenu.NOT_MAPPED;
    }

    private static List<BusSignalRef> makeDefaultDataLines() {
        List<BusSignalRef> signals = new ArrayList<>();
        for (int i = 0; i < Byte.SIZE; ++i) {
            signals.add(new BusSignalRef(0, i));
        }
        return signals;
    }
}
