(() => {
  const en = new URL(location.href).searchParams.get('lang') === 'en';
  const app = document.querySelector('#app');
  app.textContent = en ? 'Loading history…' : 'Cargando historial…';
  import('./import.mjs').catch(() => {
    app.textContent = en
      ? 'History could not be opened. Try again or update the app.'
      : 'No se pudo abrir el historial. Reintentá o actualizá la app.';
    const retry = document.createElement('button');
    retry.textContent = en ? 'Retry' : 'Reintentar';
    retry.onclick = () => location.reload();
    app.append(retry);
  });
})();
