package com.evops.workorder;

import com.evops.common.ApiResponse;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/work-orders")
public class WorkOrderController {

    private final WorkOrderService workOrderService;

    public WorkOrderController(WorkOrderService workOrderService) {
        this.workOrderService = workOrderService;
    }

    @PostMapping
    public ApiResponse<WorkOrder> create(@Validated @RequestBody WorkOrderCreateReq req) {
        return ApiResponse.ok(workOrderService.create(req));
    }

    @PostMapping("/{orderNo}/start")
    public ApiResponse<WorkOrder> start(@PathVariable String orderNo,
                                        @Validated @RequestBody StatusChangeReq req) {
        return ApiResponse.ok(workOrderService.start(orderNo, req.getRequestNo()));
    }

    @PostMapping("/{orderNo}/complete")
    public ApiResponse<WorkOrder> complete(@PathVariable String orderNo,
                                           @Validated @RequestBody WorkOrderCompleteReq req) {
        return ApiResponse.ok(workOrderService.complete(orderNo, req));
    }

    @PostMapping("/{orderNo}/reverse")
    public ApiResponse<WorkOrder> reverse(@PathVariable String orderNo,
                                          @Validated @RequestBody WorkOrderReverseReq req) {
        return ApiResponse.ok(workOrderService.reverse(orderNo, req));
    }

    @GetMapping("/{orderNo}")
    public ApiResponse<WorkOrder> get(@PathVariable String orderNo) {
        return ApiResponse.ok(workOrderService.getByOrderNo(orderNo));
    }

    @GetMapping
    public ApiResponse<List<WorkOrder>> list(@RequestParam(required = false) String machineNo) {
        return ApiResponse.ok(workOrderService.list(machineNo));
    }
}
