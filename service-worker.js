const CACHE_NAME='worktable-v18';
let migrateOldPage=false;
const ASSETS=['./','./index.html','./manifest.json','./icons/icon-192.png','./icons/icon-512.png'];
self.addEventListener('install',event=>{
  event.waitUntil((async()=>{
    const keys=await caches.keys();
    await (await caches.open(CACHE_NAME)).addAll(ASSETS.map(url=>new Request(url,{cache:'reload'})));
    // One-time migration: older pages have no update button.
    migrateOldPage=keys.some(key=>/^worktable-v(?:[1-9]|1[0-7])$/.test(key));
    if(migrateOldPage)await self.skipWaiting();
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
    if(migrateOldPage){
      const windows=await self.clients.matchAll({type:'window'});
      await Promise.all(windows.filter(client=>new URL(client.url).pathname.startsWith('/worktable/')).map(client=>client.navigate(client.url).catch(()=>{})));
    }
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
self.addEventListener('push',event=>{
 let data={};try{data=event.data?.json()||{};}catch{}
 event.waitUntil(self.registration.showNotification(data.title||'근무배치표',{
  body:typeof data.body==='string'?data.body:'당일 근무배치를 확인해 주세요.',
  icon:'./icons/icon-192.png',badge:'./icons/icon-192.png',
  tag:typeof data.tag==='string'?data.tag:'worktable-reminder',
  data:{url:self.location.origin+'/worktable/'},
 }));
});
self.addEventListener('notificationclick',event=>{
 event.notification.close();
 event.waitUntil((async()=>{
  const url=self.location.origin+'/worktable/';
  const windows=await self.clients.matchAll({type:'window',includeUncontrolled:true});
  const found=windows.find(client=>client.url.startsWith(url));
  if(found){await found.focus();return;}
  await self.clients.openWindow(url);
 })());
});
