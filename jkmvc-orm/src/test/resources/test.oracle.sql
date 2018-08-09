-- 用户表
CREATE TABLE "USER" (
    "ID" NUMBER(10) NOT NULL,
    "name" VARCHAR2(50 BYTE) NULL ,
    "AGE" NUMBER(10) NULL ,
    "AVATAR" VARCHAR2(50 BYTE) NULL
);

ALTER TABLE "USER" ADD CHECK ("ID" IS NOT NULL);
ALTER TABLE "USER" ADD PRIMARY KEY ("ID");

-- 地址表
CREATE TABLE "ADDRESS" (
    "ID" NUMBER(10) NOT NULL,
    "USER_ID" NUMBER(10) NOT NULL,
    "ADDR" VARCHAR2(50 BYTE) NULL ,
    "TEL" VARCHAR2(50 BYTE) NULL
);

ALTER TABLE "ADDRESS" ADD CHECK ("ID" IS NOT NULL);
ALTER TABLE "ADDRESS" ADD PRIMARY KEY ("ID");