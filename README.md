# 智慧零售会员权益中心 (Membership Benefit Center)

## 项目简介

面向商超收银系统、小程序、客服工具、运营后台的企业级会员权益后端服务。
基于 **Spring Boot 3.2 + JDK 17 + MySQL 8 + Redis + MyBatis-Plus** 构建，采用多模块 Maven 工程。

---

## 七类核心能力

| 能力模块 | 能力描述 | 关键接口前缀 |
|---------|---------|-----------|
| 会员识别 | 手机号/会员码查询身份、会员注册、信息更新、重复会员合并 | `/member/**` |
| 等级规则 | 成长值计算、等级判定升级降级、生日权益、等级规则管理 | `/level/**` |
| 积分账户 | 积分发放、扣减、冻结解冻、退款返还、流水查询、过期清理 | `/point/**` |
| 优惠券包 | 创建满减券/兑换券、领券、限制同日领取次数、判断券可用性 | `/coupon/**` |
| 权益核销 | 锁定权益、确认核销、退款返还、过期释放 | `/benefit/**` |
| 订单校验/消息 | 订单预校验、创建/支付/完成/退款、生日权益推送、到期提醒 | `/order/**`, `/message/**` |
| 运营查询 | 消费记录、个人权益清单、活动效果统计、运营大盘数据 | `/query/**` |

---

## 项目模块结构

```
membership-benefit-center (pom, parent)
├── mbc-common            # 公共模块：枚举、异常、响应体、工具、配置
├── mbc-member            # ①会员识别模块
├── mbc-level             # ②等级规则模块
├── mbc-point             # ③积分账户模块
├── mbc-coupon            # ④优惠券包模块
├── mbc-benefit           # ⑤权益核销模块
├── mbc-order             # ⑥订单校验模块
├── mbc-message           # ⑥消息触达模块
├── mbc-query             # ⑦运营查询模块
└── mbc-app               # ★启动模块 (Spring Boot Application)
    └── src/main/
        ├── resources/
        │   ├── application.yml   # 配置文件
        │   └── sql/schema.sql    # 数据库初始化脚本（含示例数据）
        └── java/.../MembershipBenefitApplication.java
```

---

## 环境准备

| 依赖 | 版本要求 | 验证命令 |
|-----|---------|---------|
| JDK | 17+ | `java -version` |
| Maven | 3.8+ | `mvn -v` |
| MySQL | 8.0+ | `SELECT VERSION();` |
| Redis | 6.0+ | `redis-cli ping` |

---

## 数据库初始化

1. 登录 MySQL 并执行脚本：
```bash
mysql -u root -p < mbc-app/src/main/resources/sql/schema.sql
```

2. 默认创建数据库 `mbc_center`，包含 11 张核心业务表 + 5个等级规则 + 4张示例券模板。

3. 数据库连接配置在 `mbc-app/src/main/resources/application.yml`：
```yaml
spring:
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/mbc_center?...
    username: root
    password: root
  data:
    redis:
      host: 127.0.0.1
      port: 6379
```
根据实际环境修改账号密码。

---

## 启动方式

### 方式一：Maven 命令

```bash
# 进入项目根目录
cd d:\TraeProjects\1131

# 全量编译（跳过测试）
mvn clean install -DskipTests

# 启动
mvn -pl mbc-app spring-boot:run
```

### 方式二：IDEA 运行

1. 用 IntelliJ IDEA 打开 `pom.xml`（作为 Project 打开）
2. 等待 Maven 依赖下载完成
3. 打开启动类运行：
   `mbc-app/src/main/java/com/smartretail/mbc/app/MembershipBenefitApplication.java`
4. 右键 → Run / Debug

---

## 启动成功标志

控制台将输出：
```
===============================================
  智慧零售会员权益中心启动成功!
  服务地址: http://127.0.0.1:8080/api/mbc
  API文档: http://127.0.0.1:8080/api/mbc/doc.html
===============================================
```

访问 **Knife4j API文档**：http://127.0.0.1:8080/api/mbc/doc.html

---

## 接口总览（40+ 核心 API）

### ① 会员识别模块 - `/member`

