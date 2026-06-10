package com.smartretail.mbc.query.service;

import com.smartretail.mbc.query.dto.MemberTimelineQueryDTO;
import com.smartretail.mbc.query.vo.MemberTimelineVO;

public interface MemberTimelineService {

    MemberTimelineVO getMemberTimeline(MemberTimelineQueryDTO dto);
}
