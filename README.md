# Card Draw Backend

单词集卡抽卡项目，包含 C 端 H5、Java 17 + Spring Boot 3 后端、卡池配置、卡册资产、每日次数、抽卡历史、新卡保护策略和轻量管理接口。

当前 `0920-new` 分支的 H5 使用本地卡池配置与浏览器存储完成抽卡、每日次数、卡册和历史，可由 Spring Boot 或静态服务器托管。仓库同时保留 Java 后端 API。

## 当前实现范围

- 默认卡池：海底世界（`عالم تحت البحر`），版本 `v3`，共 120 张单词卡，首页默认居中。
- 左侧卡池：`مملكة الحيوانات`，版本 `v2`，共 80 张动物主题单词卡。
- 右侧卡池：`كأس العالم الحماسي`，版本 `v1`，共 120 张世界杯主题单词卡。点击侧边卡包可切换到中央，URL 的 `poolVersion` 参数仍可指定默认卡池。
- 服务端卡池配置：`src/main/resources/card-pool/v1.json`、`v2.json`、`v3.json`。
- H5 本地配置：`src/main/resources/static/assets/h5/config/card-pool-v1.json`、`card-pool-v2.json`、`card-pool-v3.json`。
- 第三期资源：`card-images-v3/` 内 120 张整卡图片，`audio/words-v3/` 内 120 段配音，以及 `pack/ocean-pack*.png` 的完整卡包和开包上下部素材。
- 第三期清单来源：`3期海底世界主题单词卡清单_120张_修订版(1).xlsx` 的 Sheet1；编号、英文单词、中文释义、稀有度与原表一致，另补阿拉伯语显示释义。`manta ray` 的 SSR、UR 各保留一张，并按稀有度区分资源。
- H5 入口：`src/main/resources/static/h5-c.html`。
- 后台管理页：`src/main/resources/static/admin.html`。
- 每日免费次数：默认 5 次。
- 业务日切点：北京时间 05:00。
- 每次抽卡固定 5 张卡。
- 服务端保存：卡册、每日次数、抽卡历史、抽卡订单与单卡结果。
- 支持 `requestId` 抽卡幂等，避免重复提交重复扣次数。
- 支持卡牌/卡池/概率配置的基础管理接口。
- H5 已接入新版卡包、卡册、开包结果页面和 Trackify 埋点 SDK。
- 埋点公共 `t` 值：`card.000001`。
- H5 静态资源版本号统一维护在 `src/main/resources/static/h5-c.html` 顶部的 `H5_BUILD_VERSION`；每次发布时更新一次，卡框、配置、JS、关键图片、音频和视频会自动追加对应 query 参数。

## 抽卡规则

`v1`、`v2` 和 `v3` 当前使用一致的卡位基础概率：

| 卡位 | 概率 |
| --- | --- |
| 第 1-3 张 | R 95%，SR 5% |
| 第 4 张 | R 40%，SR 58%，SSR 2% |
| 第 5 张 | SR 80%，SSR 19%，UR 0.5%，LR 0.3%，XR 0.2% |

累计与新卡兜底规则：

- 用户累计第 2 次抽卡时，第 5 张固定为 SSR。
- 用户每累计抽卡 6 次，第 5 张必得 UR 及以上卡牌，在 UR、LR、XR 中按第 5 张原始权重抽取。
- 当同一卡池收集率为 51%-99%，且连续 2 次抽卡都没有新卡时，下一次抽卡会优先尝试按卡位可出现稀有度替换为未拥有卡。
- 当同一卡池收集率为 51%-99%，且当天前 4 次抽卡中新卡轮次少于 2 次时，第 5 次抽卡会优先尝试按卡位可出现稀有度替换为未拥有卡，用于提升“每日 5 轮至少约 2 轮出新卡”的稳定性。

重复与保护规则：

- 第 1-5 张均按当前稀有度收集率提高未拥有卡权重。
- 第 4、5 张不与本包内任何卡重复。
- 同一张卡单包最多出现 2 次。
- 单包最多出现一组重复卡。

新卡保护权重：

| 当前稀有度收集率 | 未拥有卡权重 |
| --- | --- |
| 0-30% | x2.6 |
| 31-50% | x2.4 |
| 51-70% | x2.2 |
| 71-85% | x2.0 |
| 86-95% | x1.7 |
| 96-99% | x1.4 |
| 100% | x1.0 |

## 埋点清单

埋点 SDK 使用 `trackify-sdk 3.0.0`，公共 `t` 值统一为：

```text
card.000001
```

公共参数：

