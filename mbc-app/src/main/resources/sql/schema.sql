-- ============================================================
-- 智慧零售会员权益中心 - 数据库初始化脚本
-- 数据库: MySQL 8.0+
-- ============================================================
CREATE DATABASE IF NOT EXISTS `mbc_center` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `mbc_center`;

-- ============================================================
-- 1. 会员表 - 存储会员基础信息
-- ============================================================
DROP TABLE IF EXISTS `t_member`;
CREATE TABLE `t_member` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `member_code`     VARCHAR(32)  NOT NULL COMMENT '会员码（唯一）',
    `phone`           VARCHAR(20)  NOT NULL COMMENT '手机号',
    `name`            VARCHAR(64)  DEFAULT NULL COMMENT '姓名',
    `nickname`        VARCHAR(64)  DEFAULT NULL COMMENT '昵称',
    `gender`          TINYINT      DEFAULT 0 COMMENT '性别：0未知 1男 2女',
    `birthday`        DATE         DEFAULT NULL COMMENT '生日',
    `avatar`          VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
    `level_code`      INT          NOT NULL DEFAULT 1 COMMENT '当前等级编码',
    `growth_value`    INT          NOT NULL DEFAULT 0 COMMENT '累计成长值',
    `current_points`  INT          NOT NULL DEFAULT 0 COMMENT '当前可用积分',
    `total_points`    INT          NOT NULL DEFAULT 0 COMMENT '累计获得积分',
    `register_source` VARCHAR(32)  DEFAULT 'POS' COMMENT '注册来源：POS/MINI_APP/CUSTOMER/ADMIN',
    `status`          TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：0禁用 1正常',
    `merged_to`       BIGINT       DEFAULT NULL COMMENT '合并到的目标会员ID',
    `remark`          VARCHAR(255) DEFAULT NULL COMMENT '备注',
    `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`       VARCHAR(64)  DEFAULT 'system' COMMENT '创建人',
    `update_by`       VARCHAR(64)  DEFAULT 'system' COMMENT '更新人',
    `is_deleted`      TINYINT      NOT NULL DEFAULT 0 COMMENT '是否删除：0否 1是',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_member_code` (`member_code`),
    KEY `idx_phone` (`phone`),
    KEY `idx_level_code` (`level_code`),
    KEY `idx_merged_to` (`merged_to`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会员主表';

-- ============================================================
-- 2. 会员等级规则表
-- ============================================================
DROP TABLE IF EXISTS `t_level_rule`;
CREATE TABLE `t_level_rule` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT,
    `level_code`      INT          NOT NULL COMMENT '等级编码',
    `level_name`      VARCHAR(32)  NOT NULL COMMENT '等级名称',
    `growth_threshold`INT          NOT NULL DEFAULT 0 COMMENT '成长值门槛',
    `growth_ratio`    DECIMAL(5,2) NOT NULL DEFAULT 1.00 COMMENT '消费成长值倍率',
    `point_ratio`     DECIMAL(5,2) NOT NULL DEFAULT 1.00 COMMENT '消费积分倍率',
    `discount_rate`   DECIMAL(5,2) NOT NULL DEFAULT 10.00 COMMENT '折扣率（10.00=不打折，9.50=95折）',
    `birthday_points` INT          NOT NULL DEFAULT 0 COMMENT '生日赠送积分',
    `birthday_coupon_id` BIGINT    DEFAULT NULL COMMENT '生日赠送优惠券ID',
    `benefit_desc`    VARCHAR(500) DEFAULT NULL COMMENT '等级权益描述',
    `icon`            VARCHAR(255) DEFAULT NULL COMMENT '等级图标',
    `status`          TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：0禁用 1启用',
    `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `create_by`       VARCHAR(64)  DEFAULT 'system',
    `update_by`       VARCHAR(64)  DEFAULT 'system',
    `is_deleted`      TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_level_code` (`level_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='等级规则表';

