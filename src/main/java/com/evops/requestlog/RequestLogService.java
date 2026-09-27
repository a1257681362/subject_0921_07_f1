package com.evops.requestlog;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.evops.common.BizException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.function.Supplier;

/**
 * 幂等模板：同一事务内先查流水，命中即重放首次结果；
 * 未命中则执行业务并登记流水，request_no 唯一约束兜底并发。
 */
@Service
public class RequestLogService {

    private final RequestLogMapper requestLogMapper;

    public RequestLogService(RequestLogMapper requestLogMapper) {
        this.requestLogMapper = requestLogMapper;
    }

    public RequestLog findByRequestNo(String requestNo) {
        return requestLogMapper.selectOne(
                new LambdaQueryWrapper<RequestLog>().eq(RequestLog::getRequestNo, requestNo));
    }

    /**
     * @param requestNo 请求号（必填）
     * @param bizType   业务类型，同一请求号不得跨业务复用
     * @param action    首次执行的业务动作，返回业务单号
     * @param replay    重放时按已登记的业务单号加载当前结果
     */
    public <T> T executeIdempotent(String requestNo, String bizType,
                                   Supplier<String> action, java.util.function.Function<String, T> replay) {
        RequestLog existed = findByRequestNo(requestNo);
        if (existed != null) {
            checkBizType(existed, bizType);
            return replay.apply(existed.getBizNo());
        }
        String bizNo;
        try {
            bizNo = action.get();
            record(requestNo, bizType, bizNo);
        } catch (DuplicateKeyException ex) {
            // 并发下同请求号或同业务单号只成功一条，其余按冲突处理。
            throw new BizException("重复提交或单号冲突: " + requestNo);
        }
        return replay.apply(bizNo);
    }

    public void record(String requestNo, String bizType, String bizNo) {
        RequestLog log = new RequestLog();
        log.setRequestNo(requestNo);
        log.setBizType(bizType);
        log.setBizNo(bizNo);
        requestLogMapper.insert(log);
    }

    public void checkBizType(RequestLog log, String expectedBizType) {
        if (!expectedBizType.equals(log.getBizType())) {
            throw new BizException("请求号已被其他业务占用: " + log.getRequestNo());
        }
    }
}
