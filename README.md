# UnboundProtection

Plugin de proteccion de mundos y regiones, con foco en **Spigot 1.20.1** y compatibilidad prevista con Arclight 1.20.1. No requiere WorldGuard ni Multiverse. Incluye mensajes centralizados y personalizables mediante `ChatUtil`.

## Compatibilidad

- Spigot API 1.20.1, Java 17.
- Arclight 1.20.1: utiliza eventos y API Bukkit/Spigot, sin NMS ni dependencias de Paper.
- Mundos Bukkit cargados, incluidos los que carga Multiverse-Core. Multiverse no es obligatorio.
- Persistencia en `plugins/UnboundProtection/regions.yml` y `worlds.yml`.

## Construccion

Necesitas JDK 17 o superior y Maven 3.9 o superior. Desde la carpeta del proyecto:

```bash
mvn clean package
```

El JAR se genera en `target/UnboundProtection-1.20.1.jar`. Copialo a `plugins/` y reinicia el servidor. Esta version no incluye una compilacion prehecha porque el entorno donde se preparo el proyecto no tiene Maven ni acceso a los repositorios para resolver la API; compila localmente con el comando anterior.

## Crear una region

1. Da la herramienta con `/up wand`.
2. Click izquierdo en un bloque para `pos1` y derecho para `pos2`. Tambien puedes usar `/up pos1` y `/up pos2` para marcar la posicion actual.
3. Opcional: ejecuta `/up expandvert` para cubrir desde la altura minima hasta la maxima del mundo.
4. Crea la region con `/up create spawn`.

Las dos posiciones deben ser del mismo mundo. Si quieres comprobar el mundo seleccionado, usa los mensajes de la seleccion o ejecuta `/up info` dentro de la region. El nombre acepta letras, numeros, guion y guion bajo, hasta 32 caracteres.

Una region nueva protege por defecto construccion, interacciones, contenedores, cubos, marcos, explosiones, fuego, cambios de bloques causados por entidades, pistones y fluidos. Los miembros y el propietario pueden construir e interactuar en esa region, pero las restricciones ambientales (por ejemplo, explosiones y fuego) siguen vigentes. Puedes cambiar cada comportamiento con flags.

## Proteccion global de un mundo

Los comandos usan los nombres que Bukkit tiene cargados. `world` puede ser `world`, `world_nether`, un mundo de Multiverse o cualquier otro mundo cargado:

```text
/up world list
/up world protect world
/up world flags world
/up world flag world pvp on
/up world flag world mob-spawning on
/up world unprotect world
```

`/up world protect <mundo>` activa las restricciones globales de construccion, interacciones, contenedores, cubos, marcos, explosiones, fuego, cambios de bloque de entidades, pistones y flujo de fluidos. Estas reglas globales afectan a todos, incluidos los propietarios y miembros de regiones. El bypass omite las restricciones de acciones de jugador; las reglas ambientales sin un jugador asociado (por ejemplo, el flujo de fluidos o una explosion de mob) siguen vigentes. `unprotect` desactiva las flags globales de ese mundo, sin borrar ni desactivar sus regiones.

## Comandos

| Comando | Funcion |
|---|---|
| `/up` o `/up help` | Ayuda |
| `/up wand` | Obtener la wand de seleccion |
| `/up pos1`, `/up pos2` | Marcar las coordenadas actuales |
| `/up expandvert` | Expandir seleccion verticalmente |
| `/up create <nombre> [mundo]` | Crear una region desde la seleccion; el mundo opcional debe coincidir |
| `/up delete <region>` | Eliminar una region |
| `/up list [mundo]` | Listar regiones globalmente o por mundo |
| `/up info [region]` | Mostrar limites, propietario y flags activas; sin argumento muestra las regiones donde estas |
| `/up flag <region\|here> <flag> <on\|off>` | Cambiar una flag de region |
| `/up flags` | Ver flags disponibles y sus valores por defecto |
| `/up member add <region> <jugador>` | Agregar miembro |
| `/up member remove <region> <jugador>` | Quitar miembro |
| `/up world list` | Listar mundos cargados por Bukkit |
| `/up world protect <mundo>` | Aplicar proteccion global de construccion |
| `/up world unprotect <mundo>` | Desactivar flags globales del mundo |
| `/up world flags <mundo>` | Ver flags globales activas |
| `/up world flag <mundo> <flag> <on\|off>` | Modificar una flag global |
| `/up bypass` | Activar/desactivar el bypass en esta sesion |
| `/up reload` | Recargar config, regiones y protecciones de mundos |