| Method | Path | 说明 | 请求体 |
|--------|------|------|-------|
| POST | `/member/register` | 会员注册(自动生成会员码) | `{phone, name, birthday}` |
| GET  | `/member/{id}` | 根据ID查询会员 | |
| GET  | `/member/by-phone/{phone}` | 按手机号查询 | |
| GET  | `/member/by-code/{memberCode}` | 按会员码查询 | |
| POST | `/member/identify` | 身份识别(收银/小程序用) | `{phone?, memberCode?}` |
| PUT  | `/member/update` | 更新会员信息 | `{memberId, ...}` |
| POST | `/member/page` | 分页查询会员列表 | 查询条件DTO |
| POST | `/member/merge` | 合并重复会员(客服/运营用) | `{sourceMemberId, targetMemberId}` |

### ② 等级规则模块 - `/level`

| Method | Path | 说明 |
|--------|------|------|
| GET  | `/level/rules` | 查询所有等级规则 |
| GET  | `/level/rule/{levelCode}` | 单个等级详情 |
| POST | `/level/rule/upsert` | 新增/修改等级规则 |
| GET  | `/level/current/{memberId}` | 查询会员当前等级+权益 |
| POST | `/level/growth/calc` | 计算并累加成长值(按订单金额) |
| POST | `/level/growth/adjust` | 人工调整成长值 |
| POST | `/level/birthday/grant` | 发放生日权益(积分+券) |

### ③ 积分账户模块 - `/point`

| Method | Path | 说明 |
|--------|------|------|
| GET  | `/point/account/{memberId}` | 积分账户概览(可用/冻结/过期) |
| POST | `/point/logs` | 积分流水分页查询 |
| POST | `/point/add` | 发放积分 |
| POST | `/point/subtract` | 扣减积分 |
| POST | `/point/freeze` | 冻结积分 |
| POST | `/point/unfreeze` | 解冻积分 |
| POST | `/point/refund-return` | 退款返还积分(幂等) |

### ④ 优惠券包模块 - `/coupon`

| Method | Path | 说明 |
|--------|------|------|
| POST | `/coupon/template/create` | 创建券模板(满减/兑换券) |
| GET  | `/coupon/template/{id}` | 模板详情 |
| POST | `/coupon/template/page` | 模板分页列表 |
| POST | `/coupon/receive` | 用户领券(每日限领+总量乐观锁) |
| POST | `/coupon/batch-issue` | 运营批量发券 |
| POST | `/coupon/check-availability` | 判断券是否可用(订单结算用) |
| GET  | `/coupon/instance/{id}` | 券实例详情 |
| POST | `/coupon/member/page` | 查询用户券包 |

### ⑤ 权益核销模块 - `/benefit`

| Method | Path | 说明 |
|--------|------|------|
| POST | `/benefit/lock` | 锁定权益(下单时) |
| POST | `/benefit/confirm` | 确认核销(支付成功) |
| POST | `/benefit/return` | 权益返还(退款时) |
| POST | `/benefit/logs` | 核销记录查询 |

### ⑥ 订单校验模块 - `/order`

| Method | Path | 说明 |
|--------|------|------|
| POST | `/order/validate` | 订单预校验(不写库,算可省金额) |
| POST | `/order/create` | 创建订单(幂等) |
| POST | `/order/pay` | 支付完成→锁定权益 |
| POST | `/order/complete` | 订单完成→核销+发积分成长值 |
| POST | `/order/refund` | 退款→返还权益 |
| POST | `/order/page` | 订单分页查询 |

### ⑦ 消息触达模块 - `/message`

| Method | Path | 说明 |
|--------|------|------|
| POST | `/message/send` | 发送单条消息(站内信/SMS/微信/Push) |
| POST | `/message/batch-send` | 批量推送 |
| POST | `/message/query` | 消息查询(分页) |
| PUT  | `/message/read/{id}` | 标记已读 |
| PUT  | `/message/read-all/{memberId}` | 全部标记已读 |
| GET  | `/message/unread/{memberId}` | 未读计数+按类型分布 |
| POST | `/message/reminder/expire` | 手动触发券/积分到期提醒 |

### ⑧ 运营查询模块 - `/query`

