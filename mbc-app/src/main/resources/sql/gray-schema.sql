-- ============================================================
-- 灰度规则表
-- ============================================================
DROP TABLE IF EXISTS `t_gray_rule`;
CREATE TABLE `t_gray_rule` (
    `id`                  BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `gray_code`           VARCHAR(64)   NOT NULL COMMENT '灰度规则编码',
    `gray_name`           VARCHAR(128)  NOT NULL COMMENT '灰度规则名称',
    `activity_id`         BIGINT        NOT NULL COMMENT '关联活动ID',
    `gray_type`           TINYINT       NOT NULL DEFAULT 1 COMMENT '灰度类型: 1城市灰度 2门店灰度 3人群灰度 4设备灰度',
    `gray_config`         TEXT          DEFAULT NULL COMMENT '灰度配置JSON',
    `rule_content`        TEXT          DEFAULT NULL COMMENT '灰度规则内容JSON(新规则配置)',
    `original_rule_content` TEXT       DEFAULT NULL COMMENT '原规则内容JSON(用于回滚)',
    `gray_ratio`          INT           NOT NULL DEFAULT 10 COMMENT '灰度流量比例 0-100',
    `status`              TINYINT       NOT NULL DEFAULT 0 COMMENT '状态: 0草稿 1灰度中 2已全量 3已回滚',
    `start_gray_time`     DATETIME      DEFAULT NULL COMMENT '开始灰度时间',
    `full_release_time`   DATETIME      DEFAULT NULL COMMENT '全量发布时间',
    `rollback_time`       DATETIME      DEFAULT NULL COMMENT '回滚时间',
    `operator`            VARCHAR(64)   DEFAULT NULL COMMENT '操作人',
    `description`         VARCHAR(500)  DEFAULT NULL COMMENT '描述',
    `create_time`         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`           VARCHAR(64)   DEFAULT 'system' COMMENT '创建人',
    `update_by`           VARCHAR(64)   DEFAULT 'system' COMMENT '更新人',
    `is_deleted`          TINYINT       NOT NULL DEFAULT 0 COMMENT '是否删除：0否 1是',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_gray_code` (`gray_code`),
    KEY `idx_activity_id` (`activity_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='灰度规则表';

-- ============================================================
-- 灰度指标表
-- ============================================================
DROP TABLE IF EXISTS `t_gray_metric`;
CREATE TABLE `t_gray_metric` (
    `id`                  BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `gray_rule_id`        BIGINT        NOT NULL COMMENT '灰度规则ID',
    `group_type`          TINYINT       NOT NULL DEFAULT 1 COMMENT '分组: 1灰度组 2对照组',
    `stat_date`           DATE          NOT NULL COMMENT '统计日期',
    `member_count`        INT           NOT NULL DEFAULT 0 COMMENT '会员数',
    `receive_count`       INT           NOT NULL DEFAULT 0 COMMENT '领券数',
    `redeem_count`        INT           NOT NULL DEFAULT 0 COMMENT '核销数',
    `redeem_amount`       DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '核销金额',
    `order_count`         INT           NOT NULL DEFAULT 0 COMMENT '订单数',
    `order_amount`        DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '订单金额',
    `refund_count`        INT           NOT NULL DEFAULT 0 COMMENT '退款数',
    `refund_amount`       DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '退款金额',
    `conversion_rate`     DECIMAL(10,4) NOT NULL DEFAULT 0.0000 COMMENT '转化率%',
    `avg_order_amount`    DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '客单价',
    `create_time`         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`           VARCHAR(64)   DEFAULT 'system' COMMENT '创建人',
    `update_by`           VARCHAR(64)   DEFAULT 'system' COMMENT '更新人',
    `is_deleted`          TINYINT       NOT NULL DEFAULT 0 COMMENT '是否删除：0否 1是',
    PRIMARY KEY (`id`),
    KEY `idx_gray_rule_id` (`gray_rule_id`),
    KEY `idx_stat_date` (`stat_date`),
    UNIQUE KEY `uk_gray_date_group` (`gray_rule_id`, `stat_date`, `group_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='灰度指标表';
