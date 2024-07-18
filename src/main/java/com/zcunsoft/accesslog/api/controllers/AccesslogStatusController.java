package com.zcunsoft.accesslog.api.controllers;

import com.zcunsoft.accesslog.api.models.accesslog.BaseAccesslogRequest;
import com.zcunsoft.accesslog.api.models.accesslog.BaseStringResponse;
import com.zcunsoft.accesslog.api.models.accesslog.GetAccesslogFlowTrendResponse;
import com.zcunsoft.accesslog.api.models.accesslog.GetAccesslogPageResponse;
import com.zcunsoft.accesslog.api.models.accesslog.GetAccesslogStatusFlowTrendResponse;
import com.zcunsoft.accesslog.api.models.accesslog.GetAccesslogStatusPageRequest;
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
@RequestMapping(path = "accesslog/status")
@Tag(name = "Accesslog状态码分析", description = "Accesslog状态码分析")
public class AccesslogStatusController {

    @Resource
    AccesslogIReportService accesslogIReportService;
    
    @Operation(summary = "获取状态码数据")
    @RequestMapping(path = "/getStatus", method = RequestMethod.POST)
    public GetAccesslogFlowTrendResponse getStatus(@RequestBody BaseAccesslogRequest baseAccesslogRequest,HttpServletRequest request) {
        return accesslogIReportService.getStatus(baseAccesslogRequest);
    }
    
    @Operation(summary = "获取状态码列表")
    @RequestMapping(path = "/getStatusList", method = RequestMethod.POST)
    public BaseStringResponse getStatusList(@RequestBody BaseAccesslogRequest baseAccesslogRequest,HttpServletRequest request) {
        return accesslogIReportService.getStatusList(baseAccesslogRequest);
    }
    
    @Operation(summary = "获取状态码指标数据")
    @RequestMapping(path = "/getStatusFlowTrend", method = RequestMethod.POST)
    public GetAccesslogStatusFlowTrendResponse getStatusFlowTrend(@RequestBody BaseAccesslogRequest baseAccesslogRequest,HttpServletRequest request) {
        return accesslogIReportService.getStatusFlowTrend(baseAccesslogRequest);
    }
    
    @Operation(summary = "【弃用】状态码详情")
    @RequestMapping(path = "/getStatusDetail", method = RequestMethod.POST)
    public GetAccesslogPageResponse getStatusDetail(@RequestBody GetAccesslogStatusPageRequest getAccesslogStatusPageRequest,HttpServletRequest request) {
        return accesslogIReportService.getPerformanceDetail(getAccesslogStatusPageRequest);
    }
    
}