| Method | Path | 说明 |
|--------|------|------|
| POST | `/query/consume/records` | 查询消费记录(分页) |
| POST | `/query/personal/benefits` | 个人权益总览(小程序/会员中心) |
| POST | `/query/activity/stats` | 活动效果统计列表(含ROI/漏斗) |
| GET  | `/query/activity/{id}/stats` | 单个活动详细统计+趋势 |
| POST | `/query/dashboard` | 运营大盘数据 |

---

## 关键技术设计

### 1. 分布式锁 & 幂等
| 场景 | 锁/幂等Key | 实现 |
|-----|----------|-----|
| 积分变更 | `mbc:lock:point:{memberId}` | Redis `setIfAbsent`, 10s |
| 领券 | `mbc:lock:coupon-template:{tid}` | Redis `setIfAbsent`, 30s |
| 会员合并 | `mbc:lock:member:merge:{src}:{tgt}` | Redis `setIfAbsent` |
| 退款返积分 | `mbc:point:refund:{refundNo}` | setnx 永久,防重复返还 |
| 创建订单 | `mbc:order:create:{orderNo}` | setnx 30min 幂等 |
| 生日权益 | `mbc:birthday:{year}:{memberId}` | setnx 365天，每年只发一次 |

### 2. 券每日限领
- Key: `mbc:limit:coupon:{templateId}:{memberId}:{yyyyMMdd}`
- 使用 `INCRBY` + 自动过期(TTL = 当日剩余秒数)
- 超过 `dailyLimit` 时自动 `DECR` 回滚

### 3. 定时任务（@Scheduled）
| 任务 | Cron | 说明 |
|-----|------|------|
| 积分过期清理 | `0 0 2 * * ?` | 每天凌晨2点扫描过期批次 |
| 券状态过期 | `0 0 * * * ?` | 每小时更新未开始/可使用/已过期状态 |
| 券3天到期提醒 | `0 0 10 * * ?` | 每天上午10点查 selectExpiringInDays(3) |
| 权益锁定超时释放 | `0 */5 * * * ?` | 每5分钟扫描超过30分钟未确认的锁定 → 释放 |

### 4. 5档会员等级体系
| 等级 | 成长值门槛 | 成长倍率 | 积分倍率 | 折扣 | 生日赠礼 |
|-----|---------|--------|--------|-----|---------|
| 青铜 | 0 | 1x | 1x | 原价 | 100积分 |
| 白银 | 500 | 1.2x | 1.2x | 98折 | 200积分 |
| 黄金 | 2000 | 1.5x | 1.5x | 95折 | 500积分+券 |
| 铂金 | 5000 | 2x | 2x | 9折 | 1000积分+券 |
| 钻石 | 10000 | 3x | 3x | 85折 | 2000积分+券 |

---

## 快速验证 - cURL 示例

### 1. 注册会员
```bash
curl -X POST http://127.0.0.1:8080/api/mbc/member/register \
  -H "Content-Type: application/json" \
  -d '{"phone":"13800138000","name":"张三","nickname":"测试用户","birthday":"1990-06-15"}'
```

### 2. 创建满减券模板
```bash
curl -X POST http://127.0.0.1:8080/api/mbc/coupon/template/create \
  -H "Content-Type: application/json" \
  -d '{
    "couponCode":"TEST_50",
    "couponName":"测试满500减50券",
    "couponType":1,
    "totalAmount":1000,
    "fullAmount":500,
    "reduceAmount":50,
    "validType":2,
    "validDays":30,
    "dailyLimit":1,
    "totalLimit":3
  }'
```

### 3. 订单预校验（查看能省多少钱）
```bash
curl -X POST http://127.0.0.1:8080/api/mbc/order/validate \
  -H "Content-Type: application/json" \
  -d '{"memberId":1,"totalAmount":688.00,"usedCouponIds":[5],"usedPoints":200}'
```

### 4. 发放生日权益
```bash
curl -X POST http://127.0.0.1:8080/api/mbc/level/birthday/grant \
  -H "Content-Type: application/json" \
  -d '{"memberId":1}'
```

---

## 业务模块依赖图

