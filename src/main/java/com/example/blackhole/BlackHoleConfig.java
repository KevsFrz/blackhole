package com.example.blackhole;

/** Todos los valores ajustables del mod. Cambia lo que quieras y recompila. */
public final class BlackHoleConfig {
	private BlackHoleConfig() {}

	// Centro del agujero negro (0 64 0 -> centro del bloque)
	public static final double CENTER_X = 0.5;
	public static final double CENTER_Y = 64.5;
	public static final double CENTER_Z = 0.5;

	// Si es true, el agujero negro empieza solo al crear el mundo / iniciar el servidor
	public static final boolean AUTO_START = true;

	// Crecimiento (radio del horizonte de sucesos, en bloques)
	public static final double START_RADIUS = 3.0;
	public static final double MAX_RADIUS = 400.0;
	/** Bloques de radio por tick (0.004 = 0.08 bloques/seg al inicio). */
	public static final double GROWTH_PER_TICK = 0.004;
	/** Cuanto menor, mas rapido se acelera el crecimiento a medida que es grande. */
	public static final double GROWTH_ACCEL_DIVISOR = 150.0;
	/** Cada cuantos bloques de radio se avisa en el chat. */
	public static final int MILESTONE_STEP = 10;

	// Atraccion de entidades: rango = radio * MULT + BASE
	public static final double PULL_RANGE_MULT = 6.0;
	public static final double PULL_RANGE_BASE = 24.0;
	public static final double PULL_BASE = 0.01;
	public static final double PULL_MAX = 0.35;
	public static final double PULL_EXPONENT = 2.5;

	// Destruccion de bloques
	/** Los bloques entre el radio y radio*(1+RIP_MULT)+RIP_BASE salen volando hacia el centro. */
	public static final double RIP_MULT = 0.35;
	public static final double RIP_BASE = 4.0;
	/** Maximo de bloques "voladores" (entidades) a la vez; el resto se destruye directo. */
	public static final int MAX_FALLING_BLOCKS = 600;
	public static final int MAX_RIPS_PER_TICK = 16;
	public static final boolean DESTROY_BEDROCK = false;

	// Efectos visuales
	public static final int RING_PARTICLES = 40;
	public static final double PARTICLE_RANGE_BASE = 128.0;
}
