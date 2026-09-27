package com.evops.workorder;

import com.evops.common.BizException;
import com.evops.machine.MachineCreateReq;
import com.evops.machine.MachineService;
import com.evops.machine.MachineUpdateReq;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class WorkOrderFlowIntegrationTest {

    @Autowired
    private MachineService machineService;
    @Autowired
    private WorkOrderService workOrderService;

    private String machineNo() {
        return "M-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private String orderNo() {
        return "WO-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private String requestNo() {
        return "REQ-" + UUID.randomUUID().toString().substring(0, 12);
    }

    private String createMachine(String machineNo) {
        MachineCreateReq req = new MachineCreateReq();
        req.setRequestNo(requestNo());
        req.setMachineNo(machineNo);
        req.setMachineName("拖拉机-" + machineNo);
        req.setMachineType("TRACTOR");
        machineService.create(req);
        return machineNo;
    }

    private WorkOrderCreateReq newOrderReq(String orderNo, String machineNo) {
        WorkOrderCreateReq req = new WorkOrderCreateReq();
        req.setRequestNo(requestNo());
        req.setOrderNo(orderNo);
        req.setMachineNo(machineNo);
        req.setWorkType("PLOW");
        req.setReportedArea(new BigDecimal("10.00"));
        return req;
    }

    @Test
    void fullFlowCreateStartCompleteReverse() {
        String machineNo = createMachine(machineNo());
        String orderNo = orderNo();

        WorkOrder created = workOrderService.create(newOrderReq(orderNo, machineNo));
        assertEquals(WorkOrderService.STATUS_PENDING, created.getStatus());
        assertEquals(WorkOrderService.DOC_NORMAL, created.getDocType());
        assertEquals(0, created.getVersion());
        assertNotNull(created.getCreateTime());

        StatusChangeReq startReq = new StatusChangeReq();
        startReq.setRequestNo(requestNo());
        WorkOrder started = workOrderService.start(orderNo, startReq.getRequestNo());
        assertEquals(WorkOrderService.STATUS_IN_PROGRESS, started.getStatus());
        assertEquals(1, started.getVersion());

        WorkOrderCompleteReq completeReq = new WorkOrderCompleteReq();
        completeReq.setRequestNo(requestNo());
        completeReq.setTrackArea(new BigDecimal("9.50"));
        WorkOrder completed = workOrderService.complete(orderNo, completeReq);
        assertEquals(WorkOrderService.STATUS_COMPLETED, completed.getStatus());
        assertEquals(0, new BigDecimal("9.50").compareTo(completed.getTrackArea()));
        // 人工上报面积保持不动，仅作参考
        assertEquals(0, new BigDecimal("10.00").compareTo(completed.getReportedArea()));
        assertEquals(2, completed.getVersion());

        WorkOrderReverseReq reverseReq = new WorkOrderReverseReq();
        reverseReq.setRequestNo(requestNo());
        reverseReq.setReason("重复派单，冲销");
        WorkOrder reversal = workOrderService.reverse(orderNo, reverseReq);

        // 反向单据：单号加前缀、指回原单、面积取负
        assertEquals(WorkOrderService.DOC_REVERSAL, reversal.getDocType());
        assertEquals(WorkOrderService.REVERSAL_NO_PREFIX + orderNo, reversal.getOrderNo());
        assertEquals(orderNo, reversal.getRefOrderNo());
        assertEquals(0, new BigDecimal("-9.50").compareTo(reversal.getTrackArea()));
        assertEquals(0, new BigDecimal("-10.00").compareTo(reversal.getReportedArea()));

        // 原单已冲销，历史保留
        WorkOrder origin = workOrderService.getByOrderNo(orderNo);
        assertEquals(WorkOrderService.STATUS_REVERSED, origin.getStatus());
        assertEquals("重复派单，冲销", origin.getReverseReason());
    }

    @Test
    void duplicateOrderNoRejected() {
        String machineNo = createMachine(machineNo());
        String orderNo = orderNo();
        workOrderService.create(newOrderReq(orderNo, machineNo));

        BizException ex = assertThrows(BizException.class,
                () -> workOrderService.create(newOrderReq(orderNo, machineNo)));
        assertTrue(ex.getMessage().contains("重复提交") || ex.getMessage().contains("冲突"));
    }

    @Test
    void sameRequestNoReplaysIdempotently() {
        String machineNo = createMachine(machineNo());
        String orderNo = orderNo();
        WorkOrderCreateReq req = newOrderReq(orderNo, machineNo);

        WorkOrder first = workOrderService.create(req);
        WorkOrder replayed = workOrderService.create(req);

        assertEquals(first.getId(), replayed.getId());
        assertEquals(0, replayed.getVersion());
    }

    @Test
    void illegalTransitionRejected() {
        String machineNo = createMachine(machineNo());
        String orderNo = orderNo();
        workOrderService.create(newOrderReq(orderNo, machineNo));

        // 未开工直接完工
        WorkOrderCompleteReq completeReq = new WorkOrderCompleteReq();
        completeReq.setRequestNo(requestNo());
        completeReq.setTrackArea(new BigDecimal("5"));
        assertThrows(BizException.class, () -> workOrderService.complete(orderNo, completeReq));

        // 完工必须带轨迹核算面积
        StatusChangeReq startReq = new StatusChangeReq();
        startReq.setRequestNo(requestNo());
        workOrderService.start(orderNo, startReq.getRequestNo());
        WorkOrderCompleteReq noArea = new WorkOrderCompleteReq();
        noArea.setRequestNo(requestNo());
        assertThrows(BizException.class, () -> workOrderService.complete(orderNo, noArea));
    }

    @Test
    void doubleReverseRejected() {
        String machineNo = createMachine(machineNo());
        String orderNo = orderNo();
        workOrderService.create(newOrderReq(orderNo, machineNo));

        WorkOrderReverseReq reverseReq = new WorkOrderReverseReq();
        reverseReq.setRequestNo(requestNo());
        workOrderService.reverse(orderNo, reverseReq);

        // 原单重复冲销
        WorkOrderReverseReq again = new WorkOrderReverseReq();
        again.setRequestNo(requestNo());
        assertThrows(BizException.class, () -> workOrderService.reverse(orderNo, again));
    }

    @Test
    void machineUpdateKeepsVersionAndIdempotent() {
        String machineNo = createMachine(machineNo());

        MachineUpdateReq req = new MachineUpdateReq();
        req.setRequestNo(requestNo());
        req.setMachineName("新名字");
        req.setStatus("DISABLED");
        machineService.update(machineNo, req);

        assertEquals(MachineService.STATUS_DISABLED, machineService.getByMachineNo(machineNo).getStatus());
        assertEquals(1, machineService.getByMachineNo(machineNo).getVersion());

        // 同请求号重放不重复加版本
        machineService.update(machineNo, req);
        assertEquals(1, machineService.getByMachineNo(machineNo).getVersion());

        // 停用农机不能建档
        String orderNo = orderNo();
        assertThrows(BizException.class, () -> workOrderService.create(newOrderReq(orderNo, machineNo)));
    }
}