```
          ┌────────────┐
          │ mbc-common │ (枚举/异常/工具/配置)
          └──────┬─────┘
                 │ 所有模块依赖
       ┌─────────┼─────────┐
       ▼         ▼         ▼
  mbc-member  mbc-level  mbc-point
       │         │         │
       ▼         ▼         ▼
    mbc-coupon (依赖 member, level)
       │
       ▼
    mbc-benefit (依赖 coupon, point)
       │
       ▼
    mbc-order (依赖 benefit)  ──┐
       │                        │
       ▼                        ▼
  mbc-message (依赖coupon)  mbc-query (聚合所有模块)
       │                        │
       └──────────┬─────────────┘
                  ▼
           mbc-app (Spring Boot 启动)
```

---

## 扩展对接建议

| 下游系统 | 建议对接方式 |
|---------|------------|
| 商超收银系统 (POS) | 内网 HTTP/REST 调用 `/identify`+`/order/*`+`/benefit/*` |
| 小程序/APP | 网关鉴权后透传调用，重点用 `/member/identify`+`/coupon/receive`+`/query/personal/benefits` |
| 客服工具 | 内部账号权限后调用 `/member/page`+`/member/merge`+`/point/*`+`/message/send` |
| 运营后台 | 全部接口权限，重点用 `/coupon/template/*`+`/level/*`+`/query/activity/stats`+`/query/dashboard` |
| 短信/微信推送 | 实现 `doSend()` 中 TODO 部分，对接阿里云 SMS / 微信模板消息 |
| 数据报表/BI | 直接读取 t_consume_order, t_growth_log, t_point_log 等业务表 |

---

## 🔗 典型调用链路完整示例 (curl)

假设服务已启动在 `http://127.0.0.1:8080/api/mbc`。

---

### 🛒 链路一：商超收银完整流程（注册→发券→试算→锁定→核销→退款）

