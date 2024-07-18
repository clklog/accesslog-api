package com.zcunsoft.accesslog.api.controllers;

import com.zcunsoft.accesslog.api.models.accesslog.GetAccesslogExceptionPvPageRequest;
import com.zcunsoft.accesslog.api.models.accesslog.GetAccesslogPageResponse;
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
@RequestMapping(path = "accesslog/exception")
@Tag(name = "Accesslog异常分析", description = "Accesslog异常分析")
public class AccesslogExceptionController {

    @Resource
    AccesslogIReportService accesslogIReportService;

    @Operation(summary = "获取访问量异常数据")
    @RequestMapping(path = "/getExceptionPv", method = RequestMethod.POST)
    public GetAccesslogPageResponse getExceptionPv(@RequestBody GetAccesslogExceptionPvPageRequest getAccesslogExceptionPvPageRequest,HttpServletRequest request) {
        return accesslogIReportService.getExceptionPv(getAccesslogExceptionPvPageRequest);
    }
    /**
    @Operation(summary = "获取IP异常数据")
    @RequestMapping(path = "/getExceptionIp", method = RequestMethod.POST)
    public GetAccesslogPageResponse getExceptionIp(@RequestBody GetAccesslogExceptionPvPageRequest getAccesslogExceptionPvPageRequest,HttpServletRequest request) {
        return accesslogIReportService.getExceptionIp(getAccesslogExceptionPvPageRequest);
    }
    */
    @Operation(summary = "获取状态码异常数据")
    @RequestMapping(path = "/getExceptionStatus", method = RequestMethod.POST)
    public GetAccesslogPageResponse getExceptionStatus(@RequestBody GetAccesslogStatusPageRequest getAccesslogStatusPageRequest,HttpServletRequest request) {
        return accesslogIReportService.getExceptionStatus(getAccesslogStatusPageRequest);
    }
}
