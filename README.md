# Kaleidoscope Cinematic

Minecraft 1.21.1 / NeoForge 21.1.230 / Java **JDK** 21 的独立附属模组工程。

**Alpha 下载：** [GitHub Releases](https://github.com/DonQuijotee/cinematic-kale/releases/tag/v0.1.0-alpha)。

本地构建产物：`build/libs/kaleidoscope-cinematic-0.1.0-alpha.jar`。
正式 `clean build` 已通过。砧板的 3 项 Minecraft 服务端 GameTest 在 CI 0.18.12 和 0.18.28 Beta 下均通过；客户端已启动到主菜单。你要求自行测试后，已停止进一步游戏验证。

## 安装与操作

客户端和服务器的 `mods` 目录均放入本 JAR，并安装 Minecraft 1.21.1、NeoForge 21.1.230 或兼容版本、Kaleidoscope Cookery 1.6.0，以及 CI 0.18.12（或单独测试 0.18.28 Beta）。不要同时安装两版 CI。

砧板：普通右键进入 → 手持食材左键放入 → 滚轮换厨房刀 → 每次左键切一刀 → 完成后再左键让原模组掉落成品。潜行右键保留原交互；ESC、潜行、移动退出场景。

炒锅在 `config/kaleidoscope_cinematic-common.toml` 中设 `pot=true` 后启用；默认为 false，未完成游戏验收。

## 实现与验证边界

- 砧板无菜单场景：普通右键进入，左键逐次请求原方块操作，滚轮选择工具，读取原方块实体切割进度。
- 双手及所持物品的世界空间渲染原型、相机朝向过渡；动作只在服务器返回成功后触发。
- 成品由森罗物语原逻辑掉落，玩家按原版距离拾取；没有伪造库存产出或自动远距离收取。
- 服务端距离、视线、存活、旁观者、建造权限、世界边界、已加载区块、设备开关和 6 tick 间隔检查。
- 原版服务端 useItemOn 继续派发 NeoForge 权限事件，并调用 KC 的接口实现。配方、切割附魔、概率耐久和成就不在本模组复制。
- 潜行右键走原方块处理器，保留取消事件及显式 useBlock 拒绝。
- 炒锅实验适配及投料/倾倒/翻炒/收取动作分支，默认关闭。

高汤锅、蒸笼、石磨尚未实现，见 [接口与风险](docs/API-REVIEW.md)。当前双手为位置动画原型，没有 CI 的完整 IK/走近路径规划；其他玩家能看到原设备变化，但不会看到自定义第一人称双手动画。

## 构建

安装 JDK 21（只有 `java` 的 JRE 不够），配置 `JAVA_HOME`。本次已在 `/workspace/tools/jdk-21.0.9+10` 安装并校验 Temurin 21.0.9；此路径只适用于当前云环境。

```sh
export JAVA_HOME=/workspace/tools/jdk-21.0.9+10
export GRADLE_USER_HOME=/workspace/.gradle
```

```sh
python3 tools/fetch_dependencies.py
./gradlew build
# 本云环境使用 HTTPS_PROXY 时：
python3 tools/gradle.py build
# 单独核验 Beta，不同时加载两份 CI：
python3 tools/gradle.py clean build -PcinematicJar=libs/Cinematic.Interaction-NeoForge-1.21.1-0.18.28.jar
```

本次已生成 `build/libs/kaleidoscope-cinematic-0.1.0-alpha.jar`，不包含测试夹具或前置 JAR。

依赖脚本下载原始 JAR 并校验记录的 SHA-256；不修改、不嵌入前置 JAR。KC 二进制来自上游 `snapshot-2026-09-26-14-06-32`，其元数据版本为 `1.6.0-neoforge+mc1.21.1`，不是已确认与所有 1.6.0 发行包完全相同的构建。

```sh
./gradlew runClient
./gradlew runServer
```

专用服务器 EULA 由实际管理员阅读并接受。发行安装需要客户端和服务端都安装本模组、KC 1.6.0 以及同一版本 CI。不可同时安装 CI 0.18.12 和 Beta。

配置 `config/kaleidoscope_cinematic-common.toml`：`choppingBoard=true`，`pot=false`。两端都需开启对应功能；客户端开启不绕过服务器关闭设置。未实现设备没有虚假的“已支持”开关。

## 验证

[测试说明与实际结果](docs/TESTING.md)。服务端自动测试已通过；真实客户端切菜、双手姿态、退出恢复、真实网络多人联机及炒锅仍待你验收。