| 字段 | 说明 |
| --- | --- |
| `project` | 固定为 `card_tcg` |
| `page_name` | 固定为 `word_card_h5` |
| `unitid` | 用户标识，优先取 URL 参数 `unitid/userId`，否则取本地测试用户 |
| `userId` | 同 `unitid`，用于大数据侧解析用户 |
| `poolVersion` | 当前卡池版本，例如 `v1` |
| `page_id` | 当前 H5 页面容器 ID |
| `stage` | 抽卡结果页内部阶段，部分事件携带 |

埋点实现约定：

- `trackCardEvent(action, extra)` 手动上报的事件会同时写入 Trackify、`window.cardTrack`、`NativeBridge.trackCardEvent`、iOS `messageHandlers.cardTrack`、`dataLayer` 和本地调试缓存。
- `payload.value` 保持为事件 `action`，不要改成用户 ID 或其他业务字段，避免兼容旧统计链路时丢失事件标识。
- 已手动上报的按钮不再配置 `data-analytics`，避免 Trackify 自动采集和手动上报重复。
- 仅纯点击、无额外业务上下文的按钮使用 `data-analytics` 自动采集。

页面访问事件：

| action | 触发时机 | 关键字段 | 主要分析用途 |
| --- | --- | --- | --- |
| `PG_CARD_MAIN` | 进入首页 | `page_id` | 首页 PV、UV、cohort 留存入口 |
| `PG_CARD_COLLECTION` | 进入卡册页 | `page_id` | 卡册访问率 |
| `PG_CARD_PACK_SELECT` | 进入选择卡包页 | `stage=stageSelectPack` | 抽卡流程漏斗 |
| `PG_CARD_PACK_ENTER` | 进入开包页 | `stage=stagePack` | 抽卡流程漏斗 |
| `PG_CARD_RESULT_FLIP` | 进入逐张翻卡页 | `cardCount`、`stage=stageFlip` | 翻卡页到达率 |
| `PG_CARD_RESULT_FINAL` | 进入最终结果页 | `cardCount`、`stage=stageFinal` | 抽卡完成展示率 |
| `PG_CARD_DETAIL` | 卡册中打开卡牌详情 | `cardId`、`rarity`、`poolVersion` | 卡牌详情查看行为 |

首页点击事件：

| action | 触发时机 | 关键字段 | 主要分析用途 |
| --- | --- | --- | --- |
| `CK_CARD_MAIN_CLOSE` | 点击首页关闭按钮 | - | H5 退出行为 |
| `CK_CARD_MAIN_PACK` | 点击首页顶部卡包图进入抽卡流程 | `number_card_pre_day`、`usedToday`、`availableQuota`、`trigger` | 首页卡包入口点击率、当天第几次抽卡意图 |
| `CK_CARD_MAIN_DRAW` | 点击首页抽卡按钮进入抽卡流程 | `number_card_pre_day`、`usedToday`、`availableQuota`、`trigger` | 抽卡按钮点击率、当天第几次抽卡意图 |
| `CK_CARD_MAIN_POOL_PREVIEW` | 点击首页卡牌预览区域 | - | 预览区到卡册转化 |
| `CK_CARD_MAIN_COLLECTION` | 点击首页卡册入口 | `page=homePage`、`poolVersion` | 卡册入口点击率 |
| `CK_CARD_MAIN_BATTLE_PASS` | 点击首页战令入口 | - | 预留功能兴趣度 |

选包与开包事件：

| action | 触发时机 | 关键字段 | 主要分析用途 |
| --- | --- | --- | --- |
| `CK_CARD_PACK_SELECT_BACK` | 选择卡包页点击返回 | - | 抽卡流程中断 |
| `CK_CARD_PACK_SELECT` | 点击某一个卡包 | `packIndex` | 用户选择卡包位置偏好 |
| `CK_CARD_PACK_ENTER_BACK` | 开包页点击返回选择卡包页 | - | 抽卡流程中断 |
| `CK_CARD_PACK_OPEN` | 点击开包按钮 | `number_pack_pre_day`、`number_card_pre_day`、`usedToday`、`availableQuota` | 当天第几个卡包打开意图 |
| `OC_CARD_PACK_OPEN_SUCCESS` | 开包成功并拿到 5 张卡 | `number_pack_pre_day`、`number_card_pre_day`、`cardCount`、`rarities`、`hasPremium` | 每日成功抽卡次数、高稀有命中分析 |

抽卡结果事件：

