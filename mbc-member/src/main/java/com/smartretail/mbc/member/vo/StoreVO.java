package com.smartretail.mbc.member.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "门店信息VO")
public class StoreVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "门店编码")
    private String storeCode;

    @Schema(description = "门店名称")
    private String storeName;

    @Schema(description = "业态类型编码")
    private Integer storeType;

    @Schema(description = "业态名称")
    private String storeTypeName;

    @Schema(description = "门店级别")
    private Integer storeLevel;

    @Schema(description = "地址")
    private String address;

    @Schema(description = "城市")
    private String city;

    @Schema(description = "省份")
    private String province;

    @Schema(description = "联系人")
    private String contact;

    @Schema(description = "联系电话")
    private String phone;

    @Schema(description = "状态：1启用 0停用")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
