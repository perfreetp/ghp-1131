package com.smartretail.mbc.order.vo;

import com.smartretail.mbc.common.vo.RiskCheckResultVO;
import com.smartretail.mbc.member.vo.MemberSimpleVO;
import com.smartretail.mbc.member.vo.StoreVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Schema(description = "收银端订单试算结果")
public class PosValidateResultVO {

    @Schema(description = "整体是否可结算")
    private Boolean valid;

    @Schema(description = "会员信息")
    private MemberSimpleVO memberInfo;

    @Schema(description = "门店信息")
    private StoreVO storeInfo;

    @Schema(description = "每个商品是否可用券")
    private List<ItemResultVO> itemResults;

    @Schema(description = "被排除商品数")
    private Integer excludedItemCount;

    @Schema(description = "可参与优惠的商品总金额")
    private BigDecimal couponableAmount;

    @Schema(description = "每张券的试用结果")
    private List<CouponTrialVO> couponTrials;

    @Schema(description = "推荐使用的券ID列表")
    private List<Long> bestCouponCombination;

    @Schema(description = "推荐组合可省金额")
    private BigDecimal bestCouponAmount;

    @Schema(description = "最终使用的券ID")
    private List<Long> finalCouponIds;

    @Schema(description = "最终券抵扣")
    private BigDecimal finalCouponAmount;

    @Schema(description = "用户当前积分")
    private Integer currentPoints;

    @Schema(description = "本次最多可用积分")
    private Integer maxUsablePoints;

    @Schema(description = "积分最多可抵扣金额")
    private BigDecimal maxUsablePointAmount;

    @Schema(description = "最终使用积分")
    private Integer finalUsedPoints;

    @Schema(description = "最终积分抵扣")
    private BigDecimal finalPointAmount;

    @Schema(description = "等级编码")
    private Integer levelCode;

    @Schema(description = "等级名称")
    private String levelName;

    @Schema(description = "折扣率")
    private BigDecimal discountRate;

    @Schema(description = "等级折扣省金额")
    private BigDecimal levelDiscount;

    @Schema(description = "商品总额")
    private BigDecimal originalAmount;

    @Schema(description = "总优惠")
    private BigDecimal totalDiscount;

    @Schema(description = "最终应付")
    private BigDecimal finalPayAmount;

    @Schema(description = "本单可得积分")
    private Integer earnablePoints;

    @Schema(description = "本单可得成长值")
    private Integer earnableGrowth;

    @Schema(description = "警告信息，如券过期等")
    private List<String> warnings;

    @Schema(description = "致命错误，无法结算")
    private List<String> errors;

    @Schema(description = "风控检查结果")
    private RiskCheckResultVO riskCheck;

    @Data
    @Schema(description = "商品试算结果")
    public static class ItemResultVO {

        @Schema(description = "SKU ID")
        private String skuId;

        @Schema(description = "SKU名称")
        private String skuName;

        @Schema(description = "小计金额")
        private BigDecimal subtotal;

        @Schema(description = "是否被排除")
        private Boolean excluded;

        @Schema(description = "排除原因")
        private String excludeReason;
    }

    @Data
    @Schema(description = "券试用结果")
    public static class CouponTrialVO {

        @Schema(description = "券实例ID")
        private Long instanceId;

        @Schema(description = "券模板ID")
        private Long templateId;

        @Schema(description = "券名称")
        private String couponName;

        @Schema(description = "券类型")
        private Integer couponType;

        @Schema(description = "减免金额")
        private BigDecimal reduceAmount;

        @Schema(description = "是否可用")
        private Boolean available;

        @Schema(description = "可用原因或不可用原因")
        private String reason;

        @Schema(description = "门店是否可用")
        private Boolean storeAvailable;

        @Schema(description = "业态是否可用")
        private Boolean businessAvailable;

        @Schema(description = "设备类型是否可用")
        private Boolean posAvailable;

        @Schema(description = "不可用原因详情")
        private String unavailableReason;

        @Schema(description = "实际可应用金额")
        private BigDecimal applicableAmount;

        @Schema(description = "这张券实际可省多少")
        private BigDecimal savedAmount;

        @Schema(description = "可应用的商品SKU列表")
        private List<String> applicableItemSkus;
    }
}
