-- =============================================================================
--  BlinkStay — MySQL Operations Runbook  (legacy, kept for reference)
-- =============================================================================
--  Database : blinkstay
--  Engine   : MySQL 8.0+  (BIN_TO_UUID / UUID_TO_BIN, sys schema)
--
--  ENVIRONMENT TAGS used on every block:
--    [DEV]        Local / development only. Destructive or test-data changes.
--    [READ]       Read-only. Safe to run anywhere, including production.
--    [PROD-WRITE] Changes data in production. Use START TRANSACTION ... COMMIT,
--                 check rows affected, ROLLBACK if it is not what you expected.
--    [ONE-TIME]   Migration / backfill. Run once per environment.
--
--  NOTES:
--    * UUID columns are binary(16). Wrap ids with BIN_TO_UUID() to read them and
--      UUID_TO_BIN('...') to filter on them. Comparing a binary(16) column to a
--      plain string ('c0a8...') matches nothing.
--    * A comment needs a space after the dashes: `-- note`. `--note` is NOT a
--      comment in MySQL and causes a syntax error. `#` also works.
-- =============================================================================


-- =============================================================================
-- 0. CONNECTION & SESSION                                              [READ]
-- =============================================================================

use blinkstay;

select database(), current_user(), @@session.time_zone, @@global.time_zone;


-- =============================================================================
-- 1. DATABASE CREATE / DROP                                             [DEV]
-- =============================================================================

-- create database if not exists blinkstay;

-- !! DESTRUCTIVE — deletes the whole database
-- drop database if exists blinkstay;


-- =============================================================================
-- 2. SCHEMA INSPECTION                                                 [READ]
-- =============================================================================

show tables;
show tables like '%booking%';
describe bookings;
show create table payments;

show indexes from listings;
show indexes from listing_geometry;
show indexes from listing_amenities;
show indexes from listing_images;
show indexes from listing_rooms;
show indexes from blocked_token;

-- CHECK constraints on a table (Hibernate creates these for enum columns)
select constraint_name, check_clause
from information_schema.check_constraints
where constraint_schema = 'blinkstay'
  and constraint_name like 'payments%';


-- =============================================================================
-- 3. QUICK DATA PEEKS                                                  [READ]
-- =============================================================================

select * from users limit 50;
select * from user_roles limit 50;
select * from blocked_token limit 50;

-- Users with readable ids
select BIN_TO_UUID(id) as id, username from users order by username;

select * from listings          limit 50;
select * from listing_amenities limit 50;
select * from listing_geometry  limit 50;
select * from listing_images    limit 50;
select * from listing_rooms     limit 50;

select * from notifications    order by created_at desc limit 50;
select * from earnings_entries order by created_at desc limit 50;


-- =============================================================================
-- 4. BOOKINGS                                                          [READ]
-- =============================================================================

select * from bookings order by created_at desc limit 10;

-- Bookings created today (IST calendar day; created_at stored in UTC)
select *
from bookings
where date(convert_tz(created_at, '+00:00', '+05:30'))
    = date(convert_tz(utc_timestamp(), '+00:00', '+05:30'))
order by created_at desc;

select booking_ref, created_at, confirmed_at,
       timediff(confirmed_at, created_at) as time_to_confirm
from bookings
order by created_at desc
limit 1;

select * from bookings where booking_ref = 'BS-KJEKSLPT';          -- replace

-- Latest bookings with guest username
select b.booking_ref, b.status, b.check_in, b.check_out, b.total_amount,
       u.username as guest, b.created_at
from bookings b
join users u on u.id = b.guest_id
order by b.created_at desc
limit 3;

select status, count(*) from bookings group by status order by status;


-- -----------------------------------------------------------------------------
-- 4b. Move a confirmed booking's dates (testing timers)                 [DEV]
-- -----------------------------------------------------------------------------
-- start transaction;
-- update bookings
-- set check_in = '2026-10-10', check_out = '2026-10-12'
-- where booking_ref = 'BS-SCEB9X89' and status = 'CONFIRMED';
-- commit;


-- =============================================================================
-- 5. PAYMENTS & REFUNDS
-- =============================================================================

-- 5a. Latest payments, binary ids made readable                        [READ]
select
    BIN_TO_UUID(id)         as id,
    BIN_TO_UUID(booking_id) as booking_id,
    booking_reference,
    BIN_TO_UUID(guest_id)   as guest_id,
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
where booking_id = UUID_TO_BIN('c0a80121-a161-1c84-81a1-616234b10000');   -- replace

-- 5c. Latest raw gateway events                                        [READ]
select * from payment_events order by received_at desc limit 5;

-- 5d. Payments that need attention                                     [READ]
select BIN_TO_UUID(id) as id, booking_reference, status, refund_attempts,
       next_refund_at, refund_error, review_reason, updated_at
from payments
where status in ('REFUND_PENDING', 'REFUND_FAILED')
   or review_reason is not null
order by updated_at desc;


