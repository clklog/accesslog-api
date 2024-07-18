package com.zcunsoft.accesslog.api.controllers;

import com.zcunsoft.accesslog.api.models.accesslog.GetAccesslogPageResponse;
import com.zcunsoft.accesslog.api.models.accesslog.GetAccesslogRequestimeGt100msRequest;
import com.zcunsoft.accesslog.api.models.accesslog.GetAccesslogRequestimeGt100msResponse;
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
@RequestMapping(path = "accesslog/performance")
@Tag(name = "Accesslog性能分析", description = "Accesslog性能分析")
public class AccesslogPerformanceController {

    @Resource
    AccesslogIReportService accesslogIReportService;
    
    @Operation(summary = "获取超过100毫秒数据")
    @RequestMapping(path = "/getRequestimeGt100ms", method = RequestMethod.POST)
    public GetAccesslogRequestimeGt100msResponse getRequestimeGt100ms(@RequestBody GetAccesslogRequestimeGt100msRequest getAccesslogRequestimeGt100msRequest,HttpServletRequest request) {
        return accesslogIReportService.getRequestimeGt100ms(getAccesslogRequestimeGt100msRequest);
    }
    
    @Operation(summary = "【弃用】性能分析详情")
    @RequestMapping(path = "/getPerformanceDetail", method = RequestMethod.POST)
    public GetAccesslogPageResponse getPerformanceDetail(@RequestBody GetAccesslogStatusPageRequest getAccesslogStatusPageRequest,HttpServletRequest request) {
        return accesslogIReportService.getPerformanceDetail(getAccesslogStatusPageRequest);
    }
    
}
