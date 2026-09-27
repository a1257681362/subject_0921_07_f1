package com.evops.machine;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.evops.common.BizException;
import com.evops.requestlog.RequestLogService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;

@Service
public class MachineService {

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_DISABLED = "DISABLED";

    static final String BIZ_CREATE = "MACHINE_CREATE";
    static final String BIZ_UPDATE = "MACHINE_UPDATE";

    private final MachineMapper machineMapper;
    private final RequestLogService requestLogService;

    public MachineService(MachineMapper machineMapper, RequestLogService requestLogService) {
        this.machineMapper = machineMapper;
        this.requestLogService = requestLogService;
    }

    public Machine getByMachineNo(String machineNo) {
        Machine machine = machineMapper.selectOne(
                new LambdaQueryWrapper<Machine>().eq(Machine::getMachineNo, machineNo));
        if (machine == null) {
            throw new BizException("农机不存在: " + machineNo);
        }
        return machine;
    }

    public List<Machine> list() {
        return machineMapper.selectList(new LambdaQueryWrapper<Machine>().orderByAsc(Machine::getId));
    }

    @Transactional(rollbackFor = Exception.class)
    public Machine create(MachineCreateReq req) {
        return requestLogService.executeIdempotent(req.getRequestNo(), BIZ_CREATE,
                () -> {
                    Machine machine = new Machine();
                    machine.setMachineNo(req.getMachineNo());
                    machine.setMachineName(req.getMachineName());
                    machine.setMachineType(req.getMachineType());
                    machine.setStatus(STATUS_ACTIVE);
                    machine.setVersion(0);
                    machineMapper.insert(machine);
                    return machine.getMachineNo();
                },
                this::getByMachineNo);
    }

    @Transactional(rollbackFor = Exception.class)
    public Machine update(String machineNo, MachineUpdateReq req) {
        return requestLogService.executeIdempotent(req.getRequestNo(), BIZ_UPDATE,
                () -> {
                    Machine machine = getByMachineNo(machineNo);
                    if (StringUtils.hasText(req.getMachineName())) {
                        machine.setMachineName(req.getMachineName());
                    }
                    if (StringUtils.hasText(req.getMachineType())) {
                        machine.setMachineType(req.getMachineType());
                    }
                    if (StringUtils.hasText(req.getStatus())) {
                        machine.setStatus(parseStatus(req.getStatus()));
                    }
                    int rows = machineMapper.updateById(machine);
                    if (rows == 0) {
                        throw new BizException("农机数据已被他人变更，请刷新后重试");
                    }
                    return machineNo;
                },
                this::getByMachineNo);
    }

    private String parseStatus(String status) {
        String normalized = status.trim().toUpperCase();
        if (!Arrays.asList(STATUS_ACTIVE, STATUS_DISABLED).contains(normalized)) {
            throw new BizException("非法农机状态: " + status);
        }
        return normalized;
    }
}
