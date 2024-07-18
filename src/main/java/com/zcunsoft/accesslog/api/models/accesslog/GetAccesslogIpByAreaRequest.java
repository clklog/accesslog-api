package com.zcunsoft.accesslog.api.models.accesslog;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "日志分析请求")
@Data
public class GetAccesslogIpByAreaRequest {
	@Schema(description = "应用编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "cnb-mgt")
    private String applicationCode;

    @Schema(description = "HttpHost", requiredMode = Schema.RequiredMode.REQUIRED, example = "")
    private String httpHost;

    @Schema(description = "开始时间", requiredMode = Schema.RequiredMode.REQUIRED, example = "2023-06-08")
    private String startTime;

    @Schema(description = "结束时间", requiredMode = Schema.RequiredMode.REQUIRED, example = "2023-11-10")
    private String endTime;

    @Schema(description = "统计选项", requiredMode = Schema.RequiredMode.REQUIRED, example = "county/province/city")
    private String summaryOptions;

    @Schema(description = "指标", requiredMode = Schema.RequiredMode.REQUIRED, example = "pv/uv/ipCount")
    private String indicator;

    @Schema(description = "国家", requiredMode = Schema.RequiredMode.REQUIRED, example = "中国/美国/英国")
    private String country;
}
