show processlist;

show engine innodb status;

create database if not exists blinkstay;
drop database blinkstay;

use blinkstay;
select * from users;
select * from user_roles;
select * from listing_amenities;
select * from listing_geometry;
select * from listing_images;
select * from listing_rooms;
select * from listings;
select * from blocked_token;

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
