package com.evops.workorder;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.evops.common.BizException;
import com.evops.machine.Machine;
import com.evops.machine.MachineService;
import com.evops.requestlog.RequestLogService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Consumer;

@Service
public class WorkOrderService {

    public static final String DOC_NORMAL = "NORMAL";
    public static final String DOC_REVERSAL = "REVERSAL";

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_REVERSED = "REVERSED";

    static final String BIZ_CREATE = "WORK_ORDER_CREATE";
    static final String BIZ_START = "WORK_ORDER_START";
    static final String BIZ_COMPLETE = "WORK_ORDER_COMPLETE";
    static final String BIZ_REVERSE = "WORK_ORDER_REVERSE";
    static final String REVERSAL_NO_PREFIX = "R";

    private final WorkOrderMapper workOrderMapper;
    private final MachineService machineService;
    private final RequestLogService requestLogService;

    public WorkOrderService(WorkOrderMapper workOrderMapper, MachineService machineService,
                            RequestLogService requestLogService) {
        this.workOrderMapper = workOrderMapper;
        this.machineService = machineService;
        this.requestLogService = requestLogService;
    }

    public WorkOrder getByOrderNo(String orderNo) {
        WorkOrder order = workOrderMapper.selectOne(
                new LambdaQueryWrapper<WorkOrder>().eq(WorkOrder::getOrderNo, orderNo));
        if (order == null) {
            throw new BizException("作业单不存在: " + orderNo);
        }
        return order;
    }

    public List<WorkOrder> list(String machineNo) {
        LambdaQueryWrapper<WorkOrder> query = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(machineNo)) {
            query.eq(WorkOrder::getMachineNo, machineNo);
        }
        query.orderByDesc(WorkOrder::getId);
        return workOrderMapper.selectList(query);
    }

    /** 建档：校验农机 -> 插单 -> 记请求流水，同一事务；单号唯一约束兜底并发。 */
    @Transactional(rollbackFor = Exception.class)
    public WorkOrder create(WorkOrderCreateReq req) {
        return requestLogService.executeIdempotent(req.getRequestNo(), BIZ_CREATE,
                () -> {
                    Machine machine = machineService.getByMachineNo(req.getMachineNo());
                    if (!MachineService.STATUS_ACTIVE.equals(machine.getStatus())) {
                        throw new BizException("农机已停用，不能建档: " + req.getMachineNo());
                    }
                    WorkOrder order = new WorkOrder();
                    order.setOrderNo(req.getOrderNo());
                    order.setDocType(DOC_NORMAL);
                    order.setMachineNo(req.getMachineNo());
                    order.setWorkType(req.getWorkType());
                    order.setStatus(STATUS_PENDING);
                    order.setReportedArea(req.getReportedArea());
                    order.setVersion(0);
                    workOrderMapper.insert(order);
                    return order.getOrderNo();
                },
                this::getByOrderNo);
    }

    @Transactional(rollbackFor = Exception.class)
    public WorkOrder start(String orderNo, String requestNo) {
        return changeStatus(orderNo, requestNo, BIZ_START,
                STATUS_PENDING, STATUS_IN_PROGRESS, null);
    }

    /** 完工：面积以轨迹核算为准，人工上报值保持不动。 */
    @Transactional(rollbackFor = Exception.class)
    public WorkOrder complete(String orderNo, WorkOrderCompleteReq req) {
        if (req.getTrackArea() == null || req.getTrackArea().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException("轨迹核算面积必须大于0");
        }
        return changeStatus(orderNo, req.getRequestNo(), BIZ_COMPLETE,
                STATUS_IN_PROGRESS, STATUS_COMPLETED,
                order -> order.setTrackArea(req.getTrackArea()));
    }

    /** 冲销：原单置已冲销 + 另开面积取负的反向单据，同一事务，失败整体回退。 */
    @Transactional(rollbackFor = Exception.class)
    public WorkOrder reverse(String orderNo, WorkOrderReverseReq req) {
        return requestLogService.executeIdempotent(req.getRequestNo(), BIZ_REVERSE,
                () -> {
                    WorkOrder origin = getByOrderNo(orderNo);
                    if (!DOC_NORMAL.equals(origin.getDocType())) {
                        throw new BizException("反向单据不能再冲销");
                    }
                    if (STATUS_REVERSED.equals(origin.getStatus())) {
                        throw new BizException("作业单已冲销，请勿重复操作");
                    }
                    origin.setStatus(STATUS_REVERSED);
                    origin.setReverseReason(req.getReason());
                    updateWithVersion(origin);

                    WorkOrder reversal = new WorkOrder();
                    reversal.setOrderNo(REVERSAL_NO_PREFIX + origin.getOrderNo());
                    reversal.setDocType(DOC_REVERSAL);
                    reversal.setRefOrderNo(origin.getOrderNo());
                    reversal.setMachineNo(origin.getMachineNo());
                    reversal.setWorkType(origin.getWorkType());
                    reversal.setStatus(STATUS_REVERSED);
                    reversal.setReportedArea(negate(origin.getReportedArea()));
                    reversal.setTrackArea(negate(origin.getTrackArea()));
                    reversal.setReverseReason(req.getReason());
                    reversal.setVersion(0);
                    workOrderMapper.insert(reversal);
                    return reversal.getOrderNo();
                },
                this::getByOrderNo);
    }

    private WorkOrder changeStatus(String orderNo, String requestNo, String bizType,
                                   String from, String to, Consumer<WorkOrder> applier) {
        return requestLogService.executeIdempotent(requestNo, bizType,
                () -> {
                    WorkOrder order = getByOrderNo(orderNo);
                    if (!DOC_NORMAL.equals(order.getDocType())) {
                        throw new BizException("反向单据不能流转");
                    }
                    if (!from.equals(order.getStatus())) {
                        throw new BizException("当前状态[" + order.getStatus() + "]不允许该操作");
                    }
                    order.setStatus(to);
                    if (applier != null) {
                        applier.accept(order);
                    }
                    updateWithVersion(order);
                    return orderNo;
                },
                this::getByOrderNo);
    }

    private void updateWithVersion(WorkOrder order) {
        int rows = workOrderMapper.updateById(order);
        if (rows == 0) {
            throw new BizException("作业单已被他人变更，请刷新后重试");
        }
    }

    private BigDecimal negate(BigDecimal value) {
        return value == null ? null : value.negate();
    }
}
