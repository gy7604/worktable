const CACHE_NAME='worktable-v8';
const ASSETS=['./','./index.html','./manifest.json','./icons/icon-192.png','./icons/icon-512.png'];
self.addEventListener('install',event=>{
  event.waitUntil((async()=>{
    const keys=await caches.keys();
    await (await caches.open(CACHE_NAME)).addAll(ASSETS);
    // One-time migration: older pages have no update button.
    if(keys.some(key=>/^worktable-v[1-6]$/.test(key)))await self.skipWaiting();
  })());
});
self.addEventListener('message',event=>{
  if(event.data && event.data.type==='SKIP_WAITING')self.skipWaiting();
});
self.addEventListener('activate',event=>{
  event.waitUntil((async()=>{
    const keys=await caches.keys();
    await Promise.all(keys.filter(key=>key.startsWith('worktable-')&&key!==CACHE_NAME).map(key=>caches.delete(key)));
    await self.clients.claim();
  })());
});
self.addEventListener('fetch',event=>{
  const url=new URL(event.request.url);
  if(event.request.method!=='GET'||url.origin!==self.location.origin||!url.pathname.startsWith('/worktable/'))return;
  event.respondWith((async()=>{
    const cache=await caches.open(CACHE_NAME);
    if(event.request.mode==='navigate'){
      try{
        const response=await fetch(event.request,{cache:'no-store'});
        if(response.ok){await cache.put(event.request,response.clone());return response;}
      }catch{}
      return await cache.match(event.request)||await cache.match('./index.html')||Response.error();
    }
    const cached=await cache.match(event.request);
    if(cached)return cached;
    const response=await fetch(event.request);
    if(response.ok)await cache.put(event.request,response.clone());
    return response;
  })());
});
