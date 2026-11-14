# Pestañas y organización en Excel — 1.7.2 (49)

Rama: `codex/swipe-tabs-organization-export`, basada en `codex/design-polish` (`d2b1c3b`). Los cambios de diseño anteriores se conservan. Sin publicación ni fusión con las ramas anteriores.

## Comportamiento

- Android usa `HorizontalPager` en Planificación y Planificador de himnos, manteniendo las pestañas pulsables.
- Web admite gestos táctiles horizontales en ambos planificadores, sin dar vueltas entre la última y la primera pestaña. Conserva desplazamiento vertical, campos editables, gestos del borde del navegador y navegación por teclado.
- Excel y CSV incluyen Organización / Organization, con las seis organizaciones traducidas y valores internos independientes del idioma.
- Al importar, una organización distinta actualiza la ficha. Una celda vacía o una columna ausente conserva la organización actual. Los participantes nuevos con organización reciben una ficha.
- Las correcciones se seleccionan explícitamente en la vista previa. Cambiar solo la organización de una participación procedente de una agenda o ficha no crea una participación importada duplicada.
- Fichas e historial se guardan en la misma transacción. Organizaciones contradictorias y fichas con el mismo nombre se rechazan. Una organización única por fila solo se interpreta en el formato de una fila por participación, no en filas con varias personas.

## Pantalla en blanco

Se reprodujo en el teléfono con 1.7.1. El APK solo contenía los archivos locales del WebView, sin los módulos compartidos ni SheetJS. El inspector mostró respuestas 403 para los módulos ausentes. Se registró explícitamente el directorio de assets compartidos y la dependencia de `preBuild` sobre `syncHistoryAssets`. Se agregó un arranque con mensaje de error y reintento si un módulo no carga.

## Validación

- 40 pruebas unitarias Android.
- `HistoryFeatureTest#bundledWebViewCanExportExcelOffline`: exportación desde los assets instalados en el WebView real.
- `HistoryOrganizationsTest`: transacciones Android contra el emulador local `demo-agenda-history-test`; cambios, conservación de valores vacíos, fichas nuevas, correcciones de participaciones manuales sin duplicar, conflictos y nombres duplicados.
- 24 pruebas Node de historial, Excel y organizaciones.
- `android-history-ui.cjs`: interfaz Android y worker Excel reales con puente simulado, español e inglés.
- `history-import-ui.cjs`: importación web, actualizaciones de fichas y creación de fichas, sin datos reales.
- `swipe-tabs-ui.cjs`: ambos planificadores, español/inglés, 390/820 px, gestos en ambos sentidos, límites, desplazamiento vertical, borde, toque y teclado.
- Teléfono Xiaomi Android 16: deslizamientos en ambos planificadores y exportación mediante el selector del sistema. El Excel guardado tenía 41 participaciones y la columna Organización con valores.

Para verificar que una compilación incluye exactamente los assets actuales:

```text
python tests/check-history-assets.py app/build/outputs/apk/debug/app-debug.apk app/build/outputs/apk/release/app-release-unsigned.apk app/build/outputs/bundle/release/app-release.aab
```

La prueba de organizaciones requiere Firestore Emulator en el puerto 8191, `adb reverse tcp:8191 tcp:8191` y el argumento de instrumentación `-e historyEmulator true`. No accede al proyecto de producción. El teléfono conserva la app normal y sus datos; no se probó la importación escribiendo sobre datos reales.
