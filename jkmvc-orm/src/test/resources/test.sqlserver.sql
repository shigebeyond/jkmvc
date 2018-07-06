-- 用户表
create table "user"
(
	id int identity(1,1),
	name varchar(50) unique,
	avatar varchar(50) default '',
	age int default 0
);

-- 地址表
create table address
(
	id int identity(1,1),
	user_id int default 0 ,
	addr varchar(50) unique,
	tel varchar(50) default ''
);