```bash
# ============================================================
# Step 1 - 注册会员
# ============================================================
curl -s -X POST http://127.0.0.1:8080/api/mbc/member/register \
  -H "Content-Type: application/json" \
  -d '{
    "phone": "13912345678",
    "name": "收银测试员",
    "nickname": "测试小明",
    "gender": 1,
    "birthday": "1995-06-15",
    "registerSource": "POS"
  }' | python -m json.tool

# 返回示例：{"code":200,"data":{"id":8,"memberCode":"M20250610000008","phone":"13912345678",...}}
# 记录返回的 memberId (假设为 8) 和 memberCode
export MEMBER_ID=8
export MEMBER_CODE=M20250610000008


# ============================================================
# Step 2 - 为新会员发券（运营后台批量发券 / 新人礼包）
# ============================================================
# 先查可用的券模板 ID：
curl -s -X POST http://127.0.0.1:8080/api/mbc/coupon/template/page \
  -H "Content-Type: application/json" \
  -d '{"pageNum":1,"pageSize":10,"status":1}' | python -m json.tool

# 给新会员批量发两张券（NEW_USER_10=ID 1, FULL_200_30=ID 2）：
curl -s -X POST http://127.0.0.1:8080/api/mbc/coupon/batch-issue \
  -H "Content-Type: application/json" \
  -d '{
    "memberIds": ['$MEMBER_ID'],
    "templateId": 1,
    "receiveSource": "REGISTER",
    "sourceId": "new_user_gift_2025"
  }' | python -m json.tool

curl -s -X POST http://127.0.0.1:8080/api/mbc/coupon/batch-issue \
  -H "Content-Type: application/json" \
  -d '{
    "memberIds": ['$MEMBER_ID'],
    "templateId": 2,
    "receiveSource": "REGISTER",
    "sourceId": "new_user_gift_2025"
  }' | python -m json.tool

# 查一下会员券列表：
curl -s -X POST http://127.0.0.1:8080/api/mbc/coupon/member/page \
  -H "Content-Type: application/json" \
  -d '{"memberId":'$MEMBER_ID',"couponStatus":1,"pageNum":1,"pageSize":10}' | python -m json.tool
# 记录返回的可用券 instance ID（假设 CI 的 ID 为 9, 10）
export COUPON_1=9
export COUPON_2=10


# ============================================================
# Step 3 - 收银端下单完整试算（含商品明细，返回每张券能否使用+原因）
# ============================================================
curl -s -X POST http://127.0.0.1:8080/api/mbc/order/pos/validate \
  -H "Content-Type: application/json" \
  -d '{
    "memberId": '$MEMBER_ID',
    "items": [
      {"skuId":"SKU001","skuName":"纯牛奶 250ml*24","quantity":2,"unitPrice":68.00,"subtotal":136.00,"categoryId":"DAIRY"},
      {"skuId":"SKU002","skuName":"金龙鱼调和油 5L","quantity":1,"unitPrice":99.90,"subtotal":99.90,"categoryId":"OIL"},
      {"skuId":"SKU003","skuName":"可口可乐 330ml*24","quantity":1,"unitPrice":58.00,"subtotal":58.00,"categoryId":"DRINK"},
      {"skuId":"SKU004","skuName":"进口牛排 200g","quantity":3,"unitPrice":59.00,"subtotal":177.00,"categoryId":"MEAT"}
    ],
    "useCouponIds": ['$COUPON_1', '$COUPON_2'],
    "tryAllCoupons": true,
    "usePoints": null,
    "storeCode": "S001",
    "posCode": "POS-01",
    "cashier": "张收银",
    "channel": "POS"
  }' | python -m json.tool

# 返回将包含：
#   couponTrials: 每张券的可用/不可用及详细原因
#   bestCouponCombination: 推荐的券组合
#   maxUsablePoints: 本次最多可用积分数
#   finalPayAmount: 最终应付金额
#   earnablePoints/Growth: 本单可得积分与成长值


# ============================================================
# Step 4 - 创建订单（幂等，用唯一orderNo）
# ============================================================
export ORDER_NO=POS$(date +%Y%m%d%H%M%S)
echo "本次订单号: $ORDER_NO"

curl -s -X POST http://127.0.0.1:8080/api/mbc/order/create \
  -H "Content-Type: application/json" \
  -d '{
    "orderNo": "'$ORDER_NO'",
    "memberId": '$MEMBER_ID',
    "orderType": 1,
    "totalAmount": 470.90,
    "usedPoints": 200,
    "usedCouponInstanceIds": ['$COUPON_2'],
    "storeCode": "S001",
    "storeName": "朝阳旗舰店",
    "posCode": "POS-01",
    "cashier": "张收银",
    "channel": "POS"
  }' | python -m json.tool


# ============================================================
# Step 5 - 支付完成 → 自动锁定所有权益（券状态变锁定、积分变冻结）
# ============================================================
curl -s -X POST http://127.0.0.1:8080/api/mbc/order/pay \
  -H "Content-Type: application/json" \
  -d '{
    "orderNo": "'$ORDER_NO'",
    "payAmount": 390.90,
    "payTime": "'$(date +%Y-%m-%dT%H:%M:%S)'"
  }' | python -m json.tool

# 可手动查看券状态变化：curl -s http://127.0.0.1:8080/api/mbc/coupon/instance/$COUPON_2 | python -m json.tool


# ============================================================
# Step 6 - 订单完成 → 确认核销所有权益（券标记已使用、积分扣减、发放积分成长值）
# ============================================================
curl -s -X POST http://127.0.0.1:8080/api/mbc/order/complete \
  -H "Content-Type: application/json" \
  -d '{
    "orderNo": "'$ORDER_NO'"
  }' | python -m json.tool

# 验证：查会员积分（current_points应该有增长）：
curl -s http://127.0.0.1:8080/api/mbc/point/account/$MEMBER_ID | python -m json.tool
# 验证：查成长值等级：
curl -s http://127.0.0.1:8080/api/mbc/level/current/$MEMBER_ID | python -m json.tool


# ============================================================
# Step 7 - 模拟退款 → 返还所有权益（券重新可用、积分返还）
# ============================================================
export REFUND_NO=REF$(date +%Y%m%d%H%M%S)

curl -s -X POST http://127.0.0.1:8080/api/mbc/order/refund \
  -H "Content-Type: application/json" \
  -d '{
    "orderNo": "'$ORDER_NO'",
    "refundNo": "'$REFUND_NO'",
    "refundAmount": 470.90,
    "reason": "顾客退货"
  }' | python -m json.tool

# 验证：券状态是否回滚为"可使用"：
curl -s http://127.0.0.1:8080/api/mbc/coupon/instance/$COUPON_2 | python -m json.tool
```

