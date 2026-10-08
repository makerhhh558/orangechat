# 橘瓣聊天主题 CSS 参考

解析器只认下面列出的选择器和属性，其余静默忽略并列在「显示设置 → 聊天主题 (CSS)」页面上。
全部来源于 [ChatThemeCssParser.kt](app/src/main/java/me/rerere/rikkahub/data/theme/ChatThemeCssParser.kt)。

主题目录：`/sdcard/Android/data/me.rerere.orangechat.debug/files/themes/<主题名>/*.css`
进不去的话用主题页的「导入主题」按钮，支持 css 和 zip。改完点「重新加载」，不用重启。

---

## 选择器

同一行里多个类名表示「且」，如 `.bubble.user`。逗号分组可用。

### 气泡

| 写法 | 作用对象 |
|---|---|
| `.bubble` | 两侧气泡 |
| `.bubble.user` | 我方气泡 |
| `.bubble.assistant` | 对方气泡 |
| `.user-bubble` `.bubble-user` `.me` `.self` | 同 `.bubble.user` |
| `.assistant-bubble` `.bubble-assistant` `.ai` `.other` | 同 `.bubble.assistant` |

### 头像

| 写法 | 作用对象 |
|---|---|
| `.avatar` | 两侧头像 |
| `.avatar.user` | 我方头像 |
| `.avatar.assistant` | 对方头像 |
| `.user-avatar` `.avatar-user` | 同 `.avatar.user` |
| `.assistant-avatar` `.avatar-assistant` | 同 `.avatar.assistant` |

### 消息列表

`.message-list` `.messages` `.chat` `.chat-list` —— 四个等价。

### 语音气泡

`.voice` `.voice-bubble` `.audio` `.audio-bubble` —— 四个等价。

### 不支持的写法

```css
.bubble .avatar    /* 后代 */
.bubble > p        /* 子 */
#id  div  *        /* ID / 标签 / 通用 */
.bubble:hover      /* 伪类 */
.bubble[data-x]    /* 属性 */
.user              /* 单独的 user/assistant，没说是气泡还是头像 */
```

整块跳过，记到「未生效」列表，不会崩。

---

## 气泡属性

写在 `.bubble` / `.bubble.user` / `.bubble.assistant` 里。

| 属性 | 别名 | 取值 | 说明 |
|---|---|---|---|
| `border-radius` | — | 1~4 个长度或百分比 | 支持 `50%`，按短边折算，上限夹 50%。混写 dp 和 % 时整体按 % |
| `border-top-left-radius` | — | 长度或百分比 | 单角 |
| `border-top-right-radius` | — | 长度或百分比 | 单角 |
| `border-bottom-right-radius` | — | 长度或百分比 | 单角 |
| `border-bottom-left-radius` | — | 长度或百分比 | 单角 |
| `padding` | — | 1~4 个长度 | 第一个值纵向，第二个横向；上下、左右不支持分开设 |
| `padding-top` | `padding-bottom` `padding-vertical` | 长度 | 纵向内边距 |
| `padding-left` | `padding-right` `padding-horizontal` | 长度 | 横向内边距 |
| `background` | `background-color` | 颜色 | 不写则沿用主题色 |
| `color` | `text-color` | 颜色 | 文字色 |
| `max-width` | — | 百分比，如 `85%` | **只是上限**。气泡按内容收缩，超过上限才换行，短消息不会被撑满。不写则不限制宽度 |
| `border` | — | `1dp solid #ccc` / `none` | 简写 |
| `border-width` | — | 长度 | |
| `border-color` | — | 颜色 | |
| `segment-spacing` | `row-gap` | 长度 | 一条消息拆成多个气泡时的间隔，**不是**消息之间的间距 |

### 尖钩

| 属性 | 别名 | 取值 | 说明 |
|---|---|---|---|
| `tail` | `tail-visible` | `true` / `false` | 开关 |
| `tail-size` | `tail-width` | 长度 | 伸出气泡的距离，`0` 等于关掉 |
| `tail-height` | `tail-base` | 长度 | 底边长度，不写则按 `tail-size` 推算 |
| `tail-position` | — | 见下表 | |
| `tail-shape` | — | `triangle`（别名 `isosceles`）/ `right-triangle`（别名 `right` `edge`） | |
| `tail-offset` | `tail-top` `tail-margin` | 长度，可负 | 沿边缘的偏移，从所在角起算 |
| `tail-color` | `tail-background` | 颜色 | **实际改的是整个气泡背景**，尖钩和气泡是同一条 Path，没法分开上色 |

`tail-position` 取值：

| 值 | 别名 |
|---|---|
| `auto` | — （按消息方向自动贴边，我方贴右对方贴左，竖直方向贴顶） |
| `none` | `hidden` `off` |
| `top-left` | `left-top` |
| `top-right` | `right-top` |
| `center-left` | `left-center` `left` `middle-left` |
| `center-right` | `right-center` `right` `middle-right` |
| `bottom-left` | `left-bottom` |
| `bottom-right` | `right-bottom` |

下划线写法也认，`top_left` 等价 `top-left`。

---

## 头像属性

