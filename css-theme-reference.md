# 橘瓣聊天主题 CSS 参考

解析器只认下面列出的选择器和属性，其余静默忽略并列在「显示设置 → 聊天主题 (CSS)」页面上。
全部来源于 [ChatThemeCssParser.kt](app/src/main/java/me/rerere/rikkahub/data/theme/ChatThemeCssParser.kt)。

主题目录：`/sdcard/Android/data/<包名>/files/themes/<主题名>/*.css`
正式版包名是 `me.rerere.orangechat`，调试版会多一个 `.debug` 后缀。
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

| 写法 | 作用对象 |
|---|---|
| `.voice` `.voice-bubble` `.audio` `.audio-bubble` | 两侧语音气泡 |
| `.voice.user` `.user-voice` `.voice-user` | 我方语音 |
| `.voice.assistant` `.assistant-voice` `.voice-assistant` | 对方语音 |

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
| `border-radius` | — | 1~4 个长度或百分比 | 裸数字是 dp，`border-radius: 1` 就是 1dp，百分比必须带 `%`。`50%` 按短边折算，上限夹 50%。混写 dp 和 % 时整体按 % |
| `border-top-left-radius` | — | 长度或百分比 | 单角 |
| `border-top-right-radius` | — | 长度或百分比 | 单角 |
| `border-bottom-right-radius` | — | 长度或百分比 | 单角 |
| `border-bottom-left-radius` | — | 长度或百分比 | 单角 |
| `padding` | — | 1~4 个长度 | 1 个值四边相同，2 个值是纵向、横向。3 个或 4 个值时只取前两个（纵向、横向），后面的忽略；上下、左右不支持分开设 |
| `padding-top` | `padding-bottom` `padding-vertical` | 长度 | 纵向内边距 |
| `padding-left` | `padding-right` `padding-horizontal` | 长度 | 横向内边距 |
| `background` | `background-color` | 颜色，或 `linear-gradient(...)` | 不写则沿用主题色。渐变见下节；`background-color` 只接受纯色，并清掉渐变 |
| `opacity` | — | `0`~`1`，或百分比 | **只作用于气泡背景**（含渐变），文字和贴纸不受影响 |
| `color` | `text-color` | 颜色 | 文字色 |
| `max-width` | — | 百分比，如 `85%` | **只是上限**。气泡按内容收缩，超过上限才换行，短消息不会被撑满。不写则不限制宽度 |
| `border` | — | `1dp solid #ccc` / `none` | 简写 |
| `border-width` | — | 长度 | |
| `border-color` | — | 颜色 | |
| `segment-spacing` | `row-gap` | 长度 | 一条消息拆成多个气泡时的间隔，**不是**消息之间的间距 |

### 背景渐变

`background` 可以写 `linear-gradient(<方向>, <颜色>, <颜色>...)`。方向可省略，默认 `180deg`（从上到下）。

| 写法 | 含义 |
|---|---|
| `45deg` / `45` | 角度。`0deg` 向上，顺时针递增 |
| `to right` | `90deg`。同样有 `to top` `to bottom` `to left` |
| `to top right` | 对角。`top/bottom` 与 `left/right` 可换顺序 |

至少两个颜色，均匀分布。颜色后面的停靠位置（如 `red 20%`、`#fff 0px`）直接丢掉，颜色照样画、仍均匀铺开。
尖钩和气泡是同一条轮廓，渐变会一起铺上去。后写的纯色 `background` / `background-color` / `tail-color` 会清掉渐变。

```css
.bubble.user { background: linear-gradient(45deg, #95ec69, #4c8ef7); }
.bubble.assistant { background: linear-gradient(to right, #ffffff, #f5f5f5); }
```

### 阴影

| 属性 | 别名 | 取值 | 说明 |
|---|---|---|---|
| `box-shadow` | `shadow` | `<offset-x> <offset-y> [blur] [spread] <颜色>` / `none` | 沿气泡实际形状绘制，包括尖钩（两者是同一条 Path） |

