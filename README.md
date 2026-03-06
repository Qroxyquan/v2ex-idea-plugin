# V2EX Reader for IntelliJ IDEA

> 写代码累了？  
> 来 IDE 里“研究一下技术趋势”（懂的都懂）。

一个让你在 IntelliJ IDEA 内优雅刷 V2EX 的插件：  
不切浏览器、不摸鱼出戏、看帖看评论一条龙。

## 这插件能干啥

### 1. 首页刷帖
- 支持 `全部 / 最热 / 技术 / 创意 / 好玩 / Apple / 酷工作` 标签。
- 标签按 V2EX `tab` 规则加载，例如 `全部 -> https://www.v2ex.com/?tab=all`。
- 帖子列表按最后活跃时间倒序，展示评论数和最后更新时间（最后一条评论时间）。
<img width="1510" height="1586" alt="image" src="https://github.com/user-attachments/assets/c87db950-8bd8-45bc-b0ab-0fc352228262" />

### 2. 搜索（内置浏览器模式）
- 点击“搜索”后，直接在 IDEA 内置浏览器打开 Google 搜索结果页（`site:v2ex.com/t`）。
- 在结果页点击帖子链接，会被插件拦截并进入详情解析页。
- 搜索页内置 `返回首页 / 后退 / 前进 / 刷新`，不用来回切窗口。
<img width="1422" height="1462" alt="image" src="https://github.com/user-attachments/assets/4da98171-870b-4359-836a-94c67c54437d" />

### 3. 帖子详情 + 评论
- 点击帖子进入详情，正文和评论可滚动查看。
- 支持图片解析（帖子与评论中的图片都可显示）。
- 详情页支持“刷新详情”和“浏览器打开”。
<img width="1482" height="1496" alt="image" src="https://github.com/user-attachments/assets/322a106d-6f61-45a4-ad8a-112104f5d474" />

### 4. 评论（可选登录态）
- 支持配置 `A2 Token` 后在插件内发表评论。
- 评论输入框默认收起，点“写评论”再展开，低调摸鱼不扎眼。

## 安装方式

1. 在 `build/distributions/` 找到打包好的 zip。  
2. IDEA -> `Settings` -> `Plugins` -> 齿轮图标 -> `Install Plugin from Disk...`  
3. 选择 zip 安装并重启 IDEA。

## 配置说明

路径：`Settings -> Tools -> V2EX Reader`

- `API Token`：用于 V2EX API 请求（可选，提升稳定性）。
- `A2 Token`：用于登录态能力（比如发评论）。

> 插件不会上传你的项目代码，也不会保存账号密码。  
> 仅按你填写的 Token 发起 V2EX 请求。

## 使用姿势（高效摸鱼版）

1. 打开右侧 Tool Window：`V2EX`。  
2. 平时看首页标签流。  
3. 有关键字就直接搜，结果页里点帖进详情。  
4. 看完一键返回首页，假装刚刚在看 CI 日志。  

## 兼容性

- IntelliJ Platform Build: `242.*` 到 `252.*`
- 推荐 IDE：IntelliJ IDEA `2024.2+`
- 开发构建：JDK 21

## 本地开发

```bash
# 运行开发版 IDE
./gradlew runIde

# 运行测试
./gradlew test

# 打包插件
./gradlew buildPlugin
```

产物目录：`build/distributions/`

## 常见问题

### 搜索出来不相关？
- 先点搜索页“刷新”。
- 更换关键词（英文关键词通常更稳定）。
- Google 在部分网络环境可能有限制，可稍后重试。

### 点进帖子没评论？
- V2EX API 偶发空评论时，插件会尝试页面解析兜底。
- 仍为空时，可能是帖子确实无评论或页面结构临时变化。

### 为什么评论按钮不可用？
- 先在设置里填 `A2 Token`，保存后再打开详情页。

## 免责声明

本插件为非官方项目，仅用于学习与效率提升。  
请遵守你所在团队/公司的使用规范，摸鱼需适度，交付要准时。
