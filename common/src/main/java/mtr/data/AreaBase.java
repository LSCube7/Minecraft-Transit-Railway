package mtr.data;

import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Tuple;

import java.util.function.Consumer;

public abstract class AreaBase extends NameColorDataBase {

	public Tuple<Integer, Integer> corner1, corner2;

	private static final String KEY_X_MIN = "x_min";
	private static final String KEY_Z_MIN = "z_min";
	private static final String KEY_X_MAX = "x_max";
	private static final String KEY_Z_MAX = "z_max";
	private static final String KEY_CORNERS = "corners";

	public AreaBase() {
		super();
	}

	public AreaBase(long id) {
		super(id);
	}

	public AreaBase(CompoundTag compoundTag) {
		super(compoundTag);
		setCorners(compoundTag.getInt(KEY_X_MIN), compoundTag.getInt(KEY_Z_MIN), compoundTag.getInt(KEY_X_MAX), compoundTag.getInt(KEY_Z_MAX));
	}

	public AreaBase(FriendlyByteBuf packet) {
		super(packet);
		setCorners(packet.readInt(), packet.readInt(), packet.readInt(), packet.readInt());
	}

	@Override
	public CompoundTag toCompoundTag() {
		final CompoundTag compoundTag = super.toCompoundTag();
		compoundTag.putInt(KEY_X_MIN, corner1 == null ? 0 : corner1.getA());
		compoundTag.putInt(KEY_Z_MIN, corner1 == null ? 0 : corner1.getB());
		compoundTag.putInt(KEY_X_MAX, corner2 == null ? 0 : corner2.getA());
		compoundTag.putInt(KEY_Z_MAX, corner2 == null ? 0 : corner2.getB());
		return compoundTag;
	}

	@Override
	public void writePacket(FriendlyByteBuf packet) {
		super.writePacket(packet);
		packet.writeInt(corner1 == null ? 0 : corner1.getA());
		packet.writeInt(corner1 == null ? 0 : corner1.getB());
		packet.writeInt(corner2 == null ? 0 : corner2.getA());
		packet.writeInt(corner2 == null ? 0 : corner2.getB());
	}

	@Override
	public void update(String key, FriendlyByteBuf packet) {
		if (key.equals(KEY_CORNERS)) {
			setCorners(packet.readInt(), packet.readInt(), packet.readInt(), packet.readInt());
		} else {
			super.update(key, packet);
		}
	}

	public void setCorners(Consumer<FriendlyByteBuf> sendPacket) {
		final FriendlyByteBuf packet = new FriendlyByteBuf(Unpooled.buffer());
		packet.writeLong(id);
		packet.writeUtf(KEY_CORNERS);
		packet.writeInt(corner1 == null ? 0 : corner1.getA());
		packet.writeInt(corner1 == null ? 0 : corner1.getB());
		packet.writeInt(corner2 == null ? 0 : corner2.getA());
		packet.writeInt(corner2 == null ? 0 : corner2.getB());
		sendPacket.accept(packet);
	}


	public boolean inArea(int x, int z) {
		return nonNullCorners(this) && RailwayData.isBetween(x, corner1.getA(), corner2.getA()) && RailwayData.isBetween(z, corner1.getB(), corner2.getB());
	}

	public BlockPos getCenter() {
		return nonNullCorners(this) ? new BlockPos((corner1.getA() + corner2.getA()) / 2, 0, (corner1.getB() + corner2.getB()) / 2) : null;
	}

	private void setCorners(int corner1a, int corner1b, int corner2a, int corner2b) {
		corner1 = corner1a == 0 && corner1b == 0 ? null : new Tuple<>(corner1a, corner1b);
		corner2 = corner2a == 0 && corner2b == 0 ? null : new Tuple<>(corner2a, corner2b);
	}

	public static boolean nonNullCorners(AreaBase station) {
		return station != null && station.corner1 != null && station.corner2 != null;
	}
}
