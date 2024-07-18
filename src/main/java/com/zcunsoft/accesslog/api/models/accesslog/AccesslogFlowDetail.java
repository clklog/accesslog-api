package com.zcunsoft.accesslog.api.models.accesslog;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "应用流量信息")
@Data
public class AccesslogFlowDetail {
	@JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "统计时间")
    private String statTime;

	@Schema(description = "国家")
    private String country;

	@Schema(description = "省份")
    private String province;

	@Schema(description = "城市")
    private String city;

    @Schema(description = "HOST")
    private String httpHost;

    @Schema(description = "HttpUserAgent")
    private String httpUserAgent;

    @Schema(description = "URL")
    private String uri;

    @Schema(description = "访问量(PV)")
    private BigDecimal pv;

    @Schema(description = "超过100毫秒次数")
    private BigDecimal slowPv;

    @Schema(description = "流出流量")
    private BigDecimal bodySentBytes;

    @Schema(description = "用户数(UV)")
    private BigDecimal uv;

//    @Schema(description = "访问时长")
//    private BigDecimal visitTime;

    @Schema(description = "最大访问时长")
    private BigDecimal maxVisitTime;

    @Schema(description = "平均访问时长")
    private BigDecimal avgVisitTime;

    @Schema(description = "访问量占比(%)")
    private BigDecimal pvRate;

    @Schema(description = "流出流量占比")
    private BigDecimal bodySentBytesRate;

    @Schema(description = "用户数占比")
    private BigDecimal uvRate;

    @Schema(description = "状态码")
	private String status;

    @Schema(description = "uri数")
	private BigDecimal uriCount;

    @Schema(description = "UA信息")
	private String browser;

    @Schema(description = "请求方式")
	private String requestMethod;

    @Schema(description = "访问来源")
    private String httpReferrer;

//    @JsonInclude(JsonInclude.Include.NON_NULL)
//    @Schema(description = "上次访问时间")
//    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
//    private Timestamp latestTime;

    @Schema(description = "IP")
	private String ip;

    @Schema(description = "IP数")
    private BigDecimal ipCount;

    @Schema(description = "IP数占比")
    private BigDecimal ipCountRate;

//    @JsonInclude(JsonInclude.Include.NON_NULL)
//    @Schema(description = "访问时间")
//    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
//    private Timestamp logTime;

//    @Schema(description = "来源网站")
//    private String httpReferer;
}
