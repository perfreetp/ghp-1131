package com.smartretail.mbc.query.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartretail.mbc.benefit.entity.BenefitUseLog;
import com.smartretail.mbc.benefit.mapper.BenefitUseLogMapper;
import com.smartretail.mbc.common.enums.ReconcileStatusEnum;
import com.smartretail.mbc.order.entity.ConsumeOrder;
import com.smartretail.mbc.order.mapper.ConsumeOrderMapper;
import com.smartretail.mbc.query.dto.ReconcileDetailQueryDTO;
import com.smartretail.mbc.query.dto.ReconcileQueryDTO;
import com.smartretail.mbc.query.entity.ReconcileRecord;
import com.smartretail.mbc.query.mapper.ReconcileRecordMapper;
import com.smartretail.mbc.query.service.ReconcileService;
import com.smartretail.mbc.query.vo.ReconcileDetailVO;
import com.smartretail.mbc.query.vo.ReconcileResultVO;
import com.smartretail.mbc.query.vo.ReconcileSummaryVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReconcileServiceImpl implements ReconcileService {

    private final ReconcileRecordMapper reconcileRecordMapper;
    private final BenefitUseLogMapper benefitUseLogMapper;
    private final ConsumeOrderMapper consumeOrderMapper;

    @Override
    public ReconcileResultVO getReconcileSummary(ReconcileQueryDTO dto) {
        ReconcileResultVO result = new ReconcileResultVO();

        List<Map<String, Object>> summaryData;
        String groupBy = dto.getGroupBy();
        if ("pos".equals(groupBy)) {
            summaryData = reconcileRecordMapper.summaryByPos(
                    dto.getStartDate(), dto.getEndDate(), dto.getStoreCode(), dto.getPosCode(), dto.getReconcileStatus());
        } else if ("template".equals(groupBy)) {
            summaryData = reconcileRecordMapper.summaryByTemplate(
                    dto.getStartDate(), dto.getEndDate(), dto.getStoreCode(), dto.getPosCode(), dto.getReconcileStatus());
        } else if ("date".equals(groupBy)) {
            summaryData = reconcileRecordMapper.summaryByDate(
                    dto.getStartDate(), dto.getEndDate(), dto.getStoreCode(), dto.getPosCode(), dto.getReconcileStatus());
        } else {
            summaryData = reconcileRecordMapper.summaryByStore(
                    dto.getStartDate(), dto.getEndDate(), dto.getStoreCode(), dto.getPosCode(), dto.getReconcileStatus());
        }

        List<ReconcileSummaryVO> summaryList = summaryData.stream().map(row -> {
            ReconcileSummaryVO vo = new ReconcileSummaryVO();
            vo.setGroupKey(getStringValue(row, "groupKey"));
            vo.setGroupLabel(getStringValue(row, "groupLabel"));
            vo.setTotalConfirmCount(getIntValue(row, "totalConfirmCount"));
            vo.setTotalConfirmAmount(getBigDecimalValue(row, "totalConfirmAmount"));
            vo.setTotalReturnCount(getIntValue(row, "totalReturnCount"));
            vo.setTotalReturnAmount(getBigDecimalValue(row, "totalReturnAmount"));
            vo.setTotalLockCount(getIntValue(row, "totalLockCount"));
            vo.setTotalRetryCount(getIntValue(row, "totalRetryCount"));
            vo.setMatchedCount(getIntValue(row, "matchedCount"));
            vo.setUnmatchedCount(getIntValue(row, "unmatchedCount"));
            int total = vo.getMatchedCount() + vo.getUnmatchedCount();
            if (total > 0) {
                vo.setUnmatchedRate(BigDecimal.valueOf(vo.getUnmatchedCount())
                        .divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100)));
            } else {
                vo.setUnmatchedRate(BigDecimal.ZERO);
            }
            return vo;
        }).collect(Collectors.toList());

        result.setSummaryList(summaryList);

        ReconcileDetailQueryDTO detailQuery = new ReconcileDetailQueryDTO();
        detailQuery.setStoreCode(dto.getStoreCode());
        detailQuery.setPosCode(dto.getPosCode());
        detailQuery.setTemplateId(dto.getTemplateId());
        detailQuery.setReconcileStatus(dto.getReconcileStatus());
        detailQuery.setStartDate(dto.getStartDate());
        detailQuery.setEndDate(dto.getEndDate());
        detailQuery.setPageNum(dto.getPageNum());
        detailQuery.setPageSize(dto.getPageSize());
        result.setDetails(getReconcileDetail(detailQuery));

        return result;
    }

    @Override
    public IPage<ReconcileDetailVO> getReconcileDetail(ReconcileDetailQueryDTO dto) {
        LambdaQueryWrapper<ReconcileRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.ge(ReconcileRecord::getReconcileDate, dto.getStartDate());
        wrapper.le(ReconcileRecord::getReconcileDate, dto.getEndDate());
        if (dto.getReconcileStatus() != null) {
            wrapper.eq(ReconcileRecord::getReconcileStatus, dto.getReconcileStatus());
        }
        wrapper.orderByDesc(ReconcileRecord::getReconcileTime);

        Page<ReconcileRecord> page = new Page<>(dto.getPageNum(), dto.getPageSize());
        IPage<ReconcileRecord> recordPage = reconcileRecordMapper.selectPage(page, wrapper);

        IPage<ReconcileDetailVO> resultPage = recordPage.convert(this::toDetailVO);

        List<ReconcileDetailVO> records = resultPage.getRecords();
        if (!records.isEmpty()) {
            List<String> useNos = records.stream().map(ReconcileDetailVO::getUseNo).collect(Collectors.toList());
            LambdaQueryWrapper<BenefitUseLog> bulWrapper = new LambdaQueryWrapper<>();
            bulWrapper.in(BenefitUseLog::getUseNo, useNos);
            List<BenefitUseLog> useLogs = benefitUseLogMapper.selectList(bulWrapper);
            Map<String, BenefitUseLog> useLogMap = useLogs.stream()
                    .collect(Collectors.toMap(BenefitUseLog::getUseNo, b -> b, (a, b) -> a));

            for (ReconcileDetailVO vo : records) {
                BenefitUseLog useLog = useLogMap.get(vo.getUseNo());
                if (useLog != null) {
                    enrichFromUseLog(vo, useLog);
                    if (dto.getStoreCode() != null && !dto.getStoreCode().equals(useLog.getStoreCode())) {
                        vo.setId(null);
                    }
                    if (dto.getPosCode() != null && !dto.getPosCode().equals(useLog.getPosCode())) {
                        vo.setId(null);
                    }
                    if (dto.getOrderNo() != null && !dto.getOrderNo().equals(useLog.getOrderNo())) {
                        vo.setId(null);
                    }
                }
            }
            records.removeIf(vo -> vo.getId() == null);
        }

        return resultPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void executeReconcile(LocalDate date) {
        log.info("开始执行对账, 日期: {}", date);

        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();

        LambdaQueryWrapper<BenefitUseLog> bulWrapper = new LambdaQueryWrapper<>();
        bulWrapper.ge(BenefitUseLog::getCreateTime, dayStart);
        bulWrapper.lt(BenefitUseLog::getCreateTime, dayEnd);
        List<BenefitUseLog> useLogs = benefitUseLogMapper.selectList(bulWrapper);

        List<String> orderNos = useLogs.stream()
                .map(BenefitUseLog::getOrderNo)
                .filter(no -> no != null && !no.isEmpty())
                .distinct()
                .collect(Collectors.toList());

        Map<String, ConsumeOrder> orderMap;
        if (!orderNos.isEmpty()) {
            LambdaQueryWrapper<ConsumeOrder> orderWrapper = new LambdaQueryWrapper<>();
            orderWrapper.in(ConsumeOrder::getOrderNo, orderNos);
            List<ConsumeOrder> orders = consumeOrderMapper.selectList(orderWrapper);
            orderMap = orders.stream()
                    .collect(Collectors.toMap(ConsumeOrder::getOrderNo, o -> o, (a, b) -> a));
        } else {
            orderMap = Map.of();
        }

        Map<String, List<BenefitUseLog>> duplicateMap = useLogs.stream()
                .filter(b -> b.getOrderNo() != null && b.getUseStatus() == 2)
                .collect(Collectors.groupingBy(b -> b.getOrderNo() + "_" + b.getBenefitType()));

        LocalDateTime now = LocalDateTime.now();
        List<ReconcileRecord> records = new ArrayList<>();

        for (BenefitUseLog useLog : useLogs) {
            ReconcileRecord record = new ReconcileRecord();
            record.setUseNo(useLog.getUseNo());
            record.setOrderNo(useLog.getOrderNo());
            record.setBenefitType(useLog.getBenefitType());
            record.setBenefitValue(useLog.getBenefitValue());
            record.setReconcileDate(date);
            record.setReconcileTime(now);

            ReconcileStatusEnum status = doReconcile(useLog, orderMap, duplicateMap);
            record.setReconcileStatus(status.getCode());
            record.setReconcileDiff(status.getDesc());

            if (useLog.getOrderNo() != null) {
                ConsumeOrder order = orderMap.get(useLog.getOrderNo());
                if (order != null) {
                    record.setPosPayAmount(order.getPayAmount());
                    record.setPosPayTime(order.getPayTime());
                }
            }

            records.add(record);
        }

        if (!records.isEmpty()) {
            LambdaQueryWrapper<ReconcileRecord> deleteWrapper = new LambdaQueryWrapper<>();
            deleteWrapper.eq(ReconcileRecord::getReconcileDate, date);
            reconcileRecordMapper.delete(deleteWrapper);

            for (ReconcileRecord record : records) {
                reconcileRecordMapper.insert(record);
            }
        }

        log.info("对账完成, 日期: {}, 处理记录数: {}", date, records.size());
    }

    private ReconcileStatusEnum doReconcile(BenefitUseLog useLog,
                                             Map<String, ConsumeOrder> orderMap,
                                             Map<String, List<BenefitUseLog>> duplicateMap) {
        String orderNo = useLog.getOrderNo();

        if (orderNo == null || orderNo.isEmpty()) {
            return ReconcileStatusEnum.MATCHED;
        }

        ConsumeOrder order = orderMap.get(orderNo);
        if (order == null) {
            return ReconcileStatusEnum.UNMATCHED_MISSING;
        }

        String dupKey = orderNo + "_" + useLog.getBenefitType();
        List<BenefitUseLog> sameKeyList = duplicateMap.get(dupKey);
        if (sameKeyList != null && sameKeyList.size() > 1 && useLog.getUseStatus() == 2) {
            return ReconcileStatusEnum.UNMATCHED_DUPLICATE;
        }

        BigDecimal orderBenefitAmount = order.getCouponAmount() != null ? order.getCouponAmount() : BigDecimal.ZERO;
        if (order.getPointAmount() != null) {
            orderBenefitAmount = orderBenefitAmount.add(order.getPointAmount());
        }
        if (order.getLevelDiscount() != null) {
            orderBenefitAmount = orderBenefitAmount.add(order.getLevelDiscount());
        }
        if (useLog.getBenefitValue() != null
                && orderBenefitAmount.compareTo(BigDecimal.ZERO) > 0
                && useLog.getBenefitValue().compareTo(orderBenefitAmount) != 0) {
            return ReconcileStatusEnum.UNMATCHED_AMOUNT;
        }

        if (order.getOrderStatus() != null && order.getOrderStatus() == 3) {
            if (useLog.getUseStatus() != 3) {
                return ReconcileStatusEnum.UNMATCHED_REFUND_MISMATCH;
            }
            if (order.getRefundAmount() != null && useLog.getBenefitValue() != null
                    && order.getRefundAmount().compareTo(useLog.getBenefitValue()) < 0) {
                return ReconcileStatusEnum.UNMATCHED_REFUND_MISMATCH;
            }
        }

        return ReconcileStatusEnum.MATCHED;
    }

    private ReconcileDetailVO toDetailVO(ReconcileRecord record) {
        ReconcileDetailVO vo = new ReconcileDetailVO();
        vo.setId(record.getId());
        vo.setUseNo(record.getUseNo());
        vo.setOrderNo(record.getOrderNo());
        vo.setBenefitType(record.getBenefitType());
        vo.setBenefitValue(record.getBenefitValue());
        vo.setPosPayAmount(record.getPosPayAmount());
        vo.setPosPayTime(record.getPosPayTime());
        vo.setReconcileStatus(record.getReconcileStatus());
        vo.setReconcileDiff(record.getReconcileDiff());
        vo.setReconcileTime(record.getReconcileTime());

        ReconcileStatusEnum statusEnum = ReconcileStatusEnum.getByCode(record.getReconcileStatus());
        vo.setReconcileStatusName(statusEnum != null ? statusEnum.getName() : "");

        return vo;
    }

    private void enrichFromUseLog(ReconcileDetailVO vo, BenefitUseLog useLog) {
        vo.setMemberId(useLog.getMemberId());
        vo.setUsedPoints(useLog.getUsedPoints());
        vo.setUseStatus(useLog.getUseStatus());
        vo.setUseStatusName(getUseStatusName(useLog.getUseStatus()));
        vo.setStoreCode(useLog.getStoreCode());
        vo.setPosCode(useLog.getPosCode());
        vo.setLockTime(useLog.getLockTime());
        vo.setConfirmTime(useLog.getConfirmTime());
        vo.setReturnTime(useLog.getReturnTime());
    }

    private String getUseStatusName(Integer useStatus) {
        if (useStatus == null) {
            return "";
        }
        return switch (useStatus) {
            case 1 -> "锁定";
            case 2 -> "确认";
            case 3 -> "返还";
            default -> "";
        };
    }

    private String getStringValue(Map<String, Object> row, String key) {
        Object val = row.get(key);
        return val != null ? val.toString() : "";
    }

    private Integer getIntValue(Map<String, Object> row, String key) {
        Object val = row.get(key);
        if (val == null) {
            return 0;
        }
        if (val instanceof Number) {
            return ((Number) val).intValue();
        }
        return 0;
    }

    private BigDecimal getBigDecimalValue(Map<String, Object> row, String key) {
        Object val = row.get(key);
        if (val == null) {
            return BigDecimal.ZERO;
        }
        if (val instanceof BigDecimal) {
            return (BigDecimal) val;
        }
        if (val instanceof Number) {
            return BigDecimal.valueOf(((Number) val).doubleValue());
        }
        return BigDecimal.ZERO;
    }
}
