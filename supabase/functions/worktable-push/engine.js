function readSetting(){return null;}
function saveSetting(key,value){try{localStorage.setItem(key,value);}catch{}}
const TEAMS={
  B:{label:'환원B조',members:['이영식','강신혁','김영진','장경준','홍찬표']},
  D:{label:'환원D조',members:['홍동완','양석진','황호현','김종구','최정목']}
};
let selectedTeam=readSetting('worktable-team')==='D'?'D':'B';
const MEMBERS    = ['이영식','강신혁','김영진','장경준','홍찬표'];
const PATTERN    = ['D','D','O','O','N','N','O','O'];
const BASE_DATE  = new Date(2026, 9, 1);
const BASE_PHASE = 7;
const CYCLE_BASE = new Date(2026, 9, 2);
const BASE_FIELD = ['Kiln','Pellet','청정'];

function dayDiff(a,b){ return Math.round((a-b)/86400000); }
function getPhase(d){ return ((BASE_PHASE+dayDiff(d,BASE_DATE))%8+8)%8; }
function getShift(d){ return PATTERN[getPhase(d)]; }
function getCycleInfo(d){
  const diff=dayDiff(d,CYCLE_BASE);
  if(diff<0)return null;
  return{cycle:Math.floor(diff/8),cyclePhase:diff%8};
}
function dateKey(d){
  return d.getFullYear()+'-'+String(d.getMonth()+1).padStart(2,'0')+'-'+String(d.getDate()).padStart(2,'0');
}
function prevInOrder(name){
  const i=MEMBERS.indexOf(name);
  return MEMBERS[(i-1+5)%5];
}

// 시뮬레이션 캐시
const cache={};
// 시뮬레이션 지속 상태 (10/2부터 누적)
let simState=null;

function initSimState(){
  return{
    ccrState:{ccr1:'강신혁',ccr2:'김영진'},
    nextStartIdx:{이영식:1,강신혁:0,김영진:2,장경준:0,홍찬표:2},
    lastCPK:null,
    lastDate:null
  };
}

function runSimUntil(targetDate){
  if(!simState) simState=initSimState();
  // 이미 계산된 날짜면 스킵
  if(simState.lastDate && simState.lastDate>=targetDate) return;

  let cur=simState.lastDate
    ? new Date(simState.lastDate.getTime()+86400000)
    : new Date(CYCLE_BASE);

  while(cur<=targetDate){
    const shift=getShift(cur);
    const key=dateKey(cur);
    if(shift==='O'){
      cache[key]={shift:'O'};
    } else {
      const info=getCycleInfo(cur);
      const cpk=info.cycle+'_'+shift;
      if(cpk!==simState.lastCPK){
        simState.lastCPK=cpk;
        if(!(info.cycle===0&&shift==='D')){
          const out=simState.ccrState.ccr2;
          const newCCR1=prevInOrder(simState.ccrState.ccr1);
          simState.nextStartIdx[out]=simState.nextStartIdx[newCCR1];
          simState.ccrState={ccr1:newCCR1,ccr2:simState.ccrState.ccr1};
        }
      }
      const field=MEMBERS.filter(m=>m!==simState.ccrState.ccr1&&m!==simState.ccrState.ccr2);
      const pm={'Kiln':null,'Pellet':null,'청정':null};
      field.forEach(m=>{ pm[BASE_FIELD[simState.nextStartIdx[m]]]=m; });
      cache[key]={
        shift,
        ccr1:simState.ccrState.ccr1,
        ccr2:simState.ccrState.ccr2,
        kiln:pm['Kiln']||'-',
        pellet:pm['Pellet']||'-',
        clean:pm['청정']||'-',
      };
      field.forEach(m=>{ simState.nextStartIdx[m]=(simState.nextStartIdx[m]+2)%3; });
    }
    simState.lastDate=new Date(cur);
    cur=new Date(cur.getTime()+86400000);
  }
}

