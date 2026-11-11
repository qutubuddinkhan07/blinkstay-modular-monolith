-- =============================================================================
--  BlinkStay — PostgreSQL Operations Runbook
-- =============================================================================
--  Database : blinkstay        Schema : public
--  Engine   : PostgreSQL 13+   (gen_random_uuid() and DROP ... WITH (FORCE))
--
--  ENVIRONMENT TAGS used on every block:
--    [DEV]        Local / development only. Destructive or test-data changes.
--    [READ]       Read-only. Safe to run anywhere, including production.
--    [PROD-WRITE] Changes data in production. Always run inside
--                 BEGIN ... COMMIT, check the row count first, and ROLLBACK
--                 if it does not match what you expected.
--    [ONE-TIME]   Migration / backfill. Run once per environment, then
--                 record the date it was run in the header of that block.
--
--  KEY DIFFERENCES FROM MYSQL (why the queries look different):
--    * UUID columns are native `uuid`. No BIN_TO_UUID / UUID_TO_BIN needed.
--      Compare with a literal:  where id = 'c0a8...'::uuid
--    * Every JOIN needs an ON clause (MySQL let you skip it and filter in WHERE).
--    * No `USE db`. The database is picked in the connection settings
--      (psql: \c blinkstay). Tables live in a schema, normally `public`.
--      You cannot write  blinkstay.users  to mean database.table — in
--      Postgres that means schema.table. See section 0.
--    * psql runs in autocommit mode. Use BEGIN; ... COMMIT; for safe writes.
--    * `--` comments work without a trailing space (MySQL requires the space).
--    * IF(cond, a, b)  ->  CASE WHEN cond THEN a ELSE b END
--    * curdate()       ->  current_date
--    * utc_timestamp() ->  now()   (for timestamptz columns, see section 10)
-- =============================================================================


-- =============================================================================
-- 0. CONNECTION & SESSION                                              [READ]
-- =============================================================================

-- -----------------------------------------------------------------------------
-- There is no `USE blinkstay;` in Postgres. Two levels decide where you are:
--   DATABASE : chosen when you connect. It cannot be switched with SQL.
--              GUI (IntelliJ / DataGrip / DBeaver): set the "Database" field
--              in the connection settings to  blinkstay , then reconnect.
--              psql:  \c blinkstay
--   SCHEMA   : a folder of tables inside the database (default: public).
--              Switch it for this session with  set search_path  (0c below).
-- -----------------------------------------------------------------------------

-- 0a. Where am I right now? Expect: blinkstay | <your user> | public
select current_database(), current_user, current_schema();

-- 0b. "relation does not exist" / no tables? Run these two queries.
-- Databases on this server. If current_database() above is not blinkstay,
-- fix the connection's Database field.
select datname from pg_database where not datistemplate order by datname;

-- Tables in every schema of this database (not just public).
-- If they show up under another schema (e.g. `blinkstay`, which pgloader
-- creates when migrating from MySQL), use 0c.
select table_schema, table_name
from information_schema.tables
where table_schema not in ('pg_catalog', 'information_schema')
order by table_schema, table_name;

-- 0c. Switch schema for this session (the closest thing to `use`).
--     It resets when you reconnect. GUI tools also have a schema picker
--     in the console toolbar.
-- set search_path to blinkstay, public;
-- show search_path;

-- Session time zone (affects how timestamptz values are displayed)
select current_setting('TimeZone') as session_tz;   -- same as: show timezone;

-- Optional: show timestamps in IST for this session only
-- set timezone = 'Asia/Kolkata';


-- =============================================================================
-- 1. DATABASE CREATE / DROP                                             [DEV]
-- =============================================================================
-- Run these while connected to the `postgres` database, not to `blinkstay`.

-- Postgres has no CREATE DATABASE IF NOT EXISTS. This psql trick does the same:
-- select 'create database blinkstay'
-- where not exists (select from pg_database where datname = 'blinkstay')\gexec

-- Plain create (errors if it already exists):
-- create database blinkstay;

-- !! DESTRUCTIVE — deletes the whole database. WITH (FORCE) kicks out
-- !! open connections (stop the Spring Boot app first anyway).
-- drop database if exists blinkstay with (force);


-- =============================================================================
-- 2. SCHEMA INSPECTION                                                 [READ]
-- =============================================================================

