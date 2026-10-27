# 接口审阅与实现边界

## 精确来源

- CI 0.18.12 tag `v0.18.12-neoforge-1.21.1`，提交 `c79e1b9e05347165f6766700007cef45c8ce4d57`。
- CI Beta tag `v0.18.28-neoforge-1.21.1-beta`，提交 `ff00fa9077470ae326bfa07b71108585bce5ad59`。
- 对比上述标签的 `src/main/java/dev/dyslexic/cinematic/api` 和 `docs/addon-api.md`，没有差异。两版文档明确不公开无菜单设备 API。
- KC 1.21.1-neoforge 分支审阅提交 `4d39e36cfa749939ba5f9d6d3b715ef9a984cb9c`。依赖二进制另见 dependencies.json 的上游快照；分支最新源码与快照不假定完全一致。

## 为什么使用独立场景

`AddonStations.Definition` 要求真实 menu class，`StationKind.ADDON.usesMenu()` 为 true。砧板不是菜单方块，不注册伪菜单，不篡改 CI Scene.station/phase，不使用 AddonMenuStation。唯一调用 CI 的方法是客户端 `Scene.active()`，避免两套相机同时占用。此方法属于内部实现而非稳定公开扩展协议，因此依赖范围限制到 0.19 之前，两版仍需实机矩阵验证。

| 设备 | 原始接口与约束 | 当前状态 |
|---|---|---|
| 砧板 | IChoppingBoard.onPutItem/onCutItem/onTakeOut。最终成品由 onCutItem 再次点击取出；onTakeOut 只退回尚未切割的原料。快速切割附魔允许一刀多次进度。 | 原型 |
| 炒锅 | IPot 的投油方法本身不检查已有油。原 PotBlock 先退料/取菜，再检查热源和油，之后处理锅铲及投料。 | 默认关闭实验原型 |
| 高汤锅 | IStockpot.onLitClick（上游名称确实是 Lit）、addSoupBase、addIngredient、takeOutProduct、hasLid，四阶段状态。必须尊重热源、盖子和容器。 | 未实现 |
| 蒸笼 | ISteamer 仅提供热源/火力更新/投料/取料；盖子属于 SteamerBlock.HAS_LID，空手潜行交互切换。堆叠影响盖子和热源。 | 未实现 |
| 石磨 | IMillstone 只有 onPutItem，实际旋转/绑定生物/计时属于 MillstoneBlockEntity。不能把一段推动动画冒充真实研磨。 | 未实现 |

## 权威与多人

自定义请求只有维度、位置和响应序号，不接收客户端库存、配方、产出或进度。服务器线程读取真实当前主手和当前设备，验证后调用 ServerPlayerGameMode.useItemOn。NeoForge 对此入口有 RightClickBlock/物品限制等 hook。区块实体更新和库存同步交给原模组/原版。多个玩家的请求在服务器线程串行处理，没有私有库存或复制配方。没有独占工作台锁；多人操作同一设备遵循原设备共享语义。

响应序号只用于匹配客户端待处理动作，不是权限凭证。不依赖客户端相机位置做距离检查。每玩家每 6 tick 最多执行一次，旧请求不能带入旧物品状态。请求维度必须匹配玩家当前维度，跨维度延迟包会被拒绝；同维度请求仍按当前设备和物品重新验证。没有会话租约，不承诺独占操作。

## 剩余风险

- Java 编译及两版 CI 的服务端 GameTest 启动已通过；正式服务器与真实玩家网络仍需验证。
- 模组间领地插件若仅监听原版网络包而非 NeoForge 交互事件，不能仅凭 hook 保证兼容；必须实测。
- 暂未增加服务器会话令牌，也未同步其他玩家的附加手臂表现；不能宣称完整多人验收通过。
- 相机只改渲染角度，不移动实体或接管 CI 内部状态。退出依靠每次渲染的条件与下一 tick 失效检测，死亡/断线/传送/菜单/被推动等都需测试。
- 手臂姿态、左右手习惯、模型穿模、曲面高度和炒锅各种物品姿势未经过视觉验收。
- 原版 useItemOn 可能被其他物品的 onItemUseFirst 拦截，这是保留原交互语义的结果；成功响应表示事件被处理，不一定代表切割进度增加。
- 原型没有吞掉输入状态之外的所有第三方快捷键；兼容性不可假定。

## 下一阶段

编译和两版 CI 的服务端砧板测试已通过。下一步由用户验证客户端砧板的手臂、相机及输入，然后完善炒锅的成功状态判定与姿态。再为高汤锅、蒸笼、石磨编写独立适配；石磨必须先确认推动到底是玩家移动、绑定还是服务端 tick 驱动，不能直接写入产出或进度。