`ON` significa que esa accion se bloquea en la region o mundo. `OFF` significa que esa flag no bloquea la accion; otras regiones superpuestas o flags globales pueden seguir bloqueandola.

## Flags

`block-break`, `block-place`, `pvp`, `explosions`, `fire`, `mob-grief`, `interact`, `container`, `bucket`, `item-frame`, `fall-damage`, `mob-spawning`, `hunger`, `pistons`, `liquid-flow`.

Ejemplos:

```text
/up flag spawn pvp on
/up flag spawn explosions off
/up flag spawn mob-spawning on
/up world flag world pvp off
```

Para `pvp`, `fall-damage`, `mob-spawning` y `hunger`, la flag se aplica como una restriccion ambiental: no concede excepciones a propietarios/miembros; bypass la omite para jugadores cuando corresponde. Las flags de construccion permiten actuar al propietario y a los miembros de la region, salvo cuando hay una flag global de mundo activa.

## Permisos

| Permiso | Predeterminado | Uso |
|---|---|---|
| `unboundprotection.use` | Todos | Acceso basico (reservado para funciones comunes) |
| `unboundprotection.wand` | OP | Wand y seleccion manual |
| `unboundprotection.region.create` | OP | Crear regiones |
| `unboundprotection.region.delete` | OP | Eliminar regiones |
| `unboundprotection.region.list` | OP | Listar regiones |
| `unboundprotection.region.info` | OP | Ver regiones |
| `unboundprotection.region.flag` | OP | Modificar flags |
| `unboundprotection.region.members` | OP | Gestionar miembros |
| `unboundprotection.world.manage` | OP | Gestionar protecciones globales de mundos |
| `unboundprotection.bypass` | OP | Usar `/up bypass` |
| `unboundprotection.reload` | OP | Recargar archivos |
| `unboundprotection.admin` | OP | Acceso completo; incluye los permisos anteriores |

## Configuracion y mensajes

`config.yml` permite cambiar el material, nombre y lore de la wand, las restricciones por defecto de las regiones y los limites de creacion (`0` significa sin limite). Los mensajes se gestionan desde `util/ChatUtil.java` y sus textos se configuran en `messages.items`.

```yaml
messages:
  enabled: true
  prefix-enabled: true
  prefix: '&8[&aUnbound&bProtection&8] &r'
  items:
    region-created:
      enabled: true
      text: '&aRegion &f%1% &acreada en el mundo &f%2%&a. Bloques: &f%3%&a.'
```

- `messages.enabled`: activa o desactiva todos los mensajes del plugin.
- `messages.prefix-enabled`: permite quitar el prefijo con `false` o mostrarlo con `true`.
- `messages.prefix`: cambia el texto y los colores del prefijo.
- `messages.items.<clave>.text`: personaliza el mensaje. Los marcadores `%1%`, `%2%`, etc. se reemplazan por los valores de esa operación.
- `messages.items.<clave>.enabled`: desactiva un mensaje concreto con `false`, útil para reducir spam. Cada mensaje también admite `prefix: false` para no repetir el prefijo en líneas de listas o detalles.

Los valores nuevos se agregan a configuraciones existentes sin reemplazar los ajustes que ya hayas personalizado. Después de editar el archivo, usa `/up reload`.

## Notas de la primera version

- Es una primera version: se recomienda probarla en una copia del servidor antes de activarla en produccion.
- La proteccion global de mundo y la proteccion de regiones funcionan simultaneamente; una flag global activada prevalece sobre los permisos de propietario o miembro.
- El plugin usa eventos Bukkit. Otros plugins que cancelen eventos con prioridades posteriores o que cambien bloques sin disparar eventos Bukkit pueden requerir compatibilidad adicional.
