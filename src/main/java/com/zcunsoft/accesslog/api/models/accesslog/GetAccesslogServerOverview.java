package com.zcunsoft.accesslog.api.models.accesslog;



import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "应用流量信息")
@Data
public class GetAccesslogServerOverview {
	
	@Schema(description = "应用统计")
	AccesslogFlowDetail totalAccesslogFlowDetail;
	
	@Schema(description = "HOST统计")
	List<AccesslogFlowDetail> accesslogFlowDetailList;    
}
