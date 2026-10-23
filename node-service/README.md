# Card Draw Node Service

轻量版单词集卡接口服务，用于替代较重的 Java 原型服务。它保留真实卡册、每日次数防刷、跨设备同步和抽卡历史能力，但不做后台管理系统。

## 技术方案

```text
H5 / OSS / CDN
      |
      v
Node.js + Fastify
      |
      v
MySQL 5.7
```

卡池、卡牌、概率和资源地址仍使用静态 JSON：

```text
../src/main/resources/card-pool/v1.json
```

MySQL 只保存用户资产和抽卡流水：

- 每日次数
- 用户卡册
- 抽卡订单
- 抽卡订单明细

## 初始化数据库

先在 MySQL 5.7 中创建库：

```sql
CREATE DATABASE card_draw DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

再执行建表脚本：

```bash
mysql -h 127.0.0.1 -P 3306 -u card_draw -p card_draw < sql/001_init.sql
```

## 本地启动

```bash
cd node-service
cp .env.example .env
npm install
npm start
```

环境变量说明：

```text
PORT=8080
MYSQL_HOST=127.0.0.1
MYSQL_PORT=3306
MYSQL_DATABASE=card_draw
MYSQL_USER=card_draw
MYSQL_PASSWORD=
MYSQL_CONNECTION_LIMIT=10
CARD_POOL_CONFIG_FILE=../src/main/resources/card-pool/v1.json
CORS_ORIGIN=*
```

## 接口

```bash
curl 'http://localhost:8080/api/ping'
curl 'http://localhost:8080/api/card-draw/activity?userId=10001'
curl -X POST 'http://localhost:8080/api/card-draw/draw?userId=10001&requestId=test-001'
curl 'http://localhost:8080/api/card-draw/my-cards?userId=10001'
curl 'http://localhost:8080/api/card-draw/history?userId=10001'
curl -X POST 'http://localhost:8080/api/card-draw/simulate-next-day?userId=10001'
```

返回结构保持：

```json
{
  "success": true,
  "message": "ok",
  "data": {}
}
```

## 抽卡事务

`POST /api/card-draw/draw` 在一个 MySQL 事务中完成：

1. 初始化并锁定用户当天次数。
2. 校验剩余次数。
3. 扣减 1 次。
4. 按静态卡池概率抽 5 张卡。
5. 写入抽卡订单。
6. 写入订单卡牌明细。
7. 更新用户卡册。
8. 提交事务。

同一用户传入相同 `requestId` 时会返回同一笔抽卡结果，避免重复扣次数。

## 测试

当前测试不依赖 MySQL，只验证静态卡池配置和抽卡规则：

```bash
npm test
```

如果本机 Node 或 npm 环境异常，可用任意 Node 20+ 执行：

```bash
node --test test/*.test.js
```

## 上线边界

本服务适合本期轻量上线：

- 不做后台管理。
- 不做动态卡池配置。
- 不接完课和练习事件。
- 保留模拟次日接口用于测试，正式环境可由网关或配置限制访问。

后续如果需要公司统一登录态，需要将 `userId` 请求参数替换为网关解析后的用户 ID。