-- List all tables  (psql shortcut: \dt)
select table_name
from information_schema.tables
where table_schema = 'public'
order by table_name;

-- Find tables by name pattern (replaces SHOW TABLES LIKE '%booking%')
select table_name
from information_schema.tables
where table_schema = 'public'
  and table_name like '%booking%';

-- Describe a table's columns (psql shortcut: \d bookings)
select column_name, data_type, is_nullable, column_default
from information_schema.columns
where table_schema = 'public'
  and table_name = 'bookings'
order by ordinal_position;

-- Full table definition (replaces SHOW CREATE TABLE)
--   psql:     \d+ payments
--   terminal: pg_dump -s -t payments -U <user> blinkstay

-- Indexes for the listing tables (replaces SHOW INDEXES FROM ...)
-- psql shortcut: \di
select tablename, indexname, indexdef
from pg_indexes
where schemaname = 'public'
  and tablename in ('listings', 'listing_geometry', 'listing_amenities',
                    'listing_images', 'listing_rooms', 'blocked_token')
order by tablename, indexname;

drop index if exists idx_listings_id;

-- CHECK constraints on a table (Hibernate creates these for enum columns).
-- Use this to find the real constraint name before dropping it (section 7).
select conname as constraint_name,
       pg_get_constraintdef(oid) as definition
from pg_constraint
where conrelid = 'payments'::regclass
  and contype = 'c';


-- =============================================================================
-- 3. QUICK DATA PEEKS                                                  [READ]
-- =============================================================================
-- In production, avoid bare `select *` on big tables — always add a LIMIT.

-- Auth
set search_path to public;
select * from users limit 50;
select * from user_roles limit 50;
select * from blocked_token limit 50;

-- Users with readable ids (no BIN_TO_UUID needed, ids are already uuid)
select id, username from users order by username;

-- Listings
select * from listings          limit 50;
select * from listing_amenities limit 50;
select * from listing_geometry  limit 50;
select * from listing_images    limit 50;
select * from listing_rooms     limit 50;

-- Notifications & earnings
select * from notifications    order by created_at desc limit 50;
select * from earnings_entries order by created_at desc limit 50;


-- =============================================================================
-- 4. BOOKINGS                                                          [READ]
-- =============================================================================

-- Latest bookings
select *
from bookings
order by created_at desc
limit 10;

-- Bookings created today (IST calendar day)
select *
from bookings
where (created_at at time zone 'Asia/Kolkata')::date
    = (now()      at time zone 'Asia/Kolkata')::date
order by created_at desc;

-- Timing of the most recent booking (created -> confirmed)
select booking_ref, created_at, confirmed_at,
       confirmed_at - created_at as time_to_confirm
from bookings
order by created_at desc
limit 1;

-- Look up one booking by its public reference
select *
from bookings
where booking_ref = 'BS-KJEKSLPT';          -- replace

-- Latest bookings with the guest's username
-- (MySQL version had JOIN without ON; Postgres requires ON)
select b.booking_ref, b.status, b.check_in, b.check_out, b.total_amount,
       u.username as guest, b.created_at
from bookings b
join users u on u.id = b.guest_id
order by b.created_at desc
limit 3;

-- Count bookings per status (handy health check)
select status, count(*)
from bookings
group by status
order by status;


-- -----------------------------------------------------------------------------
-- 4b. Move a confirmed booking's dates (testing completion/earnings timers)
--                                                                       [DEV]
-- -----------------------------------------------------------------------------
-- begin;
-- update bookings
-- set check_in = '2026-10-10', check_out = '2026-10-12'
-- where booking_ref = 'BS-SCEB9X89' and status = 'CONFIRMED';
-- -- expect: UPDATE 1
-- commit;     -- or: rollback;


-- =============================================================================
-- 5. PAYMENTS & REFUNDS
-- =============================================================================

-- 5a. Latest payments, all columns readable                            [READ]
select id, booking_id, booking_reference, guest_id, listing_id,
       amount, currency, status, provider,
       provider_order_id, provider_payment_id,
       failure_reason, paid_at,
       refunded_amount, refunded_at, refund_reason, refund_error,
       refund_attempts, next_refund_at, refund_notify, review_reason,
       version, created_at, updated_at
from payments
order by created_at desc
limit 5;

