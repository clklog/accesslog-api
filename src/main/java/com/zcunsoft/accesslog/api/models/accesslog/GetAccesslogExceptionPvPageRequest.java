package com.zcunsoft.accesslog.api.models.accesslog;



import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;


@Schema(description = "日志分页")
@Data
public class GetAccesslogExceptionPvPageRequest  extends GetAccesslogPageRequest{

    @Schema(description = "异常阀值(%)", requiredMode = Schema.RequiredMode.REQUIRED, example = "30")
    private BigDecimal exceptionOffset;
    
}
