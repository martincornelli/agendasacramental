const CACHE_NAME = "agenda-sacramental-20261022-7";
const APP_SHELL = [
  "./",
  "./index.html",
  "./styles.css?v=20261022-7",
  "./app.js?v=20261022-7",
  "./hymns.js?v=20260710-3",
  "./hymn-planner.mjs",
  "./participation-history.mjs",
  "./history-workbook.mjs",
  "./history-workbook-worker.js",
  "./vendor/xlsx-0.20.3.full.min.js",
  "./manifest.webmanifest",
  "./assets/app-icon.webp",
  "./assets/app-icon-512.webp"
];

self.addEventListener("install", (event) => {
  event.waitUntil(caches.open(CACHE_NAME).then((cache) => cache.addAll(APP_SHELL)).then(() => self.skipWaiting()));
});

self.addEventListener("activate", (event) => {
  event.waitUntil(
    caches.keys()
      .then((keys) => Promise.all(keys.filter((key) => key !== CACHE_NAME).map((key) => caches.delete(key))))
      .then(() => self.clients.claim())
  );
});

self.addEventListener("fetch", (event) => {
  if (event.request.method !== "GET" || !event.request.url.startsWith(self.location.origin)) return;
  if (event.request.mode === "navigate") {
    event.respondWith(fetch(event.request).catch(() => caches.match(new URL("./index.html", self.registration.scope).href)));
    return;
  }
  event.respondWith(
    caches.match(event.request).then((cached) => {
      const network = fetch(event.request).then((response) => {
        if (response.ok) caches.open(CACHE_NAME).then((cache) => cache.put(event.request, response.clone()));
        return response;
      });
      return cached || network;
    })
  );
});