-- 5b. Payment(s) for one booking                                       [READ]
select *
from payments
where booking_id = 'c0a80121-a161-1c84-81a1-616234b10000'::uuid;   -- replace

-- 5c. Latest raw webhook / gateway events                              [READ]
select *
from payment_events
order by received_at desc
limit 5;

-- 5d. Payments that need attention                                     [READ]
select id, booking_reference, status, refund_attempts,
       next_refund_at, refund_error, review_reason, updated_at
from payments
where status in ('REFUND_PENDING', 'REFUND_FAILED')
   or review_reason is not null
order by updated_at desc;


-- -----------------------------------------------------------------------------
-- 5e. FIX: refund stuck in REFUND_PENDING -> make the retry job pick it up now
--                                                               [PROD-WRITE]
-- -----------------------------------------------------------------------------
-- begin;
-- update payments
-- set next_refund_at = now()
-- where id = 'c0a80122-a1ba-1c1e-81a1-bae84c710011'::uuid      -- replace
--   and status = 'REFUND_PENDING';
-- -- expect: UPDATE 1
-- commit;

-- Same, but by booking reference:
-- begin;
-- update payments
-- set next_refund_at = now()
-- where booking_reference = 'BS-9BPETVZD' and status = 'REFUND_PENDING';
-- commit;

-- Retry ALL pending refunds now (check the count with 5d first):
-- begin;
-- update payments set next_refund_at = now() where status = 'REFUND_PENDING';
-- commit;


-- -----------------------------------------------------------------------------
-- 5f. Force-complete failed MOCK refunds (mock gateway testing only)    [DEV]
-- -----------------------------------------------------------------------------
-- begin;
-- update payments
-- set status          = 'REFUNDED',
--     refunded_amount = amount,
--     refunded_at     = now(),
--     refund_error    = null
-- where provider = 'MOCK' and status = 'REFUND_FAILED';
-- commit;


-- -----------------------------------------------------------------------------
-- 5g. Delete one payment row                                            [DEV]
-- -----------------------------------------------------------------------------
-- begin;
-- delete from payments
-- where id = 'c0a80122-a1ba-1c1e-81a1-bab1a6f6000d'::uuid;      -- replace
-- -- expect: DELETE 1
-- commit;


-- =============================================================================
-- 6. EARNINGS                                                      [ONE-TIME]
-- =============================================================================

-- 6a. Backfill earnings_entries for bookings made before the earnings module
--     existed. Safe to re-run: NOT EXISTS skips bookings that already have a row.
--     Commission: 10%.   Run on: dev ____   prod ____
-- begin;
-- insert into earnings_entries (
--     id, booking_id, booking_reference, host_id, listing_id, listing_title,
--     check_in, check_out, currency, gross, refunded,
--     commission_percent, commission, host_amount,
--     status, earned_at, version, created_at, updated_at)
-- select
--     gen_random_uuid(), b.id, b.booking_ref, b.host_id, b.listing_id, b.listing_title,
--     b.check_in, b.check_out, b.currency, b.total_amount, 0,
--     10,
--     round(b.total_amount::numeric * 0.10, 2),
--     b.total_amount - round(b.total_amount::numeric * 0.10, 2),
--     case when b.check_out < current_date then 'EARNED' else 'PENDING' end,
--     case when b.check_out < current_date then now() end,     -- null otherwise
--     0, b.created_at, now()
-- from bookings b
-- where b.status in ('CONFIRMED', 'COMPLETED')
--   and not exists (select 1 from earnings_entries e where e.booking_id = b.id);
-- -- check the INSERT count, then:
-- commit;

-- 6b. Fill sold_on (IST calendar date of the sale) where it is missing.
--     Run on: dev ____   prod ____
-- begin;
-- update earnings_entries
-- set sold_on = (created_at at time zone 'Asia/Kolkata')::date
-- where sold_on is null;
-- commit;
--
-- If created_at is a plain `timestamp` holding UTC (not timestamptz), use:
--     set sold_on = ((created_at at time zone 'UTC') at time zone 'Asia/Kolkata')::date


-- =============================================================================
-- 7. SCHEMA CHANGES                                    [ONE-TIME / PROD-WRITE]
-- =============================================================================
-- With ddl-auto=update Hibernate ADDS columns and tables but never drops
-- columns or old CHECK constraints. These are the manual follow-ups.

