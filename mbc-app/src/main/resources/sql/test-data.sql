-- ============================================================
-- 智慧零售会员权益中心 - 测试数据脚本
-- 使用: 初始化完 schema.sql 后，可选执行此脚本生成测试数据
-- ============================================================
USE `mbc_center`;

-- ============================================================
-- 1. 测试会员（5个不同等级）
-- ============================================================
INSERT INTO `t_member`
(`member_code`, `phone`, `name`, `nickname`, `gender`, `birthday`,
 `level_code`, `growth_value`, `current_points`, `total_points`,
 `register_source`, `status`)
VALUES
('M20240101000001', '13800000001', '青铜会员', '测试小铜', 1, '1995-06-15', 1, 0, 0, 0, 'POS', 1),
('M20240101000002', '13800000002', '白银会员', '测试小银', 2, '1993-08-20', 2, 500, 880, 1200, 'MINI_APP', 1),
('M20240101000003', '13800000003', '黄金会员', '测试小金', 1, '1988-02-10', 3, 2500, 3200, 5000, 'MINI_APP', 1),
('M20240101000004', '13800000004', '铂金会员', '测试小铂', 2, '1990-11-28', 4, 6800, 12000, 18000, 'ADMIN', 1),
('M20240101000005', '13800000005', '钻石会员', '测试小钻', 1, '1985-06-01', 5, 15000, 25000, 50000, 'CUSTOMER', 1),
('M20240101000006', '13800000006', '重复待合并A', '重复账号A', 1, '1992-03-03', 1, 100, 100, 100, 'POS', 1),
('M20240101000007', '13800000006', '重复待合并B', '重复账号B', 1, '1992-03-03', 1, 200, 150, 150, 'MINI_APP', 1);

-- ============================================================
-- 2. 测试用户优惠券（给ID 2/3/4各发几张券）
-- ============================================================
-- 先查询券模板ID: 通常 NEW_USER_10=1, FULL_200_30=2, GOLD_BIRTHDAY=3, COFFEE_FREE=4
INSERT INTO `t_coupon_instance`
(`instance_no`, `template_id`, `member_id`, `coupon_status`,
 `valid_start`, `valid_end`, `receive_source`, `receive_time`)
VALUES
-- 白银会员(ID=2) 的券
('CI2024061000001', 2, 2, 1, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 25 DAY), 'ACTIVITY', DATE_SUB(NOW(), INTERVAL 5 DAY)),
('CI2024061000002', 4, 2, 1, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 10 DAY), 'ACTIVITY', DATE_SUB(NOW(), INTERVAL 3 DAY)),
('CI2024061000003', 2, 2, 3, DATE_SUB(NOW(), INTERVAL 60 DAY), DATE_SUB(NOW(), INTERVAL 30 DAY), 'ACTIVITY', DATE_SUB(NOW(), INTERVAL 70 DAY)),
-- 黄金会员(ID=3) 的券
('CI2024061000004', 2, 3, 1, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 3 DAY), 'ACTIVITY', DATE_SUB(NOW(), INTERVAL 12 DAY)),
('CI2024061000005', 3, 3, 1, DATE_SUB(NOW(), INTERVAL 0 DAY), DATE_ADD(NOW(), INTERVAL 29 DAY), 'BIRTHDAY', DATE_SUB(NOW(), INTERVAL 1 DAY)),
('CI2024061000006', 4, 3, 1, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 14 DAY), 'ACTIVITY', DATE_SUB(NOW(), INTERVAL 5 DAY)),
-- 铂金会员(ID=4) 的券
('CI2024061000007', 2, 4, 1, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 20 DAY), 'ACTIVITY', DATE_SUB(NOW(), INTERVAL 10 DAY)),
('CI2024061000008', 2, 4, 4, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 20 DAY), 'ACTIVITY', DATE_SUB(NOW(), INTERVAL 10 DAY));

-- ============================================================
-- 3. 测试积分流水
-- ============================================================
INSERT INTO `t_point_log`
(`member_id`, `point_type`, `change_points`, `before_points`, `after_points`,
 `source_type`, `remark`, `create_time`, `expire_time`)
