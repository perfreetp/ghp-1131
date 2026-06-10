package com.smartretail.mbc.member.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.smartretail.mbc.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName("t_store_info")
public class StoreInfo extends BaseEntity {

    private String storeCode;

    private String storeName;

    private Integer storeType;

    private Integer storeLevel;

    private String address;

    private String city;

    private String province;

    private String contact;

    private String phone;

    private Integer status;
}
