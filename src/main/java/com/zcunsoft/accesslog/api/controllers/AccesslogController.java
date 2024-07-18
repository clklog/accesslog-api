package com.zcunsoft.accesslog.api.controllers;

import com.zcunsoft.accesslog.api.models.accesslog.BaseAccesslogRequest;
import com.zcunsoft.accesslog.api.models.accesslog.BaseStringResponse;
import com.zcunsoft.accesslog.api.models.accesslog.GetAccesslogFlowTrendRequest;
import com.zcunsoft.accesslog.api.models.accesslog.GetAccesslogFlowTrendResponse;
import com.zcunsoft.accesslog.api.models.accesslog.GetAccesslogServerOverviewRequest;
import com.zcunsoft.accesslog.api.models.accesslog.GetAccesslogServerOverviewResponse;
import com.zcunsoft.accesslog.api.models.accesslog.GetAccesslogHttpHostRequest;
import com.zcunsoft.accesslog.api.services.AccesslogIReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping(path = "accesslog")
@Tag(name = "Accesslog流量分析", description = "Accesslog流量分析")
public class AccesslogController {

    @Resource
    AccesslogIReportService accesslogIReportService;

   
    @Operation(summary = "获取应用编码")
    @RequestMapping(path = "/getApplicationCode", method = RequestMethod.POST)
    public BaseStringResponse getApplicationCode(HttpServletRequest request) {
        return accesslogIReportService.getApplicationCode();
    }
    
    @Operation(summary = "获取HttpHost")
    @RequestMapping(path = "/getHttpHost", method = RequestMethod.POST)
    public BaseStringResponse getHttpHost(@RequestBody GetAccesslogHttpHostRequest getHttpHostRequest,HttpServletRequest request) {
        return accesslogIReportService.getHttpHost(getHttpHostRequest);
    }
    
    @Operation(summary = "获取应用指标概览")
    @RequestMapping(path = "/getServerOverview", method = RequestMethod.POST)
    public GetAccesslogServerOverviewResponse getServerOverview(@RequestBody GetAccesslogServerOverviewRequest getAccesslogRequest,HttpServletRequest request) {
        return accesslogIReportService.getServerOverview(getAccesslogRequest);
    }
    
    @Operation(summary = "获取流量趋势统计数据")
    @RequestMapping(path = "/getFlowTrend", method = RequestMethod.POST)
    public GetAccesslogFlowTrendResponse getFlowTrend(@RequestBody GetAccesslogFlowTrendRequest getFlowTrendRequest,HttpServletRequest request) {
        return accesslogIReportService.getFlowTrend(getFlowTrendRequest);
    }
    
    @Operation(summary = "【弃用】按省获取IP分布统计数据")
    @RequestMapping(path = "/getIpByProvince", method = RequestMethod.POST)
    public GetAccesslogFlowTrendResponse getIpByProvince(@RequestBody BaseAccesslogRequest baseAccesslogRequest,HttpServletRequest request) {
        return accesslogIReportService.getIpByProvince(baseAccesslogRequest);
    }
    
    @Operation(summary = "【弃用】获取IP访问量TOP10数据")
    @RequestMapping(path = "/getIpTop10", method = RequestMethod.POST)
    public GetAccesslogFlowTrendResponse getIpTop10(@RequestBody BaseAccesslogRequest baseAccesslogRequest,HttpServletRequest request) {
        return accesslogIReportService.getIpTop10(baseAccesslogRequest);
    }
    
    @Operation(summary = "【弃用】获取耗时TOP10数据")
    @RequestMapping(path = "/getRequestTimeTop10", method = RequestMethod.POST)
    public GetAccesslogFlowTrendResponse getRequestTimeTop10(@RequestBody BaseAccesslogRequest baseAccesslogRequest,HttpServletRequest request) {
        return accesslogIReportService.getRequestTimeTop10(baseAccesslogRequest);
    }
    
    @Operation(summary = "获取UA数据")
    @RequestMapping(path = "/getUa", method = RequestMethod.POST)
    public GetAccesslogFlowTrendResponse getUa(@RequestBody BaseAccesslogRequest baseAccesslogRequest,HttpServletRequest request) {
        return accesslogIReportService.getUa(baseAccesslogRequest);
    }
    
    @Operation(summary = "获取请求方式数据")
    @RequestMapping(path = "/getRequestMethod", method = RequestMethod.POST)
    public GetAccesslogFlowTrendResponse getRequestMethod(@RequestBody BaseAccesslogRequest baseAccesslogRequest,HttpServletRequest request) {
        return accesslogIReportService.getRequestMethod(baseAccesslogRequest);
    }
    
    @Operation(summary = "获取访问来源TOP10数据")
    @RequestMapping(path = "/getReferrerTop10", method = RequestMethod.POST)
    public GetAccesslogFlowTrendResponse getReferrerTop10(@RequestBody BaseAccesslogRequest baseAccesslogRequest,HttpServletRequest request) {
        return accesslogIReportService.getReferrerTop10(baseAccesslogRequest);
    }
    
    @Operation(summary = "【弃用】获取访问TOP10数据")
    @RequestMapping(path = "/getUriTop10", method = RequestMethod.POST)
    public GetAccesslogFlowTrendResponse getUriTop10(@RequestBody BaseAccesslogRequest baseAccesslogRequest,HttpServletRequest request) {
        return accesslogIReportService.getUriTop10(baseAccesslogRequest);
    }
    
}
