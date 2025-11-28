# SharedCure

SharedCure 是一个基于 Paper API 开发的服务器插件，通过“标记 + 懒加载”机制，实现任何玩家治愈过的村民都为全服玩家共享最高折扣。

## 功能特性


## 架构概览

| 模块 | 事件 | 作用 |
| --- | --- | --- |
| 存量村民初始化 | `ChunkLoadEvent` + 启动扫描 | 为旧村民写入治愈标签，并在未知状态时深度检查 Gossip。 |
| 新村民标记 | `CreatureSpawnEvent` | 新生成村民默认写入 `NOT_CURED`。 |
| 治愈捕捉 | `EntityTransformEvent` (CURED) | 僵尸村民转化瞬间写入 `CURED`。 |
| 懒加载注入 | `PlayerInteractEntityEvent` | 交互前检查标签并注入声望，保证玩家立即享受折扣。 |

## 构建与使用

1. 安装 JDK 21 及 Maven 3.9+。
2. 在仓库根目录执行：
   ```bash
   mvn clean package
   ```
3. 生成的插件位于 （例如：`target/sharedcure-1.0.0-SNAPSHOT-shaded.jar`），将其放入 Paper / Spigot 1.21.x 服务器的 `plugins/` 目录并重新启动服务器。