颜色可以写在任意位置，省略时按 `rgba(0, 0, 0, 0.25)`。偏移允许负值，`blur` / `spread` 不能为负。
多重阴影（逗号分隔）**只取第一个**，`inset` 不支持（算值非法，跳过那一条）。

```css
.bubble.assistant { box-shadow: 0 2dp 6dp rgba(255, 182, 193, 0.1); }
.bubble.user      { box-shadow: none; }
```

> **Android 9 以下不显示阴影。** 硬件加速下底层的 `setShadowLayer` 对 Path 绘制只在 API 28+ 生效，
> 低版本退化成无阴影，其余样式不受影响，不会崩。
>
> 气泡本体范围内的阴影会被挖掉，只留在轮廓外面。`opacity` 小于 1 时，半透明背景底下不会透出一块深色。
> 这条是绘制时用 `clipOutPath` 做的，真机上再看一眼确认观感。

### 正文字体

只影响气泡**正文**，不影响昵称、时间这些周边文字。两侧可分别设置。

| 属性 | 别名 | 取值 | 说明 |
|---|---|---|---|
| `font-size` | — | 长度，`dp` / `sp` | 写 `sp` 跟随系统字体大小，`dp` 不跟随 |
| `font-weight` | — | `normal` / `bold` / `100`~`900` | 关键字：`regular`=`normal`(400)，`lighter` `light` `thin`=300，`medium`=500，`semibold` `semi-bold` `demibold`=600，`bolder` `black` `heavy`=900。非百位数就近取整 |
| `line-height` | — | 长度或倍数，如 `1.5` | 不带单位且不超过 4 按倍数（`1.5` = 1.5 倍字号）；超过 4 按 sp（`24` = 24sp）。带 `dp`/`px`/`sp` 时按写出的单位。`normal` 恢复默认 |
| `letter-spacing` | — | 长度，可负 | `normal` 恢复默认 |
| `font-family` | `font` | `serif` / `sans-serif` / `monospace` / `cursive`，或 `url("MyFont.ttf")` | 别名：`sans` `sansserif`=`sans-serif`，`mono`=`monospace`，`default` `inherit` `initial`=系统默认。也认 `.otf`；相对路径从**当前主题文件夹**找。逗号分隔的回退链只取第一个能识别的 |
| `link-color` | — | 颜色 | 正文里链接的颜色 |
| `code-background` | `code-bg` | 颜色 | 行内代码和代码块的底色 |

字体文件加载失败时回退默认字体，不崩，主题页报一条错误。

**标题和代码块怎么跟随**：只有设置了 `font-size` 时，标题（`#`~`######`）和代码块才按同样的倍数缩放，
保持原有的相对比例；没设置就沿用内置字号，非主题场景完全不受影响。
引用块、加粗、斜体继承正文字号。Markdown 和带 HTML 的消息两条渲染路径行为一致。

### 贴纸

贴在气泡角上的小图，可以露在气泡外面（类似网页主题用 `::before` 贴的小猫图）。

| 属性 | 别名 | 取值 | 说明 |
|---|---|---|---|
| `sticker` | `sticker-image` | `url("cat.png")` / `https://...` / `none` | 相对路径从当前主题文件夹找 |
| `sticker-size` | — | 长度，必须 > 0 | 边长，默认 `24dp` |
| `sticker-position` | `sticker-corner` | `top-left` / `top-right` / `bottom-left` / `bottom-right` | 别名 `left-top` `right-top` `left-bottom` `right-bottom`，下划线写法也认。默认 `top-right` |
| `sticker-offset` | — | 两个长度 `x y`，可负 | 正值往气泡里缩，**负值往气泡外伸**。只写一个值则 x、y 相同 |
| `sticker-rotate` | — | 角度，如 `10deg` / `-10deg` | |
| `sticker-rotate-random` | — | 角度 | 在 `sticker-rotate` 基础上每条消息随机加减这个范围。种子取消息 id，**同一条消息角度固定，滚动不跳** |
| `sticker-opacity` | — | `0`~`1` | 也接受 `95%` |

