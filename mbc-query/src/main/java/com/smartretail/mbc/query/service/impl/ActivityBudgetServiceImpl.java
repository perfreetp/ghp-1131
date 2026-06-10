package com.smartretail.mbc.query.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartretail.mbc.common.exception.BusinessException;
import com.smartretail.mbc.common.service.BudgetOccupyService;
import com.smartretail.mbc.query.dto.ActivityBudgetCreateDTO;
import com.smartretail.mbc.query.dto.ActivityBudgetQueryDTO;
import com.smartretail.mbc.query.entity.Activity;
import com.smartretail.mbc.query.entity.ActivityBudget;
import com.smartretail.mbc.query.entity.ActivityBudgetLog;
import com.smartretail.mbc.query.mapper.ActivityBudgetLogMapper;
import com.smartretail.mbc.query.mapper.ActivityBudgetMapper;
import com.smartretail.mbc.query.mapper.ActivityMapper;
import com.smartretail.mbc.query.service.ActivityBudgetService;
import com.smartretail.mbc.query.vo.ActivityBudgetProgressVO;
import com.smartretail.mbc.query.vo.ActivityBudgetVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityBudgetServiceImpl implements ActivityBudgetService, BudgetOccupyService {

    private final ActivityBudgetMapper activityBudgetMapper;
    private final ActivityBudgetLogMapper activityBudgetLogMapper;
    private final ActivityMapper activityMapper;

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createActivityBudget(ActivityBudgetCreateDTO dto) {
        Activity activity = activityMapper.selectById(dto.getActivityId());
        if (activity == null) {
            throw new BusinessException("活动不存在");
        }

        String storeCode = StringUtils.hasText(dto.getStoreCode()) ? dto.getStoreCode() : null;
        ActivityBudget existing = activityBudgetMapper.selectByActivityAndStore(dto.getActivityId(), storeCode);
        if (existing != null) {
            throw new BusinessException("该活动门店预算已存在");
        }

        ActivityBudget budget = new ActivityBudget();
        budget.setActivityId(dto.getActivityId());
        budget.setStoreCode(storeCode);
        budget.setStoreName(dto.getStoreName());
        budget.setBudgetType(storeCode == null ? 1 : 2);
        budget.setTotalBudget(dto.getTotalBudget());
        budget.setUsedBudget(BigDecimal.ZERO);
        budget.setIssueLimit(dto.getIssueLimit() != null ? dto.getIssueLimit() : 0);
        budget.setIssuedCount(0);
        budget.setRedeemLimit(dto.getRedeemLimit() != null ? dto.getRedeemLimit() : 0);
        budget.setRedeemedCount(0);
        budget.setStatus(0);
        activityBudgetMapper.insert(budget);
    }

    @Override
    public ActivityBudgetProgressVO getBudgetProgress(Long activityId) {
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new BusinessException("活动不存在");
        }

        List<ActivityBudget> budgets = activityBudgetMapper.selectByActivityId(activityId);

        ActivityBudgetProgressVO progress = new ActivityBudgetProgressVO();
        progress.setActivityId(activityId);
        progress.setActivityName(activity.getActivityName());

        BigDecimal totalBudget = BigDecimal.ZERO;
        BigDecimal totalUsed = BigDecimal.ZERO;
        int totalIssueLimit = 0;
        int totalIssued = 0;
        int totalRedeemLimit = 0;
        int totalRedeemed = 0;
        List<ActivityBudgetVO> storeBudgetVOs = new ArrayList<>();

        for (ActivityBudget budget : budgets) {
            ActivityBudgetVO vo = convertToVO(budget);
            storeBudgetVOs.add(vo);

            totalBudget = totalBudget.add(budget.getTotalBudget() != null ? budget.getTotalBudget() : BigDecimal.ZERO);
            totalUsed = totalUsed.add(budget.getUsedBudget() != null ? budget.getUsedBudget() : BigDecimal.ZERO);
            totalIssueLimit += budget.getIssueLimit() != null ? budget.getIssueLimit() : 0;
            totalIssued += budget.getIssuedCount() != null ? budget.getIssuedCount() : 0;
            totalRedeemLimit += budget.getRedeemLimit() != null ? budget.getRedeemLimit() : 0;
            totalRedeemed += budget.getRedeemedCount() != null ? budget.getRedeemedCount() : 0;
        }

        progress.setTotalBudget(totalBudget);
        progress.setTotalUsedBudget(totalUsed);
        progress.setTotalRemainBudget(totalBudget.subtract(totalUsed));
        progress.setTotalUsageRate(calcRate(totalUsed, totalBudget));
        progress.setStoreBudgets(storeBudgetVOs);
        progress.setIssueLimit(totalIssueLimit);
        progress.setTotalIssued(totalIssued);
        progress.setRemainIssue(Math.max(0, totalIssueLimit - totalIssued));
        progress.setRedeemLimit(totalRedeemLimit);
        progress.setTotalRedeemed(totalRedeemed);
        progress.setRemainRedeem(Math.max(0, totalRedeemLimit - totalRedeemed));

        LambdaQueryWrapper<ActivityBudgetLog> logWrapper = new LambdaQueryWrapper<>();
        logWrapper.eq(ActivityBudgetLog::getActivityId, activityId)
                .orderByDesc(ActivityBudgetLog::getCreateTime)
                .last("LIMIT 20");
        List<ActivityBudgetLog> recentLogs = activityBudgetLogMapper.selectList(logWrapper);
        progress.setRecentLogs(recentLogs.stream().map(this::convertToLogItem).collect(Collectors.toList()));

        return progress;
    }

    @Override
    public IPage<ActivityBudgetLog> getBudgetLogs(ActivityBudgetQueryDTO dto) {
        Page<ActivityBudgetLog> page = new Page<>(dto.getPageNum(), dto.getPageSize());
        return activityBudgetLogMapper.selectByActivityId(dto.getActivityId(), page);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean tryOccupyBudget(Long activityId, String storeCode, BigDecimal amount, Integer quantity, String orderNo, Long couponInstanceId) {
        String store = StringUtils.hasText(storeCode) ? storeCode : null;
        ActivityBudget budget = activityBudgetMapper.selectByActivityAndStore(activityId, store);
        if (budget == null) {
            log.warn("活动预算不存在, activityId={}, storeCode={}", activityId, storeCode);
            return false;
        }

        BigDecimal currentUsed = budget.getUsedBudget() != null ? budget.getUsedBudget() : BigDecimal.ZERO;
        BigDecimal currentTotal = budget.getTotalBudget() != null ? budget.getTotalBudget() : BigDecimal.ZERO;
        if (currentUsed.add(amount).compareTo(currentTotal) > 0) {
            log.warn("预算超限, activityId={}, storeCode={}, used={}, amount={}, total={}",
                    activityId, storeCode, currentUsed, amount, currentTotal);
            return false;
        }

        int currentIssued = budget.getIssuedCount() != null ? budget.getIssuedCount() : 0;
        int currentIssueLimit = budget.getIssueLimit() != null ? budget.getIssueLimit() : 0;
        if (currentIssueLimit > 0 && currentIssued + quantity > currentIssueLimit) {
            log.warn("发券超限, activityId={}, storeCode={}, issued={}, quantity={}, limit={}",
                    activityId, storeCode, currentIssued, quantity, currentIssueLimit);
            return false;
        }

        LambdaUpdateWrapper<ActivityBudget> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(ActivityBudget::getId, budget.getId())
                .setSql("used_budget = used_budget + " + amount)
                .setSql("issued_count = issued_count + " + quantity);
        if (currentTotal.compareTo(BigDecimal.ZERO) > 0) {
            updateWrapper.apply("used_budget + {0} <= total_budget", amount);
        }
        int affected = activityBudgetMapper.update(null, updateWrapper);
        if (affected == 0) {
            log.warn("并发超限, activityId={}, storeCode={}", activityId, storeCode);
            return false;
        }

        ActivityBudgetLog budgetLog = new ActivityBudgetLog();
        budgetLog.setActivityId(activityId);
        budgetLog.setStoreCode(store);
        budgetLog.setChangeType(1);
        budgetLog.setChangeAmount(amount);
        budgetLog.setChangeQuantity(quantity);
        budgetLog.setBeforeBudget(currentUsed);
        budgetLog.setAfterBudget(currentUsed.add(amount));
        budgetLog.setBeforeIssued(currentIssued);
        budgetLog.setAfterIssued(currentIssued + quantity);
        budgetLog.setBeforeRedeemed(budget.getRedeemedCount() != null ? budget.getRedeemedCount() : 0);
        budgetLog.setAfterRedeemed(budget.getRedeemedCount() != null ? budget.getRedeemedCount() : 0);
        budgetLog.setOrderNo(orderNo);
        budgetLog.setCouponInstanceId(couponInstanceId);
        activityBudgetLogMapper.insert(budgetLog);

        BigDecimal newUsed = currentUsed.add(amount);
        if (newUsed.compareTo(currentTotal) >= 0 && budget.getStatus() != null && budget.getStatus() == 0) {
            LambdaUpdateWrapper<ActivityBudget> statusWrapper = new LambdaUpdateWrapper<>();
            statusWrapper.eq(ActivityBudget::getId, budget.getId())
                    .set(ActivityBudget::getStatus, 1);
            activityBudgetMapper.update(null, statusWrapper);
        }

        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmBudget(Long activityId, String storeCode, BigDecimal amount, String orderNo) {
        String store = StringUtils.hasText(storeCode) ? storeCode : null;
        ActivityBudget budget = activityBudgetMapper.selectByActivityAndStore(activityId, store);
        if (budget == null) {
            throw new BusinessException("活动预算不存在");
        }

        int currentRedeemed = budget.getRedeemedCount() != null ? budget.getRedeemedCount() : 0;
        LambdaUpdateWrapper<ActivityBudget> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(ActivityBudget::getId, budget.getId())
                .setSql("redeemed_count = redeemed_count + 1");
        activityBudgetMapper.update(null, updateWrapper);

        ActivityBudgetLog budgetLog = new ActivityBudgetLog();
        budgetLog.setActivityId(activityId);
        budgetLog.setStoreCode(store);
        budgetLog.setChangeType(3);
        budgetLog.setChangeAmount(amount);
        budgetLog.setChangeQuantity(1);
        budgetLog.setBeforeBudget(budget.getUsedBudget());
        budgetLog.setAfterBudget(budget.getUsedBudget());
        budgetLog.setBeforeIssued(budget.getIssuedCount() != null ? budget.getIssuedCount() : 0);
        budgetLog.setAfterIssued(budget.getIssuedCount() != null ? budget.getIssuedCount() : 0);
        budgetLog.setBeforeRedeemed(currentRedeemed);
        budgetLog.setAfterRedeemed(currentRedeemed + 1);
        budgetLog.setOrderNo(orderNo);
        activityBudgetLogMapper.insert(budgetLog);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void releaseBudget(Long activityId, String storeCode, BigDecimal amount, Integer quantity, String orderNo, Integer changeType) {
        String store = StringUtils.hasText(storeCode) ? storeCode : null;
        ActivityBudget budget = activityBudgetMapper.selectByActivityAndStore(activityId, store);
        if (budget == null) {
            throw new BusinessException("活动预算不存在");
        }

        BigDecimal currentUsed = budget.getUsedBudget() != null ? budget.getUsedBudget() : BigDecimal.ZERO;
        int currentIssued = budget.getIssuedCount() != null ? budget.getIssuedCount() : 0;

        LambdaUpdateWrapper<ActivityBudget> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(ActivityBudget::getId, budget.getId())
                .setSql("used_budget = used_budget - " + amount)
                .setSql("issued_count = issued_count - " + quantity);
        activityBudgetMapper.update(null, updateWrapper);

        ActivityBudgetLog budgetLog = new ActivityBudgetLog();
        budgetLog.setActivityId(activityId);
        budgetLog.setStoreCode(store);
        budgetLog.setChangeType(changeType);
        budgetLog.setChangeAmount(amount);
        budgetLog.setChangeQuantity(quantity);
        budgetLog.setBeforeBudget(currentUsed);
        budgetLog.setAfterBudget(currentUsed.subtract(amount));
        budgetLog.setBeforeIssued(currentIssued);
        budgetLog.setAfterIssued(currentIssued - quantity);
        budgetLog.setBeforeRedeemed(budget.getRedeemedCount() != null ? budget.getRedeemedCount() : 0);
        budgetLog.setAfterRedeemed(budget.getRedeemedCount() != null ? budget.getRedeemedCount() : 0);
        budgetLog.setOrderNo(orderNo);
        activityBudgetLogMapper.insert(budgetLog);

        if (budget.getStatus() != null && budget.getStatus() == 1) {
            BigDecimal newUsed = currentUsed.subtract(amount);
            BigDecimal total = budget.getTotalBudget() != null ? budget.getTotalBudget() : BigDecimal.ZERO;
            if (newUsed.compareTo(total) < 0) {
                LambdaUpdateWrapper<ActivityBudget> statusWrapper = new LambdaUpdateWrapper<>();
                statusWrapper.eq(ActivityBudget::getId, budget.getId())
                        .set(ActivityBudget::getStatus, 0);
                activityBudgetMapper.update(null, statusWrapper);
            }
        }
    }

    private ActivityBudgetVO convertToVO(ActivityBudget budget) {
        ActivityBudgetVO vo = new ActivityBudgetVO();
        vo.setId(budget.getId());
        vo.setActivityId(budget.getActivityId());
        vo.setStoreCode(budget.getStoreCode());
        vo.setStoreName(budget.getStoreName());
        vo.setBudgetType(budget.getBudgetType());
        vo.setBudgetTypeName(budget.getBudgetType() != null && budget.getBudgetType() == 1 ? "总预算" : "门店预算");
        vo.setTotalBudget(budget.getTotalBudget());
        vo.setUsedBudget(budget.getUsedBudget());

        BigDecimal total = budget.getTotalBudget() != null ? budget.getTotalBudget() : BigDecimal.ZERO;
        BigDecimal used = budget.getUsedBudget() != null ? budget.getUsedBudget() : BigDecimal.ZERO;
        vo.setRemainBudget(total.subtract(used));
        vo.setUsageRate(calcRate(used, total));

        int issueLimit = budget.getIssueLimit() != null ? budget.getIssueLimit() : 0;
        int issued = budget.getIssuedCount() != null ? budget.getIssuedCount() : 0;
        vo.setIssueLimit(issueLimit);
        vo.setIssuedCount(issued);
        vo.setRemainIssue(Math.max(0, issueLimit - issued));
        vo.setIssueRate(calcRate(new BigDecimal(issued), new BigDecimal(issueLimit)));

        int redeemLimit = budget.getRedeemLimit() != null ? budget.getRedeemLimit() : 0;
        int redeemed = budget.getRedeemedCount() != null ? budget.getRedeemedCount() : 0;
        vo.setRedeemLimit(redeemLimit);
        vo.setRedeemedCount(redeemed);
        vo.setRemainRedeem(Math.max(0, redeemLimit - redeemed));
        vo.setRedeemRate(calcRate(new BigDecimal(redeemed), new BigDecimal(redeemLimit)));

        vo.setStatus(budget.getStatus());
        vo.setStatusName(budget.getStatus() != null && budget.getStatus() == 1 ? "超限" : "正常");

        LambdaQueryWrapper<ActivityBudgetLog> logWrapper = new LambdaQueryWrapper<>();
        logWrapper.eq(ActivityBudgetLog::getActivityId, budget.getActivityId())
                .eq(ActivityBudgetLog::getStoreCode, budget.getStoreCode() != null ? budget.getStoreCode() : "")
                .orderByDesc(ActivityBudgetLog::getCreateTime)
                .last("LIMIT 5");
        List<ActivityBudgetLog> recentLogs = activityBudgetLogMapper.selectList(logWrapper);
        vo.setRecentLogs(recentLogs.stream().map(this::convertToLogItem).collect(Collectors.toList()));

        return vo;
    }

    private ActivityBudgetVO.BudgetLogItem convertToLogItem(ActivityBudgetLog logEntry) {
        ActivityBudgetVO.BudgetLogItem item = new ActivityBudgetVO.BudgetLogItem();
        item.setChangeType(logEntry.getChangeType());
        item.setChangeTypeName(getChangeTypeName(logEntry.getChangeType()));
        item.setChangeAmount(logEntry.getChangeAmount());
        item.setChangeQuantity(logEntry.getChangeQuantity());
        item.setOrderNo(logEntry.getOrderNo());
        item.setCreateTime(logEntry.getCreateTime());
        return item;
    }

    private String getChangeTypeName(Integer changeType) {
        if (changeType == null) return "";
        return switch (changeType) {
            case 1 -> "领取占用";
            case 2 -> "锁定占用";
            case 3 -> "核销确认";
            case 4 -> "退款释放";
            case 5 -> "超时释放";
            default -> "未知";
        };
    }

    private BigDecimal calcRate(BigDecimal numerator, BigDecimal denominator) {
        if (denominator == null || denominator.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return numerator.multiply(HUNDRED).divide(denominator, 2, RoundingMode.HALF_UP);
    }
}
