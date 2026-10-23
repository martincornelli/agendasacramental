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
node --test tests/agenda-web.test.cjs tests/hymn-planner-web.test.mjs tests/participation-history.test.mjs tests/history-workbook.test.mjs
```

## Importación de historial (primera versión web)

En Planificación → Importar / exportar se puede descargar una plantilla vacía o
exportar el historial en **Excel (.xlsx)** o **CSV UTF-8**, con ID, Fecha, Nombre,
Participación y Tema. Se pueden importar `.xlsx`, `.xls`, `.csv` y `.tsv`.
Para Excel se eligen la hoja y la fila de encabezados; las fechas nativas de Excel
se convierten a fechas de calendario, incluidos libros con sistema de fechas 1904.
Las hojas ocultas se omiten. Las hojas con fórmulas o errores requieren una copia
con valores solamente. Los archivos se procesan en un worker local con tiempo
máximo de 15 segundos; no se envían a servicios externos para interpretarlos.

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
revisar. El límite inicial es 2 MB/5000 filas de datos por archivo y 700 KB/5000
participaciones de historial por unidad; superar el límite no guarda parcialmente.
Excel admite hasta 20 hojas, 5001 filas físicas/64 columnas por hoja y 150000
celdas visibles por libro. Los límites se informan antes de guardar.

Los registros participan en las sugerencias de discursos, oraciones y temas de la
web. No se escriben agendas ficticias ni se modifican las fichas de Android. Android
todavía no lee esta nueva fuente: se incorporará en su propia fase.

### Antes de publicar

El 2026-10-22 se revisaron las reglas reales en la consola, sin modificarlas.
Actualmente no habilitan la colección de historial; las colecciones anteriores
solo comprueban autenticación, sin separar unidades. Ver el análisis y la propuesta
de acceso específico por unidad en `../firebase/HISTORY-ACCESS.md`. La propuesta
pasó seis pruebas en el emulador y **no está desplegada**. Antes de publicar se deben
aprobar los usuarios/unidades y configurar sus accesos administrativos. No se
ampliaron permisos de producción ni se modificaron las colecciones de Android.

Se validaron en el emulador el acceso autorizado, la denegación entre unidades,
la persistencia y el rechazo de revisiones desactualizadas. Si falla la lectura,
la interfaz bloquea importación y exportación para no ofrecer un archivo incompleto.
