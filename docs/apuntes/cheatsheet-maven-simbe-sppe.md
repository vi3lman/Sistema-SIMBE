# Chuleta de Maven — SIMBE / SPPE

## Build básico

```bash
mvn clean compile      # borra target/ y recompila todo de cero
mvn clean package       # compila + testea + genera el .jar en target/
mvn test                 # corre los tests (si tenés)
```

`clean` primero = build limpio, sin restos de compilaciones viejas.

## Correr una clase directamente (sin armar el jar)

Forma general:

```bash
mvn compile exec:java -Dexec.mainClass=<paquete.completo.Clase>
```

### SIMBE

```bash
# Servidor (Módulo de Recargas)
mvn compile exec:java -Dexec.mainClass=py.una.simbe.server.SimbeServidorRecargas

# Cliente simulador
mvn compile exec:java -Dexec.mainClass=py.una.simbe.client.SolicitanteRecargaCliente
```

### SPPE (cuando tengas las clases equivalentes)

```bash
mvn compile exec:java -Dexec.mainClass=py.una.sppe.server.<NombreServidor>
```

## Pasar parámetros personalizados (host, puerto, BD)

Cualquier `System.getProperty("clave", "default")` en el código se sobreescribe agregando `-Dclave=valor` al comando:

```bash
mvn compile exec:java \
  -Dexec.mainClass=py.una.simbe.server.SimbeServidorRecargas \
  -Dsppe.host=192.168.1.50 \
  -Dsppe.port=7000 \
  -Dsimbe.db.url=jdbc:postgresql://10.0.0.5:5432/simbe \
  -Dsimbe.db.user=miusuario \
  -Dsimbe.db.password=miclave
```

Con `java -jar` (sin Maven), el `-D` va **antes** del `-jar`:

```bash
java -Dsppe.host=10.0.0.5 -Dsppe.port=6001 -jar target/simbe-billetaje-0.0.1-SNAPSHOT-jar-with-dependencies.jar
```

## Flujo típico para probar todo el sistema

```bash
# Terminal 1 (carpeta simbe-billetaje/)
mvn compile exec:java -Dexec.mainClass=py.una.simbe.server.SimbeServidorRecargas

# Terminal 2 (carpeta sppe-pagos/)
mvn compile exec:java -Dexec.mainClass=py.una.sppe.server.<NombreServidor>

# Terminal 3 (carpeta simbe-billetaje/)
mvn compile exec:java -Dexec.mainClass=py.una.simbe.client.SolicitanteRecargaCliente
```

## Otros comandos útiles

```bash
mvn dependency:tree                          # ver árbol de dependencias
mvn -o compile                                # modo offline (usa solo ~/.m2)
mvn compile -q                                 # menos verborragia en consola
mvn versions:display-dependency-updates       # ver si hay versiones nuevas de tus deps
```

## Para no olvidar: anatomía del comando largo

```
mvn   compile   exec:java   -Dexec.mainClass=py.una.simbe.server.SimbeServidorRecargas
 |       |          |                  |
 |       |          |                  └─ propiedad que lee el plugin exec: qué clase correr
 |       |          └─ goal "java" del plugin "exec" → ejecuta una clase con main()
 |       └─ fase del ciclo de vida: compilar antes de ejecutar
 └─ el ejecutable de Maven
```

`-D` = flag de la JVM para definir una system property. No es sintaxis de Maven, es de Java.
