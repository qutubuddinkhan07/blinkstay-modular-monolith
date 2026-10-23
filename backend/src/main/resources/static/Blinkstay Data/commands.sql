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