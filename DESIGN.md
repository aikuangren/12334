---
version: alpha
name: Word Card Stadium
description: A bright, collectible, mobile-first visual system for the Word Card H5 experience and its restrained utility surfaces.
colors:
  primary: "#FFE66A"
  on-primary: "#111111"
  secondary: "#1FE3F5"
  tertiary: "#FF9B78"
  background: "#EDFAFF"
  background-soft: "#F8FDFF"
  text: "#111111"
  text-muted: "#59636D"
  surface: "#FFFFFF"
  success: "#35B86E"
  warning: "#FF9B45"
  danger: "#DC2626"
  rarity-n: "#1FF529"
  rarity-r: "#1FF529"
  rarity-sr: "#1FE3F5"
  rarity-ssr: "#2563EB"
  rarity-ur: "#7C3AED"
  rarity-lr: "#F97316"
  rarity-xr: "#DC2626"
typography:
  display:
    fontFamily: '-apple-system, BlinkMacSystemFont, "PingFang SC", "Microsoft YaHei", Arial, sans-serif'
    fontSize: 32px
    fontWeight: 900
    lineHeight: 1
  heading-xl:
    fontFamily: '-apple-system, BlinkMacSystemFont, "PingFang SC", "Microsoft YaHei", Arial, sans-serif'
    fontSize: 26px
    fontWeight: 900
    lineHeight: 1
  heading-lg:
    fontFamily: '-apple-system, BlinkMacSystemFont, "PingFang SC", "Microsoft YaHei", Arial, sans-serif'
    fontSize: 22px
    fontWeight: 900
    lineHeight: 1.2
  heading-md:
    fontFamily: '-apple-system, BlinkMacSystemFont, "PingFang SC", "Microsoft YaHei", Arial, sans-serif'
    fontSize: 20px
    fontWeight: 900
    lineHeight: 1.2
  body-lg:
    fontFamily: '-apple-system, BlinkMacSystemFont, "PingFang SC", "Microsoft YaHei", Arial, sans-serif'
    fontSize: 18px
    fontWeight: 700
    lineHeight: 1.25
  body-md:
    fontFamily: '-apple-system, BlinkMacSystemFont, "PingFang SC", "Microsoft YaHei", Arial, sans-serif'
    fontSize: 16px
    fontWeight: 500
    lineHeight: 1.4
  label-lg:
    fontFamily: '-apple-system, BlinkMacSystemFont, "PingFang SC", "Microsoft YaHei", Arial, sans-serif'
    fontSize: 16px
    fontWeight: 900
    lineHeight: 1
  label-md:
    fontFamily: '-apple-system, BlinkMacSystemFont, "PingFang SC", "Microsoft YaHei", Arial, sans-serif'
    fontSize: 14px
    fontWeight: 800
    lineHeight: 1.3
  caption:
    fontFamily: '-apple-system, BlinkMacSystemFont, "PingFang SC", "Microsoft YaHei", Arial, sans-serif'
    fontSize: 12px
    fontWeight: 700
    lineHeight: 1.4
rounded:
  card-mini: 4px
  sm: 8px
  md: 12px
  lg: 18px
  panel: 24px
  full: 9999px
spacing:
  xxs: 2px
  xs: 4px
  sm: 8px
  md: 12px
  lg: 16px
  xl: 22px
  2xl: 28px
  3xl: 36px
  mobile-gutter: 22px
  tablet-gutter: 34px
