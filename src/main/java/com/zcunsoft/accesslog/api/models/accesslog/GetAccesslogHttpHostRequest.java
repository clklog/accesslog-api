package com.zcunsoft.accesslog.api.models.accesslog;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;


@Schema(description = "获取HOST请求")
@Data
public class GetAccesslogHttpHostRequest {

    @Schema(description = "应用编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "cnb-mgt")
    private String applicationCode;
    
}
