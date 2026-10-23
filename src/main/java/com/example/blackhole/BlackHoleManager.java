package com.example.blackhole;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.dragon.EnderDragonPart;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import org.joml.Vector3f;

import java.util.List;

public final class BlackHoleManager {
	private BlackHoleManager() {}

	public static final Vec3d CENTER = new Vec3d(BlackHoleConfig.CENTER_X, BlackHoleConfig.CENTER_Y, BlackHoleConfig.CENTER_Z);
	private static final BlockPos CENTER_POS = BlockPos.ofFloored(CENTER);

	private static final DustParticleEffect BLACK_DUST = new DustParticleEffect(new Vector3f(0.0f, 0.0f, 0.0f), 4.0f);
	private static final DustParticleEffect DISK_DUST = new DustParticleEffect(new Vector3f(1.0f, 0.55f, 0.1f), 2.0f);
	private static final DustParticleEffect DISK_DUST_HOT = new DustParticleEffect(new Vector3f(1.0f, 0.95f, 0.8f), 1.5f);

	/** Bloques voladores contados en el tick anterior (para limitar la carga). */
	private static int lastFallingCount = 0;

	public static void tick(MinecraftServer server) {
		BlackHoleState state = BlackHoleState.get(server);
		if (!state.active) return;
		ServerWorld world = server.getOverworld();

		grow(server, state);
		pullEntities(world, state);
		eatBlocks(world, state);
		effects(world, state);
	}

	// ------------------------------------------------------------------ crecimiento

	private static void grow(MinecraftServer server, BlackHoleState state) {
		double growth = BlackHoleConfig.GROWTH_PER_TICK * state.speed
				* (1.0 + state.radius / BlackHoleConfig.GROWTH_ACCEL_DIVISOR);
		state.radius = Math.min(BlackHoleConfig.MAX_RADIUS, state.radius + growth);

		int milestone = (int) (state.radius / BlackHoleConfig.MILESTONE_STEP);
		if (milestone > state.milestone) {
			state.milestone = milestone;
			server.getPlayerManager().broadcast(
					Text.literal("El agujero negro ha crecido: radio " + (milestone * BlackHoleConfig.MILESTONE_STEP)
							+ " bloques. ¡Aléjate!").formatted(Formatting.DARK_PURPLE, Formatting.BOLD),
					false);
		}
		state.markDirty();
	}

	// ------------------------------------------------------------------ entidades

	private static double pullRange(double radius) {
		return radius * BlackHoleConfig.PULL_RANGE_MULT + BlackHoleConfig.PULL_RANGE_BASE;
	}

	private static void pullEntities(ServerWorld world, BlackHoleState state) {
		double radius = state.radius;
		double range = pullRange(radius);
		Box box = new Box(CENTER, CENTER).expand(range);
		List<Entity> entities = world.getOtherEntities(null, box);
		int falling = 0;

		for (Entity e : entities) {
			if (e.isRemoved() || e instanceof EnderDragonPart) continue;
			if (e instanceof PlayerEntity p && p.isSpectator()) continue;

			Vec3d pos = e.getPos().add(0, e.getHeight() * 0.5, 0);
			Vec3d toCenter = CENTER.subtract(pos);
			double dist = toCenter.length();
			if (dist > range) continue;

			// Bloques arrancados: viajan en linea recta hacia el centro
			if (e instanceof FallingBlockEntity fb) {
				if (dist <= radius + 0.5) {
					fb.discard();
					continue;
				}
				falling++;
				double t = 1.0 - dist / range;
				double speed = (0.2 + 0.5 * t) * Math.min(3.0, 1.0 + radius / 40.0);
				fb.dropItem = false;
				fb.noClip = true;
				fb.setNoGravity(true);
				fb.setVelocity(toCenter.normalize().multiply(speed));
				if (fb.age % 3 == 0) fb.velocityModified = true;
				continue;
			}

			// Dentro del horizonte de sucesos: destruido
			if (dist <= radius) {
				swallow(world, e);
				continue;
			}

			// Atraccion: debil lejos, muy fuerte cerca
			double t = 1.0 - dist / range;
			double strength = BlackHoleConfig.PULL_BASE + BlackHoleConfig.PULL_MAX * Math.pow(t, BlackHoleConfig.PULL_EXPONENT);
			Vec3d push = toCenter.normalize().multiply(strength);
			e.addVelocity(push.x, push.y, push.z);
			e.velocityModified = true;
		}
		lastFallingCount = falling;
	}

	private static void swallow(ServerWorld world, Entity e) {
		if (e instanceof LivingEntity le) {
			if (le.isAlive()) {
				le.damage(world.getDamageSources().outOfWorld(), Float.MAX_VALUE);
			}
		} else {
			e.discard();
		}
	}

	// ------------------------------------------------------------------ bloques