| action | 触发时机 | 关键字段 | 主要分析用途 |
| --- | --- | --- | --- |
| `CK_CARD_RESULT_NEXT` | 翻卡页点击继续查看下一张 | `cardId`、`rarity`、`cardIndex`、`cardCount` | 翻卡完成度、卡牌逐张曝光 |
| `CK_CARD_RESULT_COLLECT_ALL` | 翻卡页点击右下角快进按钮或说明文字直接查看最终结果 | `page=resultPage`、`stage=stageFlip`、`cardIndex`、`cardCount`、`remainingCardCount`（未翻开剩余张数） | 跳过逐张翻卡、直接收集的使用率 |
| `CK_CARD_RESULT_CARD` | 最终结果页点击某张卡 | `cardId`、`rarity`、`isNew` | 结果页卡牌关注度 |
| `CK_CARD_RESULT_SAVE_ALBUM` | 点击“存入卡册” | `page=resultPage`、`stage=stageFinal`、`cardCount`、`newCardCount`、`poolVersion` | 抽卡结果保存转化 |
| `CK_CARD_RESULT_CONTINUE` | 最终结果页无新卡时点击“继续”返回首页 | `page=resultPage`、`stage=stageFinal`、`cardCount`、`newCardCount=0`、`poolVersion` | 无新卡结果页返回首页转化 |

卡册事件：

| action | 触发时机 | 关键字段 | 主要分析用途 |
| --- | --- | --- | --- |
| `CK_CARD_COLLECTION_BACK` | 卡册页点击返回首页 | - | 卡册页退出 |
| `CK_CARD_COLLECTION_POOL_TOGGLE` | 展开或收起卡池 | `poolVersion`、`poolName` | 卡册查看深度 |
| `CK_CARD_COLLECTION_CARD_DETAIL` | 点击已获得卡牌 | `cardId`、`rarity`、`poolVersion` | 已获得卡牌详情点击 |
| `CK_CARD_COLLECTED_CONTINUE` | 保存入册后点击“继续”返回首页 | `page=albumPage`、`source=saveAlbum`、`poolVersion`、`cardCount`、`newCardCount`、`usedToday`、`availableQuota` | 入册后继续流程完成率 |
| `CK_CARD_DETAIL_CLOSE` | 卡牌详情页点击关闭 | - | 详情页退出 |

异常事件：

| action | 触发时机 | 关键字段 | 主要分析用途 |
| --- | --- | --- | --- |
| `OC_CARD_LOAD_FAILED` | H5 初始化或卡池加载失败 | `message` | 上线稳定性监控 |

数据分析建议：

- cohort 留存：优先使用 `PG_CARD_MAIN`，按 `unitid/userId` 或 SDK `uuid` 聚合。
- 用户每日抽卡次数：优先使用 `OC_CARD_PACK_OPEN_SUCCESS`，按 `number_pack_pre_day` 或 `number_card_pre_day` 分析。
- 抽卡漏斗：`PG_CARD_MAIN -> CK_CARD_MAIN_DRAW/CK_CARD_MAIN_PACK -> PG_CARD_PACK_SELECT -> CK_CARD_PACK_SELECT -> PG_CARD_PACK_ENTER -> CK_CARD_PACK_OPEN -> OC_CARD_PACK_OPEN_SUCCESS -> PG_CARD_RESULT_FINAL`。
- 高稀有命中：使用 `OC_CARD_PACK_OPEN_SUCCESS.rarities` 和 `hasPremium`。
- 卡册兴趣：使用 `CK_CARD_MAIN_COLLECTION`、`PG_CARD_COLLECTION`、`CK_CARD_COLLECTION_CARD_DETAIL`。

## 卡池配置

服务端卡池配置路径：

```text
src/main/resources/card-pool/v1.json
src/main/resources/card-pool/v2.json
src/main/resources/card-pool/v3.json
```

H5 本地兜底配置路径：

```text
src/main/resources/static/assets/h5/config/card-pool-v1.json
src/main/resources/static/assets/h5/config/card-pool-v2.json
src/main/resources/static/assets/h5/config/card-pool-v3.json
```

配置中包含：

- 卡池名称、版本、卡包图片和展示排序。
- 每日免费次数。
- 卡牌列表、稀有度、图片和音频地址。
- 5 个卡位的概率。
- 新卡保护权重。
- 连续无新卡保护区间。
- 每日第 5 抽新卡保护区间。

静态配置由 `CardPoolConfigService` 加载校验，H5 读取对应本地配置。当前 Java `dev` 初始化仅写入第一期示例数据；第三期接入本次 H5 流程，未修改数据库初始化或旧 API 的数据源。

## 本地运行

推荐使用项目自带脚本：

```bash
./run-local.sh
```

脚本默认端口为 `18080`。启动后访问：

```text
http://localhost:18080/api/ping
http://localhost:18080/h5-c.html
http://localhost:18080/h5-c.html?poolVersion=v2
http://localhost:18080/h5-c.html?poolVersion=v1
http://localhost:18080/admin.html
http://localhost:18080/swagger-ui.html
http://localhost:18080/h2-console
```

如果本机已安装 JDK 17 和 Maven：

```bash
mvn spring-boot:run
```

本地默认启用 `dev` profile：

