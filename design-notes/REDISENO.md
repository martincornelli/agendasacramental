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

Pendiente antes de publicar esta rama: en la prueba física, la pulsación Atrás inyectada por ADB no cerró los DropdownMenu. Seleccionar una opción y tocar fuera sí funcionaron. No se ha determinado si ocurre también con la interacción física del usuario o fuera de la actividad QA; no se considera validado ese cierre.
