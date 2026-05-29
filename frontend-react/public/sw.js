// Minimal offline-shell service worker for Football 501 PWA.
// Caches the app shell (HTML, CSS, JS, fonts) on install so the app
// loads instantly on repeat visits, even on slow/flaky networks.

const SHELL_CACHE = "f501-shell-v1";

const SHELL_FILES = [
  "/",
  "/manifest.json",
];

self.addEventListener("install", (event) => {
  event.waitUntil(
    caches.open(SHELL_CACHE).then((cache) => cache.addAll(SHELL_FILES))
  );
  self.skipWaiting();
});

self.addEventListener("activate", (event) => {
  event.waitUntil(
    caches.keys().then((keys) =>
      Promise.all(
        keys.filter((key) => key !== SHELL_CACHE).map((key) => caches.delete(key))
      )
    )
  );
  self.clients.claim();
});

// Network-first for navigations, cache fallback for offline
self.addEventListener("fetch", (event) => {
  if (event.request.mode === "navigate") {
    event.respondWith(
      fetch(event.request).catch(() =>
        caches.match("/").then((r) => r || new Response("Offline", { status: 503 }))
      )
    );
  }
});
