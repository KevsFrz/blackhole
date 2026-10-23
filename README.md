# Agujero Negro (Fabric 1.20.1)

Un agujero negro aparece en **0 64 0**, absorbe bloques y entidades y crece cada vez más rápido.

## Compilar
Requiere JDK 17. Abre la carpeta en IntelliJ IDEA (importa el proyecto Gradle) o ejecuta:

    gradle build --no-daemon        (Gradle 8.8+)

El .jar queda en `build/libs/blackhole-1.0.0.jar`. Ponlo en `mods/` junto a Fabric API 0.92.2+1.20.1.

## Uso
Solo hace falta en el servidor (los clientes pueden ser vanilla). En un mundo nuevo arranca solo.

Comandos (nivel de op 2):
- `/blackhole status`
- `/blackhole start` / `/blackhole stop`
- `/blackhole reset`
- `/blackhole radius <n>`
- `/blackhole speed <multiplicador>`

Todos los valores (centro, velocidad de crecimiento, fuerza de atracción, rendimiento) están en `BlackHoleConfig.java`.
