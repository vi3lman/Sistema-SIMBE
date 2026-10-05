# Apuntes: Socket, ServerSocket y streams de I/O

Notas de estudio sobre programación de sockets en Java, basadas en el código
de `simbe-billetaje` (Tarea 3, Sistemas Distribuidos).

## 1. `Socket` — un extremo de conexión TCP

Representa **un tubo bidireccional de bytes** ya conectado entre dos
procesos (cliente y servidor). Por sí solo, TCP solo entiende bytes crudos —
no sabe nada de texto, líneas ni JSON.

```java
Socket socket = new Socket(host, puerto);   // como cliente: abre la conexión
```

Métodos clave:

| Método | Qué devuelve | Para qué |
|---|---|---|
| `getOutputStream()` | `OutputStream` | "caño" para escribir bytes hacia el otro lado |
| `getInputStream()` | `InputStream` | "caño" para leer bytes que llegan |
| `close()` | — | cierra la conexión (automático con try-with-resources) |
| `getRemoteSocketAddress()` | `SocketAddress` | de dónde vino la conexión (usado en logs del servidor) |

## 2. `ServerSocket` — el "portero", no una conexión

`ServerSocket` **no es** un extremo de conexión. Es un objeto que se queda
escuchando en un puerto, esperando que alguien se conecte. No tiene
`getInputStream()` ni `getOutputStream()` — no sirve para mandar/recibir
datos directamente.

```java
ServerSocket serverSocket = new ServerSocket(PUERTO);   // abre el puerto
Socket socketCliente = serverSocket.accept();             // espera un cliente
```

- **`accept()`** es el método clave: **bloquea** (se queda esperando sin
  gastar CPU) hasta que un cliente se conecta. Cuando eso pasa, devuelve un
  `Socket` **nuevo y distinto por cada cliente**, con el que recién ahí
  se puede leer/escribir.
- Patrón típico de servidor: `while (true) { Socket s = serverSocket.accept(); ... }`
  — un loop infinito que atiende conexiones una tras otra (o, si se
  combina con `Thread`, muchas en simultáneo).

## 3. El problema: los streams crudos solo manejan bytes

`OutputStream`/`InputStream` son las clases más primitivas — trabajan byte
a byte, sin noción de texto ni de "líneas". Escribir/leer JSON directo ahí
sería incómodo y propenso a errores. Por eso se usan **decoradores**: clases
que envuelven a un stream más primitivo y le agregan funcionalidad, sin
modificar la clase original (patrón *Decorator*).

## 4. Lado de escritura: `PrintWriter`

```java
PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
out.println(json.toJSONString());
```

- Envuelve el `OutputStream` y agrega métodos de texto (`print`, `println`,
  `printf`), iguales a los de `System.out`.
- El segundo parámetro (`true`) activa **autoFlush**: cada `println()`
  fuerza el envío inmediato de los datos por la red.
- **Sin autoFlush** (`false` o el constructor de un solo argumento): el
  texto queda guardado en un buffer local y no viaja hasta que el buffer se
  llena o se cierra el stream. Si el otro lado está esperando esa respuesta
  con `readLine()`, ambos procesos quedan colgados esperándose mutuamente
  para siempre — un deadlock por I/O.

## 5. Lado de lectura: dos decoradores en cadena

```java
BufferedReader in = new BufferedReader(
    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8)
);
String linea = in.readLine();
```

Acá hacen falta **dos** capas, porque cada una resuelve un problema distinto:

- **`InputStreamReader`**: traduce bytes crudos → caracteres de texto, según
  una codificación (acá, UTF-8). No sabe nada de líneas.
- **`BufferedReader`**: envuelve al anterior y agrega (a) eficiencia (lee de
  a bloques grandes en vez de a cada rato) y (b) el método `.readLine()`,
  que devuelve una línea completa de texto sin el salto de línea final.

`readLine()` **bloquea** el hilo hasta que llega una línea completa (o hasta
que la conexión se cierra, en cuyo caso devuelve `null`).

## 6. El protocolo implícito del proyecto

> Cada mensaje JSON completo viaja en **una sola línea**, terminada en `\n`.

Por eso el emisor siempre usa `println(json)` (agrega el `\n`), y el
receptor siempre usa `readLine()` (lee hasta encontrar ese `\n`). Ambos
lados están "de acuerdo" en este contrato — si uno mandara sin salto de
línea, el otro se quedaría esperando para siempre.

## 7. Diagrama de capas (resumen visual)

```
                     Socket (conexión TCP)
                    /                      \
        .getOutputStream()          .getInputStream()
              |                              |
        OutputStream                   InputStream
        (bytes crudos)                (bytes crudos)
              |                              |
      new PrintWriter(...)      new InputStreamReader(UTF-8)
              |                              |
        PrintWriter                  InputStreamReader
        println() → texto           bytes → texto
              |                              |
      out.println(json)          new BufferedReader(...)
              |                              |
        [tu código]                    BufferedReader
      String con el JSON            readLine() → línea
                                            |
                                     in.readLine()
                                            |
                                       [tu código]
                                  línea de texto (String)
```

## 8. Por qué el lado de lectura tiene una capa extra

Escribir necesita una sola responsabilidad (texto → bytes + formato de
salida), y `PrintWriter` la cubre sola. Leer separa esa responsabilidad en
dos: `InputStreamReader` solo traduce bytes → caracteres; `BufferedReader`
agrega el concepto de "línea". Java las mantiene separadas para que cada
clase decoradora haga una sola cosa — se combinan según lo que se necesite
(por ejemplo, se podría usar solo `InputStreamReader` si quisieras leer
caracter por caracter en vez de línea por línea).

## 9. try-with-resources con múltiples recursos

```java
try (Socket socket = new Socket(host, puerto);
     PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
     BufferedReader in = new BufferedReader(
         new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
    // ...
}
```

- Se **abren** en el orden que aparecen (primero el socket, porque los
  decoradores lo necesitan ya abierto).
- Se **cierran** automáticamente al salir del bloque, en **orden inverso**
  al que se abrieron.
- Evita tener que escribir un `finally` manual para cerrar cada recurso.

## Preguntas para repasar

- ¿Qué pasa si `PrintWriter` no tiene `autoFlush` activado?
- ¿Por qué `ServerSocket` no tiene `getInputStream()`?
- ¿Por qué el lado de lectura necesita dos decoradores y el de escritura
  solo uno?
- ¿Qué significa que `accept()` y `readLine()` sean *bloqueantes*?
