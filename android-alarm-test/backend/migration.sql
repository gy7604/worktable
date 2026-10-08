create table private.alarm_pairings (
 code_hash text primary key check (code_hash ~ '^[0-9a-f]{64}$'),
 user_id bigint not null references public.app_users(id) on delete cascade,
 credential_version bigint not null,
 expires_at timestamptz not null,
 consumed_at timestamptz
);
create table private.alarm_devices (
 token_hash text primary key check (token_hash ~ '^[0-9a-f]{64}$'),
 user_id bigint not null references public.app_users(id) on delete cascade,
 credential_version bigint not null,
 created_at timestamptz not null default now(),
 revoked_at timestamptz
);
alter table private.alarm_pairings enable row level security;
alter table private.alarm_devices enable row level security;
revoke all on private.alarm_pairings, private.alarm_devices from public,anon,authenticated;
create or replace function public.app_alarm_issue(p_session_hash text,p_code_hash text) returns jsonb
language plpgsql security definer set search_path='' as $$
declare s jsonb; uid bigint;
begin
 s:=public.app_session_verify(p_session_hash);
 if s->>'status' is distinct from 'ok' then return jsonb_build_object('status','invalid'); end if;
 uid:=(s->'user'->>'id')::bigint;
 if p_code_hash !~ '^[0-9a-f]{64}$' then return jsonb_build_object('status','invalid'); end if;
 delete from private.alarm_pairings where user_id=uid;
 insert into private.alarm_pairings select p_code_hash,u.id,u.credential_version,now()+interval '10 minutes',null from public.app_users u where u.id=uid;
 return jsonb_build_object('status','ok');
end $$;
create or replace function public.app_alarm_exchange(p_code_hash text,p_device_hash text) returns jsonb
language plpgsql security definer set search_path='' as $$
declare p private.alarm_pairings; u public.app_users;
begin
 if p_device_hash !~ '^[0-9a-f]{64}$' then return jsonb_build_object('status','invalid'); end if;
 select * into p from private.alarm_pairings where code_hash=p_code_hash and consumed_at is null and expires_at>now() for update;
 if not found then return jsonb_build_object('status','invalid'); end if;
 select * into u from public.app_users where id=p.user_id and is_active and credential_version=p.credential_version;
 if not found then return jsonb_build_object('status','invalid'); end if;
 update private.alarm_pairings set consumed_at=now() where code_hash=p_code_hash;
 insert into private.alarm_devices(token_hash,user_id,credential_version) values(p_device_hash,u.id,u.credential_version);
 return jsonb_build_object('status','ok','name',u.name);
end $$;
create or replace function public.app_alarm_read(p_device_hash text) returns jsonb
language plpgsql security definer set search_path='' as $$
declare u public.app_users; today date:=(now() at time zone 'Asia/Seoul')::date;
begin
 select a.* into u from private.alarm_devices d join public.app_users a on a.id=d.user_id where d.token_hash=p_device_hash and d.revoked_at is null and a.is_active and d.credential_version=a.credential_version;
 if not found then return jsonb_build_object('status','invalid'); end if;
 return jsonb_build_object('status','ok','today',today,'name',u.name,'team',u.team,'requests',coalesce((
 select jsonb_agg(jsonb_build_object('date',r.shift_date,'shift',r.shift_type,'off',r.request_kind='off' and (r.requested_by_id=u.id or (r.requested_by_id is null and r.requested_by=u.name and (select count(*) from public.app_users a where a.name=u.name)=1)),'substitute',r.status='FILLED' and (r.filled_by_id=u.id or (r.filled_by_id is null and r.filled_by=u.name and (select count(*) from public.app_users a where a.name=u.name)=1))))
 from public.requests r where r.shift_date>=today and r.shift_date<today+31 and (r.requested_by_id=u.id or r.filled_by_id=u.id or (r.requested_by_id is null and r.requested_by=u.name) or (r.filled_by_id is null and r.filled_by=u.name))),'[]'::jsonb));
end $$;
create or replace function public.app_alarm_revoke(p_session_hash text) returns jsonb
language plpgsql security definer set search_path='' as $$
declare s jsonb; uid bigint;
begin
 s:=public.app_session_verify(p_session_hash);
 if s->>'status' is distinct from 'ok' then return jsonb_build_object('status','invalid'); end if;
 uid:=(s->'user'->>'id')::bigint;
 update private.alarm_devices set revoked_at=now() where user_id=uid and revoked_at is null;
 delete from private.alarm_pairings where user_id=uid;
 return jsonb_build_object('status','ok');
end $$;
revoke all on function public.app_alarm_issue(text,text),public.app_alarm_exchange(text,text),public.app_alarm_read(text),public.app_alarm_revoke(text) from public,anon,authenticated;
grant execute on function public.app_alarm_issue(text,text),public.app_alarm_exchange(text,text),public.app_alarm_read(text),public.app_alarm_revoke(text) to service_role;
