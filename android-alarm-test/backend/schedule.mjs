export function schedule(data) {
 const offsets={A:0,C:2,D:4,B:6}, pattern=['주간','주간','휴무','휴무','야간','야간','휴무','휴무'];
 if(!(data.team in offsets)) throw new Error('unsupported team');
 const start=Date.parse(data.today+'T00:00:00Z'), base=Date.parse('2026-09-30T00:00:00Z');
 const alarms=[];
 for(let d=0;d<31;d++) {
  const date=new Date(start+d*86400000).toISOString().slice(0,10);
  const regular=pattern[(((start-base)/86400000+d+offsets[data.team])%8+8)%8];
  const shifts=new Map();
  const rows=data.requests.filter(r=>r.date===date);
  if(regular!=='휴무' && !rows.some(r=>r.off && r.shift===regular)) shifts.set(regular,'정규 근무');
  for(const r of rows) if(r.substitute && ['주간','야간'].includes(r.shift)) shifts.set(r.shift,'대근');
  for(const [shift,source] of shifts) {const time=shift==='주간'?'05:00':'17:10';alarms.push({date,shift,time,source,at:Date.parse(date+'T'+time+':00+09:00')});}
 }
 return alarms.sort((a,b)=>a.at-b.at);
}
