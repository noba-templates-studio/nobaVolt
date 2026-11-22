# Noba Volt

App de presupuestos y facturas para instaladores eléctricos. Un único código en Kotlin
(Compose Multiplatform) para **Android** e **iOS**.

## Estructura

| Carpeta | Contenido |
|---|---|
| `shared/src/commonMain` | Toda la app: pantallas, lógica, guardado de datos y generación de PDF |
| `shared/src/commonMain/composeResources` | Imágenes, fuentes (Inter) y el sonido de la intro |
| `shared/src/androidMain` | Lo propio de Android (`PlataformaAndroid`: archivos, compartir, sonido…) |
| `shared/src/iosMain` | Lo propio de iOS (`PlataformaIos`) y `MainViewController` |
| `androidApp` | La app Android: `MainActivity`, icono, temas y manifiesto |
| `iosApp` | El proyecto de Xcode de la app iOS |

## Ejecutar

- **Android:** abre la carpeta del proyecto en Android Studio, elige la configuración `androidApp` y pulsa ▶.
- **iOS (en un Mac):** en Android Studio con el plugin *Kotlin Multiplatform*, elige `iosApp` y un iPhone del
  simulador, y pulsa ▶. También puedes abrir `iosApp/iosApp.xcodeproj` en Xcode.

Para instalarla en un iPhone real, en Xcode: *Signing & Capabilities* → elige tu equipo (Apple ID).

## Datos

Los datos se guardan en el propio móvil (SharedPreferences en Android, NSUserDefaults en iOS) con el
mismo formato JSON que la versión anterior: las copias de seguridad sirven entre Android e iOS.