-- -----------------------------------------------------------------------------
-- 5e. FIX: refund stuck in REFUND_PENDING                        [PROD-WRITE]
-- -----------------------------------------------------------------------------
-- start transaction;
-- update payments
-- set next_refund_at = utc_timestamp(6)
-- where id = UUID_TO_BIN('c0a80122-a1ba-1c1e-81a1-bae84c710011')   -- replace
--   and status = 'REFUND_PENDING';
-- commit;

-- By booking reference:
-- update payments set next_refund_at = utc_timestamp(6)
-- where booking_reference = 'BS-9BPETVZD' and status = 'REFUND_PENDING';

-- All pending refunds:
-- update payments set next_refund_at = utc_timestamp(6) where status = 'REFUND_PENDING';


-- -----------------------------------------------------------------------------
-- 5f. Force-complete failed MOCK refunds                                [DEV]
-- -----------------------------------------------------------------------------
-- update payments
-- set status = 'REFUNDED', refunded_amount = amount,
--     refunded_at = utc_timestamp(6), refund_error = null
-- where provider = 'MOCK' and status = 'REFUND_FAILED';


-- -----------------------------------------------------------------------------
-- 5g. Delete one payment row                                            [DEV]
-- -----------------------------------------------------------------------------
-- delete from payments
-- where id = UUID_TO_BIN('c0a80122-a1ba-1c1e-81a1-bab1a6f6000d');   -- replace


-- =============================================================================
-- 6. EARNINGS                                                      [ONE-TIME]
-- =============================================================================

-- 6a. Backfill earnings for bookings made before the earnings module (10%)
-- start transaction;
-- insert into earnings_entries (
--     id, booking_id, booking_reference, host_id, listing_id, listing_title,
--     check_in, check_out, currency, gross, refunded,
--     commission_percent, commission, host_amount,
--     status, earned_at, version, created_at, updated_at)
-- select
--     UUID_TO_BIN(uuid()), b.id, b.booking_ref, b.host_id, b.listing_id, b.listing_title,
--     b.check_in, b.check_out, b.currency, b.total_amount, 0,
--     10, round(b.total_amount * 0.10, 2),
--     b.total_amount - round(b.total_amount * 0.10, 2),
--     if(b.check_out < curdate(), 'EARNED', 'PENDING'),
--     if(b.check_out < curdate(), utc_timestamp(6), null),
--     0, b.created_at, utc_timestamp(6)
-- from bookings b
-- where b.status in ('CONFIRMED', 'COMPLETED')
--   and not exists (select 1 from earnings_entries e where e.booking_id = b.id);
-- commit;

-- 6b. Fill sold_on (IST date) where missing
-- update earnings_entries
-- set sold_on = date(convert_tz(created_at, '+00:00', '+05:30'))
-- where sold_on is null;


-- =============================================================================
-- 7. SCHEMA CHANGES                                    [ONE-TIME / PROD-WRITE]
-- =============================================================================

-- alter table listing_rooms drop column available_rooms;

-- Old enum CHECK constraints (find names with the query in section 2)
-- alter table payments      drop check payments_chk_1;
-- alter table notifications drop check notifications_chk_1;


-- =============================================================================
-- 8. MONITORING: CONNECTIONS, TRANSACTIONS, LOCKS                      [READ]
-- =============================================================================

show processlist;

-- `user` and `time` are keywords, so the columns are prefixed with the alias p.
select p.id, p.user, p.host, p.db, p.command, p.time, p.state, p.info
from information_schema.processlist p
where p.id in (20, 52);                                             -- replace

-- Open transactions, oldest first
select trx_id, trx_mysql_thread_id, trx_state, trx_started,
       timestampdiff(second, trx_started, now()) as open_seconds
from information_schema.innodb_trx
order by trx_started;

-- Who is blocking whom
select * from sys.innodb_lock_waits;

-- Full InnoDB engine report (deadlocks, lock waits)
show engine innodb status;

-- Stop a stuck session                                          [PROD-WRITE]
-- kill query 52;   -- cancel its current query only
-- kill 52;         -- close the connection

-- End your own open transaction if you forgot
-- commit;


-- =============================================================================
-- 9. STORAGE & ROW COUNTS                                              [READ]
-- =============================================================================

select
    table_schema                                              as database_name,
    round(sum(data_length + index_length) / 1024 / 1024, 2)   as total_mb,
    round(sum(data_length)  / 1024 / 1024, 2)                 as data_mb,
    round(sum(index_length) / 1024 / 1024, 2)                 as index_mb
from information_schema.tables
where table_schema = 'blinkstay'
group by table_schema;

-- Per table, with approximate rows (compare with Postgres after migrating)
select table_name, table_rows as approx_rows,
       round((data_length + index_length) / 1024 / 1024, 2) as total_mb
from information_schema.tables
where table_schema = 'blinkstay'
order by table_name;

-- Exact counts for the core tables
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

select utc_timestamp(6) as utc_now, now(6) as session_now, @@session.time_zone;