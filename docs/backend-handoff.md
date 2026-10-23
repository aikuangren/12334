# 抽卡后端对接说明

## 1. 模块边界

一期后端聚焦抽卡闭环：

- 卡牌基础数据
- 卡池与活动
- 卡位概率
- 新卡保护
- 用户抽卡次数
- 用户卡册
- 抽卡历史
- 后台基础配置

暂不包含：

- 分享率二期功能
- 跟读唤醒卡牌
- 高稀有度动画发放
- 学习系统真实完课/练习事件

## 2. 表设计

### card

卡牌基础表。

| 字段 | 说明 |
| --- | --- |
| id | 卡牌 ID |
| word | 单词 |
| meaning | 单词释义 |
| arabic_meaning | 阿语释义 |
| audio_url | 发音音频 |
| image_url | 卡牌图 |
| rarity | N/R/SR/SSR/UR/LR/XR |
| pool_version | 卡池版本 |
| sort_no | 图鉴排序 |
| status | DRAFT/ONLINE/OFFLINE |
| created_at | 创建时间 |
| updated_at | 更新时间 |

### card_pool

卡池版本表。

| 字段 | 说明 |
| --- | --- |
| id | 卡池 ID |
| version | 版本，如 v1 |
| name | 卡池名称 |
| status | DRAFT/ONLINE/OFFLINE |
| start_time | 开始时间 |
| end_time | 结束时间 |

### draw_activity

抽卡活动表。

| 字段 | 说明 |
| --- | --- |
| id | 活动 ID |
| name | 活动名称 |
| pool_id | 绑定卡池 |
| status | DRAFT/ONLINE/OFFLINE |
| start_time | 开始时间 |
| end_time | 结束时间 |

### draw_slot_rule

卡位概率规则。

| 字段 | 说明 |
| --- | --- |
| id | 规则 ID |
| pool_id | 卡池 ID |
| slot_no | 卡位，1-5 |
| rarity | 稀有度 |
| probability_basis_points | 概率，万分比 |

例：`7500` 表示 75%。

### new_card_protection_rule

新卡保护规则。

| 字段 | 说明 |
| --- | --- |
| min_collection_rate | 收集率下限 |
| max_collection_rate | 收集率上限 |
| new_card_weight_multiplier | 未拥有卡权重倍率 |

### user_draw_quota

用户每日抽卡次数。

| 字段 | 说明 |
| --- | --- |
| user_id | 用户 ID |
| free_quota | 免费次数 |
| extra_quota | 额外次数 |
| used_today | 当日已用次数 |
| course_reset_count_today | 当日完课重置次数 |
| practice_extra_count_today | 当日练习额外次数 |
| quota_date | 业务日期，北京时间 05:00 切日 |

### user_card

用户卡牌资产。

| 字段 | 说明 |
| --- | --- |
| user_id | 用户 ID |
| card_id | 卡牌 ID |
| rarity | 稀有度 |
| owned_count | 拥有数量 |
| first_obtained_at | 首次获得时间 |

### draw_order / draw_order_card

抽卡包与单包 5 张卡明细。

## 3. App/H5 接口

### 原型验证台

`GET /prototype.html`

给产品侧验证使用，包含：

- 查看活动/次数
- 抽一包
- 模拟完课重置
- 模拟练习加次数
- 查看我的卡册
- 查看抽卡历史
- 跑概率模拟报告

### 获取活动信息

`GET /api/card-draw/activity?userId=10001`

返回：

- 当前活动
- 当前卡池
- 免费/额外/可用次数
- 按钮文案
- 提示文案

### 抽卡

`POST /api/card-draw/draw?userId=10001`

返回：

- orderId
- hasNewCard
- 5 张卡牌
- 每张卡的 slotNo、cardId、rarity、isNew、ownedCount
- 剩余次数

### 我的卡册

`GET /api/card-draw/my-cards?userId=10001`

返回：

- 已获得卡牌数
- 总卡牌数
- 各稀有度进度
- 全量图鉴列表，未拥有卡 `ownedCount = 0`

### 抽卡历史

`GET /api/card-draw/history?userId=10001`

返回最近 20 包抽卡记录。

### 概率模拟报告

`GET /api/card-draw/simulation?packs=10000`

不消耗用户次数，不写用户资产。用于验证：

- 稀有度整体产出比例
- 每个卡位产出分布
- 第 4 张 SR 保底是否生效
- 第 5 张高期待位是否生效
- 每包至少 2 张 SR 以上
- 单包重复限制
- 模拟收集进度

### 完课重置

`POST /api/card-draw/reset-by-course?userId=10001`

过渡方案：当用户当前次数为 0 时，完课可重置到 2 次。每日最多 10 次。

### 练习加次数

`POST /api/card-draw/gain-by-practice?userId=10001`

最终方案占位：练习获得经验后可加 1 次额外抽卡。每日最多 20 次。

## 4. 后台接口

### 卡牌管理

- `GET /admin/cards?poolVersion=v1`
- `POST /admin/cards`
- `PUT /admin/cards/{id}`
- `POST /admin/cards/{id}/offline`

### 卡池/活动/规则查看

- `GET /admin/card-draw/pools`
- `POST /admin/card-draw/pools`
- `GET /admin/card-draw/activities`
- `POST /admin/card-draw/activities`
- `GET /admin/card-draw/slot-rules?poolId=1`
- `GET /admin/card-draw/new-card-protection-rules`

## 5. 抽卡算法说明

每张卡分两步：

1. 先按卡位概率抽稀有度。
2. 再进入对应稀有度卡池抽具体卡牌。

新卡保护只影响第二步，不改变稀有度概率。

单包重复限制：

- 第 4、5 张不重复
- 同一张卡最多出现 2 次
- 单包最多出现一组重复

连续无新卡保护：

- 最近 3 包都没有新卡时，下一包优先在第 1-4 张中替换一张同稀有度新卡
- 不强行占用第 5 张高期待位