components:
  button-primary:
    backgroundColor: "{colors.primary}"
    textColor: "{colors.on-primary}"
    typography: "{typography.heading-md}"
    rounded: "{rounded.full}"
    width: 172px
    height: 47px
  button-primary-disabled:
    backgroundColor: "{colors.primary}"
    textColor: "{colors.text-muted}"
    typography: "{typography.heading-md}"
    rounded: "{rounded.full}"
  icon-button:
    backgroundColor: transparent
    textColor: "{colors.text}"
    rounded: "{rounded.full}"
    size: 34px
  page:
    backgroundColor: "{colors.background}"
    textColor: "{colors.text}"
    padding: 22px
    width: 375px
  environment-glow:
    backgroundColor: "{colors.tertiary}"
    textColor: "{colors.text}"
    rounded: "{rounded.full}"
  surface-panel:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.text}"
    rounded: "{rounded.panel}"
    padding: 20px
  quota-meter:
    backgroundColor: "{colors.background-soft}"
    textColor: "{colors.text}"
    typography: "{typography.body-md}"
    rounded: "{rounded.full}"
    height: 40px
  notice-muted:
    backgroundColor: transparent
    textColor: "{colors.text-muted}"
    typography: "{typography.caption}"
  luck-meter:
    backgroundColor: "{colors.success}"
    textColor: "{colors.text}"
    rounded: "{rounded.card-mini}"
    height: 7px
  luck-meter-near:
    backgroundColor: "{colors.warning}"
    textColor: "{colors.on-primary}"
    rounded: "{rounded.card-mini}"
    height: 7px
  danger-action:
    backgroundColor: transparent
    textColor: "{colors.danger}"
    typography: "{typography.label-md}"
  rarity-n:
    backgroundColor: "{colors.rarity-n}"
    textColor: "{colors.on-primary}"
    rounded: "{rounded.full}"
  rarity-r:
    backgroundColor: "{colors.rarity-r}"
    textColor: "{colors.on-primary}"
    rounded: "{rounded.full}"
  rarity-sr:
    backgroundColor: "{colors.rarity-sr}"
    textColor: "{colors.on-primary}"
    rounded: "{rounded.full}"
  rarity-ssr:
    backgroundColor: "{colors.rarity-ssr}"
    textColor: "{colors.surface}"
    rounded: "{rounded.full}"
  rarity-ur:
    backgroundColor: "{colors.rarity-ur}"
    textColor: "{colors.surface}"
    rounded: "{rounded.full}"
  rarity-lr:
    backgroundColor: "{colors.rarity-lr}"
    textColor: "{colors.on-primary}"
    rounded: "{rounded.full}"
  rarity-xr:
    backgroundColor: "{colors.rarity-xr}"
    textColor: "{colors.surface}"
    rounded: "{rounded.full}"
  card-preview:
    backgroundColor: transparent
    textColor: "{colors.text}"
    rounded: "{rounded.card-mini}"
    width: 52px
    height: 72px
  card-result:
    backgroundColor: transparent
    textColor: "{colors.text}"
    rounded: "{rounded.sm}"
    width: 92px
    height: 128px
  card-detail:
    backgroundColor: transparent
    textColor: "{colors.text}"
    rounded: "{rounded.md}"
    width: 258px
    height: 360px
  admin-primary:
    backgroundColor: "{colors.secondary}"
    textColor: "{colors.on-primary}"
    typography: "{typography.label-lg}"
    rounded: "{rounded.full}"
    height: 44px
---

# Word Card Stadium / 单词集卡设计系统

## Overview

这是一个面向移动端 App WebView 的单词集卡体验。视觉关键词是 **明亮、轻快、可收集、带一点球场庆典感**：浅冰蓝背景负责呼吸感，黄色负责推动行动，青色与珊瑚橙制造活力，正式卡框、卡包和主题底图承担收藏价值。

核心体验不是一套普通的白色卡片 UI，而是“进入卡池 → 选择卡包 → 开包 → 逐张翻卡 → 存入卡册”的仪式。界面应让内容和卡牌资源成为主角，文字与系统控件退后一步。高稀有卡可以明显更华丽，但首页和列表页必须保持轻量，避免所有元素同时争夺注意力。

设计事实的优先级如下：

