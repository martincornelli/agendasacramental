# Agenda Sacramental Web App

Aplicación web instalable y responsive, publicada con GitHub Pages desde la carpeta `docs`.

Comparte las mismas colecciones de Firebase que la aplicación Android y ofrece:

- creación, edición, lectura y exportación de agendas;
- planificación de discursos, oraciones y temas;
- aviso preventivo al escribir un nombre equivalente a otro existente, con la opción de usar el nombre sugerido o conservar el escrito;
- planificación manual y automática de himnos;
- biblioteca de himnos con historial, filtros y exclusiones por unidad;
- instalación como PWA en dispositivos compatibles.

## Publicacion

El repositorio incluye un workflow de GitHub Pages en `.github/workflows/pages.yml`. En GitHub, abrir `Settings > Pages` y seleccionar `GitHub Actions` como fuente de despliegue.

## Firebase

La web usa Firebase Authentication con Google y Firestore directo desde el navegador. Para que el login funcione en GitHub Pages, agregar el dominio `martincornelli.github.io` en Firebase Console:

`Authentication > Settings > Authorized domains`

La web usa las mismas colecciones que Android:

- `unidades`
- `agendas`
- `hermanos`
- `configuracion`

La planificación de himnos utiliza los campos de la agenda como única fuente de verdad. Por eso, una selección realizada en la web aparece también en Android y viceversa.

## Pruebas

Desde la raíz del repositorio:

```powershell
node --test tests/agenda-web.test.cjs tests/hymn-planner-web.test.mjs tests/participation-history.test.mjs
```

## Importación de historial (primera versión web)

En Planificación → Importar / exportar se puede descargar una plantilla vacía o
exportar el historial con ID, Fecha, Nombre, Participación y Tema. Abrir con Excel
y guardar como **CSV UTF-8**. La lectura directa de `.xlsx` todavía no está implementada.

El asistente admite coma, punto y coma o tabulador, selección de columnas, fechas
ISO o día/mes/año y mes/día/año, y una fila por participación o por reunión
(un discursante y dos oraciones en columnas separadas). La vista previa distingue
filas nuevas, sin cambios, correcciones, coincidencias y errores. Las variantes de
nombre requieren una decisión; las coincidencias exactas se omiten. Los ID deben
conservarse en filas existentes y quedar vacíos en filas nuevas. Las filas ausentes
del archivo nunca se borran. Las correcciones de participaciones provenientes de
agendas o de fechas manuales se realizan en esas pantallas, no en el importador.

El historial importado se guarda en `historialParticipaciones/{numeroUnidad}`,
con `numeroUnidad`, `revision`, `records`, `actualizadoEn` y `actualizadoPor`.
Cada registro tiene `id`, `date` (AAAA-MM-DD), `name`, `type` y `topic`.
Una transacción verifica la revisión antes de agregar o reemplazar los registros
seleccionados. Un cambio concurrente cancela el guardado completo para volver a
revisar. El límite inicial es 2 MB/5000 filas por archivo y 700 KB de historial
serializado por unidad; superar el límite no guarda parcialmente.

Los registros participan en las sugerencias de discursos, oraciones y temas de la
web. No se escriben agendas ficticias ni se modifican las fichas de Android. Android
todavía no lee esta nueva fuente: se incorporará en su propia fase.

### Antes de publicar

Este repositorio no contiene las reglas desplegadas de Firestore. **No desplegar
esta función sin verificar las reglas reales**: el documento de historial debe
permitir lectura y escritura únicamente a usuarios autorizados de esa unidad,
validar que `numeroUnidad` coincide con el ID de documento y conservar la política
de acceso existente. No alcanza con comprobar que el usuario esté autenticado.
No se incluyen reglas permisivas ni se modifican permisos desde el cliente.

Validar en un entorno de prueba el acceso autorizado, la denegación entre unidades,
la persistencia/reimportación y los conflictos concurrentes. Si falla la lectura,
la interfaz bloquea importación y exportación para no ofrecer un archivo incompleto.
