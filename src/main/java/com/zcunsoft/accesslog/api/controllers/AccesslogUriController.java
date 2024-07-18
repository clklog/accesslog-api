package com.zcunsoft.accesslog.api.controllers;

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
@RequestMapping(path = "accesslog/uri")
@Tag(name = "Accesslog Uri分析", description = "Accesslog Uri分析")
public class AccesslogUriController {
    @Resource
    AccesslogIReportService accesslogIReportService;

    @Operation(summary = "获取Uri列表")
    @RequestMapping(path = "/getUriList", method = RequestMethod.POST)
    public GetAccesslogPageResponse getUriList(@RequestBody GetAccesslogStatusPageRequest getAccesslogStatusPageRequest,HttpServletRequest request) {
        return accesslogIReportService.getUriList(getAccesslogStatusPageRequest);
    }
}