VALUES
(2, 1, 1200, 0, 1200, 1, '消费累计', DATE_SUB(NOW(), INTERVAL 90 DAY), DATE_ADD(NOW(), INTERVAL 275 DAY)),
(2, 2, 320, 1200, 880, 2, '积分抵扣消费', DATE_SUB(NOW(), INTERVAL 30 DAY), NULL),
(3, 1, 5000, 0, 5000, 1, '消费累计', DATE_SUB(NOW(), INTERVAL 180 DAY), DATE_ADD(NOW(), INTERVAL 185 DAY)),
(3, 1, 500, 5000, 5500, 3, '生日赠送', DATE_SUB(NOW(), INTERVAL 10 DAY), DATE_ADD(NOW(), INTERVAL 355 DAY)),
(3, 2, 2300, 5500, 3200, 2, '兑换礼品', DATE_SUB(NOW(), INTERVAL 5 DAY), NULL),
(4, 1, 18000, 0, 18000, 1, '消费累计', DATE_SUB(NOW(), INTERVAL 300 DAY), DATE_ADD(NOW(), INTERVAL 65 DAY)),
(4, 1, 1000, 18000, 19000, 4, '注册赠送', DATE_SUB(NOW(), INTERVAL 200 DAY), DATE_ADD(NOW(), INTERVAL 165 DAY)),
(4, 2, 7000, 19000, 12000, 2, '多次消费抵扣', DATE_SUB(NOW(), INTERVAL 50 DAY), NULL);

-- ============================================================
-- 4. 测试等级成长值流水
-- ============================================================
INSERT INTO `t_growth_log`
(`member_id`, `change_value`, `before_value`, `after_value`,
 `before_level`, `after_level`, `source_type`, `source_id`, `remark`, `create_time`)
VALUES
(2, 500, 0, 500, 1, 2, 1, 'ORD20240301001', '首笔消费升级白银', DATE_SUB(NOW(), INTERVAL 90 DAY)),
(3, 2000, 500, 2500, 2, 3, 1, 'ORD20240201008', '累计消费升级黄金', DATE_SUB(NOW(), INTERVAL 120 DAY)),
(4, 3000, 3800, 6800, 3, 4, 1, 'ORD20240101001', '升级铂金', DATE_SUB(NOW(), INTERVAL 165 DAY)),
(5, 5000, 10000, 15000, 4, 5, 2, 'ACT_NEWYEAR', '活动送成长值升级钻石', DATE_SUB(NOW(), INTERVAL 200 DAY)),
(5, 5000, 5000, 10000, 3, 4, 1, 'ORD20231201001', '升级铂金历史', DATE_SUB(NOW(), INTERVAL 250 DAY));

-- ============================================================
-- 5. 测试消费订单
-- ============================================================
INSERT INTO `t_consume_order`
(`order_no`, `member_id`, `order_type`, `order_status`, `total_amount`,
 `discount_amount`, `coupon_amount`, `point_amount`, `level_discount`,
 `pay_amount`, `earned_points`, `earned_growth`, `used_points`, `used_coupon_ids`,
 `store_code`, `store_name`, `pos_code`, `cashier`, `channel`,
 `pay_time`, `complete_time`, `create_time`)
VALUES
('ORD2024060100001', 2, 1, 2, 560.00, 56.00, 30.00, 20.00, 6.00, 504.00, 605, 672, 200, '1', 'S001', '朝阳店', 'POS01', '王收银', 'POS', DATE_SUB(NOW(), INTERVAL 9 DAY), DATE_SUB(NOW(), INTERVAL 9 DAY), DATE_SUB(NOW(), INTERVAL 9 DAY)),
('ORD2024060200002', 3, 1, 2, 1280.00, 180.00, 50.00, 64.00, 66.00, 1100.00, 1650, 1920, 640, '5', 'S002', '海淀店', 'POS05', '李收银', 'MINI_APP', DATE_SUB(NOW(), INTERVAL 7 DAY), DATE_SUB(NOW(), INTERVAL 7 DAY), DATE_SUB(NOW(), INTERVAL 7 DAY)),
('ORD2024060500003', 4, 1, 2, 3500.00, 650.00, 30.00, 200.00, 420.00, 2850.00, 5700, 7000, 2000, '7', 'S003', '国贸店', 'POS08', '赵收银', 'POS', DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY)),
('ORD2024060800004', 5, 1, 2, 8888.00, 2388.00, 30.00, 1000.00, 1358.00, 6500.00, 19500, 26664, 10000, NULL, 'S001', '朝阳旗舰店', 'POS01', '王收银', 'POS', DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),
('ORD2024061000005', 3, 1, 0, 688.00, 0, 0, 0, 0, 0, 0, 0, 0, NULL, 'S001', '朝阳店', 'POS02', '张收银', 'POS', NULL, NULL, DATE_SUB(NOW(), INTERVAL 1 HOUR)),
('ORD2024060900006', 2, 1, 1, 320.00, 0, 0, 0, 0, 320.00, 0, 0, 0, NULL, 'S005', '丰台店', 'POS11', '孙收银', 'POS', DATE_SUB(NOW(), INTERVAL 30 MINUTE), NULL, DATE_SUB(NOW(), INTERVAL 1 HOUR));

