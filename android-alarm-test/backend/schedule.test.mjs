import assert from 'node:assert/strict';
import {schedule} from './schedule.mjs';
const base={team:'A',today:'2026-09-30',requests:[]};
let plan=schedule(base);
assert.equal(plan[0].time,'05:00');
assert.equal(plan.find(x=>x.date==='2026-10-04').time,'17:10');
assert(!plan.some(x=>x.date==='2026-10-02'));
assert.equal(new Date(plan[0].at).toISOString(),'2026-09-29T20:00:00.000Z');
plan=schedule({...base,requests:[{date:'2026-09-30',shift:'주간',off:true},{date:'2026-10-02',shift:'야간',substitute:true}]});
assert(!plan.some(x=>x.date==='2026-09-30'));
assert.equal(plan.find(x=>x.date==='2026-10-02').time,'17:10');
plan=schedule({...base,requests:[{date:'2026-09-30',shift:'주간',substitute:true}]});
assert.equal(plan.filter(x=>x.date==='2026-09-30').length,1);
plan=schedule({...base,requests:[{date:'2026-09-30',shift:'야간',substitute:true}]});
assert.equal(plan.filter(x=>x.date==='2026-09-30').length,2);
for(const team of ['A','B','C','D']) {const p=schedule({...base,team});assert(p.length>0);assert(p.every(x=>['05:00','17:10'].includes(x.time)));}
console.log('Rotation, leave, substitution, deduplication and KST checks passed');
