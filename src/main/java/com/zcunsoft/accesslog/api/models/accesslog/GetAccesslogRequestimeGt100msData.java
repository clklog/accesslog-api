package com.zcunsoft.accesslog.api.models.accesslog;



import java.math.BigDecimal;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "耗时超过100毫秒")
@Data
public class GetAccesslogRequestimeGt100msData {
	
	private String uri;
	
	private List<BigDecimal> pv;
}
