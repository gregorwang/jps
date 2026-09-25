# 素材改造（Asset Remix）

把项目里现有的番剧图片，统一改造成 v3「アニメの文法」的线稿 + 作品色网点风格，再放进 App。
Claude 负责定规范和写提示词；图像处理交给 Gemini / Antigravity 这类有图像编辑能力的工具来做。

> **状态（2026-09-25）：暂停。** 用 Gemini 试过，线稿效果不理想，以后有更好的工具再继续。
> 在那之前，App 里用**代码滤镜兜底**：头像和立绘用 `ColorMatrix` 先去色，再提高对比度，
> 然后叠一层作品色的网点图案（`Canvas` 画圆点，`BlendMode.Multiply`）。效果不如真正的线稿，
> 但足以让不同来源的原图统一成"黑白 + 作品色"。头像加载失败时显示汉字占位圆。

- 设计画布：https://claude.ai/artifact/9x3RkMeAtAYTN64i8T8HN4 （v3 页，设计语言第 09 条）
- 为什么要改造：原图的上色风格、分辨率和画风各不相同，直接放进 App 会显得很杂。统一成线稿之后，
  1) 和界面的墨线、网点是同一种语言；2) 小尺寸下更清楚；3) 两部番放在一起不打架。

## 1. 统一风格（所有产出都遵守）

| 项 | 规则 |
|---|---|
| 线条 | 纯墨色 `#1B1B19`，粗细一致（1024px 画布上约 3px），线条闭合、干净 |
| 底色 | 头像、立绘：透明背景；主视觉、背景：纯白 |
| 阴影 | 只能用**圆点网点**表现，颜色 = 作品色，密度约 30%，网点方向约 12° |
| 作品色 | けいおん！`#C4466F`（桜）· Re:ゼロ `#6A4FC4`（菫）· 通用/登录 `#C4466F` |
| 禁止 | 灰色渐变、厚涂、额外上色、文字、签名、水印、新增物件、改变五官和比例 |

## 2. 现有素材 → 要产出的东西

源文件都在 `app/src/main/res/drawable-nodpi/`。角色图大多是"设定图"：左边是脸部特写，右边是全身，而且分辨率很低（260–441px），所以需要**重绘**，不能只加滤镜。

| 源文件 | 产出 | 用在哪里 |
|---|---|---|
| `k_on_{yui,mio,ritsu,mugi,azusa,ui,jun,sawako}_character.gif` | 头像 + 立绘 | 选番头像、对话框立绘、字幕说话人、答对反应 |
| `rezero_{subaru,emilia,rem,ram,beatrice,puck,otto,frederica,echidna,petelgeuse}_character.jpg` | 头像 + 立绘 | 同上 |
| `course_kon.webp`、`course_rezero*.webp` | 主视觉 | 「学ぶ」页顶部、アイキャッチ |
| （无源图，全新生成） | 场景背景 | 登录校门、对话框背景 |

## 3. 输出规格与命名

放到 `android-app/local-anime-assets/remix/` 下，原图不动。之后由 Claude 负责转 webp、登记到代码里。

| 类型 | 目录 | 命名 | 尺寸 |
|---|---|---|---|
| 头像 | `avatars/` | `{work}_{char}_avatar.png` | 512×512，透明 |
| 立绘 | `sprites/` | `{work}_{char}_sprite.png` | 768×1152（2:3），透明 |
| 主视觉 | `keyvisuals/` | `{work}_kv.png` | 1600×900（16:9），白底 |
| 背景 | `backgrounds/` | `bg_{scene}.png` | 1080×720（3:2），白底 |

`{work}` 用 `kon` / `rezero1` / `rezero2` / `rezero3`，`{char}` 沿用源文件里的名字（`ui`、`emilia`…）。

## 4. 提示词

先把下面这段**通用风格段**放在每个提示词最后，`{WORK_COLOR}` 换成作品色。

