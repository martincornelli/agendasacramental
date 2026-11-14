# Propuesta de rediseño · Android y web

Rama local: `codex/design-polish`. Base: `codex/release-1.7.0`, commit `470bb29`.
La base conserva Android 1.7.0 (47). La propuesta usa 1.7.1 (48).
No se publicaron ni fusionaron las ramas.

## Dirección visual

La primera iteración solo simplificaba controles. A petición del usuario se amplió a una identidad visual global: azul profundo, turquesa, fondos neutros, tipografía sans serif, superficies redondeadas y jerarquía consistente.

Preferencia posterior del usuario: tipografía contenida. Se redujeron los títulos Android a 20 sp, las tarjetas a 16 sp y la cabecera de inicio a 26 sp, con menos relleno. Se mantienen cuerpos de 14–16 sp, objetivos táctiles y la escala de texto elegida en el sistema. En web se redujeron encabezados, cifras y cabecera de acceso.

Se utilizó `ui-ux-pro-max`. La búsqueda de productividad aportó un estilo plano y una paleta turquesa adecuados para una herramienta multiplataforma. Sus patrones promocionales no aplican a este producto; no se adoptaron. La paleta se ajustó para contraste y para una identidad compartida con Android.

- Android conecta ahora el tema propio a MainActivity: antes utilizaba los colores predeterminados de Material. Tema claro/oscuro, tipografía y formas se aplican a las pantallas Compose y diálogos.
- Inicio nuevo con cabecera azul, accesos visuales y jerarquía entre módulos.
- Agendas e himnos con bloques de fecha, tarjetas y acciones secundarias separadas.
- Planificación con tarjetas más legibles e indicadores de color adaptados al modo oscuro.
- Editor con secciones Apertura, Santa Cena y Cierre, Guardar visible y acciones complementarias en menú.
- Web comparte colores, superficies, navegación, formularios y tarjetas. Barra lateral azul en escritorio y navegación compacta en móvil.
- Se mantienen ES/EN, Organización, estados y checks individuales. Aceptar todas sigue disponible en el resumen compacto.

## Validación

- Android: `:app:assembleQa :app:testDebugUnitTest`, 40 pruebas unitarias sin fallos.
- JavaScript: 30 pruebas de agendas, confirmaciones y traducciones sin fallos.
- Navegador: `agenda-confirmation-ui.cjs` y `web-i18n-ui.cjs`, ES/EN, móvil/escritorio, navegación, guardado, conflictos y traducciones.
- `design-visual-ui.cjs`: 72 vistas en ES/EN, claro/oscuro, 375/768/1280 px y movimiento reducido, sin desbordamiento horizontal ni excepciones JavaScript. Las capturas se guardan en `output/redesign-web`.
- Teléfono físico: inicio ES, agendas ES, himnos ES, planificación EN oscura con fuente 150 %, inicio horizontal y editor ES/EN.
- Guardado real contra emulador local: aceptar 12 asignaciones, guardar, comprobar CONFIRMADA con 12 aceptaciones y reabrir en inglés.
- Capturas del teléfono: `output/confirmation-phone/redesign-*.png`.

## Alcance y revisión

La app instalada es `Agenda · Diseño QA`, paquete separado `com.martincornelli.agendasacramental.design.qa`; sus pantallas de prueba usan datos de un emulador, no producción. El emulador debe estar disponible para navegar con esos datos.

Se corrigieron dos problemas detectados en las pruebas: una actualización diferida del formulario web después de entrar al modo lectura, y el contexto de la actividad QA al simular idioma/tamaño de fuente.

Esta es una propuesta visual para revisión, no un despliegue. No se ha repetido un recorrido completo de autenticación real, notificaciones del sistema ni publicación. El importador Android basado en Views/WebView y los documentos PDF conservan su presentación específica; no se han convertido al nuevo sistema Compose.

El pendiente de Atrás quedó resuelto el 14/11/2026: la regresión automatizada sobre el teléfono Android 16 comprueba que el menú se cierra sin abandonar la pantalla en inicio ES/EN, editor ES y planificación EN. También comprueba escritura completa, conservación de cambios sin guardar y persistencia de las 12 aceptaciones al guardar y reabrir en inglés.

### Compatibilidad con Atrás en Android 16

El dispositivo usa API 36. Se inspeccionó el código fuente de Compose UI 1.6.1: sus popups solo interceptaban `KEYCODE_BACK`. La versión 1.7.8 registra además `OnBackInvokedCallback` con prioridad de ventana superpuesta. Esto coincide con la [migración oficial de navegación Atrás](https://developer.android.com/guide/navigation/custom-back/predictive-back-gesture).

La dependencia efectiva ahora toma el BOM 2025.04.00 del catálogo de versiones, que resuelve Compose UI 1.7.8 y Material3 1.3.2. Los autocompletados editables de nombres e himnos utilizan `MenuAnchorType.PrimaryEditable` para conservar el foco del teclado. Los selectores de solo lectura conservan su comportamiento.

La actividad QA configura sus recursos de idioma en la propia actividad, para que las ventanas emergentes reciban el mismo idioma que la pantalla. Esta configuración no modifica MainActivity ni las preferencias reales del usuario.

Validación final: `:app:assembleQa :app:testDebugUnitTest :app:assembleRelease` pasó, incluidas 40 pruebas unitarias y la optimización R8. `node tests/phone-menu-back.cjs --with-data` pasó; reinicializa exclusivamente los datos del emulador local de QA. Capturas: `output/confirmation-phone/back-*.png`. No se repitieron las pruebas web porque esta corrección no modifica código web. La corrección existe únicamente en `codex/design-polish`; la rama 1.7.0 continúa intacta.
