package com.zcunsoft.accesslog.api.models.accesslog;

import java.util.List;
import com.zcunsoft.accesslog.api.models.BaseSort;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "日志分页")
@Data
public class GetAccesslogIpPageRequest  extends BaseSort {
	@Schema(description = "属性", requiredMode = Schema.RequiredMode.REQUIRED, example = "ip/pv/country/province/city")
	private String sortName;

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

    @Schema(description = "国家或地区", requiredMode = Schema.RequiredMode.REQUIRED, example = "[\"中国\"]")
    private List<String> country;

    @Schema(description = "省份", requiredMode = Schema.RequiredMode.REQUIRED, example = "[\"上海\",\"北京\",\"其他\"]")
    private List<String> province;

    @Schema(description = "城市", requiredMode = Schema.RequiredMode.REQUIRED, example = "[\"上海\",\"北京\",\"其他\"]")
    private List<String> city;

    @Schema(description = "IP地址", requiredMode = Schema.RequiredMode.REQUIRED, example = "192.168.100.192")
    private String ip;
}
