# Anime Japanese Lab v0.1 产品文档

## 0. 项目定位

Anime Japanese Lab 是一个 Web-first / PWA-first 的日语学习系统。

它不是普通背词 App，也不是普通字幕播放器。

它的核心是：

**用日文动漫字幕语料，把一部番剧变成可学习、可检索、可手写、可跟读、可被 AI 精读分析的个人日语语言学实验室。**

第一版以《K-ON!》作为样板作品。

---

## 1. 核心目标

第一版要同时完成两件事：

### A. 学习训练系统

用于每天学习：

* 高频词
* 常见表达
* 语法点
* 跟读句
* 手写练习
* 每集学习计划
* 错词/弱项回炉

### B. 读空气 / 语言学 RAG 引擎

用于深度分析：

* 某一句话为什么这样说
* 助词和句末语气表达了什么
* 角色真实心理是什么
* 这段对话的空气怎么变化
* 某个角色说话习惯是什么
* 找相似场景、相似语气、相似心理

---

## 2. 技术栈

### 前端 / 全栈框架

* TanStack Start
* TanStack Router
* TanStack Query
* TanStack Form
* TanStack Table
* TanStack Virtual
* TypeScript
* React
* Tailwind CSS
* PWA

### 构建与开发工具链

使用 VoidZero 生态优先：

* Vite
* Vitest
* Oxc / Oxlint
* Rolldown，稳定可用时启用
* pnpm
* TypeScript strict mode

### 部署与后端

* Cloudflare Workers
* Cloudflare Hyperdrive
* Cloudflare Vectorize
* Cloudflare Workers AI
* Cloudflare AI Gateway，可选
* Supabase Postgres
* 现有 Edge TTS Worker

### 数据库

* Supabase Postgres 作为主数据库
* Cloudflare Hyperdrive 作为 Workers 到 Supabase 的连接层
* Vectorize 只存字幕 chunk 的向量，不当主数据库

---

## 3. 数据现状

当前已经有两类数据。

### 3.1 语料库数据

已经导入 Supabase：

* works
* episodes
* subtitle_lines
* subtitle_chunks

已经配置 Vectorize：

* jps / anime-japanese-lab-subtitles
* 用于日文字幕 chunk 语义检索

这部分用于：

* 读空气 RAG
* 场景检索
* 单句精读
* 角色语言画像
* AI 语境分析

### 3.2 学习材料数据

已经导入或准备导入 Supabase：

* learning_vocab_items
* learning_vocab_occurrences
* learning_grammar_points
* learning_sentences
* learning_exercises
* episode_learning_plans

这部分用于：

* 高频词卡
* 语法卡
* 跟读句
* 手写练习
* 每集学习计划

---

## 4. 关键原则

### 4.1 日文是主语料

系统的主语料必须是日文原文字幕。

中文只作为：

* 中文释义
* 中文对照
* AI 解释
* 剧情理解辅助

不能把中文字幕当成日语语言学主语料。

### 4.2 不为每部动漫单独建表

所有作品共用同一套表。

新增作品时，只追加数据：

* 新 work_slug
* 新 episodes
* 新 subtitle_lines
* 新 subtitle_chunks
* 新 learning materials
* 新 Vectorize chunks

不要创建：

* k_on_vocab
* kokoro_connect_vocab
* bocchi_vocab

这种按作品拆表的结构。

### 4.3 Vectorize 不存完整业务真相

Vectorize 只负责语义检索。

业务数据以 Supabase 为准。

查询流程应该是：

1. 用户问题生成 embedding
2. Vectorize 找相关 chunk
3. 根据 chunk id / metadata 回查 Supabase
4. 组装上下文
5. 交给 AI 分析

---

## 5. 第一版页面结构

### 5.1 首页

首页要像学习面板，不像后台。

内容：

* 今日学习作品
* 今日学习集数
* 高频词进度
* 手写任务
* 跟读任务
* 语法任务
* 读空气入口

示例：