```css
.bubble.assistant {
    sticker: url("cat.png");
    sticker-size: 32dp;
    sticker-position: top-right;
    sticker-offset: -10dp -10dp;
    sticker-rotate: 10deg;
    sticker-rotate-random: 8deg;
    sticker-opacity: 0.95;
}
```

要点：

- 贴纸叠在气泡上层，露出气泡外的部分不会被裁。
- 贴纸参与父容器的尺寸测量，但 `sticker-offset` 只在放置阶段生效、不参与测量。负偏移能让贴纸露在气泡外面；气泡比贴纸还小时会被撑到贴纸那么大。正常字号下气泡都比贴纸大，影响可忽略。
- 不挡长按、点击，气泡原有的交互照常。
- 图片加载失败就不显示贴纸，气泡正常显示。
- zip 导入时 css 旁边的图片（`png` `jpg` `jpeg` `webp` `gif` `bmp`）和字体（`ttf` `otf` `ttc`）会一起解压，子目录结构保留，所以 `url("images/cat.png")` 这种写法也能找到文件。

### 九宫格皮肤

用一张图当气泡皮肤。四个角不拉伸，四条边单向拉伸，中间双向拉伸。

| 属性 | 别名 | 取值 | 说明 |
|---|---|---|---|
| `border-image` | — | `url(...)`，或简写 `url(...) [slice] [fill] [/ width]`，或 `none` | 相对路径从当前主题文件夹找。`none` 关掉皮肤。`fill` 接受但忽略（中间区域本来就会铺满），`/ outset` 忽略。简写里没写的 slice / width 保留已有值 |
| `border-image-source` | — | `url(...)` / `https://...` / `none` | 只认图片地址，不认简写。`none` 关掉皮肤 |
| `border-image-slice` | — | 1~4 个非负像素 | 从原图切边，顺序上、右、下、左。可写 `px`。1 个值四边相同，2 个值是上下、左右，3 个值左右相同 |
| `border-image-width` | — | 1~4 个长度（dp） | 切好的边在气泡上画多宽。不写则用 slice 的数值当 dp |

```css
.bubble.assistant {
    border-image: url("skin.png");
    border-image-slice: 20 24 28 24;
    border-image-width: 12dp 16dp;
}
```

要点：

- 皮肤加载成功时不画 `background`、`border`、`box-shadow`，尖钩强制关闭。
- 图片加载失败就回退纯色气泡（照常画背景和边框），主题页报一条错误。本地文件缺失在加载时就报，远程地址等真正加载失败再报。
- 贴纸照常叠在皮肤上面。
- 语音气泡用同一组属性，见下方「语音气泡属性」。

### 尖钩

| 属性 | 别名 | 取值 | 说明 |
|---|---|---|---|
| `tail` | `tail-visible` | `true` / `false` | 开关 |
| `tail-size` | `tail-width` | 长度 | 伸出气泡的距离，`0` 等于关掉 |
| `tail-height` | `tail-base` | 长度 | 底边长度，不写则按 `tail-size` 推算 |
| `tail-position` | — | 见下表 | |
| `tail-shape` | — | `triangle`（别名 `isosceles`）/ `right-triangle`（别名 `right` `edge`） | |
| `tail-offset` | `tail-top` `tail-margin` | 长度，可负 | 沿边缘的偏移，从所在角起算 |
| `tail-color` | `tail-background` | 颜色 | **实际改的是整个气泡背景**，尖钩和气泡是同一条 Path，没法分开上色。同时清掉渐变 |

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
| `border-radius` | — | 长度或百分比 | 长度会把 `shape` 从 circle 切成 rounded。`50%` 及以上直接变成圆形；小于 `50%` 按短边百分比画圆角 |
| `gap` | `margin` `margin-right` `margin-left` | 长度 | 与气泡/昵称的间距 |
| `position` | `avatar-position` | `top` / `side` | `top` = 气泡上方一行，`side` = 与气泡并排 |
| `avatar-align` | `align` | `top` / `center` / `bottom` | **只在 `position: side` 时生效**，头像相对气泡的竖直对齐。别名 `start`=`top`，`middle`=`center`，`end`=`bottom`。默认 `top` |
| `display` | `visible` `visibility` | `true` / `false` | 不写则沿用显示设置里的开关 |
| `avatar-frame` | — | `url(...)` / `https://...` / `none` | 头像挂件。不写则沿用显示设置里的挂件；`none` 连显示设置的挂件也关掉 |
| `avatar-frame-size` | — | 长度或百分比 | 挂件相对头像的大小，默认 `140%`。后写的覆盖先写的 |
| `avatar-frame-offset` | — | 两个长度 `x y`，可负 | 相对头像中心的偏移。只写一个值则 x、y 相同 |
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