- 数据库：H2 内存库。
- JDBC URL：`jdbc:h2:mem:carddraw`
- H2 用户名：`sa`
- H2 密码：空。
- JPA：`ddl-auto=create-drop`。
- Flyway：关闭。

## Docker 运行

后端镜像：

```bash
docker build -t card-draw-backend .
docker run --rm -p 8080:8080 card-draw-backend
```

Gaea 静态镜像位于 `gaea/Dockerfile`，主要用于 H5 静态资源托管场景；如果使用纯静态部署，需要确认 `/api/` 由网关或后端服务提供。

```bash
docker build -f gaea/Dockerfile -t card-draw-h5:gaea .
docker run --rm -p 8080:8080 card-draw-h5:gaea
```

## API 快速参考

C 端接口：

```bash
curl 'http://localhost:18080/api/ping'
curl 'http://localhost:18080/api/card-draw/pools'
curl 'http://localhost:18080/api/card-draw/activity?userId=10001&poolVersion=v2'
curl -X POST 'http://localhost:18080/api/card-draw/draw?userId=10001&poolVersion=v2&requestId=test-001'
curl 'http://localhost:18080/api/card-draw/my-cards?userId=10001&poolVersion=v2'
curl 'http://localhost:18080/api/card-draw/history?userId=10001&poolVersion=v2'
curl -X POST 'http://localhost:18080/api/card-draw/simulate-next-day?userId=10001'
```

管理接口：

```text
GET  /admin/card-draw/pools
POST /admin/card-draw/pools
PUT  /admin/card-draw/pools/{id}
GET  /admin/card-draw/slot-rules?poolId={poolId}
PUT  /admin/card-draw/slot-rules?poolId={poolId}
GET  /admin/card-draw/new-card-protection-rules
GET  /admin/cards?poolVersion=v2
POST /admin/cards
PUT  /admin/cards/{id}
POST /admin/cards/{id}/online
POST /admin/cards/{id}/offline
POST /admin/cards/import-excel
POST /admin/resources/upload-image
POST /admin/resources/upload-audio
```

说明：

- `requestId` 用于抽卡幂等；同一用户重复提交相同 `requestId` 会返回同一笔抽卡结果。
- `simulate-next-day` 只用于本地和测试环境验证次日重置。
- 生产环境应通过网关、登录态或上游服务保证 `userId` 可信。

## 生产运行

生产环境启用 `prod` profile 并接入 MySQL：

```bash
SPRING_PROFILES_ACTIVE=prod \
MYSQL_URL='jdbc:mysql://mysql-host:3306/card_draw?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai' \
MYSQL_USERNAME='card_draw' \
MYSQL_PASSWORD='******' \
java -jar target/card-draw-backend-0.1.0-SNAPSHOT.jar
```

生产配置说明：

- 数据库建议使用公司 MySQL 5.7。
- 生产环境关闭 H2 Console。
- 生产环境 `ddl-auto=validate`。
- Flyway 默认启用，迁移脚本位于 `src/main/resources/db/migration`。
- Swagger 默认关闭，可通过 `SWAGGER_ENABLED=true` 临时开启。
- 数据库地址、账号、密码必须通过环境变量或配置中心注入，不写入仓库。

## 验证命令

```bash
mvn test
```

当前测试覆盖：

- 抽卡概率选择基础逻辑。
- 同一 `requestId` 的抽卡幂等。
- 并发抽卡下的次数扣减一致性。
- 静态卡池配置加载和校验。

## 资源与发布注意

- H5 资源位于 `src/main/resources/static/assets/h5/`。
- 卡牌图片、卡框、卡包、音频、视频资源如需迁移 OSS/CDN，需要同步更新卡池配置和 H5 资源引用。
- 服务端配置和 H5 本地兜底配置需要保持一致：`card-pool/*.json` 与 `static/assets/h5/config/card-pool-*.json`。
- 上传资源默认落到 `uploads/`，该目录用于本地/测试上传，不建议直接提交临时文件。
- 发布前确认埋点 SDK、公共参数、`userId/unitid`、`poolVersion` 与大数据口径一致。

## Git 提交参考

提交前建议执行：

```bash
mvn test
git status
```

只提交本次修改相关文件，例如：

```bash
git add README.md
git commit -m "docs: update 0803 new README"
git push origin HEAD:0803-new
```

注意：不要误提交本地临时文件或无关 HTML。

## 后续对接重点

- 接入正式登录态，避免生产环境直接信任前端传入的 `userId`。
- 明确 OSS/CDN 资源最终路径，并同步卡池配置。
- 按生产域名确认 Trackify 上报链路。
- 如后续调整卡池概率、新卡权重或新卡兜底区间，需要同时更新服务端配置、H5 兜底配置、迁移脚本和 README。
