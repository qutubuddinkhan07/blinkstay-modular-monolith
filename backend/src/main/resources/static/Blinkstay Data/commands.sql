show processlist;

show engine innodb status;

create database if not exists blinkstay;
drop database blinkstay;

use blinkstay;

show tables;

--alter table notifications drop check notifications_chk_1;

select * from users;
select * from user_roles;
select * from listing_amenities;
select * from listing_geometry;
select * from listing_images;
select * from listing_rooms;
select * from listings;
select * from blocked_token;
select * from payment_events;
select * from listing_rooms;

select * from payments
order by created_at desc
limit 3;

--UPDATE payments
--SET next_refund_at = UTC_TIMESTAMP(6)
--WHERE status = 'REFUND_PENDING';

update payments set status = 'REFUNDED', refunded_amount = amount,
       refunded_at = utc_timestamp(6), refund_error = null
where provider = 'MOCK' and status = 'REFUND_FAILED';

select * from bookings
order by created_at desc;

select * from bookings
where date(created_at) = curdate();

select booking_ref, created_at, confirmed_at from bookings order by created_at desc limit 1;

select * from blinkstay.users;

select BIN_TO_UUID(id) as readable_id, username from blinkstay.users;

show indexes from listings;
show indexes from listing_geometry;
show indexes from listing_amenities;
show indexes from listing_images;
show indexes from listing_rooms;
show indexes from blocked_token;

show tables;
#drop table users;
#drop table user_roles;
#drop table blocked_token;

select * from payments
where booking_id = UUID_TO_BIN('c0a80121-a161-1c84-81a1-616234b10000');

select id, booking_reference, status, host_id, listing_id
from payments order by created_at desc limit 5;

select * from bookings;

show tables like '%booking%';
describe bookings;   -- use the table name from the line above
select * from bookings order by created_at desc limit 3;

update bookings set check_in = '2026-10-10', check_out = '2026-10-12'
where booking_ref = 'BS-SCEB9X89' and status = 'CONFIRMED';


select trx_id, trx_started, trx_state, trx_mysql_thread_id
from information_schema.innodb_trx;

--SELECT id, user, host, db, command, time, state, info
--FROM information_schema.processlist
--WHERE id IN (20, 52);

alter table listing_rooms drop column available_rooms;

show create table payments;
alter table payments drop check payments_chk_1;

commit;

-- to check whether any transaction holding anything
select trx_mysql_thread_id, trx_started, trx_state
from information_schema.innodb_trx;