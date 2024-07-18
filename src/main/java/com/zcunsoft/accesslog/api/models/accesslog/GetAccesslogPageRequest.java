package com.zcunsoft.accesslog.api.models.accesslog;



import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;


@Schema(description = "日志分页")
@Data
public class GetAccesslogPageRequest  {

    @Schema(description = "页码", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private int pageNum;

    @Schema(description = "页长", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    private int pageSize;

    @Schema(description = "应用编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "cnb-mgt")
    private String applicationCode;
	
    @Schema(description = "HttpHost", requiredMode = Schema.RequiredMode.REQUIRED, example = "")
    private String httpHost;
    
    @Schema(description = "开始时间", requiredMode = Schema.RequiredMode.REQUIRED, example = "2023-08-01")
    private String startTime;
    @Schema(description = "结束时间", requiredMode = Schema.RequiredMode.REQUIRED, example = "2023-11-01")
    private String endTime;
    
}
