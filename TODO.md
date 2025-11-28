# 项目开发文档：SharedCure (共享治愈)

文档版本: 1.0

项目名称: SharedCure

包名结构: com.lingnc.sharedcure

目标平台: Paper / Spigot (兼容 Minecraft 1.21.x)

开发语言: Java 21 (推荐 1.21 环境)

## 1. 项目概述 (Overview)

SharedCure 是一个旨在优化多人服务器经济体验的插件。

在原版 Minecraft 中，只有亲自治愈僵尸村民的玩家才能获得永久打折福利（Major Positive Gossip）。本项目通过 "标记 + 懒加载" 的机制，实现**“一人治愈，全服共享”**的功能。

只要一个村民被任意玩家治愈过，服务器内的所有玩家（包括未来加入的新玩家）在与该村民交互时，都将自动获得最高等级的声望打折。

## 2. 核心架构设计 (Core Architecture)

为了保证服务器性能，避免存储大量离线玩家数据，本项目**不使用**“在治愈瞬间遍历玩家列表”的方式，而是采用 **PDC (PersistentDataContainer) 三态管理** 配合 **JIT (Just-In-Time) 懒加载注入**。

### 2.1 数据存储 (Data Storage)

我们需要在每一个村民实体 (Villager) 的 `PersistentDataContainer` 中维护一个状态标签。

-   **Key:** `new NamespacedKey(plugin, "discount_status")`
-   **Type:** `PersistentDataType.BYTE`

### 2.2 状态定义 (State Definitions)

| 状态值 (Byte) | 状态名称 | 含义 | 处理逻辑 |
| --- | --- | --- | --- |
| NULL | UNKNOWN (未知) | 从未被插件接管 (如旧存档遗留) | 需在加载时进行 NBT 检查，判断其历史声望。 |
| 0 | NOT_CURED (未治愈) | 普通村民，无打折记录 | 正常游戏逻辑，无需操作。 |
| 1 | CURED (已治愈) | 英雄村民，全服共享打折 | 任何玩家与之交互时，强制注入最高声望。 |

## 3. 详细逻辑流程 (Detailed Logic)

### 3.1 模块 A：存量村民初始化 (Chunk Load)

目标： 处理旧存档中已存在的村民，将其纳入管理体系。

事件： ChunkLoadEvent (推荐异步处理或分批处理)

1.  当区块加载时，获取区块内所有 `Villager` 实体。
2.  检查是否含有 `discount_status` 标签。
3.  **如果有标签** -> 跳过 (利用缓存)。
4.  **如果无标签 (UNKNOWN)** -> 执行 **深度检查 (Deep Check)**：
    -   检查该村民的 Gossip 数据（通过 Paper API `getReputation` 或 NMS）。
    -   判断是否存在类型为 `MAJOR_POSITIVE` 的声望记录？
        -   **是** -> 写入标签 `1 (CURED)`。
        -   **否** -> 写入标签 `0 (NOT_CURED)`。

### 3.2 模块 B：新村民生成 (Spawning)

目标： 标记新生成的实体（繁殖、刷怪蛋）。

事件： CreatureSpawnEvent

1.  事件触发，实体为 `Villager`。
2.  默认写入标签 `0 (NOT_CURED)`。
3.  *注意：需确保此事件优先级不干扰下方的“治愈转化”逻辑。*

### 3.3 模块 C：治愈转化 (Curing)

目标： 捕捉治愈瞬间，确立共享资格。

事件： EntityTransformEvent

条件： reason == CURED && transformedEntity == Villager

1.  僵尸村民转化为普通村民。
2.  直接向新村民强制写入标签 `1 (CURED)`。
3.  无需操作玩家数据。

### 3.4 模块 D：懒加载注入 (Interaction & JIT)

目标： 在交易前一刻，让玩家获得打折资格。

事件： PlayerInteractEntityEvent

1.  玩家右键点击村民。
2.  **双重保险：** 如果村民标签不存在 (UNKNOWN)，立即执行一次 **3.1 中的深度检查**。
3.  读取标签状态：
    -   **状态 0 (NOT_CURED):** return，不做任何事。
    -   **状态 1 (CURED):**
        -   检查 `event.getPlayer()` 在该村民处的声望。
        -   如果 `Reputation < MAX` (通常为 20/25)：
            -   **注入声望：** 使用 `villager.setReputation(uuid, ReputationType.MAJOR_POSITIVE, 20)`。
            -   *注：注入操作在毫秒级完成，随后客户端打开交易界面显示的即为打折价格。*

## 4. 代码实现建议 (Implementation Guide)

以下是基于 Paper API 的核心代码骨架，请以此为基础进行开发。

### 4.1 Maven 依赖 (pom.xml)

请确保使用 Paper API 以获得更好的声望操作支持。

