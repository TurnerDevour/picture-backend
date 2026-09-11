-- 创建数据库
create database if not exists picture_db default character set utf8mb4 default collate utf8mb4_general_ci;

-- 使用数据库
use picture_db;

-- 用户表
create table if not exists user
(
    id             bigint auto_increment comment '用户 ID' primary key,
    user_account   varchar(255)                           not null comment '账号',
    user_password  varchar(255)                           not null comment '密码',
    username       varchar(255)                           not null comment '用户昵称',
    user_avatar    varchar(1024)                          null comment '用户头像',
    user_profile   varchar(512)                           null comment '用户简介',
    user_role      varchar(255) default 'user'            not null comment '用户角色（角色：user，admin）',
    edit_time      datetime     default current_timestamp not null comment '编辑时间',
    create_time    datetime     default current_timestamp not null comment '创建时间',
    update_time    datetime     default current_timestamp not null comment '更新时间',
    is_delete      tinyint      default 0                 null comment '是否删除（0：未删除，1：已删除）',
    account_active VARCHAR(256) GENERATED ALWAYS AS (IF(is_delete = 0, user_account, NULL)) STORED,
    UNIQUE KEY uk_account_active (account_active),
    INDEX idx_user_account (user_account)
) engine = InnoDB comment '用户表'
  collate utf8mb4_general_ci;

-- 图片表
create table if not exists picture
(
    id           bigint auto_increment comment 'id' primary key,
    url          varchar(512)                       not null comment '图片 url',
    name         varchar(128)                       not null comment '图片名称',
    introduction varchar(512)                       null comment '简介',
    category     varchar(64)                        null comment '分类',
    tags         varchar(512)                       null comment '标签（JSON 数组）',
    pic_size     bigint                             null comment '图片体积',
    pic_width    int                                null comment '图片宽度',
    pic_height   int                                null comment '图片高度',
    pic_scale    double                             null comment '图片宽高比例',
    pic_format   varchar(32)                        null comment '图片格式',
    user_id      bigint                             not null comment '创建用户 id',
    create_time  datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    edit_time    datetime default CURRENT_TIMESTAMP not null comment '编辑时间',
    update_time  datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    is_delete    tinyint  default 0                 not null comment '是否删除',
    INDEX idx_name (name),                 -- 提升基于图片名称的查询性能
    INDEX idx_introduction (introduction), -- 用于模糊搜索图片简介
    INDEX idx_category (category),         -- 提升基于分类的查询性能
    INDEX idx_tags (tags),                 -- 提升基于标签的查询性能
    INDEX idx_user_id (user_id)            -- 提升基于用户 ID 的查询性能
) engine = InnoDB comment '图片'
  collate utf8mb4_general_ci;
