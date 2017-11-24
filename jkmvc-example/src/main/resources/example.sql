# 用户表
CREATE TABLE IF NOT EXISTS `user` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT COMMENT '用户id',
  `username` varchar(50) NOT NULL DEFAULT '' COMMENT '用户名',
  `password` varchar(50) NOT NULL DEFAULT '' COMMENT '密码',
  `name` varchar(50) NOT NULL DEFAULT '' COMMENT '中文名',
  `age` tinyint(4) unsigned NOT NULL DEFAULT '0' COMMENT '年龄',
  `avatar` varchar(250) DEFAULT NULL COMMENT '头像',
  PRIMARY KEY (`id`)
)ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='用户';

# 地址表
CREATE TABLE IF NOT EXISTS `address` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT COMMENT '地址id',
  `user_id` int(11) unsigned NOT NULL COMMENT '用户id',
  `addr` varchar(50) NOT NULL DEFAULT '' COMMENT '地址',
  `tel` varchar(50) NOT NULL DEFAULT '' COMMENT '电话',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=30 DEFAULT CHARSET=utf8 COMMENT='地址';

# 包裹表
CREATE TABLE IF NOT EXISTS `parcel` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT COMMENT '包裹id',
  `sender_id` int(11) unsigned NOT NULL COMMENT '寄件人id',
  `receiver_id` int(11) unsigned NOT NULL COMMENT '收件人id',
  `content` varchar(50) NOT NULL DEFAULT '' COMMENT '寄件内容',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=30 DEFAULT CHARSET=utf8 COMMENT='包裹';