-- ============================================================
-- 3. 等级成长值流水表
-- ============================================================
DROP TABLE IF EXISTS `t_growth_log`;
CREATE TABLE `t_growth_log` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT,
    `member_id`       BIGINT       NOT NULL COMMENT '会员ID',
    `change_value`    INT          NOT NULL COMMENT '变更值（正为增，负为减）',
    `before_value`    INT          NOT NULL COMMENT '变更前成长值',
    `after_value`     INT          NOT NULL COMMENT '变更后成长值',
    `before_level`    INT          NOT NULL COMMENT '变更前等级',
    `after_level`     INT          NOT NULL COMMENT '变更后等级',
    `source_type`     TINYINT      NOT NULL COMMENT '来源：1消费 2活动 3调整 4退款扣回',
    `source_id`       VARCHAR(64)  DEFAULT NULL COMMENT '关联业务ID（订单号等）',
    `remark`          VARCHAR(255) DEFAULT NULL,
    `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `create_by`       VARCHAR(64)  DEFAULT 'system',
    `is_deleted`      TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_member_id` (`member_id`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='成长值流水表';

-- ============================================================
-- 4. 积分账户流水表
-- ============================================================
DROP TABLE IF EXISTS `t_point_log`;
CREATE TABLE `t_point_log` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT,
    `member_id`       BIGINT       NOT NULL,
    `point_type`      TINYINT      NOT NULL COMMENT '积分类型：1增加 2扣减 3冻结 4解冻',
    `change_points`   INT          NOT NULL COMMENT '变更积分数',
    `before_points`   INT          NOT NULL COMMENT '变更前可用积分',
    `after_points`    INT          NOT NULL COMMENT '变更后可用积分',
    `frozen_points`   INT          NOT NULL DEFAULT 0 COMMENT '当前冻结积分',
    `source_type`     TINYINT      NOT NULL COMMENT '来源：1消费 2签到 3生日 4注册 5退款返还 6后台调整',
    `source_id`       VARCHAR(64)  DEFAULT NULL COMMENT '关联业务ID',
    `expire_time`     DATETIME     DEFAULT NULL COMMENT '积分过期时间',
    `remark`          VARCHAR(255) DEFAULT NULL,
    `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `create_by`       VARCHAR(64)  DEFAULT 'system',
    `is_deleted`      TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_member_id` (`member_id`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_expire_time` (`expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='积分流水表';

-- ============================================================
-- 5. 优惠券模板表
-- ============================================================
DROP TABLE IF EXISTS `t_coupon_template`;
CREATE TABLE `t_coupon_template` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT,
    `coupon_code`     VARCHAR(64)  NOT NULL COMMENT '优惠券编码',
    `coupon_name`     VARCHAR(128) NOT NULL COMMENT '优惠券名称',
    `coupon_type`     TINYINT      NOT NULL COMMENT '类型：1满减券 2兑换券',
    `total_amount`    INT          NOT NULL DEFAULT -1 COMMENT '发放总量：-1=不限量',
    `received_count`  INT          NOT NULL DEFAULT 0 COMMENT '已领取数量',
    `used_count`      INT          NOT NULL DEFAULT 0 COMMENT '已使用数量',
    `full_amount`     DECIMAL(10,2) DEFAULT NULL COMMENT '满减门槛金额（满减券用）',
    `reduce_amount`   DECIMAL(10,2) DEFAULT NULL COMMENT '减免金额（满减券用）',
    `exchange_item`   VARCHAR(255) DEFAULT NULL COMMENT '兑换商品/内容（兑换券用）',
    `valid_type`      TINYINT      NOT NULL DEFAULT 1 COMMENT '有效期类型：1固定起止 2领取后N天',
    `valid_start`     DATETIME     DEFAULT NULL COMMENT '有效期开始（valid_type=1）',
    `valid_end`       DATETIME     DEFAULT NULL COMMENT '有效期结束（valid_type=1）',
    `valid_days`      INT          DEFAULT NULL COMMENT '领取后有效天数（valid_type=2）',
    `min_level`       INT          NOT NULL DEFAULT 0 COMMENT '最低领取等级：0=不限',
    `daily_limit`     INT          NOT NULL DEFAULT 0 COMMENT '每日限领次数：0=不限制',
    `total_limit`     INT          NOT NULL DEFAULT 0 COMMENT '每人限领总数：0=不限制',
    `apply_scenes`    VARCHAR(255) DEFAULT NULL COMMENT '适用场景：ALL/线上/线下/指定门店（逗号分隔）',
    `exclude_items`   TEXT         DEFAULT NULL COMMENT '排除商品ID列表（JSON数组）',
    `stackable`       TINYINT      NOT NULL DEFAULT 0 COMMENT '是否可叠加：0否 1是',
    `description`     VARCHAR(500) DEFAULT NULL COMMENT '使用说明',
    `status`          TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：0下架 1上架 2过期',
    `activity_id`     BIGINT       DEFAULT NULL COMMENT '关联活动ID',
    `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `create_by`       VARCHAR(64)  DEFAULT 'system',
    `update_by`       VARCHAR(64)  DEFAULT 'system',
    `is_deleted`      TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_coupon_code` (`coupon_code`),
    KEY `idx_status` (`status`),
    KEY `idx_coupon_type` (`coupon_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='优惠券模板表';

-- ============================================================
-- 6. 用户优惠券实例表
-- ============================================================
DROP TABLE IF EXISTS `t_coupon_instance`;
CREATE TABLE `t_coupon_instance` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT,
    `instance_no`     VARCHAR(64)  NOT NULL COMMENT '券实例编号（唯一）',
    `template_id`     BIGINT       NOT NULL COMMENT '优惠券模板ID',
    `member_id`       BIGINT       NOT NULL COMMENT '所属会员ID',
    `coupon_status`   TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：0未开始 1可使用 2已使用 3已过期 4已锁定 5已失效',
    `valid_start`     DATETIME     NOT NULL COMMENT '生效时间',
    `valid_end`       DATETIME     NOT NULL COMMENT '过期时间',
    `used_time`       DATETIME     DEFAULT NULL COMMENT '使用时间',
    `used_order_no`   VARCHAR(64)  DEFAULT NULL COMMENT '使用订单号',
    `locked_time`     DATETIME     DEFAULT NULL COMMENT '锁定时间',
    `lock_order_no`   VARCHAR(64)  DEFAULT NULL COMMENT '锁定订单号',
    `receive_source`  VARCHAR(32)  DEFAULT 'ACTIVITY' COMMENT '领取来源：REGISTER/BIRTHDAY/ACTIVITY/ADMIN',
    `receive_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '领取时间',
    `source_id`       VARCHAR(64)  DEFAULT NULL COMMENT '来源关联ID',
    `remark`          VARCHAR(255) DEFAULT NULL,
    `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `create_by`       VARCHAR(64)  DEFAULT 'system',
    `update_by`       VARCHAR(64)  DEFAULT 'system',
    `is_deleted`      TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_instance_no` (`instance_no`),
    KEY `idx_member_id` (`member_id`),
    KEY `idx_template_id` (`template_id`),
    KEY `idx_coupon_status` (`coupon_status`),
    KEY `idx_valid_end` (`valid_end`),
    KEY `idx_member_status` (`member_id`, `coupon_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户优惠券实例表';

-- ============================================================
-- 7. 权益核销记录表
-- ============================================================
DROP TABLE IF EXISTS `t_benefit_use_log`;
CREATE TABLE `t_benefit_use_log` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT,
    `use_no`          VARCHAR(64)  NOT NULL COMMENT '核销单号',
    `member_id`       BIGINT       NOT NULL,
    `benefit_type`    TINYINT      NOT NULL COMMENT '权益类型：1优惠券 2积分抵扣 3等级折扣 4兑换权益',
    `benefit_id`      BIGINT       DEFAULT NULL COMMENT '关联权益ID（券实例ID等）',
    `use_status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1锁定 2核销成功 3已返还',
    `order_no`        VARCHAR(64)  NOT NULL COMMENT '关联订单号',
    `order_amount`    DECIMAL(10,2) DEFAULT NULL COMMENT '订单实付金额',
    `benefit_value`   DECIMAL(10,2) NOT NULL COMMENT '权益价值（减免金额/抵扣金额）',
    `used_points`     INT          DEFAULT NULL COMMENT '使用积分数量',
    `store_code`      VARCHAR(32)  DEFAULT NULL COMMENT '门店编码',
    `pos_code`        VARCHAR(32)  DEFAULT NULL COMMENT '收银机编码',
    `operator`        VARCHAR(64)  DEFAULT NULL COMMENT '操作人',
    `lock_time`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '锁定时间',
    `confirm_time`    DATETIME     DEFAULT NULL COMMENT '确认核销时间',
    `return_time`     DATETIME     DEFAULT NULL COMMENT '返还时间',
    `return_reason`   VARCHAR(255) DEFAULT NULL COMMENT '返还原因',
    `remark`          VARCHAR(255) DEFAULT NULL,
    `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `create_by`       VARCHAR(64)  DEFAULT 'system',
    `update_by`       VARCHAR(64)  DEFAULT 'system',
    `is_deleted`      TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_use_no` (`use_no`),
    KEY `idx_member_id` (`member_id`),
    KEY `idx_order_no` (`order_no`),
    KEY `idx_benefit_type` (`benefit_type`),
    KEY `idx_use_status` (`use_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权益核销记录表';

-- ============================================================
-- 8. 消费订单表（用于校验与统计）
-- ============================================================
DROP TABLE IF EXISTS `t_consume_order`;
CREATE TABLE `t_consume_order` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT,
    `order_no`        VARCHAR(64)  NOT NULL COMMENT '订单号',
    `member_id`       BIGINT       DEFAULT NULL COMMENT '关联会员ID',
    `order_type`      TINYINT      NOT NULL DEFAULT 1 COMMENT '订单类型：1消费 2充值 3退款',
    `order_status`    TINYINT      NOT NULL DEFAULT 0 COMMENT '状态：0待支付 1已支付 2已完成 3已取消 4退款中 5已退款',
    `total_amount`    DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '订单总金额',
    `discount_amount` DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '优惠总金额',
    `coupon_amount`   DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '优惠券抵扣',
    `point_amount`    DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '积分抵扣',
    `level_discount`  DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '等级折扣减免',
    `pay_amount`      DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '实付金额',
    `earned_points`   INT          NOT NULL DEFAULT 0 COMMENT '本单获得积分',
    `earned_growth`   INT          NOT NULL DEFAULT 0 COMMENT '本单获得成长值',
    `used_points`     INT          NOT NULL DEFAULT 0 COMMENT '本单使用积分',
    `used_coupon_ids` VARCHAR(500) DEFAULT NULL COMMENT '使用的券实例ID（逗号分隔）',
    `store_code`      VARCHAR(32)  DEFAULT NULL COMMENT '门店编码',
    `store_name`      VARCHAR(128) DEFAULT NULL COMMENT '门店名称',
    `pos_code`        VARCHAR(32)  DEFAULT NULL COMMENT '收银机编码',
    `cashier`         VARCHAR(64)  DEFAULT NULL COMMENT '收银员',
    `pay_time`        DATETIME     DEFAULT NULL COMMENT '支付时间',
    `complete_time`   DATETIME     DEFAULT NULL COMMENT '完成时间',
    `refund_time`     DATETIME     DEFAULT NULL COMMENT '退款时间',
    `refund_no`       VARCHAR(64)  DEFAULT NULL COMMENT '退款单号',
    `refund_amount`   DECIMAL(10,2) DEFAULT NULL COMMENT '退款金额',
    `channel`         VARCHAR(32)  NOT NULL DEFAULT 'POS' COMMENT '渠道：POS/MINI_APP/ONLINE',
    `remark`          VARCHAR(255) DEFAULT NULL,
    `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `create_by`       VARCHAR(64)  DEFAULT 'system',
    `update_by`       VARCHAR(64)  DEFAULT 'system',
    `is_deleted`      TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    KEY `idx_member_id` (`member_id`),
    KEY `idx_order_status` (`order_status`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_store_code` (`store_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消费订单表';

-- ============================================================
-- 9. 消息触达记录表
-- ============================================================
DROP TABLE IF EXISTS `t_message_log`;
CREATE TABLE `t_message_log` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT,
    `msg_no`          VARCHAR(64)  NOT NULL COMMENT '消息编号',
    `member_id`       BIGINT       NOT NULL,
    `msg_type`        TINYINT      NOT NULL COMMENT '消息类型：1生日权益 2券到期 3积分到期 4等级变更 5领券成功',
    `msg_title`       VARCHAR(255) NOT NULL COMMENT '消息标题',
    `msg_content`     TEXT         NOT NULL COMMENT '消息内容',
    `channel`         VARCHAR(32)  NOT NULL DEFAULT 'INNER' COMMENT '推送渠道：INNER/SMS/WECHAT/APP_PUSH',
    `target`          VARCHAR(128) DEFAULT NULL COMMENT '推送目标（手机号/openid/设备ID）',
    `send_status`     TINYINT      NOT NULL DEFAULT 0 COMMENT '状态：0待发送 1发送成功 2发送失败 3已阅读',
    `retry_count`     TINYINT      NOT NULL DEFAULT 0 COMMENT '重试次数',
    `fail_reason`     VARCHAR(500) DEFAULT NULL COMMENT '失败原因',
    `send_time`       DATETIME     DEFAULT NULL COMMENT '发送时间',
    `read_time`       DATETIME     DEFAULT NULL COMMENT '阅读时间',
    `biz_id`          VARCHAR(64)  DEFAULT NULL COMMENT '业务关联ID（券ID/积分批次等）',
    `biz_data`        TEXT         DEFAULT NULL COMMENT '业务扩展数据(JSON)',
    `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `create_by`       VARCHAR(64)  DEFAULT 'system',
    `update_by`       VARCHAR(64)  DEFAULT 'system',
    `is_deleted`      TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_msg_no` (`msg_no`),
    KEY `idx_member_id` (`member_id`),
    KEY `idx_msg_type` (`msg_type`),
    KEY `idx_send_status` (`send_status`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息触达记录表';

-- ============================================================
-- 10. 会员合并记录表
-- ============================================================
DROP TABLE IF EXISTS `t_member_merge_log`;
CREATE TABLE `t_member_merge_log` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT,
    `merge_no`        VARCHAR(64)  NOT NULL COMMENT '合并编号',
    `source_member_id`BIGINT       NOT NULL COMMENT '被合并的会员ID',
    `target_member_id`BIGINT       NOT NULL COMMENT '保留的会员ID',
    `merged_points`   INT          NOT NULL DEFAULT 0 COMMENT '合并迁移的积分',
    `merged_growth`   INT          NOT NULL DEFAULT 0 COMMENT '合并迁移的成长值',
    `merged_coupons`  INT          NOT NULL DEFAULT 0 COMMENT '合并迁移的优惠券数量',
    `operator`        VARCHAR(64)  NOT NULL DEFAULT 'system' COMMENT '操作人',
    `reason`          VARCHAR(255) DEFAULT NULL COMMENT '合并原因',
    `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `is_deleted`      TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_merge_no` (`merge_no`),
    KEY `idx_source_member` (`source_member_id`),
    KEY `idx_target_member` (`target_member_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会员合并记录表';

-- ============================================================
-- 11. 活动表（用于活动效果统计）
-- ============================================================
DROP TABLE IF EXISTS `t_activity`;
CREATE TABLE `t_activity` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT,
    `activity_code`   VARCHAR(64)  NOT NULL COMMENT '活动编码',
    `activity_name`   VARCHAR(255) NOT NULL COMMENT '活动名称',
    `activity_type`   TINYINT      NOT NULL COMMENT '类型：1发券活动 2积分活动 3等级活动 4生日活动',
    `start_time`      DATETIME     NOT NULL,
    `end_time`        DATETIME     NOT NULL,
    `target_level`    INT          DEFAULT 0 COMMENT '目标等级（0=不限）',
    `budget_points`   INT          NOT NULL DEFAULT 0 COMMENT '预算积分',
    `budget_coupons`  INT          NOT NULL DEFAULT 0 COMMENT '预算券数量',
    `used_points`     INT          NOT NULL DEFAULT 0 COMMENT '消耗积分',
    `used_coupons`    INT          NOT NULL DEFAULT 0 COMMENT '已发券数',
    `exposed_count`   INT          NOT NULL DEFAULT 0 COMMENT '曝光人数',
    `participated_count` INT       NOT NULL DEFAULT 0 COMMENT '参与人数',
    `converted_count` INT          NOT NULL DEFAULT 0 COMMENT '转化人数（用券人数）',
    `driven_order_amount` DECIMAL(15,2) NOT NULL DEFAULT 0 COMMENT '带动消费金额',
    `driven_order_count`   INT     NOT NULL DEFAULT 0 COMMENT '带动订单数',
    `status`          TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：0草稿 1进行中 2已结束 3已取消',
    `rule_config`     TEXT         DEFAULT NULL COMMENT '活动规则配置(JSON)',
    `description`     VARCHAR(1000) DEFAULT NULL,
    `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `create_by`       VARCHAR(64)  DEFAULT 'system',
    `update_by`       VARCHAR(64)  DEFAULT 'system',
    `is_deleted`      TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_activity_code` (`activity_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='活动表';

-- ============================================================
-- 初始化数据
-- ============================================================

-- 初始化等级规则
INSERT INTO `t_level_rule` (`level_code`, `level_name`, `growth_threshold`, `growth_ratio`, `point_ratio`, `discount_rate`, `birthday_points`, `benefit_desc`, `status`) VALUES
(1, '青铜会员', 0, 1.00, 1.00, 10.00, 100, '注册即享，消费1:1累积积分与成长值', 1),
(2, '白银会员', 500, 1.20, 1.20, 9.80, 200, '享98折，消费1.2倍积分与成长值', 1),
(3, '黄金会员', 2000, 1.50, 1.50, 9.50, 500, '享95折，消费1.5倍积分与成长值，专属客服', 1),
(4, '铂金会员', 5000, 2.00, 2.00, 9.00, 1000, '享9折，消费2倍积分与成长值，免费停车2小时', 1),
(5, '钻石会员', 10000, 3.00, 3.00, 8.50, 2000, '享85折，消费3倍积分与成长值，VIP专属礼遇，生日礼包');

-- 初始化示例优惠券模板
INSERT INTO `t_coupon_template` (`coupon_code`, `coupon_name`, `coupon_type`, `total_amount`, `full_amount`, `reduce_amount`, `valid_type`, `valid_days`, `min_level`, `daily_limit`, `total_limit`, `description`, `status`) VALUES
('NEW_USER_10', '新人满100减10券', 1, 10000, 100.00, 10.00, 2, 30, 0, 0, 1, '新用户专享，满100元可用，领取后30天内有效', 1),
('FULL_200_30', '满200减30通用券', 1, 50000, 200.00, 30.00, 1, NULL, 0, 1, 5, '全场通用，满200元减30元', 1),
('GOLD_BIRTHDAY', '黄金会员生日50元券', 1, -1, 300.00, 50.00, 2, 30, 3, 0, 1, '黄金以上会员生日专属，满300元可用', 1),
('COFFEE_FREE', '美式咖啡兑换券', 2, 5000, NULL, NULL, 2, 15, 0, 1, 3, '凭券可兑换中杯美式咖啡一杯', 1);
