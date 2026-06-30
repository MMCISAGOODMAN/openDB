# 国产数据库 JDBC 驱动

openDB 内置 **openGauss** 驱动（Maven Central）。**达梦 DM8** 与 **人大金仓 KingbaseES** 的 JDBC 驱动受厂商许可限制，需自行下载并放置到本目录或用户数据目录下的 `drivers/` 文件夹。

## 放置位置（任选其一）

| 路径 | 说明 |
|------|------|
| `backend/drivers/` | 开发模式，相对于后端工作目录 |
| `$OPENDDB_DATA_DIR/drivers/` | 桌面版 / 生产模式推荐 |
| `$OPENDDB_DRIVERS_DIR/` | 显式指定驱动目录 |

## 所需 JAR

| 数据库 | 驱动类 | 建议文件名 |
|--------|--------|------------|
| 达梦 DM8 | `dm.jdbc.driver.DmDriver` | `DmJdbcDriver18.jar`（以厂商包为准） |
| KingbaseES | `com.kingbase8.Driver` | `kingbase8-*.jar` |

## 获取方式

- **达梦**：从 [达梦官网](https://www.dameng.com/) 下载 DM JDBC 驱动
- **KingbaseES**：从 [人大金仓官网](https://www.kingbase.com.cn/) 下载 JDBC 驱动
- **openGauss**：已随 `opendb-server` JAR 打包，无需额外放置

## Maven 本地构建（可选）

若希望在编译期将 vendor JAR 打入 classpath，可启用 profile：

```bash
# 将 JAR 放入 backend/drivers/ 后
mvn -B package -Pdomestic-drivers
```

CI 默认 **不** 包含 vendor 驱动，仅编译与打包桌面安装程序。
