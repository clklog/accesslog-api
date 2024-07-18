package com.zcunsoft.accesslog.api.models.accesslog;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;


@Schema(description = "日志分析请求")
@Data
public class BaseAccesslogRequest {

	@Schema(description = "应用编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "cnb-mgt")
    private String applicationCode;
	
    @Schema(description = "HttpHost", requiredMode = Schema.RequiredMode.REQUIRED, example = "")
    private String httpHost;
    
    @Schema(description = "开始时间", requiredMode = Schema.RequiredMode.REQUIRED, example = "2023-06-08")
    private String startTime;
    @Schema(description = "结束时间", requiredMode = Schema.RequiredMode.REQUIRED, example = "2023-11-10")
    private String endTime;
    
}