-- 7a. Drop a column that was removed from the entity
-- alter table listing_rooms drop column if exists available_rooms;

-- 7b. Drop an old enum CHECK constraint after adding a new enum value.
--     Postgres names them <table>_<column>_check (MySQL used payments_chk_1).
--     Confirm the real name with the query in section 2 first.
-- alter table payments      drop constraint if exists payments_status_check;
-- alter table notifications drop constraint if exists notifications_type_check;


-- =============================================================================
-- 8. MONITORING: CONNECTIONS, TRANSACTIONS, LOCKS                      [READ]
-- =============================================================================

-- 8a. Who is connected and what are they running (replaces SHOW PROCESSLIST)
select pid, usename, application_name, client_addr, state,
       now() - query_start as running_for,
       left(query, 120)    as query
from pg_stat_activity
where datname = 'blinkstay'
  and pid <> pg_backend_pid()
order by query_start nulls last;

-- 8b. Open transactions, oldest first (replaces information_schema.innodb_trx).
--     'idle in transaction' for a long time = someone forgot to COMMIT.
select pid, usename, state, xact_start,
       now() - xact_start as open_for,
       left(query, 120)   as last_query
from pg_stat_activity
where datname = 'blinkstay'
  and xact_start is not null
order by xact_start;

-- 8c. Who is blocked and by whom (closest to SHOW ENGINE INNODB STATUS for locks)
select pid,
       pg_blocking_pids(pid) as blocked_by,
       wait_event_type, wait_event,
       now() - query_start   as waiting_for,
       left(query, 120)      as query
from pg_stat_activity
where cardinality(pg_blocking_pids(pid)) > 0;

-- 8d. Inspect specific sessions (replaces processlist WHERE id IN (...))
select pid, usename, state, now() - query_start as running_for, query
from pg_stat_activity
where pid in (20, 52);                                             -- replace

-- 8e. Stop a stuck session                                      [PROD-WRITE]
-- select pg_cancel_backend(52);     -- cancel its current query only (gentle)
-- select pg_terminate_backend(52);  -- close the whole connection, rolls back its transaction


-- =============================================================================
-- 9. STORAGE & ROW COUNTS                                              [READ]
-- =============================================================================

-- 9a. Total database size
select pg_size_pretty(pg_database_size('blinkstay')) as total_size;

-- 9b. Size per table: data vs indexes, biggest first
select relname                                        as table_name,
       pg_size_pretty(pg_total_relation_size(relid))  as total,
       pg_size_pretty(pg_relation_size(relid))        as data,
       pg_size_pretty(pg_indexes_size(relid))         as indexes
from pg_catalog.pg_statio_user_tables
order by pg_total_relation_size(relid) desc;

-- 9c. Approximate row count per table (fast; refreshed by autovacuum/ANALYZE).
--     Compare against the MySQL version after a migration.
select relname as table_name, n_live_tup as approx_rows
from pg_stat_user_tables
order by relname;

-- 9d. Exact counts for the core tables (slower, exact)
select 'users' as table_name, count(*) from users
union all select 'listings',         count(*) from listings
union all select 'listing_rooms',    count(*) from listing_rooms
union all select 'bookings',         count(*) from bookings
union all select 'payments',         count(*) from payments
union all select 'payment_events',   count(*) from payment_events
union all select 'earnings_entries', count(*) from earnings_entries
union all select 'notifications',    count(*) from notifications;


-- =============================================================================
-- 10. TIME & TIME ZONE CHECKS                                          [READ]
-- =============================================================================

-- Server time in UTC, in the session zone, and the session zone itself
-- (replaces SELECT utc_timestamp(6), now(6))
select now() at time zone 'UTC'          as utc_now,
       now()                             as now_with_tz,
       now() at time zone 'Asia/Kolkata' as ist_now,
       current_setting('TimeZone')       as session_tz;

-- Are the *_at columns timestamptz or plain timestamp?
-- 'timestamp with time zone' -> now() is correct in updates (used above)
-- 'timestamp without time zone' -> use (now() at time zone 'UTC') instead
select table_name, column_name, data_type
from information_schema.columns
where table_schema = 'public'
  and table_name in ('payments', 'bookings', 'earnings_entries')
  and column_name like '%\_at'
order by table_name, column_name;