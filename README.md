# openDB

开源 Web 数据库客户端：连接、查询、结果分析、AI 辅助 SQL，全部在浏览器一个 Tab 里完成。亦提供 **Electron 桌面版**，内嵌 JRE，无需本机安装 Java。

[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](./LICENSE)

**仓库地址：** [github.com/MMCISAGOODMAN/openDB](https://github.com/MMCISAGOODMAN/openDB)

## 亮点

| 能力 | 说明 |
|------|------|
| **查完即析** | 查询结果内置智能分析：列画像、直方图、皮尔逊相关、IQR 离群、选区统计（纯前端计算） |
| **EXPLAIN 洞察** | 执行计划自动解读，标记全表扫描 / filesort，问题行高亮 |
| **Schema 感知 AI** | 携带当前库表结构上下文：问答、解释 / 优化 SQL、建表、模板、自然语言转 SQL |
| **多方言架构** | MySQL、PostgreSQL、Oracle、H2、达梦、KingbaseES、openGauss，通过 `SqlDialect` 插件化扩展 |
| **Schema 健康** | 表结构评分、ER 洞察、索引建议 |
| **桌面版** | Electron 单窗口，内嵌 JRE + Spring Boot，GitHub Releases 三平台安装包 |
| **MIT 开源** | 可 fork、可商用，支持内网部署与 Ollama 本地 AI |

## 支持的数据库

| 类型 | 状态 | 说明 |
|------|------|------|
| MySQL | ✅ | 内置驱动 |
| PostgreSQL | ✅ | 内置驱动 |
| Oracle | ✅ | 内置驱动 |
| H2 | ✅ | 内置驱动 |
| 达梦 DM8 | ✅ | 需自备 JDBC 驱动 JAR，见 [backend/drivers/README.md](./backend/drivers/README.md) |
| 人大金仓 KingbaseES | ✅ | 需自备 JDBC 驱动 JAR |
| openGauss | ✅ | 内置 Maven Central 驱动 |

### 国产库连接示例

| 类型 | JDBC URL 示例 | 默认端口 |
|------|---------------|----------|
| 达梦 DM8 | `jdbc:dm://127.0.0.1:5236/DAMENG` | 5236 |
| KingbaseES | `jdbc:kingbase8://127.0.0.1:54321/test` | 54321 |
| openGauss | `jdbc:opengauss://127.0.0.1:5432/postgres` | 5432 |

Vendor 驱动放置目录：`backend/drivers/` 或 `$OPENDDB_DATA_DIR/drivers/`。

## 功能概览

**连接与浏览**

- 连接配置、测试连接、Profile 本地持久化
- 数据库 / 表 / 字段树形浏览，右键预览、导出、设计
- 连接批量测速（延迟评级）

**SQL 工作台**

- CodeMirror 6 编辑器，Schema 自动补全
- 多语句执行、查询历史与统计分析
- 结果表格：拖选、TSV 复制、分页

**分析与 AI**

- 结果面板「智能分析」：概览、分布、相关性、数据质量、分组统计
- EXPLAIN 执行计划分析页
- AI 助手（SSE 流式）：OpenAI / Claude / DeepSeek / Ollama 等兼容 API
- AI 回复 SQL 一键应用到编辑器

**工具箱**

- ER 图与 Schema 洞察
- 表结构设计器、CSV 导入（类型推断）
- 数据传输、结构同步、数据对比

## 技术栈

| 模块 | 技术 |
|------|------|
| 前端 | Vue 3 · TypeScript · Vite · CodeMirror 6 |
| 后端 | Spring Boot 3 · Java 21 · JDBC |
| 桌面 | Electron · electron-builder · 内嵌 JRE 21 |
| AI | OpenAI 兼容 Chat Completions · SSE |
| 协议 | [MIT](./LICENSE) |

## 项目结构

```text
openDB/
├── backend/          # Spring Boot API
├── frontend/         # Vue Web 客户端
├── desktop/          # Electron 桌面壳
├── scripts/          # 构建脚本（build-desktop.sh）
├── .github/workflows/# CI / Release
├── LICENSE
└── README.md
```

## 快速开始

### 桌面版（推荐）

从 [GitHub Releases](https://github.com/MMCISAGOODMAN/openDB/releases) 下载对应平台的安装包（`.dmg` / `.exe` / `.AppImage`），安装后双击运行即可。应用数据（连接 Profile、AI 配置）保存在用户目录，可通过环境变量 `OPENDDB_DATA_DIR` 自定义。

### Web 开发模式

#### 前置条件

- JDK 21+
- Maven 3.9+
- Node.js 18+
- 可访问的目标数据库实例

#### 1. 启动后端

```bash
cd backend
mvn spring-boot:run
```

默认地址：`http://localhost:8080`

#### 2. 启动前端

```bash
cd frontend
npm install
npm run dev
```

默认地址：`http://localhost:5173`

### 生产 / 桌面打包模式

前后端合一（JAR 内嵌前端静态资源）：

```bash
./scripts/build-desktop.sh
cd desktop && npm install && npm run dist
```

桌面模式默认端口 `18080`（可通过 `OPENDDB_SERVER_PORT` 覆盖）。

### 启用 AI（可选）

环境变量：

```bash
export OPENDDB_AI_ENABLED=true
export OPENDDB_AI_API_KEY=your-api-key
# 可选
export OPENDDB_AI_API_URL=https://api.openai.com/v1/chat/completions
export OPENDDB_AI_MODEL=gpt-4o-mini
```

或在界面 **AI 设置** 中配置 Ollama / DeepSeek 等。未配置 AI 时，连库、查询、智能分析等功能仍可正常使用。

### 环境变量

| 变量 | 说明 | 默认 |
|------|------|------|
| `OPENDDB_SERVER_PORT` | HTTP 服务端口 | `8080`（桌面默认 `18080`） |
| `OPENDDB_DATA_DIR` | Profile / AI 配置存储目录 | `./data` |
| `OPENDDB_DRIVERS_DIR` | Vendor JDBC 驱动目录 | 见 drivers README |

## 主要 API

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/health` | 健康检查（桌面启动探测） |
| GET | `/api/database-types` | 支持的数据库类型 |
| GET/POST/DELETE | `/api/connections` | 连接管理 |
| POST | `/api/connections/test` | 测试连接 |
| GET | `/api/connections/{id}/databases` | 数据库列表 |
| POST | `/api/connections/{id}/query` | 执行 SQL |
| POST | `/api/connections/{id}/ai/chat` | AI 对话（SSE） |

## 路线图

- [x] 多方言 JDBC 架构（MySQL / PG / Oracle / H2）
- [x] 国产数据库（达梦 / KingbaseES / openGauss）
- [x] 连接 Profile 与对象树浏览
- [x] SQL 编辑器与结果展示
- [x] 查询结果智能分析
- [x] EXPLAIN 执行计划分析
- [x] AI 助手（Schema 上下文 · SSE · 一键应用 SQL）
- [x] ER 图、表设计、CSV 导入、数据传输 / 对比 / 同步
- [x] Desktop Release（Electron 三平台）
- [ ] Redis 等更多数据源
- [ ] Docker 一键部署镜像
- [ ] 插件化扩展体系

## 贡献

欢迎 Issue 与 Pull Request：

1. Fork 本仓库
2. 创建分支：`git checkout -b feature/your-feature`
3. 提交改动并发起 PR

## 免责声明

请在生产环境谨慎执行 SQL。AI 生成的语句可能存在错误，执行前请自行审查。
