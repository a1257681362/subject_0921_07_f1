# EVOPS 基础工作区

这是 springboot的骨架项目。

技术栈：Java 8、Spring Boot 2.7、MyBatis-Plus、H2 本地文件数据库、Shiro、Thymeleaf。

启动前准备：

1. 执行 `mvn -q -DskipTests compile` 验证骨架构建。
2. 执行 `mvn spring-boot:run` 启动应用；Spring Boot 会自动执行 `src/main/resources/schema.sql`。
3. H2 数据文件默认写入工作区 `data/evops`。如需修改路径，编辑 `application.yml` 中的 `jdbc:h2:file:` 连接串。

根目录的 `..\..\docs\schema\evops.sql` 是交付副本，应与工作区 `src/main/resources/schema.sql` 保持一致。

## F1 合作社作业管理主链路

**底账表**（`schema.sql`，历史数据一律不物理删除）：

- `t_machine` 农机底账，停用走 `status=DISABLED`
- `t_work_order` 作业单；冲销不删单，另开 `doc_type=REVERSAL` 的反向单据（面积取负抵消原单，`ref_order_no` 指回原单）
- `t_request_log` 请求流水，`request_no` 全库唯一，是所有写请求的幂等账本

**约定**：

- 所有新增/变更请求必须携带 `requestNo`；同一请求号重放返回首次处理结果，换请求号撞单号/唯一约束即失败（并发下同单号只成功一条）
- 每行带 `version`，更新走乐观锁，冲突返回"已被他人变更"
- 作业面积以 `track_area`（轨迹核算，完工时写入）为准，`reported_area`（人工上报）仅作参考
- 跨表写入（建档+流水、冲销原单+反向单）在同一事务内，失败整体回退
- 作业单状态机：`PENDING → IN_PROGRESS → COMPLETED`，任意正向单可冲销为 `REVERSED`（终态）

**接口**（统一返回 `ApiResponse{success,message,data}`，匿名可访问）：

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/machines` | 农机建档 |
| PUT | `/api/machines/{machineNo}` | 农机变更（名称/类型/状态） |
| GET | `/api/machines`、`/api/machines/{machineNo}` | 农机查询 |
| POST | `/api/work-orders` | 作业单建档 |
| POST | `/api/work-orders/{orderNo}/start` | 开工 |
| POST | `/api/work-orders/{orderNo}/complete` | 完工，必传 `trackArea` |
| POST | `/api/work-orders/{orderNo}/reverse` | 冲销，开反向单据 |
| GET | `/api/work-orders/{orderNo}` | 按作业单号检索 |
| GET | `/api/work-orders?machineNo=` | 作业单列表 |

示例：

```bash
curl -X POST localhost:8080/api/machines -H 'Content-Type: application/json' \
  -d '{"requestNo":"req-m-1","machineNo":"M001","machineName":"东方红拖拉机","machineType":"TRACTOR"}'
curl -X POST localhost:8080/api/work-orders -H 'Content-Type: application/json' \
  -d '{"requestNo":"req-o-1","orderNo":"WO001","machineNo":"M001","workType":"PLOW","reportedArea":100}'
```

验证：`mvn test`（`WorkOrderFlowIntegrationTest`，H2 内存库覆盖主链路闭环、重复单号、幂等重放、非法流转、双重冲销、版本递增）。



