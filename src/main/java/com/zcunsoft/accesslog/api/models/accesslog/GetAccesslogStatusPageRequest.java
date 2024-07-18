package com.zcunsoft.accesslog.api.models.accesslog;


import java.util.ArrayList;
import java.util.List;

import com.zcunsoft.accesslog.api.models.BaseSort;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;


@Schema(description = "日志分页")
@Data
public class GetAccesslogStatusPageRequest  extends BaseSort {

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
    
    @Schema(description = "状态码", requiredMode = Schema.RequiredMode.REQUIRED, example = "400,500")
    private String status;
    
    @Schema(description = "超过1秒", requiredMode = Schema.RequiredMode.REQUIRED, example = "true/false")
    private Boolean isOverOneSecond;
    
    @Schema(description = "后缀限制", requiredMode = Schema.RequiredMode.REQUIRED, example = "[\".css\",\".js\"]")
    private List<String> limits = new ArrayList<>();
}