---

### 🎡 链路二：小程序个人中心（身份→权益清单→领券→消息）

```bash
# ============================================================
# Step 1 - 小程序身份识别（手机号登录）
# ============================================================
curl -s -X POST http://127.0.0.1:8080/api/mbc/member/identify \
  -H "Content-Type: application/json" \
  -d '{"phone":"13912345678"}' | python -m json.tool


# ============================================================
# Step 2 - 小程序个人中心完整权益总览
# ============================================================
curl -s -X POST http://127.0.0.1:8080/api/mbc/query/mini/personal-benefit \
  -H "Content-Type: application/json" \
  -d '{
    "memberId": '$MEMBER_ID',
    "couponStatusFilter": [1, 4, 2, 3],
    "pageNum": 1,
    "pageSize": 10
  }' | python -m json.tool
# 返回：会员卡信息+积分+生日权益状态+等级权益+过期提醒+券列表(分页)+消费统计


# ============================================================
# Step 3 - 小程序领券中心领券
# ============================================================
curl -s -X POST http://127.0.0.1:8080/api/mbc/coupon/receive \
  -H "Content-Type: application/json" \
  -d '{
    "memberId": '$MEMBER_ID',
    "templateId": 4,
    "receiveSource": "MINI_APP"
  }' | python -m json.tool
# templateId=4 对应"美式咖啡兑换券"


# ============================================================
# Step 4 - 查看领券成功消息通知
# ============================================================
curl -s http://127.0.0.1:8080/api/mbc/message/unread/$MEMBER_ID | python -m json.tool

curl -s -X POST http://127.0.0.1:8080/api/mbc/message/query \
  -H "Content-Type: application/json" \
  -d '{"memberId":'$MEMBER_ID',"pageNum":1,"pageSize":5}' | python -m json.tool
```

---

### 👨‍💼 链路三：运营后台（创建活动→查看效果→发放生日权益）

```bash
# ============================================================
# Step 1 - 创建618满减券活动
# ============================================================
curl -s -X POST http://127.0.0.1:8080/api/mbc/query/activity/create \
  -H "Content-Type: application/json" \
  -d '{
    "activityCode": "ACT_618_FULL_2025",
    "activityName": "2025年中大促满减券活动",
    "activityType": 1,
    "startTime": "2025-06-01T00:00:00",
    "endTime": "2025-06-20T23:59:59",
    "targetLevel": 0,
    "couponTemplateIds": [1, 2],
    "budgetCoupons": 50000,
    "budgetPoints": 100000,
    "applyScenes": "ALL",
    "description": "618全场每满200减30，新人额外满100减10",
    "status": 1
  }' | python -m json.tool
# 记录返回的 activityId，假设为 4
export ACT_ID=4


# ============================================================
# Step 2 - 发布活动（草稿→进行中）
# ============================================================
curl -s -X PUT http://127.0.0.1:8080/api/mbc/query/activity/status \
  -H "Content-Type: application/json" \
  -d '{
    "activityId": '$ACT_ID',
    "targetStatus": 1,
    "reason": "活动正式上线"
  }' | python -m json.tool


# ============================================================
# Step 3 - 手动为某会员发放生日权益
# ============================================================
curl -s -X POST http://127.0.0.1:8080/api/mbc/level/birthday/grant \
  -H "Content-Type: application/json" \
  -d '{"memberId": '$MEMBER_ID'}' | python -m json.tool


# ============================================================
# Step 4 - 活动结束后查看详细效果统计（含退款影响）
# ============================================================
# 先结束活动：
curl -s -X PUT http://127.0.0.1:8080/api/mbc/query/activity/status \
  -H "Content-Type: application/json" \
  -d '{
    "activityId": '$ACT_ID',
    "targetStatus": 2,
    "reason": "活动正常结束"
  }' | python -m json.tool

# 查看完整效果（含ROI、券效果明细、等级分布、日趋势、退款影响）：
curl -s http://127.0.0.1:8080/api/mbc/query/activity/$ACT_ID/effect-detail | python -m json.tool

# 查看活动效果列表：
curl -s -X POST http://127.0.0.1:8080/api/mbc/query/activity/effect-page \
  -H "Content-Type: application/json" \
  -d '{"pageNum":1,"pageSize":10}' | python -m json.tool

# 运营大盘总览：
curl -s -X POST http://127.0.0.1:8080/api/mbc/query/dashboard \
  -H "Content-Type: application/json" \
  -d '{
    "startTime": "'$(date -d 'first day of this month' +%Y-%m-%d)'T00:00:00",
    "endTime": "'$(date +%Y-%m-%d)'T23:59:59"
  }' | python -m json.tool
```