-- ============================================================
-- 6. 测试活动
-- ============================================================
INSERT INTO `t_activity`
(`activity_code`, `activity_name`, `activity_type`,
 `start_time`, `end_time`, `target_level`,
 `budget_points`, `budget_coupons`, `used_points`, `used_coupons`,
 `exposed_count`, `participated_count`, `converted_count`,
 `driven_order_amount`, `driven_order_count`,
 `status`, `description`)
VALUES
('ACT_618_SALE', '618年中大促发券活动', 1,
 DATE_SUB(NOW(), INTERVAL 20 DAY), DATE_ADD(NOW(), INTERVAL 10 DAY), 0,
 0, 10000, 0, 1256,
 50000, 8500, 4250,
 885600.00, 12580,
 1, '618全场满减券，5档优惠券可领'),
('ACT_NEW_USER', '新人注册礼包', 1,
 DATE_SUB(NOW(), INTERVAL 365 DAY), DATE_ADD(NOW(), INTERVAL 365 DAY), 0,
 50000, 30000, 28600, 12800,
 0, 12800, 8600,
 680000.00, 9800,
 1, '新人注册即送积分+券礼包'),
('ACT_BIRTHDAY_2024', '2024年度生日礼遇', 4,
 STR_TO_DATE('2024-01-01 00:00:00', '%Y-%m-%d %H:%i:%s'), STR_TO_DATE('2024-12-31 23:59:59', '%Y-%m-%d %H:%i:%s'), 2,
 200000, 10000, 56800, 3200,
 8000, 8000, 6500,
 320000.00, 5600,
 1, '黄金以上会员生日专属积分+券');

-- ============================================================
-- 7. 测试消息
-- ============================================================
INSERT INTO `t_message_log`
(`msg_no`, `member_id`, `msg_type`, `msg_title`, `msg_content`,
 `channel`, `target`, `send_status`, `retry_count`, `send_time`, `create_time`, `biz_id`)
VALUES
('MSG2024060800001', 5, 4, '恭喜升级钻石会员！', '尊敬的测试小钻，您已升级为钻石会员，享受85折等专属礼遇~',
 'INNER', NULL, 3, 0, DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY), NULL),
('MSG2024060900001', 2, 2, '您的优惠券3天后过期', '您有1张满200减30券将在3天后过期，快去使用吧！',
 'INNER', NULL, 1, 0, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), '1'),
('MSG2024061000001', 3, 1, '生日快乐！权益已到账', '祝您生日快乐，500积分+50元生日券已到账，请查收~',
 'INNER', NULL, 1, 0, DATE_SUB(NOW(), INTERVAL 1 HOUR), DATE_SUB(NOW(), INTERVAL 1 HOUR), '5');

-- ============================================================
-- 8. 测试核销记录
-- ============================================================
INSERT INTO `t_benefit_use_log`
(`use_no`, `member_id`, `benefit_type`, `benefit_id`, `use_status`,
 `order_no`, `order_amount`, `benefit_value`, `used_points`,
 `store_code`, `pos_code`, `operator`,
 `lock_time`, `confirm_time`, `create_time`)
VALUES
('BNF20240601001', 2, 1, 1, 2, 'ORD2024060100001', 560.00, 30.00, NULL, 'S001', 'POS01', '王收银', DATE_SUB(NOW(), INTERVAL 9 DAY), DATE_SUB(NOW(), INTERVAL 9 DAY), DATE_SUB(NOW(), INTERVAL 9 DAY)),
('BNF20240601002', 2, 2, NULL, 2, 'ORD2024060100001', 560.00, 20.00, 200, 'S001', 'POS01', '王收银', DATE_SUB(NOW(), INTERVAL 9 DAY), DATE_SUB(NOW(), INTERVAL 9 DAY), DATE_SUB(NOW(), INTERVAL 9 DAY)),
('BNF20240601003', 2, 3, NULL, 2, 'ORD2024060100001', 560.00, 6.00, NULL, 'S001', 'POS01', '王收银', DATE_SUB(NOW(), INTERVAL 9 DAY), DATE_SUB(NOW(), INTERVAL 9 DAY), DATE_SUB(NOW(), INTERVAL 9 DAY));
