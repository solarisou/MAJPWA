const CACHE_NAME = 'ecocook-cache-v1';
const urlsToCache = [
  '/',
  '/css/style.css',
  '/manifest.json',
  'https://cdn.jsdelivr.net/npm/bulma@0.9.4/css/bulma.min.css',
  'https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css'
];

// Installation du Service Worker
self.addEventListener('install', event => {
  event.waitUntil(
    caches.open(CACHE_NAME)
      .then(cache => {
        console.log('Cache ouvert');
        return cache.addAll(urlsToCache);
      })
      .catch(err => {
        console.log('Erreur lors du cache:', err);
      })
  );
  self.skipWaiting();
});

// Activation et nettoyage des anciens caches
self.addEventListener('activate', event => {
  event.waitUntil(
    caches.keys().then(cacheNames => {
      return Promise.all(
        cacheNames.map(cacheName => {
          if (cacheName !== CACHE_NAME) {
            console.log('Suppression ancien cache:', cacheName);
            return caches.delete(cacheName);
          }
        })
      );
    })
  );
  self.clients.claim();
});

// Strategie Network First avec fallback cache
self.addEventListener('fetch', event => {
  // Ignorer les requetes non-GET
  if (event.request.method !== 'GET') {
    return;
  }

  // Ignorer les requetes API (toujours reseau)
  if (event.request.url.includes('/api/')) {
    return;
  }

  event.respondWith(
    fetch(event.request)
      .then(response => {
        // Cloner la reponse pour le cache
        if (response.status === 200) {
          const responseClone = response.clone();
          caches.open(CACHE_NAME)
            .then(cache => {
              cache.put(event.request, responseClone);
            });
        }
        return response;
      })
      .catch(() => {
        // Fallback sur le cache si hors ligne
        return caches.match(event.request)
          .then(response => {
            if (response) {
              return response;
            }
            // Page offline par defaut
            if (event.request.mode === 'navigate') {
              return caches.match('/');
            }
          });
      })
  );
});
