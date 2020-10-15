-- 用户表
CREATE TABLE IF NOT EXISTS `user` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT COMMENT '用户编号',
  `name` varchar(50) NOT NULL DEFAULT '' COMMENT '用户名',
  `age` tinyint(4) unsigned NOT NULL DEFAULT '0' COMMENT '年龄',
  `avatar` varchar(250) DEFAULT NULL COMMENT '头像',
  PRIMARY KEY (`id`)
)ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='用户';

-- 地址表
CREATE TABLE IF NOT EXISTS `address` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT COMMENT '地址编号',
  `user_id` int(11) unsigned NOT NULL DEFAULT '0' COMMENT '用户编号',
  `addr` varchar(50) NOT NULL DEFAULT '' COMMENT '地址',
  `tel` varchar(50) NOT NULL DEFAULT '' COMMENT '电话',
  `is_home` tinyint(4) unsigned NOT NULL DEFAULT '0' COMMENT '是否是家庭住址',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='地址';

-- 包裹表
CREATE TABLE `parcel` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT COMMENT '包裹id',
  `sender_id` int(11) unsigned NOT NULL DEFAULT '0' COMMENT '寄件人id',
  `receiver_id` int(11) unsigned NOT NULL DEFAULT '0' COMMENT '收件人id',
  `content` varchar(50) NOT NULL DEFAULT '' COMMENT '寄件内容',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='包裹';

-- 消息表
CREATE TABLE `message` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT COMMENT '消息id',
  `from_uid` int(11) unsigned NOT NULL DEFAULT '0' COMMENT '发送人id',
  `to_uid` int(11) unsigned NOT NULL DEFAULT '0' COMMENT '接收人id',
  `content` varchar(50) NOT NULL DEFAULT '' COMMENT '消息内容',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='消息';
