package com.zcunsoft.accesslog.api.models.accesslog;

import com.zcunsoft.accesslog.api.models.BaseSort;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 作者：yechangzhong
 * 创建日期：2024/4/23 9:41
 * 描述：获取IP详情分页请求
 */
@Schema(description = "IP详情分页")
@Data
public class GetAccesslogIpDetailPageRequest extends BaseSort {
    @Schema(description = "页码", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private int pageNum;

    @Schema(description = "页长", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    private int pageSize;

    @Schema(description = "应用编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "cnb-mgt")
    private String applicationCode;

    @Schema(description = "HttpHost", requiredMode = Schema.RequiredMode.REQUIRED, example = "")
    private String httpHost;

    @Schema(description = "开始时间", requiredMode = Schema.RequiredMode.REQUIRED, example = "2024-01-01")
    private String startTime;

    @Schema(description = "结束时间", requiredMode = Schema.RequiredMode.REQUIRED, example = "2024-01-07")
    private String endTime;

    @Schema(description = "IP地址", requiredMode = Schema.RequiredMode.REQUIRED, example = "192.168.100.192")
    private String ip;

    @Schema(description = "状态码", requiredMode = Schema.RequiredMode.REQUIRED, example = "400,500")
    private String status;

    @Schema(description = "超过1秒", requiredMode = Schema.RequiredMode.REQUIRED, example = "true/false")
    private Boolean isOverOneSecond;

    @Schema(description = "后缀限制", requiredMode = Schema.RequiredMode.REQUIRED, example = "[\".css\",\".js\"]")
    private List<String> limits = new ArrayList<>();
}