1. `src/main/resources/static/assets/h5/` 下的正式图片、卡包、卡框、音视频资源。
2. `src/main/resources/static/h5-c.html` 中已经落地的尺寸、响应式规则、状态与动效。
3. 本文中的机器可读令牌和使用原则。
4. `h5-design.html`、`prototype.html` 与 `admin.html` 仅作为辅助参考，不得覆盖正式 H5 的视觉事实。

主要语言环境当前为阿拉伯语，页面使用 RTL；同时系统应能承载中文和英文。布局方向、返回箭头、文本对齐和动画起点必须跟随 `dir`，不得把 RTL 当作翻译完成后的补丁。

## Colors

色彩分成三层：安静的冰蓝环境、明确的黄色行动、由稀有度驱动的收藏色。

- **Primary / Action Yellow (`#FFE66A`)：** 主按钮、加载进度与少量关键提示。搭配近黑文字，不承载大面积正文背景。
- **Secondary / Energy Cyan (`#1FE3F5`)：** 扫光、环境光、高亮边缘和工具界面主操作。它是能量色，不应和黄色同时抢占主按钮地位。
- **Tertiary / Coral (`#FF9B78`)：** 背景渐变中的温暖平衡色，用于庆典感和柔和强调。
- **Background (`#EDFAFF` → `#F8FDFF`)：** 页面基底使用非常浅的冰蓝到近白渐变，并允许低透明度黄、青、珊瑚光晕缓慢流动。
- **Text (`#111111`)：** C 端核心文字采用近黑，确保在明亮背景和黄色按钮上清晰。
- **Muted (`#59636D`)：** 次级说明、剩余次数提示和非关键状态。

稀有度颜色是语义，不是装饰：

- N / R 使用亮绿；SR 使用亮青；SSR 使用皇家蓝；UR 使用紫色；LR 使用橙色；XR 使用红色。
- 稀有度必须同时由卡框、文字或标签表达，不能只依靠颜色。
- SSR 以上可逐级增加辉光、虹彩、颗粒和动态反射；N/R 不使用高级全息特效。
- XR 的红色与错误红同色时，必须通过位置、卡框和标签文案避免混淆。

## Typography

使用系统字体栈，优先保证 WebView 首屏速度、多语言覆盖和不同设备上的稳定性。不要为了“游戏感”引入难以加载或缺失阿拉伯字形的展示字体。

- 页面主标题使用 26px、900 字重，短、居中、有强识别度。
- 开包加载等强情绪标题可使用 32px、900 字重。
- 主按钮使用 20px、900 字重；常规按钮与关键数字使用 16–18px、700–900 字重。
- 正文默认 16px；说明文字 12–14px，字重保持 700 左右以抵抗复杂背景。
- 卡面单词与释义属于正式卡牌资产的排版层，遵循卡框可读区，不套用普通页面标题样式。
- 阿拉伯语不要人为增加字距；中文与英文也默认 `letter-spacing: 0`。避免全大写英文长标签。

文案节奏应短促、可行动：一个页面只给一个主要动作。抽卡流程中的提示优先使用“选择、点击、继续、存入”一类动词，不在动画高潮中放长段解释。

## Layout

正式 H5 采用 **mobile-first、居中、受控最大宽度** 的单列舞台：

- 手机基准宽度为 375px，水平安全边距 22px；页面至少占满一个视口高度。
- iPhone 长屏通过纵向间距变量增加呼吸感；短屏优先压缩间距和素材尺寸，不隐藏核心动作。
- 600px 以上进入平板布局，页面舞台约 520–560px，水平边距 34px；卡牌和入口随之放大。
- 900px 以上仍保持单个 H5 舞台，不把核心抽卡流程扩展成桌面多栏应用。
- 顶部、底部必须叠加 `safe-area-inset-*`，关闭、返回、保存等控件不得落入刘海或 Home Indicator 区域。

核心尺寸以已实现页面为准：

