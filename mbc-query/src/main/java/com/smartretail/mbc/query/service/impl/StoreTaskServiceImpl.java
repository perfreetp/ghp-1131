package com.smartretail.mbc.query.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartretail.mbc.benefit.entity.BenefitUseLog;
import com.smartretail.mbc.benefit.entity.IdempotentRecord;
import com.smartretail.mbc.benefit.mapper.BenefitUseLogMapper;
import com.smartretail.mbc.benefit.mapper.IdempotentRecordMapper;
import com.smartretail.mbc.common.enums.StoreTaskBizTypeEnum;
import com.smartretail.mbc.common.enums.StoreTaskPriorityEnum;
import com.smartretail.mbc.common.enums.StoreTaskSourceEnum;
import com.smartretail.mbc.common.enums.StoreTaskStatusEnum;
import com.smartretail.mbc.common.enums.StoreTaskTypeEnum;
import com.smartretail.mbc.common.exception.BusinessException;
import com.smartretail.mbc.member.dto.StoreTaskHandleDTO;
import com.smartretail.mbc.member.dto.StoreTaskQueryDTO;
import com.smartretail.mbc.member.entity.StoreInfo;
import com.smartretail.mbc.member.entity.StoreTask;
import com.smartretail.mbc.member.mapper.StoreInfoMapper;
import com.smartretail.mbc.member.mapper.StoreTaskMapper;
import com.smartretail.mbc.member.service.StoreTaskService;
import com.smartretail.mbc.member.vo.StoreTaskBoardVO;
import com.smartretail.mbc.member.vo.StoreTaskVO;
import com.smartretail.mbc.member.vo.TaskStatVO;
import com.smartretail.mbc.query.entity.ActivityBudget;
import com.smartretail.mbc.query.entity.ReconcileRecord;
import com.smartretail.mbc.query.entity.RiskRecord;
import com.smartretail.mbc.query.mapper.ActivityBudgetMapper;
import com.smartretail.mbc.query.mapper.ReconcileRecordMapper;
import com.smartretail.mbc.query.mapper.RiskRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StoreTaskServiceImpl implements StoreTaskService {

    private final StoreTaskMapper storeTaskMapper;
    private final StoreInfoMapper storeInfoMapper;
    private final RiskRecordMapper riskRecordMapper;
    private final ReconcileRecordMapper reconcileRecordMapper;
    private final ActivityBudgetMapper activityBudgetMapper;
    private final IdempotentRecordMapper idempotentRecordMapper;
    private final BenefitUseLogMapper benefitUseLogMapper;

    @Override
    public StoreTaskBoardVO getStoreTaskBoard(StoreTaskQueryDTO dto) {
        if (dto.getStoreCode() == null || dto.getStoreCode().isEmpty()) {
            throw new BusinessException("门店编码不能为空");
        }

        StoreTaskBoardVO result = new StoreTaskBoardVO();
        result.setStoreCode(dto.getStoreCode());

        StoreInfo storeInfo = getStoreInfo(dto.getStoreCode());
        if (storeInfo != null) {
            result.setStoreName(storeInfo.getStoreName());
        }

        LambdaQueryWrapper<StoreTask> pendingWrapper = buildBaseWrapper(dto);
        pendingWrapper.eq(StoreTask::getStatus, StoreTaskStatusEnum.PENDING.getCode());
        Long totalPending = storeTaskMapper.selectCount(pendingWrapper);
        result.setTotalPending(totalPending != null ? totalPending.intValue() : 0);

        LambdaQueryWrapper<StoreTask> highPriorityWrapper = buildBaseWrapper(dto);
        highPriorityWrapper.eq(StoreTask::getStatus, StoreTaskStatusEnum.PENDING.getCode());
        highPriorityWrapper.eq(StoreTask::getPriority, StoreTaskPriorityEnum.HIGH.getCode());
        Long highPriorityCount = storeTaskMapper.selectCount(highPriorityWrapper);
        result.setHighPriorityCount(highPriorityCount != null ? highPriorityCount.intValue() : 0);

        List<TaskStatVO> taskStats = getTaskStats(dto);
        result.setTaskStats(taskStats);

        IPage<StoreTaskVO> tasks = getTaskPage(dto);
        result.setTasks(tasks);

        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleTask(StoreTaskHandleDTO dto) {
        StoreTask task = storeTaskMapper.selectById(dto.getTaskId());
        if (task == null) {
            throw new BusinessException("任务不存在");
        }

        if (!StoreTaskStatusEnum.PENDING.getCode().equals(task.getStatus())
                && !StoreTaskStatusEnum.PROCESSING.getCode().equals(task.getStatus())) {
            throw new BusinessException("任务状态不允许处理");
        }

        Integer action = dto.getAction();
        if (action == null) {
            action = 1;
        }

        Integer targetStatus;
        switch (action) {
            case 1:
                targetStatus = StoreTaskStatusEnum.DONE.getCode();
                break;
            case 2:
                targetStatus = StoreTaskStatusEnum.IGNORED.getCode();
                break;
            case 3:
                targetStatus = StoreTaskStatusEnum.PROCESSING.getCode();
                break;
            default:
                targetStatus = StoreTaskStatusEnum.DONE.getCode();
        }

        task.setStatus(targetStatus);
        task.setHandler(dto.getHandler());
        task.setHandleTime(LocalDateTime.now());
        task.setHandleResult(dto.getHandleResult());

        storeTaskMapper.updateById(task);

        syncRelatedRecords(task, dto.getHandler(), dto.getHandleResult());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void generateDailyTasks(String storeCode, LocalDate date) {
        if (storeCode == null || storeCode.isEmpty()) {
            throw new BusinessException("门店编码不能为空");
        }

        StoreInfo storeInfo = getStoreInfo(storeCode);
        if (storeInfo == null) {
            throw new BusinessException("门店不存在");
        }
        String storeName = storeInfo.getStoreName();

        log.info("开始生成门店每日任务, storeCode: {}, date: {}", storeCode, date);

        LocalDate yesterday = date.minusDays(1);
        int generatedCount = 0;

        generatedCount += generateReconcileTasks(storeCode, storeName, yesterday);

        generatedCount += generateRiskTasks(storeCode, storeName, date);

        generatedCount += generateBudgetTasks(storeCode, storeName);

        generatedCount += generateIdempotentTasks(storeCode, storeName);

        log.info("门店每日任务生成完成, storeCode: {}, date: {}, 生成任务数: {}", storeCode, date, generatedCount);
    }

    private LambdaQueryWrapper<StoreTask> buildBaseWrapper(StoreTaskQueryDTO dto) {
        LambdaQueryWrapper<StoreTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StoreTask::getStoreCode, dto.getStoreCode());
        if (dto.getTaskType() != null) {
            wrapper.eq(StoreTask::getTaskType, dto.getTaskType());
        }
        if (dto.getStatus() != null) {
            wrapper.eq(StoreTask::getStatus, dto.getStatus());
        }
        if (dto.getPriority() != null) {
            wrapper.eq(StoreTask::getPriority, dto.getPriority());
        }
        return wrapper;
    }

    private List<TaskStatVO> getTaskStats(StoreTaskQueryDTO dto) {
        List<TaskStatVO> stats = new ArrayList<>();

        for (StoreTaskTypeEnum typeEnum : StoreTaskTypeEnum.values()) {
            TaskStatVO stat = new TaskStatVO();
            stat.setTaskType(typeEnum.getCode());
            stat.setTaskTypeName(typeEnum.getName());

            LambdaQueryWrapper<StoreTask> wrapper = buildBaseWrapper(dto);
            wrapper.eq(StoreTask::getTaskType, typeEnum.getCode());
            wrapper.eq(StoreTask::getStatus, StoreTaskStatusEnum.PENDING.getCode());
            Long pendingCount = storeTaskMapper.selectCount(wrapper);
            stat.setPendingCount(pendingCount != null ? pendingCount.intValue() : 0);

            LambdaQueryWrapper<StoreTask> processingWrapper = buildBaseWrapper(dto);
            processingWrapper.eq(StoreTask::getTaskType, typeEnum.getCode());
            processingWrapper.eq(StoreTask::getStatus, StoreTaskStatusEnum.PROCESSING.getCode());
            Long processingCount = storeTaskMapper.selectCount(processingWrapper);
            stat.setProcessingCount(processingCount != null ? processingCount.intValue() : 0);

            LambdaQueryWrapper<StoreTask> doneWrapper = buildBaseWrapper(dto);
            doneWrapper.eq(StoreTask::getTaskType, typeEnum.getCode());
            doneWrapper.and(w -> w.eq(StoreTask::getStatus, StoreTaskStatusEnum.DONE.getCode())
                    .or().eq(StoreTask::getStatus, StoreTaskStatusEnum.IGNORED.getCode()));
            Long doneCount = storeTaskMapper.selectCount(doneWrapper);
            stat.setDoneCount(doneCount != null ? doneCount.intValue() : 0);

            stat.setTotalCount(stat.getPendingCount() + stat.getProcessingCount() + stat.getDoneCount());

            stats.add(stat);
        }

        return stats;
    }

    private IPage<StoreTaskVO> getTaskPage(StoreTaskQueryDTO dto) {
        LambdaQueryWrapper<StoreTask> wrapper = buildBaseWrapper(dto);
        wrapper.orderByAsc(StoreTask::getPriority);
        wrapper.orderByDesc(StoreTask::getCreateTime);

        Page<StoreTask> page = new Page<>(dto.getPageNum(), dto.getPageSize());
        IPage<StoreTask> taskPage = storeTaskMapper.selectPage(page, wrapper);

        return taskPage.convert(this::toTaskVO);
    }

    private StoreTaskVO toTaskVO(StoreTask task) {
        StoreTaskVO vo = new StoreTaskVO();
        vo.setId(task.getId());
        vo.setTaskNo(task.getTaskNo());
        vo.setTaskType(task.getTaskType());
        vo.setBizType(task.getBizType());
        vo.setBizId(task.getBizId());
        vo.setTitle(task.getTitle());
        vo.setDescription(task.getDescription());
        vo.setPriority(task.getPriority());
        vo.setStatus(task.getStatus());
        vo.setHandler(task.getHandler());
        vo.setHandleTime(task.getHandleTime());
        vo.setHandleResult(task.getHandleResult());
        vo.setSource(task.getSource());
        vo.setCreateTime(task.getCreateTime());

        StoreTaskTypeEnum typeEnum = StoreTaskTypeEnum.getByCode(task.getTaskType());
        vo.setTaskTypeName(typeEnum != null ? typeEnum.getName() : "");

        StoreTaskBizTypeEnum bizTypeEnum = StoreTaskBizTypeEnum.getByCode(task.getBizType());
        vo.setBizTypeName(bizTypeEnum != null ? bizTypeEnum.getName() : "");

        StoreTaskPriorityEnum priorityEnum = StoreTaskPriorityEnum.getByCode(task.getPriority());
        vo.setPriorityName(priorityEnum != null ? priorityEnum.getName() : "");

        StoreTaskStatusEnum statusEnum = StoreTaskStatusEnum.getByCode(task.getStatus());
        vo.setStatusName(statusEnum != null ? statusEnum.getName() : "");

        StoreTaskSourceEnum sourceEnum = StoreTaskSourceEnum.getByCode(task.getSource());
        vo.setSourceName(sourceEnum != null ? sourceEnum.getName() : "");

        return vo;
    }

    private StoreInfo getStoreInfo(String storeCode) {
        LambdaQueryWrapper<StoreInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StoreInfo::getStoreCode, storeCode);
        return storeInfoMapper.selectOne(wrapper);
    }

    private void syncRelatedRecords(StoreTask task, String handler, String handleResult) {
        if (StoreTaskTypeEnum.RISK_CONFIRM.getCode().equals(task.getTaskType())) {
            syncRiskRecord(task, handler, handleResult);
        } else if (StoreTaskTypeEnum.RECONCILE_EXCEPTION.getCode().equals(task.getTaskType())) {
            syncReconcileRecord(task, handleResult);
        }
    }

    private void syncRiskRecord(StoreTask task, String handler, String handleResult) {
        try {
            Long riskRecordId = Long.parseLong(task.getBizId());
            RiskRecord riskRecord = riskRecordMapper.selectById(riskRecordId);
            if (riskRecord != null) {
                riskRecord.setHandleResult(2);
                riskRecord.setHandleStaff(handler);
                riskRecord.setHandleTime(LocalDateTime.now());
                riskRecord.setHandleRemark(handleResult);
                riskRecordMapper.updateById(riskRecord);
            }
        } catch (Exception e) {
            log.warn("同步风险记录失败, taskId: {}, bizId: {}", task.getId(), task.getBizId(), e);
        }
    }

    private void syncReconcileRecord(StoreTask task, String handleResult) {
        try {
            LambdaQueryWrapper<ReconcileRecord> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ReconcileRecord::getUseNo, task.getBizId());
            ReconcileRecord record = reconcileRecordMapper.selectOne(wrapper);
            if (record != null) {
                record.setReconcileDiff(handleResult);
                reconcileRecordMapper.updateById(record);
            }
        } catch (Exception e) {
            log.warn("同步对账记录失败, taskId: {}, bizId: {}", task.getId(), task.getBizId(), e);
        }
    }

    private int generateReconcileTasks(String storeCode, String storeName, LocalDate date) {
        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();

        LambdaQueryWrapper<BenefitUseLog> bulWrapper = new LambdaQueryWrapper<>();
        bulWrapper.eq(BenefitUseLog::getStoreCode, storeCode);
        bulWrapper.ge(BenefitUseLog::getCreateTime, dayStart);
        bulWrapper.lt(BenefitUseLog::getCreateTime, dayEnd);
        List<BenefitUseLog> useLogs = benefitUseLogMapper.selectList(bulWrapper);
        if (useLogs.isEmpty()) {
            return 0;
        }

        List<String> useNos = useLogs.stream()
                .map(BenefitUseLog::getUseNo)
                .filter(no -> no != null && !no.isEmpty())
                .collect(Collectors.toList());
        if (useNos.isEmpty()) {
            return 0;
        }

        Map<String, BenefitUseLog> useLogMap = useLogs.stream()
                .collect(Collectors.toMap(BenefitUseLog::getUseNo, b -> b, (a, b) -> a));

        LambdaQueryWrapper<ReconcileRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReconcileRecord::getReconcileDate, date);
        wrapper.ne(ReconcileRecord::getReconcileStatus, 1);
        wrapper.in(ReconcileRecord::getUseNo, useNos);
        wrapper.orderByDesc(ReconcileRecord::getReconcileTime);

        List<ReconcileRecord> records = reconcileRecordMapper.selectList(wrapper);
        if (records.isEmpty()) {
            return 0;
        }

        int count = 0;
        for (ReconcileRecord record : records) {
            if (taskExists(StoreTaskTypeEnum.RECONCILE_EXCEPTION.getCode(), record.getUseNo())) {
                continue;
            }

            StoreTask task = new StoreTask();
            task.setTaskNo(generateTaskNo());
            task.setStoreCode(storeCode);
            task.setStoreName(storeName);
            task.setTaskType(StoreTaskTypeEnum.RECONCILE_EXCEPTION.getCode());
            task.setBizType(getBizTypeByBenefitType(record.getBenefitType()));
            task.setBizId(record.getUseNo());
            task.setTitle("核销异常待处理");
            task.setDescription(record.getReconcileDiff());
            task.setPriority(StoreTaskPriorityEnum.LOW.getCode());
            task.setStatus(StoreTaskStatusEnum.PENDING.getCode());
            task.setSource(StoreTaskSourceEnum.SYSTEM.getCode());

            storeTaskMapper.insert(task);
            count++;
        }

        return count;
    }

    private int generateRiskTasks(String storeCode, String storeName, LocalDate date) {
        LambdaQueryWrapper<RiskRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RiskRecord::getStoreCode, storeCode);
        wrapper.ge(RiskRecord::getRiskLevel, 2);
        wrapper.isNull(RiskRecord::getHandleResult);
        wrapper.orderByDesc(RiskRecord::getCreateTime);

        List<RiskRecord> records = riskRecordMapper.selectList(wrapper);
        if (records.isEmpty()) {
            return 0;
        }

        int count = 0;
        for (RiskRecord record : records) {
            String bizId = String.valueOf(record.getId());
            if (taskExists(StoreTaskTypeEnum.RISK_CONFIRM.getCode(), bizId)) {
                continue;
            }

            StoreTask task = new StoreTask();
            task.setTaskNo(generateTaskNo());
            task.setStoreCode(storeCode);
            task.setStoreName(storeName);
            task.setTaskType(StoreTaskTypeEnum.RISK_CONFIRM.getCode());
            task.setBizType(StoreTaskBizTypeEnum.ORDER.getCode());
            task.setBizId(bizId);
            task.setTitle("风控订单待确认");
            task.setDescription(record.getRuleName() + ": " + record.getCurrentValue());
            task.setPriority(StoreTaskPriorityEnum.MEDIUM.getCode());
            task.setStatus(StoreTaskStatusEnum.PENDING.getCode());
            task.setSource(StoreTaskSourceEnum.SYSTEM.getCode());

            storeTaskMapper.insert(task);
            count++;
        }

        return count;
    }

    private int generateBudgetTasks(String storeCode, String storeName) {
        LambdaQueryWrapper<ActivityBudget> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ActivityBudget::getStoreCode, storeCode);
        wrapper.eq(ActivityBudget::getStatus, 0);
        wrapper.gt(ActivityBudget::getTotalBudget, BigDecimal.ZERO);

        List<ActivityBudget> budgets = activityBudgetMapper.selectList(wrapper);
        if (budgets.isEmpty()) {
            return 0;
        }

        int count = 0;
        for (ActivityBudget budget : budgets) {
            if (budget.getTotalBudget() == null || budget.getTotalBudget().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            BigDecimal used = budget.getUsedBudget() != null ? budget.getUsedBudget() : BigDecimal.ZERO;
            BigDecimal usageRate = used.divide(budget.getTotalBudget(), 4, RoundingMode.HALF_UP);

            if (usageRate.compareTo(new BigDecimal("0.8")) < 0) {
                continue;
            }

            String bizId = String.valueOf(budget.getActivityId());
            if (taskExists(StoreTaskTypeEnum.BUDGET_WARNING.getCode(), bizId)) {
                continue;
            }

            boolean isOverLimit = usageRate.compareTo(BigDecimal.ONE) >= 0;

            StoreTask task = new StoreTask();
            task.setTaskNo(generateTaskNo());
            task.setStoreCode(storeCode);
            task.setStoreName(storeName);
            task.setTaskType(StoreTaskTypeEnum.BUDGET_WARNING.getCode());
            task.setBizType(StoreTaskBizTypeEnum.COUPON.getCode());
            task.setBizId(bizId);
            task.setTitle(isOverLimit ? "活动预算已超限" : "活动预算即将超限");
            task.setDescription("预算使用率: " + usageRate.multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP) + "%");
            task.setPriority(isOverLimit ? StoreTaskPriorityEnum.HIGH.getCode() : StoreTaskPriorityEnum.MEDIUM.getCode());
            task.setStatus(StoreTaskStatusEnum.PENDING.getCode());
            task.setSource(StoreTaskSourceEnum.SYSTEM.getCode());

            storeTaskMapper.insert(task);
            count++;
        }

        return count;
    }

    private int generateIdempotentTasks(String storeCode, String storeName) {
        LocalDateTime thirtyMinutesAgo = LocalDateTime.now().minusMinutes(30);

        LambdaQueryWrapper<IdempotentRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(IdempotentRecord::getProcessStatus, 1);
        wrapper.lt(IdempotentRecord::getCreateTime, thirtyMinutesAgo);
        wrapper.orderByAsc(IdempotentRecord::getCreateTime);

        List<IdempotentRecord> records = idempotentRecordMapper.selectList(wrapper);
        if (records.isEmpty()) {
            return 0;
        }

        int count = 0;
        for (IdempotentRecord record : records) {
            String bizId = record.getBusinessNo();
            if (taskExists(StoreTaskTypeEnum.IDEMPOTENT_EXCEPTION.getCode(), bizId)) {
                continue;
            }

            StoreTask task = new StoreTask();
            task.setTaskNo(generateTaskNo());
            task.setStoreCode(storeCode);
            task.setStoreName(storeName);
            task.setTaskType(StoreTaskTypeEnum.IDEMPOTENT_EXCEPTION.getCode());
            task.setBizType(StoreTaskBizTypeEnum.COUPON.getCode());
            task.setBizId(bizId);
            task.setTitle("幂等处理超时");
            task.setDescription("处理中超过30分钟，重试次数: " + record.getRetryCount());
            task.setPriority(StoreTaskPriorityEnum.LOW.getCode());
            task.setStatus(StoreTaskStatusEnum.PENDING.getCode());
            task.setSource(StoreTaskSourceEnum.SYSTEM.getCode());

            storeTaskMapper.insert(task);
            count++;
        }

        return count;
    }

    private boolean taskExists(Integer taskType, String bizId) {
        LambdaQueryWrapper<StoreTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StoreTask::getTaskType, taskType);
        wrapper.eq(StoreTask::getBizId, bizId);
        wrapper.and(w -> w.eq(StoreTask::getStatus, StoreTaskStatusEnum.PENDING.getCode())
                .or().eq(StoreTask::getStatus, StoreTaskStatusEnum.PROCESSING.getCode()));
        Long count = storeTaskMapper.selectCount(wrapper);
        return count != null && count > 0;
    }

    private Integer getBizTypeByBenefitType(Integer benefitType) {
        if (benefitType == null) {
            return StoreTaskBizTypeEnum.COUPON.getCode();
        }
        return switch (benefitType) {
            case 1 -> StoreTaskBizTypeEnum.COUPON.getCode();
            case 2 -> StoreTaskBizTypeEnum.POINT.getCode();
            default -> StoreTaskBizTypeEnum.ORDER.getCode();
        };
    }

    private String generateTaskNo() {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
        return "STK" + dateStr + uuid;
    }
}
