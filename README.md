# InterCommerce App

MVP de comercio electrónico **nativo para Android**, escrito en Kotlin con Jetpack Compose y diseñado **Offline-First**: el catálogo y el carrito se leen siempre desde SQLite (Room), y la red solo escribe en esa base. La app sigue siendo útil sin conexión y se recupera sola cuando la conexión vuelve.

Consume exclusivamente la API pública de [DummyJSON](https://dummyjson.com/docs/products).

---

## Contenido

1. [Descripción](#1-descripción)
2. [Stack tecnológico](#2-stack-tecnológico)
3. [Arquitectura y paquetes](#3-arquitectura-y-organización-de-paquetes)
4. [Por qué Room/SQLite](#4-justificación-de-roomsqlite)
5. [Estrategia Offline-First](#5-estrategia-offline-first)
6. [Manejo de errores y recuperación](#6-manejo-de-errores-y-recuperación)
7. [Persistencia del carrito](#7-persistencia-del-carrito)
8. [Casos de uso](#8-casos-de-uso)
9. [Preguntas de profundidad técnica](#9-preguntas-de-profundidad-técnica)
10. [Instalación](#10-instalación)
11. [Ejecución](#11-ejecución)
12. [Pruebas](#12-pruebas)
13. [Supuestos y limitaciones](#13-supuestos-técnicos-y-limitaciones-conocidas)

---

## 1. Descripción

| Módulo | Qué hace |
|---|---|
| **A. Catálogo** | Grid adaptativo (`LazyVerticalGrid`) con scroll infinito (Paging 3 + `RemoteMediator`), skeletons con shimmer, búsqueda con debounce, pull-to-refresh y estados de carga, contenido, vacío y error. Los productos se guardan en Room y la UI los observa desde ahí. |
| **B. Detalle** | Navegación type-safe (`@Serializable` routes), galería con indicador, precio con descuento, stock, garantía, envío, devoluciones, etiquetas y reseñas. Botón "Agregar al carrito" con snackbar, contador animado y **vibración háptica nativa** (`HapticFeedbackConstants.CONFIRM` / `REJECT`). |
| **C. Carrito** | Incrementar, disminuir y eliminar productos; subtotal, descuentos, IVA y total calculados en Domain. El estado es reactivo (`StateFlow`) y se persiste en Room. Sobrevive a cierre, force-stop, muerte del proceso y reinicio del dispositivo. |

La UI está en español, con modo claro y oscuro, y pasa a dos paneles en pantallas de 600 dp o más (teléfono horizontal o tablet).

---

## 2. Stack tecnológico

| Componente | Tecnología | Versión |
|---|---|---|
| Lenguaje | Kotlin | 2.2.21 |
| Build | AGP / Gradle | 8.13.1 / 8.14.3 |
| UI | Jetpack Compose + Material 3 | BOM 2025.12.00 |
| Navegación | Navigation Compose type-safe | 2.9.6 |
| Arquitectura | Clean Architecture + MVVM, package-by-feature | — |
| DI | Hilt (KSP) | 2.57.2 |
| Red | Retrofit + OkHttp (interceptores de cabeceras y logs) | 3.0.0 / 4.12.0 |
| Serialización | Kotlinx Serialization | 1.9.0 |
| Persistencia | Room (SQLite) + room-paging | 2.8.4 |
| Paginación | Paging 3 con `RemoteMediator` | 3.3.6 |
| Imágenes | Coil 3 (caché en memoria y en disco) | 3.3.0 |
| Concurrencia | Coroutines, `StateFlow`, `Channel` | — |
| Tests | JUnit 4, MockK, Turbine, Robolectric, MockWebServer, paging-testing | — |

- **SDK:** `minSdk 26`, `targetSdk`/`compileSdk 36`.
- **Por qué este toolchain:** es estable y de finales de 2025, para que el proyecto abra en versiones de Android Studio de hasta un año de antigüedad. AGP 9.x obligaría al revisor a usar la versión más reciente de Android Studio.

---

## 3. Arquitectura y organización de paquetes

```
Presentation (Compose + ViewModel)  ──usa──▶  Domain (Kotlin puro)  ◀──implementa──  Data (Room, Retrofit, Coil)
```

- **Domain** no depende de ningún framework. Contiene entidades inmutables, interfaces de repositorio, casos de uso y reglas de precio. **`DomainPurityTest` hace fallar el build** si algún archivo de un paquete `domain` importa algo distinto de Kotlin, coroutines, `java.math`, `javax.inject` o el tipo `PagingData`. `PagingData` viene de `paging-common`, que es Kotlin puro.
- **Presentation** solo conoce casos de uso. Ningún ViewModel importa DAOs, Retrofit ni Room; se verificó con búsqueda en el código y lo refuerzan las interfaces.
- **Data** implementa los contratos de Domain. La única pieza que decide qué implementación respalda cada contrato es `core/di/BindingsModule.kt`.

```
com.intercommerce
├── core
│   ├── domain/            AppError, Money (centavos), NetworkMonitor (interfaz)
│   ├── data/
│   │   ├── network/       NetworkModule (Retrofit/OkHttp/Json), ApiErrors (toAppError, safeApiCall)
│   │   ├── database/      AppDatabase, Converters, DatabaseModule
│   │   └── connectivity/  AndroidNetworkMonitor
│   ├── di/                BindingsModule (@Binds), DispatchersModule (@IoDispatcher)
│   └── presentation/
│       ├── designsystem/  Theme, Dimens, Components (shimmer, imagen, precio…), LocalIsOnline
│       ├── navigation/    Rutas @Serializable + AppNavHost
│       └── Formatting.kt  Formato de moneda, mensajes de error
├── catalog
│   ├── domain/            Product, ProductRepository, GetPaged/Observe/RefreshProduct UseCases
│   ├── data/              CatalogRemoteMediator, ProductRepositoryImpl, ThumbnailPrefetcher, mappers,
│   │                      local/ (CatalogDao, entidades), remote/ (ProductApi, DTOs)
│   └── presentation/      CatalogViewModel, CatalogScreen
├── productdetail/
│   └── presentation/      ProductDetailViewModel, ProductDetailScreen   (reutiliza el dominio de catalog)
└── cart
    ├── domain/            CartItem, Cart/CartLine, CartRepository, CalculateCartTotals + casos de uso
    ├── data/              CartRepositoryImpl, mappers, local/ (CartDao, CartItemEntity)
    └── presentation/      CartViewModel, CartScreen
```

- **Un solo módulo Gradle.** Para 3 pantallas, partir en varios módulos agrega configuración sin aportar valor. La pureza de Domain, que es lo que la modularización garantizaría, la verifica el test de arquitectura.
- **`productdetail` no tiene capa de datos propia**, porque el detalle es el mismo producto del catálogo leído por id. Duplicar el repositorio solo crearía dos fuentes de verdad.

---

## 4. Justificación de Room/SQLite

| Necesidad | Por qué Room |
|---|---|
| **Transacciones** al sincronizar páginas | `@Transaction` en el DAO: los productos, su posición en el listado y la clave de la siguiente página se escriben de forma atómica. |
| **Datos observables** | Room devuelve un `Flow` y un `PagingSource` que se invalidan solos tras cada escritura; esa es la base de Offline-First. |
| **Integración con Paging 3** | `room-paging` provee el `PagingSource`, y `RemoteMediator` es el patrón oficial para paginar desde la red hacia la base. |
| **Consultas relacionales** | Un join `catalog_entries ⋈ products` permite que el catálogo y cada búsqueda tengan su propio orden sobre los mismos productos. |
| **Actualizaciones atómicas** | `UPDATE … SET quantity = quantity + :delta WHERE … BETWEEN :min AND :max` evita perder incrementos cuando el usuario toca dos veces rápido. |
| **Verificación en compilación y migraciones** | Las consultas se validan al compilar y el esquema se exporta a `app/schemas/`. |
| **Herramientas** | Database Inspector de Android Studio. |

Se descartaron otras opciones:

- **SharedPreferences o DataStore:** el enunciado los prohíbe, y además no ofrecen consultas, transacciones ni paginación.
- **ObjectBox:** también es válido, pero no tiene `PagingSource` oficial para Paging 3 ni consultas SQL ad hoc.

---

## 5. Estrategia Offline-First

**La UI nunca recibe datos directamente de la red.** El flujo es este:

```
                ┌──────────── UI (Compose) ◀── observa ──┐
                │                                        │
 usuario ─▶ ViewModel ─▶ UseCase ─▶ Pager ──────▶ PagingSource (Room)  ◀── invalida ──┐
                                      │                                            │
                                      └──▶ RemoteMediator ─▶ Retrofit ─▶ DummyJSON  │
                                                  │                                 │
                                                  └── @Transaction savePage() ──────┘
```

1. Al abrir el catálogo, Room emite **de inmediato** lo que ya está guardado.
2. Al mismo tiempo, `RemoteMediator` pide la primera página (`InitializeAction.LAUNCH_INITIAL_REFRESH`).
3. Si la respuesta llega bien, `savePage()` la escribe en una transacción y Room notifica a la UI.
4. Si falla, **no se toca nada de lo guardado**: la UI sigue mostrando los datos y un aviso. El aviso dice "Sin conexión" o "No pudimos actualizar", según la causa real.
5. Si no hay nada guardado, se muestra un error recuperable con botón **Reintentar**.
6. Cuando la conexión vuelve, `AndroidNetworkMonitor` lo detecta y la UI llama a `retry()`. Los mismos reintentos aplican al detalle y a las imágenes que fallaron.

### Mecanismos para evitar conflictos e inconsistencias

| Riesgo | Mecanismo |
|---|---|
| **El catálogo sobrescribe el carrito** | Son tablas separadas. `cart_items` guarda una *copia* (snapshot) del producto y **no tiene llave foránea** hacia `products`. La sincronización solo escribe en `products`, `catalog_entries` y `catalog_remote_keys`. Lo prueba `CartDaoTest › catalog sync never touches cart rows`. |
| **Estados a medias** | `savePage()` es `@Transaction`: un observador nunca ve productos sin posición ni una clave que apunte más allá de lo guardado. |
| **Una búsqueda desordena el catálogo** | El orden se guarda por consulta en `catalog_entries(query, productId, position)`; `query = ""` es el catálogo. Lo prueba `CatalogDaoTest`. |
| **Solicitudes duplicadas** | Paging 3 ejecuta como máximo una carga por dirección. Además, la búsqueda tiene debounce de 350 ms y `distinctUntilChanged` sobre la consulta normalizada ("Phone " = "phone"), y `flatMapLatest` cancela la búsqueda anterior. |
| **Datos guardados de un producto que ya no se lista** | Un refresh reemplaza el *orden* del listado, pero los productos se actualizan (upsert) y **nunca se borran**: el detalle sigue disponible offline. |
| **Búsqueda sin conexión** | Si una búsqueda nunca se hizo con conexión, se resuelve con `LIKE` sobre los productos guardados (título, descripción, marca y categoría). Ese listado se marca como "fin alcanzado", y el siguiente refresh exitoso lo reemplaza por el resultado del servidor. Solo se activa ante errores de *conectividad*: ante un 500 no se inventan resultados. |
| **Imágenes offline** | Coil usa caché en memoria (25 %) y en disco (100 MB). Además, `ThumbnailPrefetcher` descarga a disco las miniaturas de cada página guardada, aunque nunca se hayan visto en pantalla. |
| **Muerte del proceso** | El catálogo y el carrito están en Room. La búsqueda escrita está en `SavedStateHandle`, y el back stack y los argumentos de navegación se restauran solos. |

No se implementó sincronización bidireccional porque DummyJSON no persiste carritos. Las operaciones del carrito son 100 % locales y no dependen de la red, así que no puede haber conflictos con el servidor. La [Pregunta 2](#pregunta-2--estrategias-offline) describe cómo se extendería.

---

## 6. Manejo de errores y recuperación

Todos los fallos de infraestructura se traducen **en un solo lugar** (`core/data/network/ApiErrors.kt`) a un tipo controlado de Domain:

| Excepción de infraestructura | `AppError` | Qué ve el usuario |
|---|---|---|
| `SocketTimeoutException` | `Timeout` | "El servidor tardó demasiado en responder." |
| `IOException` (sin DNS, conexión cortada a mitad de la respuesta) | `NoConnection` | "Revisa tu conexión a internet…" |
| HTTP 404 | `NotFound` | "Este producto ya no existe." (sin botón de reintento) |
| Otro HTTP (500, 503…) | `Http(code)` | "El servidor no está disponible…" |
| `SerializationException` | `Serialization` | "Recibimos una respuesta inesperada…" |
| Otra | `Unknown` | "Ocurrió un error inesperado." |

- **`safeApiCall`** aplica esa traducción y **nunca se traga una `CancellationException`**, así que cancelar una corrutina sigue funcionando.
- **`AppError` hereda de `Exception`.** Así, el mismo tipo viaja por `LoadState.Error` de Paging y por las funciones `suspend` del detalle.
- **Deserialización tolerante:** `ignoreUnknownKeys`, `coerceInputValues` y `explicitNulls = false`, con valores por defecto en todos los campos salvo `id`, `title` y `price`. En la API real, 92 de los 194 productos no traen `brand`.
- **Timeouts de OkHttp:** conexión 10 s, lectura 15 s y llamada completa 25 s, definidos como constantes.
- **Hilos:** la llamada de red y el mapeo corren en `@IoDispatcher` (`Dispatchers.IO`, inyectado para poder reemplazarlo en tests). Las funciones `suspend` de Room ya usan su propio executor.
- **Cobertura:** `CatalogRemoteMediatorTest` prueba con MockWebServer cada caso de la tabla y comprueba que la caché sobrevive a cada uno.

---

## 7. Persistencia del carrito

- **Dónde se guarda:** en la tabla `cart_items` de Room (`productId` es la llave primaria). Cada fila tiene la cantidad, una copia del título, la imagen, el precio en centavos y el descuento, el stock al momento de agregar (`maxQuantity`) y la fecha en que se agregó.
- **Durabilidad:** cada operación es una escritura de SQLite que se confirma antes de terminar. No hay copia en memoria que se pueda perder: **el ViewModel no guarda el carrito**, lo observa (`ObserveCartUseCase` → `StateFlow`). Lo que se muestra es siempre lo persistido.
- **Concurrencia:**
  - `addOrIncrement` es una `@Transaction`.
  - `changeQuantity` es relativo y acotado en una sola sentencia (`quantity + delta BETWEEN min AND max`); los límites los decide Domain (1 y el stock).
  - Dos toques rápidos no pueden leer el mismo valor viejo y perder un incremento.
- **Sin migración destructiva:** `fallbackToDestructiveMigration` no está activado, porque borraría el carrito. El esquema se exporta para escribir migraciones explícitas.
- **Verificado en el emulador:** sobrevive al cierre normal, a `am force-stop`, a `am kill` en segundo plano (memoria baja; la app vuelve al detalle con su argumento) y a `adb reboot`, abriendo la app sin red.

---

## 8. Casos de uso

| Caso de uso | Responsabilidad |
|---|---|
| `GetPagedProductsUseCase` | Normaliza la búsqueda (`trim` + minúsculas) para que consultas equivalentes compartan un listado guardado, y devuelve `Flow<PagingData<Product>>`. |
| `ObserveProductUseCase` | Observa un producto guardado; emite `null` si nunca se guardó. |
| `RefreshProductUseCase` | Trae el detalle de la API y lo guarda. Lanza `AppError` si falla. |
| `AddToCartUseCase` | Agrega una unidad sin superar el stock. Devuelve `ADDED`, `LIMIT_REACHED` u `OUT_OF_STOCK`; ese resultado alimenta el snackbar y la vibración. |
| `ChangeCartItemQuantityUseCase` | Cambia la cantidad en ±n dentro de [1, stock]. Bajar de 1 no elimina: eliminar es una acción explícita. |
| `RemoveFromCartUseCase` | Elimina un producto del carrito. |
| `ObserveCartUseCase` | Combina lo persistido con `CalculateCartTotalsUseCase` y emite un `Cart` ya calculado. |
| `CalculateCartTotalsUseCase` | Reglas de precio, explicadas abajo. |

### Reglas de precio

1. **Descuento:** se calcula por unidad (`discountPercentage` sobre el precio unitario, redondeado al centavo) y se multiplica por la cantidad. Así, la línea siempre coincide con el precio unitario que el usuario vio en el catálogo: 2 × $8.94 = $17.88.
2. **IVA del 19 %:** se aplica sobre el subtotal ya con descuento, con un solo redondeo.
3. **Dinero:** se maneja en centavos (`Money`, un value class sobre `Long`) con `BigDecimal` y `HALF_UP`. Nunca se usa `Double`, que acumula errores al sumar líneas.

---

## 9. Preguntas de profundidad técnica

> Para cada pregunta se separa lo **implementado** de lo **propuesto como evolución**.

### Pregunta 1 — Arquitectura y resiliencia

*¿Cómo permiten la inversión de dependencias y las interfaces de Domain cambiar Room por ObjectBox sin tocar la UI de Compose?*

**Lo implementado:**

- **Las dependencias apuntan hacia Domain.** Domain define los contratos (`ProductRepository`, `CartRepository` y `NetworkMonitor`) sin saber quién los implementa. Data depende de Domain, nunca al revés.
- **Ningún componente de Presentation conoce Data.** Las pantallas hablan con los ViewModels, los ViewModels con los casos de uso, y los casos de uso con las interfaces. Las firmas solo usan tipos de Domain (`Product`, `CartItem`, `Cart`, `AppError`) más `PagingData`, que es Kotlin puro.
- **Un único punto de composición:** `core/di/BindingsModule.kt` enlaza cada interfaz con su implementación con `@Binds`.
- **Está verificado:**
  - `DomainPurityTest` impide que Domain importe Room u otro framework.
  - Una búsqueda en el código confirma que ningún archivo de `presentation` importa `*.data.*`.
  - Los tests de ViewModels usan MockK sobre los casos de uso, sin Room.

**Lo que cambiaría en una migración a ObjectBox (propuesta):**

| Se modifica | No se modifica |
|---|---|
| `catalog/data/local/*` y `cart/data/local/*`: entidades `@Entity` de ObjectBox y acceso con `Box<T>` | Todo `*/presentation/*`: pantallas, componentes y ViewModels |
| `ProductRepositoryImpl` y `CartRepositoryImpl`, o implementaciones nuevas | Todo `*/domain/*`: modelos, interfaces y casos de uso, incluidas las reglas de precio |
| `DatabaseModule` (crear el `BoxStore`) y las 2 líneas de `BindingsModule` | Rutas de navegación y sistema de diseño |

Hay dos puntos que exigen cuidado:

- **Paginación:** ObjectBox no tiene `PagingSource` oficial para Paging 3. Habría que escribir uno sobre un `Query` de ObjectBox (con `offset`/`limit` e invalidación mediante `DataObserver`). El contrato `Flow<PagingData<Product>>` no cambia.
- **Atomicidad:** la sincronización transaccional y el incremento acotado del carrito se reimplementarían con `boxStore.runInTx { … }`.

Los tests de Domain y de Presentation seguirían pasando sin cambios. Los de Data (DAO, mediator) se reescribirían para la nueva implementación.

### Pregunta 2 — Estrategias offline

*¿Cómo evitar que la sincronización del catálogo sobrescriba lo que el usuario agregó al carrito sin conexión?*

**Lo implementado:**

1. **Datos de referencia y datos transaccionales en tablas distintas.** El catálogo (`products`, `catalog_entries`, `catalog_remote_keys`) es una caché reemplazable. El carrito (`cart_items`) es dato del usuario.
2. **Sin llave foránea, con snapshot.** El carrito no referencia `products`, así que un refresh, un upsert o una eventual limpieza del catálogo no puede borrar ni modificar líneas del carrito por cascada. Lo prueba `CartDaoTest › catalog sync never touches cart rows`: el catálogo cambia el precio a $99 y el stock a 0, y el carrito conserva $9.99 y 3 unidades.
3. **La sincronización tiene un alcance cerrado.** `CatalogDao.savePage()` es la única escritura que hace la red, y es una `@Transaction` que solo toca las tablas del catálogo.
4. **El carrito no pasa por la red.** Todas sus operaciones son locales y no esperan conectividad.
5. **Sin carreras locales.** Las cantidades cambian de forma relativa y acotada en una sola sentencia SQL, así que dos escrituras concurrentes no se pisan.

**Propuesta si existiera un carrito en el servidor:**

- **Bandeja de salida (outbox):** cada operación local se registraría en una tabla `pending_cart_ops(id, productId, delta, createdAt, idempotencyKey)` dentro de la misma transacción que modifica `cart_items`.
- **Envío en orden con WorkManager**, con restricción `NetworkType.CONNECTED` y backoff exponencial. La clave de idempotencia evita que un reintento duplique una operación.
- **Resolución de conflictos por operación, no por estado:** el servidor aplica *deltas* ("+1") en lugar de reemplazar el carrito. Si se requiere estado final, se usaría última escritura gana por línea con un `version` o `updatedAt` del servidor.
- **Precio de referencia:** el carrito conserva el precio de cuando se agregó el producto. Al pagar se compararía con el precio vigente y se informaría al usuario si cambió, en lugar de cambiarlo sin aviso.

### Pregunta 3 — Seguridad y profiling

**Lo implementado:** ninguno de los datos es sensible (productos públicos y un carrito sin datos personales), así que **la base de datos no está cifrada**. Tampoco se implementaron pinning de certificados ni ofuscación con R8 (`isMinifyEnabled = false`). Lo que sigue es la estrategia propuesta.

**Cifrado de la base (propuesta):**

- **SQLCipher for Android** (`net.zetetic:sqlcipher-android`), conectado a Room con `openHelperFactory(SupportOpenHelperFactory(passphrase))`. Cifra con AES-256 todo el archivo SQLite, incluidos índices y el journal WAL, sin cambiar los DAOs.
- **Cifrado por campo** con Tink (AEAD AES-GCM) como alternativa, si solo unas pocas columnas son sensibles y se quiere conservar la capacidad de búsqueda en las demás.
- **Endurecimiento complementario:**
  - `android:allowBackup="false"` o `dataExtractionRules` que excluyan la base. Hoy `allowBackup` está en `true`.
  - R8 activado en release.
  - `FLAG_SECURE` en pantallas sensibles.
  - Pinning de certificados en OkHttp (`CertificatePinner`).

**Protección de la clave (propuesta):**

1. Generar una passphrase aleatoria de 256 bits con `SecureRandom` en el primer arranque. Nunca hardcodearla ni derivarla del dispositivo.
2. Cifrarla con una clave AES-GCM del **Android Keystore**. Esa clave no es extraíble y queda respaldada por hardware (TEE, o StrongBox con `setIsStrongBoxBacked(true)` cuando existe).
3. Guardar solo la passphrase *envuelta* (texto cifrado + IV) en un archivo privado de la app, y desenvolverla en memoria al abrir la base.
4. Para datos muy sensibles, exigir autenticación del usuario para usar la clave (`setUserAuthenticationRequired` + `BiometricPrompt`).

`EncryptedSharedPreferences` (`security-crypto`) no se recomienda porque la biblioteca está deprecada; se usaría el Keystore directamente o Tink.

**Herramientas para inspeccionar SQLite y su rendimiento:**

| Herramienta | Uso |
|---|---|
| **App Inspection → Database Inspector** | Ver las tablas de `intercommerce.db` en vivo mientras se usa la app; con *Live updates* se ve cómo `savePage()` y el carrito cambian las filas. Permite ejecutar SQL ad hoc, revisar el historial de consultas de los DAOs y correr `EXPLAIN QUERY PLAN` para confirmar que el join del catálogo usa el índice `(query, position)`. Si se cifrara, inspeccionar una base SQLCipher requeriría la build sin cifrar o abrirla con la clave. |
| **Profiler → CPU (System Trace con Perfetto)** | Ver en qué hilo corre cada consulta (el executor de Room, no `main`), cuánto duran las transacciones y si una escritura coincide con frames perdidos (*jank*). |
| **Profiler → CPU (Java/Kotlin Method Trace)** | Medir el costo de mapear DTO → entidad → dominio y de los `TypeConverter` JSON. |
| **Profiler → Memory** | Detectar acumulación de objetos (páginas de Paging, bitmaps de Coil) y fugas. |
| **App Inspection → Network Inspector** | Comprobar el tamaño y la duración de cada página de DummyJSON y que no haya solicitudes duplicadas. |
| **Logs de SQLite** | `RoomDatabase.Builder.setQueryCallback(...)` en debug, o `adb shell setprop log.tag.SQLiteStatements VERBOSE` y `log.tag.SQLiteTime VERBOSE`, para registrar cada sentencia y su duración. |

---

## 10. Instalación

### Opción A: instalar la APK entregada

La APK es `InterCommerce-1.0.0-debug.apk`, de debug y firmada con la clave debug de Android. Se entrega adjunta, no dentro del repositorio.

- En la copia local queda en `dist/`, que está excluida de git.
- Si se compila desde el código, Gradle la genera en `app/build/outputs/apk/debug/app-debug.apk`.

**Con adb:**

1. En el teléfono, activar *Opciones de desarrollador* → *Depuración USB*.
2. Conectar el teléfono y ejecutar:

```bash
adb install -r dist/InterCommerce-1.0.0-debug.apk
```

**Sin computador:** copiar el archivo `.apk` al teléfono, abrirlo y permitir "Instalar apps desconocidas" para el gestor de archivos. Requiere Android 8.0 (API 26) o superior.

### Opción B: compilar

**Requisitos:**

- JDK 17 o superior. El JBR de Android Studio sirve.
- Android SDK 36.
- Android Studio Narwhal 3 Feature Drop (2025.1.3) o posterior, la primera versión compatible con AGP 8.13.

**Pasos:**

1. Abrir la carpeta en Android Studio y sincronizar Gradle.
2. Si `local.properties` no existe, Android Studio lo crea con `sdk.dir`.

**Desde la terminal:**

```bash
./gradlew assembleDebug
```

---

## 11. Ejecución

1. En Android Studio, elegir la configuración **app** y un dispositivo o emulador, y pulsar **Run**.
2. También se puede abrir la app ya instalada desde el launcher (ícono *InterCommerce*).

### Prueba sugerida de corte de red en caliente

1. Con conexión, abrir el catálogo y hacer scroll (cada página guarda productos y miniaturas).
2. Agregar productos al carrito desde el detalle.
3. Activar el **modo avión**. Aparece el aviso "Sin conexión. Mostrando productos guardados.", y el catálogo, el detalle de los productos vistos, las imágenes y el carrito siguen funcionando.
4. Buscar algo ya guardado (por ejemplo `red`): se resuelve contra la caché local.
5. Modificar el carrito. Luego **forzar la detención** de la app o **reiniciar** el teléfono y volver a abrirla: el carrito sigue intacto.
6. Desactivar el modo avión. El aviso desaparece, el listado se actualiza solo y el scroll infinito continúa.

---

## 12. Pruebas

Suite completa (debug y release):

```bash
./gradlew test
```

Solo la variante debug, más rápida:

```bash
./gradlew testDebugUnitTest
```

Lint (`warningsAsErrors` activado):

```bash
./gradlew lintDebug
```

Reportes de estabilidad del compilador de Compose:

```bash
./gradlew assembleRelease -PcomposeReports
```

**54 tests, todos en la JVM.** Room y Android se emulan con Robolectric, así que no hace falta un dispositivo.

| Capa | Suite | Qué prueba |
|---|---|---|
| Arquitectura | `DomainPurityTest` | Domain sin dependencias de frameworks. |
| Domain | `CalculateCartTotalsUseCaseTest`, `CartUseCasesTest`, `CatalogUseCasesTest`, `MoneyTest` | Totales, impuestos, descuentos y redondeo; límites de stock; normalización de la búsqueda; propagación de errores. |
| Data | `CatalogRemoteMediatorTest` (MockWebServer + Room) | Paginación `limit`/`skip`, endpoint de búsqueda, 500, JSON malformado, timeout, corte a mitad de respuesta, respaldo local offline y que la caché se preserva en cada fallo. |
| Data | `ProductRepositoryImplTest`, `CatalogDaoTest`, `CartDaoTest`, `ProductMappersTest`, `ApiErrorsTest` | Lectura local cuando falla la API, transacciones, aislamiento entre carrito y catálogo, mapeo DTO → entidad → dominio, traducción de excepciones. |
| Presentation | `CatalogViewModelTest`, `ProductDetailViewModelTest` | Carga, éxito, error con y sin caché, reintento, debounce, `SavedStateHandle`, eventos de agregar al carrito. |
| Integración | `CartViewModelTest` (ViewModel + Room real, sin mocks) | Actualizaciones del carrito y que un ViewModel nuevo lee el estado persistido. |

**Los tests verifican comportamiento real**, no solo que se llamó a un mock. Se comprobó introduciendo fallos a propósito, y los tres hicieron fallar los tests correspondientes:

- Quitar el debounce.
- Activar el respaldo local también ante errores 500.
- Hacer que el refresh no reemplace el listado.

**Robolectric con SDK 35:** está fijado en `app/src/test/resources/robolectric.properties`, porque el sandbox del SDK 36 exige JDK 21 y así `./gradlew test` funciona con JDK 17.

---

## 13. Supuestos técnicos y limitaciones conocidas

**Supuestos de negocio:**

- **Precios:** los de DummyJSON se interpretan en **USD**. Se aplica **IVA del 19 %** (tarifa general en Colombia) sobre el subtotal con descuento.
- **Descuento:** el `discountPercentage` de cada producto se toma como descuento vigente.
- **Precio congelado:** el carrito guarda el precio de cuando se agregó el producto y no lo recalcula si el catálogo cambia; sería tarea del checkout (ver Pregunta 2).
- **Tope de cantidad:** es el stock en el momento de agregar el producto.
- **`minimumOrderQuantity`:** se ignora; el carrito arranca en 1 unidad. En DummyJSON, por ejemplo, el producto 1 exige 48.
- **Sin checkout ni login:** el enunciado no los pide.

**Limitaciones técnicas:**

- **Búsqueda offline aproximada:** solo encuentra productos ya guardados y los ordena por título, no por la relevancia del servidor.
- **Scroll tras reabrir:** al reabrir la app con conexión, el refresh reemplaza el listado por la primera página y el scroll vuelve arriba; las siguientes páginas se cargan al bajar. Es el comportamiento estándar de `RemoteMediator`.
- **Caché del catálogo sin límite:** no se desaloja. DummyJSON tiene 194 productos, menos de 1 MB.
- **Imágenes de la galería:** solo quedan offline si se vieron. Las miniaturas sí se precargan.
- **Portal cautivo:** `NetworkMonitor` considera "en línea" solo las redes que Android marca como `VALIDATED`, así que un portal cautivo cuenta como sin conexión.
- **Sin tests instrumentados de Compose UI:** el flujo de UI se verificó manualmente en el emulador (corte de red, muerte del proceso, rotación, modo oscuro, reinicio).
- **Seguridad pendiente:** no hay cifrado, pinning ni R8 (ver Pregunta 3).
- **Toolchain:** versiones fijadas a finales de 2025; las advertencias de lint sobre versiones nuevas están desactivadas a propósito en `app/build.gradle.kts`.
