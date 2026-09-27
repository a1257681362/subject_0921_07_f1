package com.evops.machine;

import com.evops.common.ApiResponse;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/machines")
public class MachineController {

    private final MachineService machineService;

    public MachineController(MachineService machineService) {
        this.machineService = machineService;
    }

    @PostMapping
    public ApiResponse<Machine> create(@Validated @RequestBody MachineCreateReq req) {
        return ApiResponse.ok(machineService.create(req));
    }

    @PutMapping("/{machineNo}")
    public ApiResponse<Machine> update(@PathVariable String machineNo,
                                       @Validated @RequestBody MachineUpdateReq req) {
        return ApiResponse.ok(machineService.update(machineNo, req));
    }

    @GetMapping("/{machineNo}")
    public ApiResponse<Machine> get(@PathVariable String machineNo) {
        return ApiResponse.ok(machineService.getByMachineNo(machineNo));
    }

    @GetMapping
    public ApiResponse<List<Machine>> list() {
        return ApiResponse.ok(machineService.list());
    }
}
