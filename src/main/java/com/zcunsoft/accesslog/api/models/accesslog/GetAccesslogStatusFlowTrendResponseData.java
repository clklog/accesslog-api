package com.zcunsoft.accesslog.api.models.accesslog;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "状态码流量信息")
@Data
public class GetAccesslogStatusFlowTrendResponseData  {

	String httpHost;
	
	List<AccesslogFlowDetail> rows;

}
