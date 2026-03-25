# EvenMoreFishLeaderboard

一個 [EvenMoreFish](https://github.com/Oheers/EvenMoreFish) 的擴充插件，使用 [DecentHolograms](https://github.com/DecentSoftware-eu/DecentHolograms) 顯示釣魚競賽的即時排行榜全息投影。

## 功能特色

- 即時顯示釣魚競賽排行榜（前 10 名）
- 每位玩家獨立的全息投影實例
- 點擊全息投影切換不同的競賽
- 即時倒數計時更新
- 支援所有 EvenMoreFish 競賽類型

## 需求

| 插件 | 版本 |
|------|------|
| Minecraft Server (Paper) | 1.21+ |
| [EvenMoreFish](https://github.com/Oheers/EvenMoreFish) | 2.1.13+ |
| [DecentHolograms](https://github.com/DecentSoftware-eu/DecentHolograms) | 2.9.9+ |

## 安裝

1. 下載最新版本的 `EvenMoreFishLeaderboard.jar`
2. 將 JAR 檔案放入伺服器的 `plugins/` 資料夾
3. 確保已安裝 [EvenMoreFish](https://github.com/Oheers/EvenMoreFish) 和 [DecentHolograms](https://github.com/DecentSoftware-eu/DecentHolograms)
4. 重新啟動伺服器
5. 使用 `/flb setpos` 設定全息投影顯示位置

## 設定

首次啟動後，插件會在 `plugins/EvenMoreFishLeaderboard/` 下自動產生 `config.yml`：

```yaml
hologram-location:
  world: "world"
  x: 0.0
  y: 100.0
  z: 0.0
```

你也可以在遊戲中使用 `/flb setpos` 直接設定全息投影的位置。

## 使用方式

### 指令

| 指令 | 權限 | 說明 |
|------|------|------|
| `/flb setpos` | `evenmorefish.leaderboard.setpos` | 將全息投影設定在目前站立的位置 |
| `/flb status` | `evenmorefish.leaderboard.admin` | 顯示目前競賽狀態 |
| `/flb listids` | `evenmorefish.leaderboard.admin` | 列出所有已設定的競賽 ID |
| `/flb reload` | `evenmorefish.leaderboard.admin` | 重新載入插件設定檔 |
| `/flb help` | — | 顯示說明訊息 |

> 指令別名：`/fishleaderboard`、`/fishlb`、`/emflb`

### 全息投影互動

1. **主選單** — 顯示所有競賽清單，綠色圓點表示進行中，灰色圓點表示未進行
2. **點擊全息投影** — 切換瀏覽不同的競賽排行榜
3. **競賽排行榜** — 顯示競賽名稱、類型、剩餘時間及前 10 名玩家排行

### 支援的競賽類型

| 類型 | 說明 |
|------|------|
| LARGEST_FISH | 最大的魚 |
| MOST_FISH | 釣最多魚 |
| SPECIFIC_FISH | 指定魚種 |
| SPECIFIC_RARITY | 指定稀有度 |
| LARGEST_TOTAL | 最大總重量 |
| SHORTEST_FISH | 最短的魚 |
| SHORTEST_TOTAL | 最短總長度 |
| RANDOM | 隨機 |

## 權限

| 權限 | 預設 | 說明 |
|------|------|------|
| `evenmorefish.leaderboard.setpos` | OP | 設定全息投影位置 |
| `evenmorefish.leaderboard.admin` | OP | 管理員指令（status、listids、reload） |

## 建置

```bash
git clone https://github.com/ninepin/EvenMoreFishLeaderboard.git
cd EvenMoreFishLeaderboard
mvn clean package
```

建置完成後，JAR 檔案會產生在 `target/` 資料夾中。

## 授權

本專案採用 MIT 授權條款。