---

### 👩‍💼 链路四：客服工具（合并预览→正式合并→查询合并记录）

```bash
# ============================================================
# Step 0 - 先制造两个重复会员（同手机号注册两次）
# ============================================================
curl -s -X POST http://127.0.0.1:8080/api/mbc/member/register \
  -H "Content-Type: application/json" \
  -d '{"phone":"13700001111","name":"王小明","nickname":"王A","registerSource":"POS"}' | python -m json.tool
# 返回ID 假设为 9
export SRC_MEMBER=9

curl -s -X POST http://127.0.0.1:8080/api/mbc/member/register \
  -H "Content-Type: application/json" \
  -d '{"phone":"13700001111","name":"王小明","nickname":"王B","registerSource":"MINI_APP"}' | python -m json.tool
# 返回ID 假设为 10（因测试脚本未真实防重，实际系统已限制手机号唯一。这里可用测试数据已有的 ID 6,7）
export TGT_MEMBER=10
# 若重复注册被拦截，使用测试数据中已有的 6 和 7：
export SRC_MEMBER=6
export TGT_MEMBER=7


# ============================================================
# Step 1 - 客服端先做合并预览，看到差异再确认
# ============================================================
curl -s -X POST http://127.0.0.1:8080/api/mbc/member/merge/preview \
  -H "Content-Type: application/json" \
  -d '{
    "sourceMemberId": '$SRC_MEMBER',
    "targetMemberId": '$TGT_MEMBER'
  }' | python -m json.tool
# 返回内容包括：
#   sourceMember / targetMember: 双方会员详细信息
#   diffSummary: 手机号差异、等级差异(如青铜→白银)、可迁移积分/券/成长值数量
#   mergePreview: 合并后模拟结果（最终积分/等级/券数量/消费统计）
#   warnings: 风险提示（手机号变更、被合并方等级更高提醒等）


# ============================================================
# Step 2 - 预览确认后执行正式合并
# ============================================================
curl -s -X POST http://127.0.0.1:8080/api/mbc/member/merge \
  -H "Content-Type: application/json" \
  -d '{
    "sourceMemberId": '$SRC_MEMBER',
    "targetMemberId": '$TGT_MEMBER',
    "reason": "同一顾客重复注册，手机号一致",
    "operator": "客服-李姐"
  }' | python -m json.tool


# ============================================================
# Step 3 - 查询合并记录
# ============================================================
curl -s -X POST http://127.0.0.1:8080/api/mbc/member/merge/logs \
  -H "Content-Type: application/json" \
  -d '{
    "operator": "客服",
    "pageNum": 1,
    "pageSize": 10
  }' | python -m json.tool
# 或按手机号搜：'{"sourcePhone":"13700001111"}'
```

---

## 常见问题

**Q1: 启动时报 `Table 'mbc_center.xxx' doesn't exist`？**
→ 先执行 `sql/schema.sql` 初始化数据库。

**Q2: 启动 Redis 连接失败？**
→ 本地没装 Redis 可先装 Windows 版 Memurai 或改 application.yml 指向可用 Redis。

**Q3: 领券接口返回「今日已达领取上限」？**
→ 修改模板 `dailyLimit` 字段，或用不同 memberId 测试。

**Q4: 积分变更报「获取分布式锁失败」？**
→ 同一会员并发变更会触发，稍后重试或检查是否有死锁（锁超时10秒自动释放）。

