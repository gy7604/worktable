'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

const html = fs.readFileSync(path.join(__dirname, '..', 'index.html'), 'utf8').replace(/\r\n/g, '\n');
const begin = html.indexOf('const TEAMS={');
const end = html.indexOf('// =============================================\n// 앱 상태', begin);
assert.ok(begin !== -1 && end > begin, 'assignment logic must be present');
const assignmentScript = html.slice(begin, end);

function createRoster(team) {
  return vm.runInNewContext(
    assignmentScript + '\n;({ TEAMS, getBaseAsgn, applyDailyChanges, getMyPos })',
    { readSetting: key => key === 'worktable-team' ? team : null, localStorage: {} }
  );
}

function workday(roster, team) {
  for (let i = 0; i < 20; i++) {
    const date = new Date(2026, 10, 9 + i);
    const base = roster.getBaseAsgn(date);
    if (base.shift === 'D' || base.shift === 'N') {
      const field = roster.TEAMS[team].members.filter(n => n !== base.ccr1 && n !== base.ccr2);
      assert.equal(field.length, 3);
      return { team, date, base, field, shift: base.shift === 'D' ? '주간' : '야간' };
    }
  }
  throw new Error('No workday found: ' + team);
}

function leaveRows(info, absent, helper = null) {
  const d = info.date;
  const shiftDate = d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0')
    + '-' + String(d.getDate()).padStart(2, '0');
  return absent.map((name, index) => ({
    requested_by: name, team: info.team, shift_date: shiftDate,
    shift_type: info.shift, request_kind: 'leave',
    status: helper && index === 0 ? 'FILLED' : 'APPROVED',
    filled_by: helper && index === 0 ? helper : null,
  }));
}

for (const team of ['B', 'D']) {
  const roster = createRoster(team);
  const info = workday(roster, team);
  const { base, date, field } = info;

  for (let survivor = 0; survivor < 3; survivor++) {
    test(team + ': field 2 off, survivor ' + survivor + ' moves to 청정', () => {
      const absent = field.filter((_, i) => i !== survivor);
      const result = roster.applyDailyChanges(base, date, leaveRows(info, absent), team);
      assert.equal(result.clean, field[survivor]);
      assert.equal(result.kiln, '당일 확인');
      assert.equal(result.pellet, '대근자 미정');
      assert.equal(result.ccr1, base.ccr1);
      assert.equal(result.ccr2, base.ccr2);
      assert.equal(result.fieldConfirmation, undefined);
      assert.equal(roster.getMyPos(result, field[survivor]), '청정');
      for (const position of ['ccr1', 'ccr2', 'kiln', 'pellet', 'clean']) {
        assert.ok(!absent.includes(result[position]), 'off-duty worker in ' + position);
      }
    });
  }

  test(team + ': one helper remains Pellet, lone field worker is 청정', () => {
    const result = roster.applyDailyChanges(base, date, leaveRows(info, field.slice(0, 2), '대근자A'), team);
    assert.equal(result.clean, field[2]);
    assert.equal(result.pellet, '대근자A');
  });

  test(team + ': field 2 off plus CCR off never duplicates 청정', () => {
    const result = roster.applyDailyChanges(base, date, leaveRows(info, [field[0], field[1], base.ccr1]), team);
    assert.equal(result.clean, field[2]);
    assert.notEqual(result.ccr1, field[2]);
    assert.notEqual(result.ccr2, field[2]);
    assert.match(result.note, /CCR/);
  });

  test(team + ': manager is not counted as an external helper', () => {
    const rows = leaveRows(info, field.slice(0, 2), '임병철');
    const result = roster.applyDailyChanges(base, date, rows, team);
    assert.equal(result.clean, field[2]);
    assert.equal(result.pellet, '대근자 미정');
  });

  test(team + ': single field off preserves the existing substitute flow', () => {
    const result = roster.applyDailyChanges(base, date, leaveRows(info, [field[0]], '대근자A'), team);
    assert.equal(result.pellet, '대근자A');
    assert.equal(result.changed, true);
  });

  test(team + ': three field workers off still requires field confirmation', () => {
    const result = roster.applyDailyChanges(base, date, leaveRows(info, field), team);
    assert.equal(result.fieldConfirmation, true);
    assert.match(result.note, /확인 필요/);
  });
}
