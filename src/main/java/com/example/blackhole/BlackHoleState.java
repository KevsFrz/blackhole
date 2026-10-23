package com.example.blackhole;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;

/** Estado guardado en el mundo (sobrevive a reinicios). */
public class BlackHoleState extends PersistentState {
	public double radius = BlackHoleConfig.START_RADIUS;
	public boolean active = BlackHoleConfig.AUTO_START;
	public double speed = 1.0;
	public int milestone = 0;

	public static BlackHoleState fromNbt(NbtCompound nbt) {
		BlackHoleState s = new BlackHoleState();
		s.radius = nbt.getDouble("radius");
		s.active = nbt.getBoolean("active");
		s.speed = nbt.getDouble("speed");
		s.milestone = nbt.getInt("milestone");
		if (s.radius <= 0) s.radius = BlackHoleConfig.START_RADIUS;
		if (s.speed <= 0 && !nbt.contains("speed")) s.speed = 1.0;
		return s;
	}

	@Override
	public NbtCompound writeNbt(NbtCompound nbt) {
		nbt.putDouble("radius", radius);
		nbt.putBoolean("active", active);
		nbt.putDouble("speed", speed);
		nbt.putInt("milestone", milestone);
		return nbt;
	}

	public static BlackHoleState get(MinecraftServer server) {
		return server.getOverworld().getPersistentStateManager()
				.getOrCreate(BlackHoleState::fromNbt, BlackHoleState::new, "blackhole");
	}
}
