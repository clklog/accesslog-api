package com.zcunsoft.accesslog.api.models.accesslog;

import com.zcunsoft.accesslog.api.entity.clickhouse.Accesslogbydate;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 作者：yechangzhong
 * 创建日期：2024/4/23 9:47
 * 描述：获取IP详情分页响应数据
 */
@Schema(description = "IP详情分页的响应数据")
@Data
public class GetAccesslogIpDetailPageResponseData {
    private int total;

    private List<AccesslogFlowDetail> rows;
}
