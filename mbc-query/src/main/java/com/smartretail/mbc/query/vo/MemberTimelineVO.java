package com.smartretail.mbc.query.vo;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "会员时间线VO")
public class MemberTimelineVO {

    @Schema(description = "会员ID")
    private Long memberId;

    @Schema(description = "会员名称")
    private String memberName;

    @Schema(description = "总记录数")
    private Long totalCount;

    @Schema(description = "分页事件列表")
    private IPage<TimelineEventVO> events;
}