```
<dependencies>
    <dependency>
        <groupId>io.papermc.paper</groupId>
        <artifactId>paper-api</artifactId>
        <version>1.21.1-R0.1-SNAPSHOT</version> <!-- 请根据实际版本调整 -->
        <scope>provided</scope>
    </dependency>
</dependencies>
```

### 4.2 核心管理器 (VillagerManager.java)

```
package com.lingnc.sharedcure;

import com.destroystokyo.paper.entity.villager.Reputation;
import com.destroystokyo.paper.entity.villager.ReputationType;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Villager;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public class VillagerManager {

    private final JavaPlugin plugin;
    private final NamespacedKey STATUS_KEY;

    // 状态常量
    public static final byte STATUS_NOT_CURED = 0;
    public static final byte STATUS_CURED = 1;

    public VillagerManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.STATUS_KEY = new NamespacedKey(plugin, "discount_status");
    }

    /**
     * 核心方法：检查并打标 (处理 Unknown 状态)
     */
    public void checkAndTagVillager(Villager villager) {
        if (hasTag(villager)) return;

        // 深度检查逻辑：查看该村民是否有过大恩大德的记录
        // Paper API 提供了直接访问 Reputation 的映射，或者需要通过 NMS/Reflection 检查
        // 此处为逻辑伪代码，需根据具体 API 实现 gossip 检查
        boolean hasMajorPositive = checkVillagerHistory(villager);

        if (hasMajorPositive) {
            setTag(villager, STATUS_CURED);
        } else {
            setTag(villager, STATUS_NOT_CURED);
        }
    }

    /**
     * 核心福利：给玩家注入折扣
     */
    public void applyDiscount(Villager villager, org.bukkit.entity.Player player) {
        // 使用 Paper API 设置声望
        int currentRep = villager.getReputation(player.getUniqueId(), ReputationType.MAJOR_POSITIVE);

        // 只有当声望不足时才注入，避免重复操作
        if (currentRep < 20) {
            villager.setReputation(player.getUniqueId(), ReputationType.MAJOR_POSITIVE, 20);
        }
    }

    // --- PDC 辅助方法 ---

    public void setTag(Villager villager, byte status) {
        villager.getPersistentDataContainer().set(STATUS_KEY, PersistentDataType.BYTE, status);
    }

    public Byte getTag(Villager villager) {
        if (!hasTag(villager)) return null;
        return villager.getPersistentDataContainer().get(STATUS_KEY, PersistentDataType.BYTE);
    }

    public boolean hasTag(Villager villager) {
        return villager.getPersistentDataContainer().has(STATUS_KEY, PersistentDataType.BYTE);
    }

    // TODO: 实现 checkVillagerHistory，可能需要遍历 villager.getReputations()
    private boolean checkVillagerHistory(Villager villager) {
        // 实现细节：检查是否存在任何 UUID 拥有 MAJOR_POSITIVE > 0
        return false; // 占位符
    }
}
```

### 4.3 事件监听器 (SharedCureListener.java)

```
package com.lingnc.sharedcure;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityTransformEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.world.ChunkLoadEvent;

public class SharedCureListener implements Listener {

    private final VillagerManager manager;

    public SharedCureListener(VillagerManager manager) {
        this.manager = manager;
    }

    // 模块 A: 区块加载检查
    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkLoad(ChunkLoadEvent event) {
        for (Entity entity : event.getChunk().getEntities()) {
            if (entity instanceof Villager) {
                manager.checkAndTagVillager((Villager) entity);
            }
        }
    }

    // 模块 B: 新生成 (标记为未治愈)
    @EventHandler(priority = EventPriority.LOW)
    public void onSpawn(CreatureSpawnEvent event) {
        if (event.getEntity() instanceof Villager) {
            // 只有当没有标签时才标记为0，避免覆盖 EntityTransform 的逻辑
            if (!manager.hasTag((Villager) event.getEntity())) {
                manager.setTag((Villager) event.getEntity(), VillagerManager.STATUS_NOT_CURED);
            }
        }
    }

    // 模块 C: 治愈转化 (标记为已治愈)
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCure(EntityTransformEvent event) {
        if (event.getTransformReason() == EntityTransformEvent.TransformReason.CURED
            && event.getTransformedEntity() instanceof Villager) {

            manager.setTag((Villager) event.getTransformedEntity(), VillagerManager.STATUS_CURED);
        }
    }

    // 模块 D: 交互 (懒加载应用)
    @EventHandler(priority = EventPriority.LOW)
    public void onInteract(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Villager)) return;

        Villager villager = (Villager) event.getRightClicked();

        // 1. 保底检查 (防止漏网之鱼)
        if (!manager.hasTag(villager)) {
            manager.checkAndTagVillager(villager);
        }

        // 2. 如果是英雄村民，给玩家福利
        Byte status = manager.getTag(villager);
        if (status != null && status == VillagerManager.STATUS_CURED) {
            manager.applyDiscount(villager, event.getPlayer());
        }
    }
}
```