```txt
今日学习：K-ON! EP01

今日任务：
- 高频词 20 个
- 手写词 10 个
- 跟读句 5 句
- 语法点 3 个
- 读空气分析 1 个场景

按钮：
[开始学习]
[只练手写]
[读空气分析]
```

---

### 5.2 作品页

展示所有已导入作品。

每个作品展示：

* 标题
* 集数
* 已导入字幕数
* 已生成学习材料数
* 已向量化状态
* 学习进度

第一版只有 K-ON! 也要按多作品设计。

---

### 5.3 集数页

单集学习中心。

模块：

* 本集高频词
* 本集语法点
* 本集跟读句
* 本集手写练习
* 本集台词浏览
* 本集读空气

---

### 5.4 高频词页

目标：解决“听得懂但认不出、写不出”的问题。

每张词卡显示：

* 日文表记
* 假名
* 罗马音
* 中文意思
* 词性/用途
* 出现次数
* 出现集数
* 是否适合手写
* 是否适合跟读
* 语气标签

操作：

* 播放 TTS
* 查看例句
* AI 精讲
* 加入手写
* 标记：会 / 模糊 / 不会
* 收藏

---

### 5.5 语法页

每集展示 8–15 个重点语法。

重点不是“语法书解释”，而是：

* 这句里为什么这样用
* 语气是什么
* 现实中能不能用
* 是不是动漫腔
* 和相近表达有什么区别

语法点类型包括：

* 助词
* 句末语气
* のだ / んだ
* って
* てしまう
* ないと
* しかない
* わけじゃない
* 省略
* 命令形
* 请求表达

---

### 5.6 跟读页

第一版使用 TTS 标准音，不做自动发音评分。

功能：

* 播放标准音
* 慢速播放
* 循环播放
* 显示假名
* 显示罗马音
* 显示中文意思
* 用户自评：像 / 一般 / 不像
* 不像的句子加入复习

TTS 调用现有 Edge TTS Worker。

---

### 5.7 手写练习页

第一版使用 Web Canvas，不做 OCR 自动判字。

适配 iPad Safari / PWA。

练习类型：

* 假名 → 写日文
* 中文 → 写日文
* 听音 → 写日文
* 原形 → 活用形
* 助词填空

页面布局：

```txt
左侧：题目
右侧：Canvas 手写区

按钮：
[播放]
[查看答案]
[清空]
[写对了]
[模糊]
[写错了]
```

---

### 5.8 台词浏览页

用于查看某一集的日文台词。

功能：

* 按时间顺序浏览台词
* 日文原文
* 中文对照
* 时间轴
* 点击一句进入单句精读
* 点击场景进入读空气分析

---

### 5.9 单句精读页

针对一句日文台词进行语言学分析。

AI 输出结构：

1. 字面意思
2. 词法拆解
3. 句法结构
4. 助词说明
5. 句末语气
6. 语用功能
7. 角色心理
8. 现实日语可用性
9. 相近表达对比

---

### 5.10 读空气页

这是项目的特色核心。

用户可以选择：

* 某一集
* 某个 chunk
* 某一句台词
* 自己输入问题

系统检索 Vectorize，拿到相关场景，再交给 AI。

问题示例：

```txt
这段对话里谁在回避？
这里为什么气氛突然尴尬？
唯这句话是天然还是装傻？
律这里是在开玩笑还是转移话题？
找一下类似“嘴上说没事但其实在意”的场景。
```

AI 输出结构：

1. 表层对话
2. 潜台词
3. 角色心理
4. 关系变化
5. 语言证据
6. 语气/句末/助词证据
7. 相似场景
8. 学习价值

---

### 5.11 角色语言画像页

按角色聚合语言现象。

第一版可以先做 K-ON! 的角色占位：

* 唯
* 澪
* 律
* 紬
* 梓

展示：

* 常见口癖
* 句末倾向
* 礼貌度
* 情绪表达方式
* 被吐槽/吐槽别人时的语言模式
* 典型场景

第一版角色识别不必完美，可以先通过 AI 对 chunk 进行分析生成。

---

### 5.12 AI 造句批改页

用户选择一个词或语法点，自己造句。

