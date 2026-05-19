# Agenda Sacramental Web

Web app estatica para publicar con GitHub Pages desde la carpeta `docs`.

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
