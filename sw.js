self.addEventListener('install', e => {
  e.waitUntil(
    caches.open('jarvis-v1').then(cache => cache.addAll(['./', './Jarvis8-PWA.html']))
  );
});

self.addEventListener('fetch', e => {
  e.respondWith(
    caches.match(e.request).then(res => res || fetch(e.request))
  );
});