`position: side` 时，昵称和时间仍留在气泡上方（不带头像图标），头像挪到气泡旁边，按 `avatar-align` 竖直对齐。挂件叠在头像上层、居中，比头像大的部分不被裁，不改变头像占位，也不挤动气泡和昵称。图片加载失败就不显示挂件。

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
| `border-radius` | — | 长度或百分比 |
| `background` | — | 颜色，或 `linear-gradient(...)` |
| `background-color` | — | 颜色 |
| `waveform-color` | `wave-color` `color` | 颜色 |
| `text-color` | — | 颜色 |
| `opacity` | — | `0`~`1`，或百分比 |
| `box-shadow` | `shadow` | 同气泡，或 `none` |
| `border` | — | `1dp solid #ccc` / `none` |
| `border-width` | — | 长度 |
| `border-color` | — | 颜色 |
| `icon-size` | — | 长度 |
| `waveform-width` | `wave-width` | 长度 |
| `waveform-height` | `wave-height` | 长度 |
| `bar-count` | `wave-count` | 正整数，上限 200 |
| `bar-width` | — | 长度 |
| `padding` | — | 1~4 个长度 |
| `padding-top` | `padding-bottom` `padding-vertical` | 长度 |
| `padding-left` | `padding-right` `padding-horizontal` | 长度 |
| `sticker` | `sticker-image` | 同气泡 |
| `sticker-size` | `sticker-position` `sticker-offset` `sticker-rotate` `sticker-rotate-random` `sticker-opacity` | 同气泡 |
| `border-image` | — | 同气泡，含简写 |
| `border-image-source` | — | `url(...)` / `none` |
| `border-image-slice` | — | 1~4 个非负像素 |
| `border-image-width` | — | 1~4 个长度（dp） |

`border-radius` 的百分比按短边折算，上限夹 50%。`background` 的渐变规则和气泡一样，停靠位置忽略；`background-color` 只接受纯色并清掉渐变。`opacity` 只作用于背景。`color` 仍是波形色的别名，秒数和转写文字用 `text-color`。阴影、边框、贴纸的行为和文字气泡一样；有皮肤时不画背景、边框和阴影。padding 写在皮肤里面（文字和波形不会压到皮肤边上）。`.voice` 是两侧公共，`.voice.user` / `.voice.assistant` 只改对应一侧。

---

## 取值格式

**长度**：`12` `12dp` `12px` `0`。`px` 和 `dp` 等价，都是密度无关像素；裸数字也按 dp。`em` / `rem` 不支持，写了算值非法。除 `tail-offset`、`letter-spacing`、`box-shadow` 的偏移、`sticker-offset` 和 `avatar-frame-offset` 外不能为负。
字号（`font-size` `line-height` `letter-spacing`）还可以写 `16sp`，`sp` 跟随系统字体大小，`dp` 不跟随。

