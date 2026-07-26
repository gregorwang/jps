# Anime Japanese Lab Android：融合改造保留基线

日期：2026-07-10

## 产品灵魂

- 产品目标是借助动漫语境学习真实日语，不是复刻另一款学习 App。
- 数据结构继续以 `作品 -> 集数 -> 本集材料池 -> 训练/复盘` 为核心。
- Worker/Supabase 与根目录 Web 实现继续作为数据和行为事实源。
- 不引入手写模块，除非用户后续明确覆盖 `android-app/AGENTS.md` 的限制。

## 绝不能退化的能力

1. 作品与集数切换必须保留用户上次位置，并继续支持 K-ON!、Re:Zero 与远端目录扩展。
2. 训练题必须继续来自真实词汇、语法、句子和语言学题；不能换成 Mirror seed/mock 数据。
3. `LessonEngine`、`LabViewModel`、登录、云进度、错题与复盘行为不由视觉层改写。
4. Re:Zero 原声优先、显式音频字段优先、问题音频禁止自动播放等规则必须保留。
5. 完成页、路径页和反馈层显示的 XP、正确率、连续天数、能量均来自目标 App 状态，不使用固定演示数值。
6. 任何 Mirror 视觉加载失败时，核心文字、CTA、返回和复盘信息仍可使用。
7. reduced motion、48dp 点击目标、长中文/日文内容不裁切和纵向滚动能力必须保留。

## 当前技术优势

- Kotlin + Jetpack Compose 单模块，业务状态集中在 `LabUiState`，便于逐 section 替换表现层。
- 课程生成与答题判定已有纯 Kotlin 模型和 JVM 单测。
- 已有 Lottie/Rive、反馈事件、音效、触觉和静态 fallback 基础。
- `ui/completion` 已经从主训练屏拆出，适合作为第一条低风险视觉垂直切片。

## 当前基线风险

- `LessonScreen.kt`、`ReadAirScreen.kt` 和 `LabViewModel.kt` 体积过大；后续按 screen state-holder/content 边界拆分。
- 当前完整本地素材目录直接加入 app source set，Debug APK 体积较大；后续改成 production whitelist pack 与 debug Asset Lab。
- 当前缺少 Compose UI test、自动截图回归、release/R8 性能与无障碍门禁。
- 第三方参考素材只允许个人内部使用；公开分发前必须清权或替换成自有视觉。

