create or replace function public.keep_alive_probe()
returns jsonb
language sql
security invoker
set search_path = ''
as $$
  select pg_catalog.jsonb_build_object('ok', true)
$$;

revoke all on function public.keep_alive_probe() from public;
grant execute on function public.keep_alive_probe() to anon, authenticated;
