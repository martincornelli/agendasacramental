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
node --test tests/agenda-web.test.cjs tests/hymn-planner-web.test.mjs
```