AI 返回：

* 语法是否正确
* 是否自然
* 语气是否合适
* 更自然改写
* 用法提醒
* 评分

---

## 6. 后端 API 设计

### 6.1 学习材料 API

需要提供：

* 获取作品列表
* 获取集数列表
* 获取某集高频词
* 获取某集语法点
* 获取某集跟读句
* 获取某集手写题
* 获取某集学习计划
* 更新用户自评状态

### 6.2 语料库 API

需要提供：

* 获取某集台词
* 获取某个 chunk
* 根据 chunk id 回查台词
* 根据时间轴查询台词
* 根据关键词搜索台词

### 6.3 RAG API

需要提供：

* 查询 Vectorize
* 回查 Supabase 上下文
* 组装 AI prompt
* 返回读空气分析

### 6.4 AI API

需要提供：

* 单词解释
* 语法解释
* 单句精读
* 场景读空气
* 角色语言画像
* 造句批改

### 6.5 TTS API

前端调用已有 Edge TTS Worker。

第一版可以直接前端请求 TTS Worker。
后续可加服务端缓存和 R2 音频缓存。

---

## 7. 推荐项目结构

```txt
src/
  routes/
    __root.tsx
    index.tsx
    works/
    episodes/
    vocab/
    grammar/
    sentences/
    practice/
    rag/
    ai/
    progress/

  server/
    db/
    repositories/
    services/
    rag/
    ai/
    tts/

  components/
    layout/
    cards/
    practice/
    canvas/
    ai/
    rag/

  lib/
    env.ts
    validators.ts
    japanese.ts
    canvas.ts
    cn.ts

  styles/
```

不要把所有逻辑塞进 route 文件。
route 负责页面，server/services 负责业务。

---

## 8. 数据访问策略

第一版使用 Cloudflare Worker server functions 访问数据库。

数据库访问走：

```txt
TanStack Start server function
↓
Cloudflare Worker runtime
↓
Hyperdrive
↓
Supabase Postgres
```

前端不要直接用 Supabase anon client 访问全部数据。

原因：

* 统一数据访问
* 方便以后接权限
* 方便缓存
* 方便 AI/RAG 组合查询
* 避免前端泄露不该暴露的逻辑

---

## 9. AI 策略

第一版 AI 不追求全自动，不追求完美。

采用“结构化 prompt + 缓存”的方式。

### 必须缓存

以下内容生成后要缓存：

* 单词解释
* 语法解释
* 单句精读
* 场景读空气
* 角色画像
* 造句批改历史

避免每次点开都重新烧 AI。

### AI 输出要求

AI 输出尽量结构化：

* title
* summary
* bullets
* evidence
* caveats
* learning_points

前端按结构展示，不要直接整段 Markdown 糊上去。

---

## 10. RAG 流程

读空气 RAG 标准流程：

```txt
用户问题
↓
生成 query embedding
↓
Vectorize 检索相关 chunks
↓
根据 chunk metadata 回查 Supabase
↓
组装上下文
↓
AI 分析
↓
返回结构化结果
```

第一版只做 topK 5。

必须展示来源：

* 作品
* 集数
* chunk 编号
* 起止时间
* 相关台词摘录

---

## 11. iPad / PWA 体验要求

第一版必须优先适配 iPad 横屏。

要求：

* 大字号日文
* 大按钮
* 卡片间距舒服
* Canvas 手写区不能误触滚动
* 可以添加到主屏幕
* 深色模式可后做
* 页面不要太像后台管理系统

首页和学习页要像 App，不像表格系统。

---

## 12. 不做的事情

第一版不做：

* 原生 iPad App
* Apple Pencil Pro 高级能力
* 自动 OCR 判字
* 自动发音评分
* 用户上传字幕
* 商业化订阅
* 社区
* 排行榜
* 多用户复杂权限
* 自动角色识别大系统
* 自动切音频片段

---

## 13. 开发顺序

### Step 1：项目初始化

