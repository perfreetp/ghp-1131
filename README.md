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

## 常见问题

**Q1: 启动时报 `Table 'mbc_center.xxx' doesn't exist`？**
→ 先执行 `sql/schema.sql` 初始化数据库。

**Q2: 启动 Redis 连接失败？**
→ 本地没装 Redis 可先装 Windows 版 Memurai 或改 application.yml 指向可用 Redis。

**Q3: 领券接口返回「今日已达领取上限」？**
→ 修改模板 `dailyLimit` 字段，或用不同 memberId 测试。

**Q4: 积分变更报「获取分布式锁失败」？**
→ 同一会员并发变更会触发，稍后重试或检查是否有死锁（锁超时10秒自动释放）。