const dCache={};
let dState={ccr1:0,ccr2:4,nextIdx:[0,2,0,1,0],lastDay:0};
function getDAsgn(date){
  const start=new Date(2026,9,1);
  const target=dayDiff(date,start)+1;
  if(target<1)return {shift:'U'};
  for(let day=dState.lastDay+1;day<=target;day++){
    const phase=(5+day-1)%8,shift=PATTERN[phase];
    const row={shift};
    if(shift!=='O'&&shift!=='U'){
      if(day>1&&(phase===0||phase===4)){
        const incoming=(dState.ccr1+1)%5;
        dState.nextIdx[dState.ccr2]=dState.nextIdx[incoming];
        dState.ccr2=dState.ccr1;dState.ccr1=incoming;
      }
      row.ccr1=TEAMS.D.members[dState.ccr1];row.ccr2=TEAMS.D.members[dState.ccr2];
      ['kiln','pellet','clean'].forEach(key=>row[key]='-');
      for(let i=0;i<5;i++)if(i!==dState.ccr1&&i!==dState.ccr2){
        row[['kiln','pellet','clean'][dState.nextIdx[i]]]=TEAMS.D.members[i];
        dState.nextIdx[i]=(dState.nextIdx[i]+2)%3;
      }
    }
    dCache[day]=row;dState.lastDay=day;
  }
  return dCache[target];
}
function getBaseAsgn(date){
  if(selectedTeam==='D')return getDAsgn(date);
  const key=dateKey(date);
  if(!cache[key]){
    const end=new Date(date.getFullYear(),date.getMonth()+1,0);
    runSimUntil(end);
  }
  return cache[key]||{shift:'O'};
}

function applyDailyChanges(base,date,records,team=selectedTeam){
  if(base.shift==='O'||base.shift==='U')return {...base};
  const shift=base.shift==='D'?'주간':'야간';
  const members=TEAMS[team].members;
  const rows=records.filter(r=>members.includes(r.requested_by) && r.team===team && r.shift_date===dateKey(date) && r.shift_type===shift && r.request_kind!=='support' && !/정수|지원/.test(String(r.reason||'')) && !['CANCELLED','CANCELED','DELETED'].includes(r.status));
  const off=new Set(rows.map(r=>r.requested_by).filter(n=>members.includes(n)));
  const helpers=[...new Set(rows.filter(r=>r.status==='FILLED'&&r.filled_by&&!members.includes(r.filled_by)).map(r=>String(r.filled_by)))];
  if(!off.size&&!helpers.length)return {...base};
  const result={...base,changed:true,off:[...off]};
  const roles=['ccr1','ccr2','kiln','pellet','clean'];
  function unconfirmed(message){
    for(const key of roles)if(off.has(result[key]))result[key]='공석';
    result.note=message;return result;
  }
  // Multi-helper field allocation has not yet been specified.
  if(helpers.length>1)return unconfirmed('대근자 2명 이상: 현장 배치 규칙 확인 필요');
  const direction=team==='B'?-1:1;
  function candidates(name,step){const start=members.indexOf(name);return Array.from({length:5},(_,i)=>members[(start+step*i+50)%5]).filter(n=>!off.has(n));}
  const first=off.has(base.ccr1)?candidates(base.ccr1,direction):[base.ccr1];
  const second=off.has(base.ccr2)?candidates(base.ccr2,-direction):[base.ccr2];
  const forbidden=(a,b)=>team==='B'&&[a,b].includes('강신혁')&&[a,b].includes('장경준');
  let pair=null;
  for(const a of first){for(const b of second)if(a!==b&&!forbidden(a,b)){pair=[a,b];break;}if(pair)break;}
  if(!pair)return unconfirmed('CCR 배치 가능한 조합 없음 · 관리자 확인 필요');
  result.ccr1=pair[0];result.ccr2=pair[1];
  const used=new Set(pair);
  if(helpers.length===1){result.pellet=helpers[0];used.add(helpers[0]);}
  else result.pellet='공석';
  // Keep available field staff at their original positions first.
  for(const role of ['kiln','clean']){
    const name=base[role];
    if(!off.has(name)&&!used.has(name)){result[role]=name;used.add(name);}else result[role]='공석';
  }
  const remaining=members.filter(n=>!off.has(n)&&!used.has(n));
  for(const role of ['kiln','clean'])if(result[role]==='공석'&&remaining.length){result[role]=remaining.shift();used.add(result[role]);}
  if(!helpers.length&&!off.has(base.pellet)&&!used.has(base.pellet)){result.pellet=base.pellet;used.add(base.pellet);}
  if(remaining.some(n=>!used.has(n)))return unconfirmed('현장 배치 규칙 확인 필요');
  result.note=helpers.length===1?'휴무 반영 · 대근자 Pellet 배치 · 당일만 변경':'휴무 반영 · 대근 미등록 / 공석 확인 필요';
  return result;
}

export function assignment(team,date,records){selectedTeam=team;return applyDailyChanges(getBaseAsgn(date),date,records,team);}
export { TEAMS };
