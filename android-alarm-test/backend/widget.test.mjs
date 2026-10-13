import assert from 'node:assert/strict';
import {schedule,workdays} from './schedule.mjs';
const data={team:'B',today:'2026-10-13',requests:[{date:'2026-10-16',shift:'야간',off:false,substitute:true},{date:'2026-10-18',shift:'주간',off:true,substitute:false}]};
const days=workdays(data);
assert.equal(days.length,31);
assert.equal(days[3].work.find(x=>x.source==='대근').hours,'19:00–익일 07:00');
assert.deepEqual(days[5].off,['주간']);
assert.equal(days[5].work.length,0);
for(const team of ['A','B','C','D']){
 const input={...data,team};
 const actual=workdays(input).flatMap(d=>d.work.map(w=>({date:d.date,shift:w.shift,source:w.source==='대근'?'대근':'정규 근무'})));
 assert.deepEqual(actual,schedule(input).map(({date,shift,source})=>({date,shift,source})));
}
assert.equal(workdays({...data,today:'2026-12-29'})[6].date,'2027-01-04');
console.log('Widget schedule tests passed: all teams, leave, substitute, next-day hours, year rollover');