**百分比**：必须带 `%`，如 `85%`。例外：`max-width`、`opacity`、`sticker-opacity` 也接受裸小数（`0.85` = 85%）。吃百分比的有：气泡 / 头像 / 语音的 `border-radius`、`max-width`、`opacity`、`sticker-opacity`。`avatar-frame-size` 也吃百分比，但可以超过 `100%`（如 `140%`），且不接受裸小数。

**角度**：`10deg` `-10deg`，不带单位时按度处理。用于 `sticker-rotate` / `sticker-rotate-random`，以及 `linear-gradient` 的方向。渐变里 `0deg` 向上、顺时针递增。

**url()**：`url("cat.png")` `url('MyFont.ttf')` `url(cat.png)`，引号可省。
`http://` / `https://` 直接联网取；其余按**相对路径**从当前 css 所在的主题文件夹里找，
不允许用 `../` 跳出主题文件夹。找不到文件时该图层不显示（字体则回退默认字体，皮肤则回退纯色气泡）。

**渐变**：`linear-gradient(45deg, #95ec69, #4c8ef7)`、`linear-gradient(to right, white, #f5f5f5)`、`linear-gradient(to top right, red, blue)`。方向可省略。用于气泡和语音的 `background`。颜色后的停靠位置忽略。

**颜色**：

- `#rgb` `#rgba` `#rrggbb` `#rrggbbaa`
- `rgb(255, 0, 0)` `rgba(255, 0, 0, 0.5)`，分量支持百分比
- `transparent` / `none`
- 色名：`black` `white` `red` `green` `blue` `yellow` `orange` `purple` `pink` `gray`/`grey` `lightgray`/`lightgrey` `darkgray`/`darkgrey` `cyan` `magenta` `brown` `gold` `silver` `navy` `teal` `olive` `lime` `indigo` `violet` `skyblue` `lightblue` `lightgreen` `beige` `ivory` `khaki` `salmon` `coral` `tomato` `orchid` `plum` `wheat` `snow`

**布尔**：

- 真：`true` `yes` `on` `1` `visible` `show` `block`
- 假：`false` `no` `off` `0` `hidden` `hide` `none`

**border 简写**：`1dp solid #ccc`、`1px #ccc`、`none`、`0`。`solid` `dashed` `dotted` `double` `groove` `ridge` 都接受但只有 solid 的效果（实线）。

值写错时那一条声明被跳过，同块其余声明照常生效，错误带行号显示在主题页。
声明末尾的 `!important`（大小写不敏感，`!` 前后可以有空格）会在解析前剥掉，不影响取值。

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

    box-shadow: 0 2dp 6dp rgba(0, 0, 0, 0.12);

    font-size: 16sp;
    line-height: 1.4;
    letter-spacing: 0;
}

/* 我方：渐变 + 背景透明度 */
.bubble.user {
    background: linear-gradient(45deg, #95ec69, #4c8ef7);
    opacity: 0.9;
    color: #111111;
    border-radius: 18dp 4dp 18dp 18dp;
}

/* 对方：九宫格皮肤 + 贴纸 */
.bubble.assistant {
    border-image: url("skin.png");
    border-image-slice: 20 24 28 24;
    border-image-width: 12dp 16dp;
    color: #222222;

    font-weight: normal;
    font-family: sans-serif;
    link-color: #4c8ef7;
    code-background: #f2f2f2;

    sticker: url("cat.png");
    sticker-size: 32dp;
    sticker-position: top-right;
    sticker-offset: -10dp -10dp;
    sticker-rotate: 10deg;
    sticker-rotate-random: 8deg;
    sticker-opacity: 0.95;
}

/* 头像：并排、竖直居中、挂件 */
.avatar {
    size: 40dp;
    shape: rounded;
    border-radius: 10dp;
    gap: 8dp;
    position: side;
    avatar-align: center;
    avatar-frame: url("frame.png");
    avatar-frame-size: 140%;
    avatar-frame-offset: 0 2dp;
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