---

## 门店/业态/设备差异化权益策略

### 功能说明
支持按门店范围、业态类型、收银设备类型差异化配置优惠券适用范围，实现精细化运营。

### 核心能力
- **门店范围**：支持白名单（指定门店可用）和黑名单（指定门店不可用）两种模式
- **业态限制**：支持大卖场、便利店、生鲜专区、家电专区、服饰专区、线上商城等多种业态
- **设备限制**：支持标准POS、自助收银、移动POS、小程序收银、APP收银等设备类型
- **智能补全**：后端可根据门店编码自动补全业态类型

### 门店表 SQL
```sql
CREATE TABLE `t_store_info` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `store_code` varchar(64) NOT NULL COMMENT '门店编码(唯一)',
    `store_name` varchar(128) NOT NULL COMMENT '门店名称',
    `store_type` int DEFAULT NULL COMMENT '业态类型编码',
    `store_level` int DEFAULT NULL COMMENT '门店级别',
    `address` varchar(256) DEFAULT NULL COMMENT '地址',
    `city` varchar(64) DEFAULT NULL COMMENT '城市',
    `province` varchar(64) DEFAULT NULL COMMENT '省份',
    `contact` varchar(32) DEFAULT NULL COMMENT '联系人',
    `phone` varchar(32) DEFAULT NULL COMMENT '联系电话',
    `status` tinyint DEFAULT '1' COMMENT '状态：0停用 1启用',
    `create_time` datetime DEFAULT NULL COMMENT '创建时间',
    `update_time` datetime DEFAULT NULL COMMENT '更新时间',
    `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
    `update_by` varchar(64) DEFAULT NULL COMMENT '更新人',
    `is_deleted` tinyint DEFAULT '0' COMMENT '是否删除：0否 1是',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_store_code` (`store_code`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='门店信息表';
```

### 券模板扩展字段
在 `t_coupon_template` 表中新增以下字段：
```sql
ALTER TABLE `t_coupon_template`
    ADD COLUMN `apply_store_codes` varchar(512) DEFAULT NULL COMMENT '适用门店编码列表(逗号分隔，空=全部适用)' AFTER `activity_id`,
    ADD COLUMN `exclude_store_codes` varchar(512) DEFAULT NULL COMMENT '排除门店编码列表(逗号分隔)' AFTER `apply_store_codes`,
    ADD COLUMN `apply_business_types` varchar(128) DEFAULT NULL COMMENT '适用业态列表(逗号分隔，空=全业态)' AFTER `exclude_store_codes`,
    ADD COLUMN `apply_pos_types` varchar(128) DEFAULT NULL COMMENT '适用收银设备类型(逗号分隔，空=全部支持)' AFTER `apply_business_types`,
    ADD COLUMN `store_limit_flag` tinyint DEFAULT '0' COMMENT '门店限制模式：0=白名单模式 1=黑名单模式' AFTER `apply_pos_types`;
```

### 业态枚举（BusinessTypeEnum）
| Code | Name | Desc |
|------|------|------|
| 1 | 大卖场 | 大型综合超市 |
| 2 | 便利店 | 社区便利店 |
| 3 | 生鲜专区 | 生鲜水果区 |
| 4 | 家电专区 | 家电商场 |
| 5 | 服饰专区 | 服装鞋包 |
| 6 | 线上商城 | 小程序/APP |

### 收银设备枚举（PosTypeEnum）
| Code | Name | Desc |
|------|------|------|
| 1 | 标准POS | 常规收银机 |
| 2 | 自助收银 | 自助结账机 |
| 3 | 移动POS | 手持扫码枪 |
| 4 | 小程序收银 | 线上下单 |
| 5 | APP收银 | APP下单 |

### 门店管理接口
- `GET /store/{id}` - 根据ID查询门店
- `GET /store/by-code/{storeCode}` - 根据编码查询门店
- `GET /store/list-all` - 查询所有启用门店
- `POST /store/page` - 分页查询门店列表
- `POST /store/create` - 新增门店
- `PUT /store/update` - 更新门店信息
- `DELETE /store/{id}` - 删除门店

