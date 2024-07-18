package com.zcunsoft.accesslog.api.models.accesslog;



import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "应用流量信息")
@Data
public class GetAccesslogRequestimeGt100msResponseData {
	
	List<GetAccesslogRequestimeGt100msData> rows;
	
	List<String> times;    
}
