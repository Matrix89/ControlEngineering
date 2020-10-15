package malte0811.controlengineering.tiles.bus;

import blusunrize.immersiveengineering.api.wires.Connection;
import blusunrize.immersiveengineering.api.wires.ConnectionPoint;
import blusunrize.immersiveengineering.api.wires.ImmersiveConnectableTileEntity;
import blusunrize.immersiveengineering.api.wires.LocalWireNetwork;
import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import malte0811.controlengineering.bus.BusState;
import malte0811.controlengineering.bus.IBusConnector;
import malte0811.controlengineering.bus.IBusInterface;
import malte0811.controlengineering.bus.LocalBusHandler;
import malte0811.controlengineering.tiles.CETileEntities;
import malte0811.controlengineering.util.Clearable;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3d;

import javax.annotation.Nonnull;
import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

public class BusInterfaceTile extends ImmersiveConnectableTileEntity implements IBusConnector {
    private Map<Direction, Pair<WeakReference<IBusInterface>, Runnable>> clearers = new EnumMap<>(Direction.class);

    public BusInterfaceTile() {
        super(CETileEntities.BUS_INTERFACE.get());
    }

    @Override
    public int getMinBusWidthForConfig(ConnectionPoint cp) {
        return 0;
    }

    @Override
    public void onBusUpdated(ConnectionPoint updatedPoint) {
        BusState state = getBusHandler(updatedPoint).getState();
        getConnectedTile().ifPresent(iBusInterface -> iBusInterface.onBusUpdated(state));
    }

    @Override
    public BusState getEmittedState(ConnectionPoint checkedPoint) {
        return getConnectedTile()
                .map(IBusInterface::getEmittedState)
                .orElseGet(BusState::new);
    }

    @Override
    public LocalWireNetwork getLocalNet(int cpIndex) {
        return globalNet.getLocalNet(pos);
    }

    @Override
    public Vector3d getConnectionOffset(@Nonnull Connection con, ConnectionPoint here) {
        return new Vector3d(0.5, 0.5, 0.5);
    }

    private Optional<IBusInterface> getConnectedTile() {
        //TODO facing
        for (Direction d : Direction.VALUES) {
            TileEntity neighbor = world.getTileEntity(pos.offset(d));
            if (neighbor instanceof IBusInterface) {
                IBusInterface i = (IBusInterface) neighbor;
                if (i.canConnect(d.getOpposite())) {
                    Pair<WeakReference<IBusInterface>, Runnable> existing = clearers.get(d);
                    if (existing == null || existing.getFirst().get() != i) {
                        Pair<Clearable<Runnable>, Runnable> newClearer = Clearable.create(() -> getBusHandler(new ConnectionPoint(
                                pos,
                                0
                        )).requestUpdate());
                        i.addMarkDirtyCallback(newClearer.getFirst());
                        clearers.put(d, Pair.of(new WeakReference<>(i), newClearer.getSecond()));
                    }
                    return Optional.of(i);
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public Collection<ResourceLocation> getRequestedHandlers() {
        return ImmutableList.of(LocalBusHandler.NAME);
    }

    @Override
    public void remove() {
        super.remove();
        clearers.values().stream()
                .map(Pair::getSecond)
                .forEach(Runnable::run);
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        clearers.values().stream()
                .map(Pair::getSecond)
                .forEach(Runnable::run);
    }
}