	private static Vec3d randomUnit(Random rnd) {
		double z = rnd.nextDouble() * 2.0 - 1.0;
		double a = rnd.nextDouble() * Math.PI * 2.0;
		double s = Math.sqrt(1.0 - z * z);
		return new Vec3d(s * Math.cos(a), z, s * Math.sin(a));
	}

	private static void eatBlocks(ServerWorld world, BlackHoleState state) {
		double r = state.radius;
		double ripR = r + BlackHoleConfig.RIP_BASE + r * BlackHoleConfig.RIP_MULT;
		Random rnd = world.getRandom();
		BlockPos.Mutable mp = new BlockPos.Mutable();

		int attempts = (int) Math.min(3000, 500 + r * 15);

		// 1) Interior del horizonte: se borra directamente
		for (int i = 0; i < attempts; i++) {
			double d = r * Math.cbrt(rnd.nextDouble());
			Vec3d u = randomUnit(rnd);
			mp.set(CENTER.x + u.x * d, CENTER.y + u.y * d, CENTER.z + u.z * d);
			BlockState bs = readable(world, mp);
			if (bs == null) continue;
			world.setBlockState(mp, Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
		}

		// 2) Corona exterior: los bloques salen volando hacia el centro
		int ripsLeft = BlackHoleConfig.MAX_RIPS_PER_TICK;
		int fallingNow = lastFallingCount;
		double r3 = r * r * r;
		double ripR3 = ripR * ripR * ripR;
		for (int i = 0; i < attempts; i++) {
			double d = Math.cbrt(r3 + rnd.nextDouble() * (ripR3 - r3));
			Vec3d u = randomUnit(rnd);
			mp.set(CENTER.x + u.x * d, CENTER.y + u.y * d, CENTER.z + u.z * d);
			BlockState bs = readable(world, mp);
			if (bs == null) continue;

			boolean canFly = ripsLeft > 0 && fallingNow < BlackHoleConfig.MAX_FALLING_BLOCKS
					&& !bs.hasBlockEntity() && bs.getFluidState().isEmpty();
			if (canFly) {
				FallingBlockEntity fb = FallingBlockEntity.spawnFromBlock(world, mp.toImmutable(), bs);
				fb.dropItem = false;
				fb.noClip = true;
				fb.setNoGravity(true);
				ripsLeft--;
				fallingNow++;
			} else {
				world.setBlockState(mp, Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
			}
		}
	}

	/** Devuelve el estado si el bloque es destruible y su chunk esta cargado; si no, null. */
	private static BlockState readable(ServerWorld world, BlockPos pos) {
		if (world.isOutOfHeightLimit(pos)) return null;
		if (!world.getChunkManager().isChunkLoaded(pos.getX() >> 4, pos.getZ() >> 4)) return null;
		BlockState bs = world.getBlockState(pos);
		if (bs.isAir()) return null;
		if (!BlackHoleConfig.DESTROY_BEDROCK && bs.getHardness(world, pos) < 0) return null;
		return bs;
	}

	// ------------------------------------------------------------------ efectos

	private static void effects(ServerWorld world, BlackHoleState state) {
		double r = state.radius;
		long time = world.getTime();
		Random rnd = world.getRandom();
		double visRange = BlackHoleConfig.PARTICLE_RANGE_BASE + r * 3.0;
		double visRangeSq = visRange * visRange;

		for (ServerPlayerEntity p : world.getPlayers()) {
			if (p.squaredDistanceTo(CENTER) > visRangeSq) continue;

			// Nucleo negro
			int core = (int) Math.min(200, 30 + r * 4);
			world.spawnParticles(p, BLACK_DUST, true, CENTER.x, CENTER.y, CENTER.z, core, r * 0.5, r * 0.5, r * 0.5, 0.0);

			// Particulas de portal siendo succionadas
			world.spawnParticles(p, ParticleTypes.PORTAL, true, CENTER.x, CENTER.y, CENTER.z, 50, r, r, r, 1.0);

			// Disco de acrecion (anillo inclinado)
			double tilt = 0.35;
			for (int i = 0; i < BlackHoleConfig.RING_PARTICLES; i++) {
				double a = rnd.nextDouble() * Math.PI * 2.0;
				double rr = r * (1.3 + rnd.nextDouble() * 0.9);
				double px = CENTER.x + Math.cos(a) * rr;
				double py = CENTER.y + Math.sin(a) * rr * Math.sin(tilt);
				double pz = CENTER.z + Math.sin(a) * rr * Math.cos(tilt);
				DustParticleEffect fx = rnd.nextInt(4) == 0 ? DISK_DUST_HOT : DISK_DUST;
				world.spawnParticles(p, fx, true, px, py, pz, 0, 0, 0, 0, 0);
			}
		}

		if (time % 40 == 0) {
			float vol = (float) Math.min(60.0, 4.0 + r * 0.5);
			world.playSound(null, CENTER_POS, SoundEvents.BLOCK_PORTAL_AMBIENT, SoundCategory.AMBIENT, vol, 0.5f);
		}
	}
}