```text
STYLE: Convert into clean monochrome manga line art. Pure black ink lines (#1B1B19),
consistent line weight (about 3 px at 1024 px), crisp closed outlines. No gray gradients,
no painterly shading, no color fills. Render shadows ONLY as a halftone screentone of small
round dots in {WORK_COLOR} (about 30% density, dot grid rotated ~12°). Keep the character's
exact identity: same face, hairstyle, eye shape, outfit, pose and expression. Redraw at high
resolution; do not simply upscale. Do not add objects, text, logos, signatures or watermarks.
Do not change proportions.
```

### A. 头像（每个角色一张）

```text
Use the face close-up from this character sheet. Crop to head and shoulders, face centered,
eyes on the upper third. Output a 512×512 square PNG with a transparent background.
Keep the face free of screentone; screentone is allowed only in hair shadows.
[STYLE…]
```

### B. 立绘（每个角色一张）

```text
From this character sheet, use ONLY the standing full-body figure and ignore the separate
face close-up. Isolate the figure and output a 768×1152 portrait PNG with a transparent
background. Keep the original pose and a friendly neutral expression; the figure should be
cut off at the knees if the full body does not fit.
[STYLE…]
```

### C. 主视觉（每部番一张）

```text
Redraw this promotional image as a single manga panel, 16:9, 1600×900, white background,
same composition and framing. Put screentone only on the largest dark areas
(hair, blazer, shadows under the instrument).
[STYLE…]
```

### D. 场景背景（全新生成，没有源图）

```text
Create an ORIGINAL background illustration for a visual novel: {SCENE}.
No people. Clean monochrome manga line art, black ink lines on white, clear perspective.
Halftone screentone dots in {WORK_COLOR} only in the sky and cast shadows.
1080×720. Keep the bottom 30% visually quiet because a dialogue box will cover it.
```

`{SCENE}` 建议的一组：

| 文件 | SCENE | 作品色 |
|---|---|---|
| `bg_school_gate` | a Japanese high school front gate in spring, cherry trees in bloom, a small clock tower, a vertical name plate on the gate pillar left blank | `#C4466F` |
| `bg_kon_clubroom` | a high school light-music club room: drum kit, amplifiers, a tea set on a desk, a whiteboard, afternoon window light | `#C4466F` |
| `bg_kon_home_morning` | a cozy Japanese suburban home, a bedroom doorway and hallway in the morning, curtains, soft window light | `#C4466F` |
| `bg_rezero_mansion` | a long hallway in a western-style fantasy mansion, tall windows, carpet, chandeliers | `#6A4FC4` |
| `bg_rezero_capital` | a medieval fantasy capital street with market stalls and stone buildings | `#6A4FC4` |
| `bg_rezero_forest` | a quiet forest path with tall trees and light rays | `#6A4FC4` |

## 5. 做法（保证整套风格统一）

1. **先做一张"风格锚点"**：用提示词 B 做 `kon_ui_sprite`，反复调到满意为止。
2. **之后每次请求都附上这张锚点图**，并在提示词里加一句：
   `Match the line weight, screentone size and density of the reference image exactly.`
3. **按作品分批做**：一部番的所有头像连着做，再做这部番的立绘，避免风格漂移。
4. 头像和立绘必须来自同一张源图，同一个角色两张图要能对得上。

## 6. 验收清单（每张图都过一遍）

- [ ] 缩到 44px 圆形里还能认出是谁
- [ ] 画面里没有灰色，只有墨线、白色和作品色网点
- [ ] 头像和立绘是透明背景，边缘没有白边
- [ ] 线条粗细和锚点图一致
- [ ] 没有文字、签名、水印，没有凭空多出来的东西
- [ ] 文件名、尺寸和第 3 节完全一致

## 7. 授权提醒

改造后的角色图仍然是原作角色的衍生图，和现有素材一样**只能用于个人 localSlim 版本**。
公开发布版继续受 `AJL_PUBLIC_ASSETS_CLEARED` 的发布检查约束（见 `ANDROID_ENVIRONMENT.md`）。
第 4 节 D 里全新生成的场景背景不来自原作，但仍要确认所用生成工具的使用条款。
