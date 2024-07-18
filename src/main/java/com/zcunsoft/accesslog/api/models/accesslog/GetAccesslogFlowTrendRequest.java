package com.zcunsoft.accesslog.api.models.accesslog;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;


@Schema(description = "日志分析请求")
@Data
public class GetAccesslogFlowTrendRequest {

	@Schema(description = "应用编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "cnb-mgt")
    private String applicationCode;
	
    @Schema(description = "HttpHost", requiredMode = Schema.RequiredMode.REQUIRED, example = "cnb-sms.alltobid.com")
    private String httpHost;
    
    @Schema(description = "开始时间", requiredMode = Schema.RequiredMode.REQUIRED, example = "2023-06-08")
    private String startTime;
    @Schema(description = "结束时间", requiredMode = Schema.RequiredMode.REQUIRED, example = "2023-11-10")
    private String endTime;
    
    @Schema(description = "时间类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "hour/day/week/month")
    private String timeType;
}
