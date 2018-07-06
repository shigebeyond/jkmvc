-- 用户表
create table "user"
(
	id int primary key ,
	name varchar(50) unique,
	avatar varchar(50) default '',
	age int default 0
);

-- 地址表
create table address
(
	id int primary key ,
	user_id int default 0 ,
	addr varchar(50) unique,
	tel varchar(50) default ''
);
