package com.zcunsoft.accesslog.api.controllers;

import com.zcunsoft.accesslog.api.models.accesslog.*;
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
@RequestMapping(path = "accesslog/ip")
@Tag(name = "Accesslog IP分析", description = "Accesslog IP分析")
public class AccesslogIpController {
    @Resource
    AccesslogIReportService accesslogIReportService;

    @Operation(summary = "获取IP列表")
    @RequestMapping(path = "/getIpList", method = RequestMethod.POST)
    public GetAccesslogPageResponse getIpList(@RequestBody GetAccesslogIpPageRequest getAccesslogIpPageRequest,HttpServletRequest request) {
        return accesslogIReportService.getIpList(getAccesslogIpPageRequest);
    }

    @Operation(summary = "按地域获取IP分布统计数据")
    @RequestMapping(path = "/getIpByArea", method = RequestMethod.POST)
    public GetAccesslogFlowTrendResponse getIpByArea(@RequestBody GetAccesslogIpByAreaRequest getAccesslogIpByAreaRequest,HttpServletRequest request) {
        return accesslogIReportService.getIpByArea(getAccesslogIpByAreaRequest);
    }

    @Operation(summary = "获取IP详情列表")
    @RequestMapping(path = "/getIpDetailList", method = RequestMethod.POST)
    public GetAccesslogIpDetailPageResponse getIpDetailList(@RequestBody GetAccesslogIpDetailPageRequest getAccesslogIpDetailPageRequest, HttpServletRequest request) {
        return accesslogIReportService.getIpDetailList(getAccesslogIpDetailPageRequest);
    }
}
