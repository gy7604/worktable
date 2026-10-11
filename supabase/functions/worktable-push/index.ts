import webpush from 'npm:web-push@3.6.7';
import {createClient} from 'npm:@supabase/supabase-js@2.57.4';
import {assignment,TEAMS} from './engine.js';
const origin='https://gy7604.github.io';
const headers={'Access-Control-Allow-Origin':origin,'Access-Control-Allow-Headers':'content-type, apikey, x-app-session, authorization','Access-Control-Allow-Methods':'POST, OPTIONS','Content-Type':'application/json','Cache-Control':'no-store','Vary':'Origin'};
const reply=(body:unknown,status=200)=>new Response(JSON.stringify(body),{status,headers});
const hash=async(s:string)=>Array.from(new Uint8Array(await crypto.subtle.digest('SHA-256',new TextEncoder().encode(s))),b=>b.toString(16).padStart(2,'0')).join('');
const url=Deno.env.get('SUPABASE_URL')!;
const key=Deno.env.get('SUPABASE_SERVICE_ROLE_KEY') || JSON.parse(Deno.env.get('SUPABASE_SECRET_KEYS')||'{}').default;
const db=createClient(url,key,{auth:{persistSession:false}});
const publicKey=Deno.env.get('VAPID_PUBLIC_KEY')!;
webpush.setVapidDetails(Deno.env.get('VAPID_SUBJECT')!,publicKey,Deno.env.get('VAPID_PRIVATE_KEY')!);
function validSubscription(sub:any){
 if(!sub||typeof sub.endpoint!=='string'||sub.endpoint.length>4096||typeof sub.keys?.auth!=='string'||typeof sub.keys?.p256dh!=='string')return false;
 try{const u=new URL(sub.endpoint);return u.protocol==='https:'&&!u.username&&!u.password&&(!u.port||u.port==='443')&&(u.hostname==='fcm.googleapis.com'||u.hostname==='updates.push.services.mozilla.com'||u.hostname.endsWith('.push.apple.com')||u.hostname.endsWith('.notify.windows.com'))&&/^[A-Za-z0-9_-]{16,256}$/.test(sub.keys.auth)&&/^[A-Za-z0-9_-]{40,256}$/.test(sub.keys.p256dh);}catch{return false;}
}
function findWork(name:string,date:Date,records:any[],shift:string){
 for(const team of ['B','D']){
  const a=assignment(team,date,records);
  if(a.shift!==shift||a.off?.includes(name))continue;
  for(const [role,label] of Object.entries({ccr1:'1기 CCR',ccr2:'2기 CCR',kiln:'Kiln',pellet:'Pellet',clean:'청정'}))if(a[role]===name){
   const uncertain=/규칙 확인 필요|관리자 확인/.test(a.note||'');
   return {team,role:label+(a.fieldConfirmation&&['kiln','pellet','clean'].includes(role)?' (확인)':''),note:uncertain?'휴무 변동이 있어 담당 포지션을 확인해 주세요.':a.note};
  }
  if(a.changed&&TEAMS[team].members.includes(name)&&!a.off?.includes(name)&&['kiln','pellet','clean'].some(role=>a[role]==='당일 확인'))return {team,role:'현장 · 당일 확인',note:'현장 배치를 당일 확인해 주세요.'};
  // Unspecified multi-helper allocation must not silently suppress a known substitution shift.
  const helper=records.some(r=>r.team===team&&r.shift_date===date.toISOString().slice(0,10)&&r.shift_type===(shift==='D'?'주간':'야간')&&r.status==='FILLED'&&r.filled_by===name&&TEAMS[team].members.includes(r.requested_by)&&r.request_kind!=='support'&&!/정수|지원/.test(r.reason||''));
  if(helper)return {team,role:'현장 대근 · 당일 확인',note:'현장 배치를 확인해 주세요.'};
 }
 return null;
}
async function sendOne(row:any,title:string,body:string,tag:string){
 if(!validSubscription(row.subscription))return false;
 try{await webpush.sendNotification(row.subscription,JSON.stringify({title,body,tag,url:'https://gy7604.github.io/worktable/'}),{TTL:900,urgency:'high'});return true;}
 catch(error){const status=(error as any)?.statusCode;if(status===404||status===410)await db.from('worktable_push_subscriptions').update({enabled:false}).eq('endpoint',row.endpoint);return false;}
}
Deno.serve(async req=>{
 if(req.headers.get('origin')&&req.headers.get('origin')!==origin)return reply({success:false},403);
 if(req.method==='OPTIONS')return new Response('ok',{headers});
 if(req.method!=='POST')return reply({success:false},405);
 try{
  const raw=await req.text();if(raw.length>16384)return reply({success:false},413);
  const body=JSON.parse(raw);
  if(body.action==='dispatch'){
   const auth=req.headers.get('authorization')||'';
   const {data:allowed,error}=await db.rpc('worktable_push_cron_authorized',{p_token_hash:await hash(auth.replace(/^Bearer /,''))});
   if(error||allowed!==true)return reply({success:false},401);
   const kst=new Date(Date.now()+9*3600000);const minutes=kst.getUTCHours()*60+kst.getUTCMinutes();
   const shift=minutes>=350&&minutes<=365?'D':minutes>=1070&&minutes<=1085?'N':null;
   if(!shift)return reply({success:true,sent:0,message:'Outside scheduled window'});
   const dateKey=kst.toISOString().slice(0,10),date=new Date(dateKey+'T00:00:00Z');
   const {data:requests,error:readError}=await db.from('requests').select('team,shift_date,shift_type,requested_by,filled_by,status,request_kind,reason').eq('shift_date',dateKey).in('team',['B','D']);
   if(readError)throw readError;
   const {data:subs,error:subError}=await db.from('worktable_push_subscriptions').select('*').eq('enabled',true);
   if(subError)throw subError;
   let sent=0,failed=0;
   for(const row of subs||[]){
    const {data:user,error:userError}=await db.from('app_users').select('name,team,is_active').eq('id',row.user_id).maybeSingle();
    if(userError||!user?.is_active||user.name!==row.user_name||user.team!==row.team)continue;
    const work=findWork(user.name,date,requests||[],shift);if(!work)continue;
    const sendKey=dateKey+'-'+shift;if(row.last_sent_key===sendKey)continue;
    const {data:claimed,error:claimError}=await db.rpc('worktable_push_claim',{p_endpoint:row.endpoint,p_send_key:sendKey});
    if(claimError||!claimed)continue;
    const title=(shift==='D'?'오늘 주간':'오늘 야간')+' 근무 안내';
    const message=dateKey+' · 환원'+work.team+'조 · '+user.name+'님\n'+work.role+(work.note?'\n'+work.note:'');
    if(await sendOne(row,title,message,'worktable-'+sendKey)){
      await db.from('worktable_push_subscriptions').update({last_sent_key:sendKey,claim_until:null}).eq('endpoint',row.endpoint);sent++;
    }else{await db.from('worktable_push_subscriptions').update({claim_until:null}).eq('endpoint',row.endpoint);failed++;}
   }
   return reply({success:true,sent,failed});
  }
  const token=req.headers.get('x-app-session')||'';if(!/^[0-9a-f]{64}$/.test(token))return reply({success:false,message:'계정 연결 후 이용해 주세요.'},401);
  const {data:session,error}=await db.rpc('app_session_verify',{p_token_hash:await hash(token)});
  if(error||session?.status!=='ok')return reply({success:false,message:'연결이 만료되었습니다. 계정을 다시 연결해 주세요.'},401);
  const user=session.user;
  if(!['B','D'].includes(user.team)||!TEAMS[user.team].members.includes(user.name))return reply({success:false,message:'배치표에 등록된 본인 계정으로 연결해 주세요.'},403);
  if(body.action==='register'){
   if(body.name!==user.name||body.team!==user.team)return reply({success:false,message:'선택한 조와 이름을 연결 계정과 일치시켜 주세요.'},403);
   if(!validSubscription(body.subscription))return reply({success:false,message:'알림 정보를 확인해 주세요.'},400);
   const endpoint=body.subscription.endpoint;
   const {error}=await db.from('worktable_push_subscriptions').upsert({endpoint,user_id:user.id,user_name:user.name,team:user.team,subscription:body.subscription,enabled:true,updated_at:new Date().toISOString()},{onConflict:'endpoint'});
   if(error)throw error;return reply({success:true,name:user.name,team:user.team});
  }
  if(!['status','disable','test'].includes(body.action)||typeof body.endpoint!=='string')return reply({success:false},400);
  const {data:row,error:rowError}=await db.from('worktable_push_subscriptions').select('*').eq('endpoint',body.endpoint).eq('user_id',user.id).maybeSingle();if(rowError)throw rowError;
  if(body.action==='status')return reply({success:true,enabled:row?.enabled===true,name:user.name,team:user.team,public_key:publicKey});
  if(body.action==='disable'){if(row)await db.from('worktable_push_subscriptions').update({enabled:false}).eq('endpoint',row.endpoint).eq('user_id',user.id);return reply({success:true});}
  if(!row?.enabled)return reply({success:false,message:'먼저 알림을 켜 주세요.'},400);
  const age=Date.now()-Date.parse(row.last_test_at||'1970-01-01');if(age<60000)return reply({success:false,message:'테스트는 1분 후 다시 이용해 주세요.'},429);
  await db.from('worktable_push_subscriptions').update({last_test_at:new Date().toISOString()}).eq('endpoint',row.endpoint);
  const ok=await sendOne(row,'근무배치표 알림 테스트',user.name+'님 · 주간 05:50 / 야간 17:50에 당일 근무를 알려드립니다.','worktable-test');
  return reply({success:ok,message:ok?'테스트 알림을 보냈습니다.':'알림 전송에 실패했습니다.'},ok?200:502);
 }catch{return reply({success:false,message:'알림 처리에 실패했습니다. 잠시 후 다시 시도해 주세요.'},500);}
});
