# Anime Japanese Lab × Mirror 融合决策表

日期：2026-07-10

| 目标区域 | 必须保留 | Mirror 候选 | 证据 | 决策 | 回退 |
|---|---|---|---|---|---|
| 训练完成页 | 真实结算数据、下一批、重练、返回、复盘安排 | `session_complete_bea_duo`、粒子与星光、完成页信息节奏 | E3/V2 | `HYBRID`，第一阶段垂直切片 | 原 `LegacyLessonCompleteScreen` + 原创 JL 静态 Hero |
| 训练路径 | 节点派生、锁定、批次、作品/集数切换 | Unit Header、oval node、角色/宝箱 slot | mixed | `HYBRID`，第一阶段已接角色氛围、锁定角色/宝箱与 guidebook slot | 当前 Compose 路径 + rollout 开关 |
| 答题页 | `LessonEngine`、题型、音频、答案与同步 | 进度条、选项卡、反馈 dock、局部角色反馈 | E2-E3 | `REPLACE_VISUAL_ONLY` | 当前题型 Composable |
| 资料页 | 作品/集数/词汇/语法/跟读与 AI 行为 | Practice Hero、卡片结构、图标节奏 | E3/V2 | `HYBRID` | 当前资料列表 |
| 复盘页 | 本地错题、云端 review、AI 讲解 | Practice/Review 卡片与错因反馈视觉 | E2-E3 | `HYBRID` | 当前复盘页 |
| 我的 | 登录、账号、设置、同步 | Profile header、completion card | E3/V2 | `HYBRID` | 当前 Mine/Settings |
| Reward | 目标 XP/能量/连续天数规则 | chest/gem/streak 视觉与反馈 | mixed | `FEATURE_FLAGGED` | 静态 Compose/Lottie |
| Calls / Super / Max | 当前产品没有对应业务真相 | STATIC_INFERRED 研究页 | E2/V1-V2 | `DROP`，不得为了素材创造伪业务 | 不接入 |
| Mirror 导航/后端/P0 lesson | 目标 App 全部现有骨架 | Mirror demo route、mock、seed、grader | 不适用 | `DROP` | 不进入依赖图 |

## 第一阶段边界

- 新增纯语义视觉契约、编译期资源 Resolver、素材元数据和独立 rollout 开关。
- 改造训练完成页，并在训练路径的纯视觉 slot 接入白名单素材；`LabViewModel`、`LessonEngine`、同步协议和训练路径状态机不变。
- 完成页的新 UI 使用本项目语义：作品、集数、本轮真实正确率、掌握内容和复盘计划。
- Mirror 动画只占 hero slot；加载失败或关闭富动画时显示原创 `JL` 静态视觉。
- 路径页继续使用目标 App 的动漫课程封面、EP 数据、节点状态与点击分发；Mirror 素材仅作 decorative companion、locked chest 和 guidebook 图标。