| 属性 | 别名 | 取值 | 说明 |
|---|---|---|---|
| `size` | `width` `height` | 长度 | |
| `shape` | `avatar-type` | `circle` / `rounded` / `square` | 见下表别名 |
| `border-radius` | — | **只认长度，不支持百分比** | 写了会自动把 `shape` 从 circle 切成 rounded |
| `gap` | `margin` `margin-right` `margin-left` | 长度 | 与气泡/昵称的间距 |
| `position` | `avatar-position` | `top` / `side` | `top` = 气泡上方一行，`side` = 与气泡并排 |
| `display` | `visible` `visibility` | `true` / `false` | 不写则沿用显示设置里的开关 |
| `border` | — | `1dp solid #ccc` / `none` | |
| `border-width` | — | 长度 | |
| `border-color` | — | 颜色 | |

`shape` 取值别名：

- `circle` = `round` `circular`
- `rounded` = `rounded-square` `roundrect` `squircle`
- `square` = `rect` `rectangle`

`position` 取值别名：

- `top` = `above` `header`
- `side` = `left` `inline` `beside`

---

## 消息列表属性

| 属性 | 别名 | 取值 | 说明 |
|---|---|---|---|
| `gap` | `spacing` `row-gap` `margin` `margin-bottom` `margin-top` `margin-vertical` `message-spacing` `bubble-spacing` | 长度 | **消息之间**的间距 |
| `padding` | `padding-left` `padding-right` `padding-horizontal` | 长度 | 列表左右内边距 |

---

## 语音气泡属性

| 属性 | 别名 | 取值 |
|---|---|---|
| `border-radius` | — | 长度（不支持百分比） |
| `background` | `background-color` | 颜色 |
| `waveform-color` | `wave-color` `color` | 颜色 |
| `icon-size` | — | 长度 |
| `waveform-width` | `wave-width` | 长度 |
| `waveform-height` | `wave-height` | 长度 |
| `bar-count` | `wave-count` | 正整数，上限 200 |
| `bar-width` | — | 长度 |
| `padding` | — | 1~4 个长度 |
| `padding-top` | `padding-bottom` `padding-vertical` | 长度 |
| `padding-left` | `padding-right` `padding-horizontal` | 长度 |

---

## 取值格式

**长度**：`12` `12dp` `12px` `0`。除 `tail-offset` 外不能为负。

**百分比**：`85%`，也可写 `0.85`。只有气泡的 `border-radius` 和 `max-width` 吃百分比。

**颜色**：

- `#rgb` `#rgba` `#rrggbb` `#rrggbbaa`
- `rgb(255, 0, 0)` `rgba(255, 0, 0, 0.5)`，分量支持百分比
- `transparent` / `none`
- 色名：`black` `white` `red` `green` `blue` `yellow` `orange` `purple` `pink` `gray`/`grey` `lightgray` `darkgray` `cyan` `magenta` `brown` `gold` `silver` `navy` `teal` `olive` `lime` `indigo` `violet` `skyblue` `lightblue` `lightgreen` `beige` `ivory` `khaki` `salmon` `coral` `tomato` `orchid` `plum` `wheat` `snow`

**布尔**：

- 真：`true` `yes` `on` `1` `visible` `show` `block`
- 假：`false` `no` `off` `0` `hidden` `hide` `none`

**border 简写**：`1dp solid #ccc`、`1px #ccc`、`none`、`0`。`solid` `dashed` `dotted` `double` `groove` `ridge` 都接受但只有 solid 的效果（实线）。

值写错时那一条声明被跳过，同块其余声明照常生效，错误带行号显示在主题页。

---

## 出错行为

| 情况 | 行为 |
|---|---|
| 属性名不认识 | 静默忽略，列在「未生效属性」 |
| 选择器不认识 | 整块跳过，列在「未生效属性」 |
| 值非法 | 跳过**那一条**声明，同块其余照常生效，报错带行号 |
| 缺少 `}` | 报「`<选择器>` 的块缺少闭合的 `}`」，指向漏写的那一行；后面的规则仍能正常解析 |
| 多余 `}` | 报「多余的 `}`」 |
| `/*` 没闭合 | 报「注释 `/*` 缺少闭合的 `*/`」 |
| 解析器自身异常（文件损坏） | 整体回退内置默认样式 |

任何情况都不崩。

---

## 示例

```css
/* 消息列表 */
.message-list {
    gap: 12dp;
    padding: 16dp;
}

/* 两侧公共 */
.bubble {
    border-radius: 18dp;
    padding: 8dp 12dp;
    max-width: 85%;
    tail-width: 7dp;
    tail-height: 14dp;
    tail-position: auto;
    tail-shape: triangle;
    tail-offset: 14dp;
    segment-spacing: 4dp;
}

/* 我方 */
.bubble.user {
    background: #95ec69;
    color: #111111;
    border-radius: 18dp 4dp 18dp 18dp;
}

/* 对方 */
.bubble.assistant {
    background: #ffffff;
    color: #222222;
    border: 1dp solid #e5e5e5;
    border-radius: 4dp 18dp 18dp 18dp;
}

/* 头像 */
.avatar {
    size: 40dp;
    shape: rounded;
    border-radius: 10dp;
    gap: 8dp;
    position: side;
}

.avatar.assistant {
    border: 1dp solid #e0e0e0;
}

/* 语音 */
.voice {
    border-radius: 16dp;
    padding: 8dp 12dp;
    icon-size: 24dp;
    waveform-width: 60dp;
    waveform-height: 24dp;
    waveform-color: #4c8ef7;
    bar-count: 24;
    bar-width: 2.5dp;
}
```

胶囊气泡 + 圆头像：

```css
.bubble { border-radius: 50%; }
.avatar { shape: circle; size: 40dp; }
```
