# Kaleidoscope Cinematic 0.1.0 Alpha

Minecraft 1.21.1 / NeoForge 21.1.230 / Java 21。

## 下载与安装

下载 `kaleidoscope-cinematic-0.1.0-alpha.jar`，放到客户端及服务器的 `mods` 文件夹。
另行安装 Kaleidoscope Cookery 1.6.0 与 Cinematic Interactions 0.18.12，或单独测试 CI 0.18.28 Beta。不要同时安装两版 CI。
前置模组 JAR 不包含在本发布中，也未被修改。

## 当前范围

- 砧板：独立无菜单场景，右键进入、左键放料/逐刀切割/取出成品，潜行右键保留原交互。
- 配方、消耗、耐久和产出交由原模组；服务器验证距离、视线、当前设备、权限及请求间隔。
- 炒锅：实验适配，默认关闭；配置 `pot=true` 后启用。
- 高汤锅、蒸笼、石磨：尚未实现。

## 实际验证与限制

正式 clean build 通过。CI 0.18.12 与 0.18.28 Beta 各有 3/3 Minecraft 服务端 GameTest 通过，覆盖鳕鱼切割/产出、非法操作拒绝，以及两名模拟玩家共享进度不重复产出。
客户端启动至主菜单已确认。客户端切菜、手臂姿态、退出恢复、真实网络多人及炒锅尚未完成验收；这是供测试的 Alpha，不是完整稳定版。
KC 验证二进制来自上游 snapshot-2026-09-26-14-06-32，声明版本 1.6.0-neoforge+mc1.21.1。其他同版本发行包仍需兼容性确认。

## 附件

- 主 JAR：安装使用。
- sources.jar：生产 Java 源码。
- source.zip：完整工程、Gradle Wrapper、测试夹具、文档与依赖下载脚本。
- SHA256SUMS：上述文件的 SHA-256。

完整测试步骤见仓库 docs/TESTING.md。反馈时请提供 Minecraft/NeoForge/前置版本、复现步骤和 latest.log 或崩溃报告。