- 首页主题区：约 310px 高；卡包 119px 宽。
- 卡牌预览：325 × 106px 容器，单卡 52 × 72px。
- 主抽卡按钮：172 × 47px。
- 选包网格：约 316px 宽，单包 90 × 152px。
- 逐张翻卡：238 × 332px；卡牌详情：258 × 360px。
- 最终结果：两列布局，单卡 92 × 128px。
- 卡册：手机五列，单卡 48 × 67px；平板扩展到六至七列。

布局遵循“舞台中心优先”：卡包、当前翻开的卡、结果卡组始终占据视觉中心；返回按钮和说明退到边缘。不要用通用仪表盘栅格重排抽卡流程。

后台与验证台是例外：允许 1440px 最大宽度、多栏表单与数据网格，使用 24px 面板圆角、18–20px 内边距和半透明白色表面，但继续沿用冰蓝背景、系统字体和青色操作色。

## Elevation & Depth

深度来自四种手段，强度必须有层级：

1. **正式资产自身的边框与纹理**：卡框、卡包、主题底图拥有最高视觉权重。
2. **柔和投影**：普通可点击素材使用约 `0 7px 12px rgba(0,0,0,.18)`；悬浮卡包可用 `drop-shadow`。
3. **环境光与扫光**：大面积、低透明度、慢速移动，用于营造空气感而非显示状态。
4. **稀有卡特效**：仅在翻卡大卡和卡牌详情中完整启用；卡册缩略图只保留轻投影。

禁止用厚重黑框或大量相同强度的阴影制造层级。普通面板应像浮在浅色环境中的薄玻璃，高稀有卡才像真正的发光物体。

## Shapes

形状语言由“柔软 UI + 真实收藏卡”组成：

- 主按钮、进度条、标签和轻量状态使用胶囊形 `9999px`。
- 普通工具面板使用 18–24px 圆角。
- 卡牌缩略图只使用 4px 小圆角，避免削弱实体卡片的比例与边框。
- 卡牌详情可使用 8–12px 外层圆角，但不得裁掉正式卡框或全息边缘。
- 关闭与返回优先使用已有图片或图标资源；不使用 emoji、字符箭头或临时 CSS 图形替代正式图标。

保持素材原始比例。卡包、卡框、入口背景不拉伸、不压扁，不用不匹配的截图填充。图片使用 `object-fit` 时要先确认主体不会被裁切。

## Components

### Page shell

`.app` 负责全视口的渐变环境和缓慢光晕，`.page` 负责居中的内容舞台。页面切换以同一舞台内的状态变化为主，不引入桌面式侧边栏。RTL 页面必须在根节点声明方向；局部需要 LTR 的卡组、数字或动画轨道再单独覆盖。

### Top bar

顶部栏使用“三列镜像”结构：两侧 42px 保留区，中间标题居中，图标按钮 34px。标题视觉居中不能被单侧操作挤偏。返回图标应根据方向旋转，所有图标按钮至少保留 44px 的可触达热区，即使可见图标较小。

### Primary draw action

主抽卡、确认和存入卡册按钮使用正式 `button.png` 资源；`primary` 令牌是资源缺失时的颜色基线，不应默认用纯色 CSS 替代已经存在的按钮资产。

按钮允许 2.4 秒左右的轻微呼吸动画和周期性扫光，但点击后必须立即进入清晰的忙碌或不可用状态。次数用尽时同时降低饱和度、透明度并展示重置说明，不只依赖 `disabled` 属性。

### Pack and opening stage

首页卡包保持固定循环顺序：默认「动物｜海底世界｜世界杯」，点击右侧后依次为「海底世界｜世界杯｜动物」「世界杯｜动物｜海底世界」，再点击右侧回到默认排列；点击左侧沿反方向循环。侧边卡包可点击或通过键盘切换到中央，两侧取循环中的相邻卡包，不重新按新旧排序；不足三期时隐藏重复或空位。最新一期默认居中，显式 `poolVersion` 链接可覆盖默认选择，物理左右位置不随 RTL 交换。