* 创建 TanStack Start 项目
* 配置 TypeScript strict
* 配置 Tailwind
* 配置 Vite / VoidZero 工具链
* 配置 Vitest
* 配置 Oxlint
* 配置 Cloudflare Workers 部署
* 配置环境变量

验收：

* 本地能跑
* Cloudflare 能部署
* 首页能打开

---

### Step 2：数据库连接

* 配置 Hyperdrive
* 连接 Supabase
* 写基础 repository
* 查询 works / episodes
* 查询 subtitle_lines / subtitle_chunks
* 查询 learning materials

验收：

* 页面能显示 K-ON!
* 能显示 EP01–EP14
* 能显示某集学习计划

---

### Step 3：学习系统页面

实现：

* 首页
* 作品页
* 集数页
* 高频词页
* 语法页
* 跟读句页
* 手写练习页

验收：

* 能按作品/集数查看学习材料
* 能完成一次手写自评
* 能播放一次 TTS
* 能标记会/模糊/不会

---

### Step 4：Canvas 手写

实现：

* Canvas 绘制
* 支持鼠标、触摸、Apple Pencil
* 清空
* 查看答案
* 自评
* 保存练习结果

验收：

* iPad Safari 可写
* 手写时页面不乱滚动
* 自评结果能保存

---

### Step 5：TTS 跟读

实现：

* 调用 Edge TTS Worker
* 播放 audio/mpeg
* loading 状态
* 错误提示
* 慢速播放后做

验收：

* 高频词可播放
* 跟读句可播放
* 失败时显示可复制错误

---

### Step 6：Vectorize 查询

实现：

* RAG query endpoint
* 调用 Vectorize
* 返回相关 chunks
* 回查 Supabase 上下文

验收：

* 输入“找一下唯很天然被吐槽的场景”
* 能返回相关日文 chunk
* 能显示来源

---

### Step 7：AI 读空气

实现：

* 基于 Vectorize 检索结果组装 prompt
* 调用 AI
* 返回结构化分析
* 缓存结果

验收：

* 能分析一个场景
* 能解释角色心理
* 能引用日文证据
* 能提示学习点

---

### Step 8：单句精读 / 语法精读

实现：

* 点击一句台词
* AI 分析词法、句法、助词、句末、语气
* 缓存分析结果

验收：

* 任意一句日文台词可精读
* 输出不是单纯翻译
* 有语言学说明

---

### Step 9：进度与复习

实现：

* 用户本地/数据库进度
* 错词回炉
* 今日复习
* 高频优先
* 手写错题优先

验收：

* 写错的词会重新出现
* 模糊词会进入复习
* 首页能显示今日任务

---

## 14. Cursor 实施总提示

请按小步实现，不要一次写完整大系统。

优先级：

1. 能跑起来
2. 能查数据库
3. 能显示 K-ON! 学习材料
4. 能手写练习
5. 能播放 TTS
6. 能查 Vectorize
7. 能 AI 读空气

不要先做复杂美化。
不要先做登录。
不要先做商业化。
不要先做自动 OCR。
不要先做复杂后台。

---

## 15. MVP 验收标准

第一版完成后必须做到：

1. 打开网站看到 K-ON!。
2. 进入 EP01。
3. 看到本集高频词。
4. 点一个词能播放 TTS。
5. 点一个词能进入 AI 精讲。
6. 进入手写页能用 iPad 写字。
7. 手写后能自评。
8. 进入语法页能看本集语法点。
9. 进入跟读页能播放句子。
10. 进入台词页能看日文台词和中文对照。
11. 进入读空气页能提问。
12. Vectorize 能返回相关日文 chunk。
13. AI 能基于 chunk 做读空气分析。
14. 结果里能显示来源：作品、集数、时间、台词。

---

## 16. 最终产品判断

这个项目不是普通日语 App。

它的真正形态是：

```txt
日文字幕语料库
+ 高频学习训练
+ iPad 手写练习
+ TTS 跟读
+ AI 语言学精读
+ RAG 读空气引擎
```

第一版目标不是商业化，而是做出一个自己每天真的能用的 Anime Japanese Lab。
