package com.zcunsoft.accesslog.api.services;

import com.zcunsoft.accesslog.api.models.accesslog.*;


public interface AccesslogIReportService {

	/**
	 * 获取应用列表
	 * @return
	 */
	BaseStringResponse getApplicationCode();

	/**
	 * 获取应用下的host列表
	 * @param getHttpHostRequest
	 * @return
	 */
	BaseStringResponse getHttpHost(GetAccesslogHttpHostRequest getHttpHostRequest);

	/**
	 * 获取应用概览
	 * @param getAccesslogRequest
	 * @return
	 */
	GetAccesslogServerOverviewResponse getServerOverview(GetAccesslogServerOverviewRequest getAccesslogRequest);

	/**
	 * 获取流量趋势
	 */
	GetAccesslogFlowTrendResponse getFlowTrend(GetAccesslogFlowTrendRequest getFlowTrendRequest);


	/**
	 * 按省获取IP分布统计数据
	 */
	GetAccesslogFlowTrendResponse getIpByProvince(BaseAccesslogRequest baseAccesslogRequest);

	/**
	 * IP访问量TOP10数据
	 */
	GetAccesslogFlowTrendResponse getIpTop10(BaseAccesslogRequest baseAccesslogRequest);

	/**
	 * 获取耗时TOP10数据
	 */
	GetAccesslogFlowTrendResponse getRequestTimeTop10(BaseAccesslogRequest baseAccesslogRequest);

	/**
	 * 获取状态TOP10数据
	 */
	GetAccesslogFlowTrendResponse getStatus(BaseAccesslogRequest baseAccesslogRequest);

	/**
	 * 获取UATOP10数据
	 */
	GetAccesslogFlowTrendResponse getUa(BaseAccesslogRequest baseAccesslogRequest);

	/**
	 * 获取请求数据
	 */
	GetAccesslogFlowTrendResponse getRequestMethod(BaseAccesslogRequest baseAccesslogRequest);

	/**
	 * IP访问量TOP10数据
	 */
	GetAccesslogFlowTrendResponse getReferrerTop10(BaseAccesslogRequest baseAccesslogRequest);

	/**
	 * 获取访问URITOP10数据
	 */
	GetAccesslogFlowTrendResponse getUriTop10(BaseAccesslogRequest baseAccesslogRequest);

	/**
	 * 获取异常pv
	 * @param getAccesslogExceptionPvPageRequest
	 * @return
	 */
	GetAccesslogPageResponse getExceptionPv(GetAccesslogExceptionPvPageRequest getAccesslogExceptionPvPageRequest);

	/**
	 * 获取异常ip
	 * @param getAccesslogExceptionPvPageRequest
	 * @return
	 */
	GetAccesslogPageResponse getExceptionIp(GetAccesslogExceptionPvPageRequest getAccesslogExceptionPvPageRequest);

	/**
	 * 获取异常status
	 * @param getAccesslogStatusPageRequest
	 * @return
	 */
	GetAccesslogPageResponse getExceptionStatus(GetAccesslogStatusPageRequest getAccesslogStatusPageRequest);

	/**
	 * 获取状态吗列表
	 * @param baseAccesslogRequest
	 * @return
	 */
	BaseStringResponse getStatusList(BaseAccesslogRequest baseAccesslogRequest);

	/**
	 * 获取状态码流量指标
	 * @param baseAccesslogRequest
	 * @return
	 */
	GetAccesslogStatusFlowTrendResponse getStatusFlowTrend(BaseAccesslogRequest baseAccesslogRequest);


	/**
	 * 获取状态码uri详情
	 * @param getAccesslogStatusPageRequest
	 * @return
	 */
	GetAccesslogPageResponse getStatusDetail(GetAccesslogStatusPageRequest getAccesslogStatusPageRequest);

	/**
	 * 获取url耗时超过1秒的详情
	 * @param getAccesslogStatusPageRequest
	 * @return
	 */
	GetAccesslogPageResponse getPerformanceDetail(GetAccesslogStatusPageRequest getAccesslogStatusPageRequest);

	/**
	 * 获取超过100毫秒数据
	 * @param getAccesslogRequestimeGt100msRequest
	 * @return
	 */
	GetAccesslogRequestimeGt100msResponse getRequestimeGt100ms(GetAccesslogRequestimeGt100msRequest getAccesslogRequestimeGt100msRequest);

	/**
	 * 获取url相关分析
	 * @param getAccesslogStatusPageRequest
	 * @return
	 */
	GetAccesslogPageResponse getUriList(GetAccesslogStatusPageRequest getAccesslogStatusPageRequest);

	/**
	 * 获取ip相关分析
	 * @param getAccesslogIpPageRequest
	 * @return
	 */
	GetAccesslogPageResponse getIpList(GetAccesslogIpPageRequest getAccesslogIpPageRequest);

	/**
	 * 按省获取IP分布统计数据
	 */
	GetAccesslogFlowTrendResponse getIpByArea(GetAccesslogIpByAreaRequest getAccesslogIpByAreaRequest);

	/**
	 * 获取ip详情列表
	 * @param getAccesslogIpDetailPageRequest
	 * @return
	 */
	GetAccesslogIpDetailPageResponse getIpDetailList(GetAccesslogIpDetailPageRequest getAccesslogIpDetailPageRequest);
}
