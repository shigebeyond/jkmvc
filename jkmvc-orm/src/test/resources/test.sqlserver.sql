-- 用户表
create table "user"
(
	id int identity(1,1),
	name varchar(50),
	avatar varchar(50) default '',
	age int default 0
);

-- 地址表
create table address
(
	id int identity(1,1),
	user_id int default 0,
	addr varchar(50),
	tel varchar(50) default '',
	is_home int default 0
);

-- 包裹表
create table parcel
(
	id int identity(1,1),
	sender_id int default 0 ,
	receiver_id int default 0 ,
	content varchar(50) default ''
);

-- 消息表
create table "message"
(
    id int identity(1,1),
    from_uid int default 0 ,
    to_uid int default 0 ,
    content varchar(50) default ''
);