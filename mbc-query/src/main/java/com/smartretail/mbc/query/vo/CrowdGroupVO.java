package com.smartretail.mbc.query.vo;

import com.smartretail.mbc.query.dto.CrowdGroupCreateDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "人群组详情VO")
public class CrowdGroupVO {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "人群编码")
    private String crowdCode;

    @Schema(description = "人群名称")
    private String crowdName;

    @Schema(description = "人群类型：1静态 2动态")
    private Integer crowdType;

    @Schema(description = "人群类型名称")
    private String crowdTypeName;

    @Schema(description = "圈选规则JSON")
    private String ruleConfig;

    @Schema(description = "圈选规则列表")
    private List<CrowdGroupCreateDTO.CrowdRuleItem> ruleList;

    @Schema(description = "预估人数")
    private Integer estimatedCount;

    @Schema(description = "实际人数")
    private Integer actualCount;

    @Schema(description = "预览成员数量")
    private Integer previewMemberCount;

    @Schema(description = "状态：0草稿 1已生效 2已失效")
    private Integer status;

    @Schema(description = "状态名称")
    private String statusName;

    @Schema(description = "最近一次计算时间")
    private LocalDateTime refreshTime;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