选择页展示一组等权卡包，点击后进入单包舞台。开包动画顺序必须稳定：选择 → 确认 → 开包视频/声音 → 卡堆 → 逐张翻卡 → 最终结果。每个阶段只有一个主操作，返回行为不能跨越用户尚未确认的状态。

开包视频、点击音效和卡牌发音是体验的一部分，但必须尊重静音状态；同一张卡自动播放一次，用户点击不重复触发。

### Word card

卡牌必须使用正式卡面图片与卡框。不同语境采用不同渲染负载：

- 首页预览和卡册：静态缩略图，最多轻投影。
- 最终结果：展示稀有度与“NEW”状态，允许有限高亮。
- 逐张翻卡和详情：允许倾斜联动、全息、珠光、虹彩和闪烁粒子。

倾斜效果跟随鼠标或触摸，但幅度克制；离开后平滑回正。动效不能改变卡面文字的最终可读位置，也不能导致剩余卡堆跳动。

### Collection

卡册在手机上使用五列紧凑网格。未获得卡使用统一灰色占位与编号，已获得卡显示正式卡面；两者必须保持完全相同的尺寸和位置。卡池标题使用正式 `album-pool.png` / `album-rarity.png` 资源，并明确支持展开与收起。

保存入册后的飞行动画应从结果卡的真实位置移动到卡册目标格；目标不可见时优雅降级，不制造虚假的落点。

### Loading, empty, error, and unavailable states

- 加载态使用悬浮卡包、短文案和循环进度条，避免空白屏。
- 加载失败后暂停装饰动画，展示一个明确的重试按钮。
- 空卡册与锁定卡都需要可理解的文案或编号，不只显示灰块。
- 次数耗尽时显示下一业务日（北京时间 05:00）或倒计时说明。
- 所有动态状态都应有文本或结构性反馈，不只依靠颜色、声音或动效。

### Admin and prototype surfaces

后台使用浅冰蓝渐变、半透明白面板、青色主按钮和深蓝灰正文。圆角与阴影可以比 C 端更统一、更安静。优先保证表格、表单、概率合计、危险操作和响应式折行的可读性；不要把开包扫光、全息卡特效或漂浮动画移植到管理页面。

## Do's and Don'ts

### Do

- 优先复用 `assets/h5` 中已有卡包、卡框、背景、按钮、图标、音频和视频。
- 保持“浅冰蓝环境 + 黄色行动 + 稀有度收藏色”的层级关系。
- 用页面状态组织完整抽卡仪式，并保证每一步都可返回、可理解、可埋点。
- 同时检查 iPhone、iPad 和 App WebView；适配安全区、长短屏和 RTL。
- 让高稀有卡在大卡场景更华丽，在列表场景更安静。
- 为 `prefers-reduced-motion` 提供降级：停止背景流动、扫光、呼吸、漂浮和倾斜，同时保留状态变化。
- 保证触控目标至少 44 × 44px；正文与背景达到 WCAG AA，重要信息不只依赖颜色。

### Don't

- 不要把正式 H5 重做成通用 SaaS 仪表盘、纯白组件库或暗黑电竞界面。
- 不要凭空新增另一套主色、字体、卡框、图标风格或 8px 栅格来覆盖现有资产比例。
- 不要用 emoji、文本符号、CSS 画图或占位框替代已有的可见资产。
- 不要在首页、卡册和最终结果的所有卡牌上同时播放完整全息特效。
- 不要拉伸卡包和卡牌，不要裁掉卡框，不要让阴影或辉光遮住单词与释义。
- 不要把 RTL 仅实现为 `text-align: right`；导航方向、动画、轨道和触控位置也要镜像。
- 不要删除现有文件或资源；如确需移除，必须先取得项目所有者许可。
