import {schedule} from './schedule.mjs';
const origin='https://gy7604.github.io';
const headers={'Access-Control-Allow-Origin':origin,'Access-Control-Allow-Headers':'authorization,apikey,x-client-info,content-type,x-app-session,x-alarm-token','Access-Control-Allow-Methods':'POST,OPTIONS','Content-Type':'application/json','Cache-Control':'no-store','Vary':'Origin'};
const reply=(data:unknown,status=200)=>new Response(JSON.stringify(data),{status,headers});
const hex=(b:Uint8Array)=>Array.from(b,x=>x.toString(16).padStart(2,'0')).join('');
const random=()=>hex(crypto.getRandomValues(new Uint8Array(32)));
const hash=async(t:string)=>hex(new Uint8Array(await crypto.subtle.digest('SHA-256',new TextEncoder().encode(t))));
Deno.serve(async(req:Request)=>{
 if(req.headers.get('origin') && req.headers.get('origin')!==origin)return reply({success:false},403);
 if(req.method==='OPTIONS')return new Response('ok',{headers});
 if(req.method!=='POST')return reply({success:false},405);
 try {
  const raw=await req.text();if(raw.length>2048)return reply({success:false},413);
  const b=JSON.parse(raw);let rpc:string,args:Record<string,unknown>,secret:string|undefined;
  if(b.action==='issue'||b.action==='revoke') {
   const token=req.headers.get('x-app-session')||'';if(!/^[0-9a-f]{64}$/.test(token))return reply({success:false},401);
   rpc=b.action==='issue'?'app_alarm_issue':'app_alarm_revoke';args={p_session_hash:await hash(token)};
   if(b.action==='issue'){secret=random();args.p_code_hash=await hash(secret);}
  }else if(b.action==='exchange'){
   if(typeof b.code!=='string'||!/^[0-9a-f]{64}$/.test(b.code))return reply({success:false,message:'연결 코드를 확인해 주세요.'},400);
   secret=random();rpc='app_alarm_exchange';args={p_code_hash:await hash(b.code),p_device_hash:await hash(secret)};
  }else if(b.action==='sync'){
   const token=req.headers.get('x-alarm-token')||'';if(!/^[0-9a-f]{64}$/.test(token))return reply({success:false},401);
   rpc='app_alarm_read';args={p_device_hash:await hash(token)};
  }else return reply({success:false},400);
  const url=Deno.env.get('SUPABASE_URL')!,key=Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!;
  const r=await fetch(url+'/rest/v1/rpc/'+rpc,{method:'POST',headers:{apikey:key,Authorization:'Bearer '+key,'Content-Type':'application/json'},body:JSON.stringify(args)});
  if(!r.ok)return reply({success:false,message:'잠시 후 다시 시도해 주세요.'},503);
  const data=await r.json();if(data.status!=='ok')return reply({success:false,message:'연결이 만료되었거나 해제되었습니다. 다시 연결해 주세요.'},401);
  if(b.action==='issue')return reply({success:true,code:secret,expires_minutes:10});
  if(b.action==='exchange')return reply({success:true,token:secret,name:data.name});
  if(b.action==='sync')return reply({success:true,name:data.name,team:data.team,alarms:schedule(data),fetched_at:Date.now()});
  return reply({success:true});
 }catch{return reply({success:false,message:'일정을 불러오지 못했습니다.'},503);}
});
