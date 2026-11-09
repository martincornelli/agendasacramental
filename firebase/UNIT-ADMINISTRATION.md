# Administración de unidades

Implementación conjunta en Android y web (español e inglés). Incluye la corrección
de fechas de participación del Planificador en el mismo árbol de trabajo.

## Contrato y permisos

Cada documento `unidades/{numero}` conserva su contraseña e información original,
y agrega `administradores` (mapa UID → correo normalizado) e `invitacionesAdmin`
(lista de correos normalizados). Los roles se comprueban mediante el UID del token
Firebase, con proveedor Google y correo verificado. No se confía en el correo
que envíe el cliente para autorizar cambios.

La creación es transaccional: el primer creador queda como administrador; nunca
se sobrescribe una unidad existente. Todos los administradores tienen los mismos
permisos y pueden invitar, cancelar invitaciones, quitar administradores y cambiar
la contraseña. El destinatario debe aceptar la invitación con su propia cuenta.
Se permiten hasta 30 administradores y 30 invitaciones. No se puede quitar al
último administrador, incluso ante operaciones concurrentes.

Las invitaciones se pueden agregar en grupo, separando los correos con comas,
punto y coma o saltos de línea. Se normalizan y deduplican antes de guardar;
un correo inválido, ya administrador o un exceso del límite rechaza el grupo
entero. El destinatario puede rechazar solo su propia invitación sin adquirir
ningún rol ni modificar las invitaciones ajenas. Puede ser invitado nuevamente.

Las unidades anteriores se migran en una transacción cuando ingresa su creador
Google verificado, comparando `creadoPor` sin distinguir mayúsculas. La migración
solo agrega los dos campos de administración. No cambia contraseña ni datos.
Si falta el creador o ya no corresponde, el responsable de la aplicación debe
resolver el caso desde Firebase tras verificar la identidad. El creador no puede
recuperar automáticamente un rol que otro administrador le haya retirado.

## Pantallas

- Acceso: acciones independientes para ingresar, crear y administrar/recuperar.
- Ajustes: Administradores de la unidad, visible a administradores. Las
  invitaciones pendientes se presentan al ingresar con la cuenta invitada;
  también se pueden aceptar desde Administración / recuperar contraseña.
- Unidad ya creada: aviso con Volver al ingreso y Solicitar revisión.
- Solicitud: destinatario `martin.cornelli.lds@gmail.com`, unidad y cuenta Google
  incluidos. Gmail web, gestor de correo y copia manual en web; gestor y copia
  manual en Android. El usuario revisa y envía; la app no envía correos.
- Las invitaciones se guardan en la aplicación; no disparan un correo. La
  interfaz pide al administrador que avise al destinatario.
- Cambiar la contraseña o rechazar una invitación confirma el resultado y cierra
  el diálogo. Abrir el correo también cierra la solicitud y muestra «Mensaje
  preparado», nunca «Correo enviado»: los gestores externos no notifican la
  entrega. Si Android no puede abrir el gestor, conserva el diálogo para copiar.

## Publicación coordinada

`firestore.rules` es el archivo completo preparado para publicar. Se contrastó
con las reglas activas en la consola el 9 de noviembre de 2026 (publicación del
23 de octubre). Solo cambia el bloque de unidades y sus funciones auxiliares.
`unit-admin.rules.fragment` es la política aislada; no debe agregarse junto al
antiguo `allow read, write`, porque las autorizaciones se sumarían.

Antes del despliegue, volver a exportar/comparar las reglas activas para preservar
cualquier cambio concurrente. Publicar las reglas y distribuir los clientes como
un cambio coordinado. Los clientes anteriores podrán seguir leyendo unidades y
trabajando con agendas, pero no crear unidades con el formato anterior. Tampoco
podrán restablecer contraseñas de unidades sin administrador migrado o desde una
cuenta sin ese rol. Las reglas se publicaron y verificaron el 9 de noviembre de 2026: ruleset `7008b759-fb39-4256-afe8-693e7f13b9da`.

La política de acceso a agendas, hermanos, configuración e historial se conserva.
Esto protege la administración y el cambio de contraseña; no introduce membresía
individual para leer/editar agendas ni revocación de sesiones al cambiar la clave.
Esos son cambios separados del modelo de contraseña compartida.

## Verificación

- `node --test tests/unit-admin.test.mjs tests/agenda-web.test.cjs tests/participation-history.test.mjs`
- `gradlew.bat :app:testDebugUnitTest :app:assembleDebug`
- Emulador local: Java 21, Firestore en `127.0.0.1:8188`, proyecto
  `demo-agenda-admin`; desde `firebase/tests`, ejecutar
  `node --test unit-admin-rules.test.mjs`. Prueba el archivo completo de reglas.
- Pruebas de reglas: identidad Google verificada, creación, rechazo de
  sobrescritura, migración, permisos de contraseña, aceptación/cancelación de
  invitaciones, imposibilidad de quitar al último administrador, creación y
  bajas concurrentes.
- Vista web aislada: `node tests/serve-unit-admin-preview.cjs`, luego
  `http://127.0.0.1:4182/?case=access&lang=es`. Casos `invited`, `ordinary`,
  `legacy`, `review`, `exists`; idioma `en` o `es`. Solo usa datos ficticios en
  memoria, no importa Firebase ni envía correos ni instala service workers.
