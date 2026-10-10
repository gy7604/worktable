create extension if not exists pg_cron with schema pg_catalog;
create table if not exists public.worktable_push_subscriptions (
 endpoint text primary key check(length(endpoint)<=4096),
 user_id bigint not null references public.app_users(id) on delete cascade,
 user_name text not null,
 team text not null check(team in ('B','D')),
 subscription jsonb not null,
 enabled boolean not null default true,
 last_sent_key text,
 claim_until timestamptz,
 last_test_at timestamptz,
 updated_at timestamptz not null default now()
);
alter table public.worktable_push_subscriptions enable row level security;
revoke all on public.worktable_push_subscriptions from public,anon,authenticated;
grant all on public.worktable_push_subscriptions to service_role;
create index if not exists worktable_push_user_idx on public.worktable_push_subscriptions(user_id);
do $$begin
 if not exists(select 1 from vault.secrets where name='worktable_push_cron_token') then
  perform vault.create_secret(encode(extensions.gen_random_bytes(32),'hex'),'worktable_push_cron_token','Private scheduler authentication for worktable reminders');
 end if;
end$$;
create or replace function public.worktable_push_cron_authorized(p_token_hash text)
returns boolean language sql security definer set search_path='' as $$
 select coalesce((select encode(extensions.digest(decrypted_secret,'sha256'),'hex')=p_token_hash from vault.decrypted_secrets where name='worktable_push_cron_token'),false);
$$;
revoke all on function public.worktable_push_cron_authorized(text) from public,anon,authenticated;
grant execute on function public.worktable_push_cron_authorized(text) to service_role;
create or replace function public.worktable_push_claim(p_endpoint text,p_send_key text)
returns boolean language plpgsql security invoker set search_path='' as $$
declare updated_rows integer;
begin
 update public.worktable_push_subscriptions set claim_until=now()+interval '10 minutes'
 where endpoint=p_endpoint and enabled and last_sent_key is distinct from p_send_key
 and (claim_until is null or claim_until<now());
 get diagnostics updated_rows=row_count;
 return updated_rows=1;
end$$;
revoke all on function public.worktable_push_claim(text,text) from public,anon,authenticated;
grant execute on function public.worktable_push_claim(text,text) to service_role;

select cron.schedule('worktable-reminders','50-59 8,20 * * *', $job$select net.http_post(url:='https://lqqjyedfwlqinppcrvjf.supabase.co/functions/v1/worktable-push',headers:=jsonb_build_object('Content-Type','application/json','Authorization','Bearer '||(select decrypted_secret from vault.decrypted_secrets where name='worktable_push_cron_token')),body:='{"action":"dispatch"}'::jsonb,timeout_milliseconds:=60000);$job$); select cron.schedule('worktable-reminder-retry','0-5 9,21 * * *', $job$select net.http_post(url:='https://lqqjyedfwlqinppcrvjf.supabase.co/functions/v1/worktable-push',headers:=jsonb_build_object('Content-Type','application/json','Authorization','Bearer '||(select decrypted_secret from vault.decrypted_secrets where name='worktable_push_cron_token')),body:='{"action":"dispatch"}'::jsonb,timeout_milliseconds:=60000);$job$);
