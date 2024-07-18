package com.zcunsoft.accesslog.api.services;

import com.zcunsoft.accesslog.api.cfg.ClklogApiSetting;
import com.zcunsoft.accesslog.api.constant.Constants;
import com.zcunsoft.accesslog.api.entity.clickhouse.Accesslogbydate;
import com.zcunsoft.accesslog.api.handlers.ConstsDataHolder;
import com.zcunsoft.accesslog.api.models.App;
import com.zcunsoft.accesslog.api.models.TimeFrame;
import com.zcunsoft.accesslog.api.models.accesslog.*;
import com.zcunsoft.accesslog.api.utils.TimeUtils;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AccesslogReportServiceImpl implements AccesslogIReportService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final NamedParameterJdbcTemplate clickHouseJdbcTemplate;

    private final ThreadLocal<DateFormat> yMdFORMAT = new ThreadLocal<DateFormat>() {
        @Override
        protected DateFormat initialValue() {
            return new SimpleDateFormat("yyyy-MM-dd");
        }
    };

    private final ThreadLocal<DateFormat> yMdHmsFORMAT = new ThreadLocal<DateFormat>() {
        @Override
        protected DateFormat initialValue() {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        }
    };

    private static final List<String> hostList;

    static {
        hostList = new ArrayList<String>() {
            {
                add("huoqingqing.com");
                add("h5.huoqingqing.com");
                add("app.huoqingqing.com");
                add("group.huoqingqing.com");
            }
        };
    }

    private final ClklogApiSetting clklogApiSetting;

    private final ConstsDataHolder constsDataHolder;

    public AccesslogReportServiceImpl(NamedParameterJdbcTemplate clickHouseJdbcTemplate, ClklogApiSetting clklogApiSetting, ConstsDataHolder constsDataHolder) {
        this.clickHouseJdbcTemplate = clickHouseJdbcTemplate;
        this.clklogApiSetting = clklogApiSetting;
        this.constsDataHolder = constsDataHolder;
    }

    private static final ThreadLocal<DecimalFormat> decimalFormat = new ThreadLocal<DecimalFormat>() {
        @Override
        protected DecimalFormat initialValue() {
            return new DecimalFormat("0.####");
        }
    };

    public BaseStringResponse getApplicationCode() {
        List<App> appList = getAppList();
        BaseStringResponse response = new BaseStringResponse();

        response.setData(appList.stream().map(App::getAppCode).collect(Collectors.toList()));
        return response;
    }

    private List<App> getAppList() {
        MapSqlParameterSource paramMap = new MapSqlParameterSource();
        String getListSql = "select app_code,app_name from tbl_app order by app_code ";
        List<App> appList = clickHouseJdbcTemplate.query(getListSql, paramMap,
                new BeanPropertyRowMapper<App>(App.class));
        return appList;
    }

    @Override
    public BaseStringResponse getHttpHost(GetAccesslogHttpHostRequest getHttpHostRequest) {
        BaseStringResponse response = new BaseStringResponse();
        if (StringUtils.equalsIgnoreCase(Constants.DEFAULT_SERVER_NAME_HQQ, getHttpHostRequest.getApplicationCode())) {
            response.setData(hostList);
            return response;
        }
        MapSqlParameterSource paramMap = new MapSqlParameterSource();
        String getListSql = "select t.http_host as http_host from  gp_nginx_access t ";
        String where = "";
        where = buildApplicationCodeListFilter(getHttpHostRequest.getApplicationCode(), paramMap, where);
        if (StringUtils.isNotBlank(where)) {
            getListSql += " where " + where.substring(4);
        }
        getListSql += " group by t.http_host";
        List<Accesslogbydate> accesslogbydateList = clickHouseJdbcTemplate.query(getListSql, paramMap,
                new BeanPropertyRowMapper<Accesslogbydate>(Accesslogbydate.class));
        List<String> hostList = new ArrayList<String>();
        for (Accesslogbydate accessLogbydate : accesslogbydateList) {
            hostList.add(accessLogbydate.getHttpHost());
        }
        response.setData(hostList);
        return response;
    }

    @Override
    public GetAccesslogServerOverviewResponse getServerOverview(GetAccesslogServerOverviewRequest getAccesslogRequest) {
        MapSqlParameterSource paramMap = new MapSqlParameterSource();
        String getListSql = "select http_host as http_host,count(1) as pv,countDistinct(remote_addr) as uv,sum(if(body_sent_bytes ='', 0, toFloat32(body_sent_bytes))) as body_sent_bytes,avg(request_time) as avg_visit_time from gp_nginx_access t ";
        String getSummarySql = "select count(1) as pv,countDistinct(remote_addr) as uv,sum(if(body_sent_bytes ='', 0, toFloat32(body_sent_bytes))) as body_sent_bytes,avg(request_time) as avg_visit_time from gp_nginx_access t ";
        String where = "";
        where = buildApplicationCodeListFilter(getAccesslogRequest.getApplicationCode(), paramMap, where);
        where = buildHttpHostFilter(getAccesslogRequest.getHttpHost(), paramMap, where);
        where = buildStatDateStartFilter(getAccesslogRequest.getStartTime(), paramMap, where);
        where = buildStatDateEndFilter(getAccesslogRequest.getEndTime(), paramMap, where);
        if (StringUtils.isNotBlank(where)) {
            getListSql += " where " + where.substring(4);
            getSummarySql += " where " + where.substring(4);
        }
        getListSql += " group by t.http_host ";

        List<Accesslogbydate> accesslogbydateList = clickHouseJdbcTemplate.query(getListSql, paramMap,
                new BeanPropertyRowMapper<Accesslogbydate>(Accesslogbydate.class));
        Accesslogbydate totalAccesslogbydate = clickHouseJdbcTemplate.queryForObject(getSummarySql, paramMap,
                new BeanPropertyRowMapper<Accesslogbydate>(Accesslogbydate.class));
        AccesslogFlowDetail totalFlowDetail = assemblyFlowDetail(totalAccesslogbydate, null);

        GetAccesslogServerOverviewResponse response = new GetAccesslogServerOverviewResponse();
        GetAccesslogServerOverview getServerOverview = new GetAccesslogServerOverview();
        List<AccesslogFlowDetail> flowDetaiList = new ArrayList<AccesslogFlowDetail>();

        Accesslogbydate totalAccessLogbydate = new Accesslogbydate();
        totalAccessLogbydate.setUv(BigDecimal.ZERO);
        totalAccessLogbydate.setPv(BigDecimal.ZERO);
        totalAccessLogbydate.setBodySentBytes(BigDecimal.ZERO);
        for (Accesslogbydate accessLogbydate : accesslogbydateList) {
            totalAccessLogbydate.setUv(accessLogbydate.getUv().add(totalAccessLogbydate.getUv()));
            totalAccessLogbydate.setPv(accessLogbydate.getPv().add(totalAccessLogbydate.getPv()));
            totalAccessLogbydate.setBodySentBytes(accessLogbydate.getBodySentBytes().add(totalAccessLogbydate.getBodySentBytes()));
        }


        for (Accesslogbydate accessLogbydate : accesslogbydateList) {
//			if(StringUtils.isEmpty(getAccesslogRequest.getHttpHost()) || StringUtils.equalsIgnoreCase(getAccesslogRequest.getHttpHost(),Constants.DEFAULT_ALL) || StringUtils.equalsIgnoreCase(getAccesslogRequest.getHttpHost(), accessLogbydate.getHttpHost())) {
            AccesslogFlowDetail flowDetail = assemblyFlowDetail(accessLogbydate, totalAccessLogbydate);
            flowDetail.setHttpHost(accessLogbydate.getHttpHost());
            flowDetaiList.add(flowDetail);
//			}
        }
        getServerOverview.setTotalAccesslogFlowDetail(totalFlowDetail);
        getServerOverview.setAccesslogFlowDetailList(flowDetaiList);
        response.setData(getServerOverview);
        return response;
    }

    @Override
    public GetAccesslogFlowTrendResponse getIpByProvince(BaseAccesslogRequest baseAccesslogRequest) {
        MapSqlParameterSource paramMap = new MapSqlParameterSource();
        String getListSql = "select province as province,countDistinct(remote_addr) as uv from gp_nginx_access t ";
        String where = "";
        where = buildApplicationCodeListFilter(baseAccesslogRequest.getApplicationCode(), paramMap, where);
        where = buildHttpHostFilter(baseAccesslogRequest.getHttpHost(), paramMap, where);
        where = buildStatDateStartFilter(baseAccesslogRequest.getStartTime(), paramMap, where);
        where = buildStatDateEndFilter(baseAccesslogRequest.getEndTime(), paramMap, where);
        if (StringUtils.isNotBlank(where)) {
            getListSql += " where " + where.substring(4);
        }
        getListSql += " group by t.province order by uv desc ";

        List<Accesslogbydate> accesslogbydateList = clickHouseJdbcTemplate.query(getListSql, paramMap,
                new BeanPropertyRowMapper<Accesslogbydate>(Accesslogbydate.class));
        Accesslogbydate totalAccessLogbydate = new Accesslogbydate();
        totalAccessLogbydate.setUv(BigDecimal.ZERO);
        for (Accesslogbydate accessLogbydate : accesslogbydateList) {
            totalAccessLogbydate.setUv(accessLogbydate.getUv().add(totalAccessLogbydate.getUv()));
        }
        List<AccesslogFlowDetail> flowDetaiList = new ArrayList<AccesslogFlowDetail>();
        for (Accesslogbydate accessLogbydate : accesslogbydateList) {
            AccesslogFlowDetail flowDetail = assemblyFlowDetail(accessLogbydate, totalAccessLogbydate);
            flowDetail.setProvince(accessLogbydate.getProvince());
            flowDetaiList.add(flowDetail);
        }
        GetAccesslogFlowTrendResponse response = new GetAccesslogFlowTrendResponse();
        response.setData(flowDetaiList);
        return response;
    }

    @Override
    public GetAccesslogFlowTrendResponse getIpByArea(GetAccesslogIpByAreaRequest getAccesslogIpByAreaRequest) {
        MapSqlParameterSource paramMap = new MapSqlParameterSource();
        //在本次统计中ipCount等于uv
        if (getAccesslogIpByAreaRequest.getIndicator() == null || getAccesslogIpByAreaRequest.getIndicator().isEmpty()
                || "ipCount".equals(getAccesslogIpByAreaRequest.getIndicator())) {
            getAccesslogIpByAreaRequest.setIndicator("uv");
        }
        String getListSqlByCountry = "select country as country, count(remote_addr) as pv,countDistinct(remote_addr) as uv from gp_nginx_access t ";
        String getListSqlByProvince = "select province as province, count(remote_addr) as pv,countDistinct(remote_addr) as uv from gp_nginx_access t ";
        String getListSqlByCity = "select city as city, count(remote_addr) as pv,countDistinct(remote_addr) as uv from gp_nginx_access t ";
        String where = "";
        where = buildApplicationCodeListFilter(getAccesslogIpByAreaRequest.getApplicationCode(), paramMap, where);
        where = buildHttpHostFilter(getAccesslogIpByAreaRequest.getHttpHost(), paramMap, where);
        where = buildStatDateStartFilter(getAccesslogIpByAreaRequest.getStartTime(), paramMap, where);
        where = buildStatDateEndFilter(getAccesslogIpByAreaRequest.getEndTime(), paramMap, where);
        if (getAccesslogIpByAreaRequest.getCountry() != null && !getAccesslogIpByAreaRequest.getCountry().isEmpty()) {
            where = where + " and country = '" + getAccesslogIpByAreaRequest.getCountry() + "' ";
        }
        if (StringUtils.isNotBlank(where)) {
            getListSqlByCountry += " where " + where.substring(4) + " group by t.country order by " + getAccesslogIpByAreaRequest.getIndicator() + " desc ";
            getListSqlByProvince += " where " + where.substring(4) + " group by t.province order by " + getAccesslogIpByAreaRequest.getIndicator() + " desc ";
            getListSqlByCity += " where " + where.substring(4) + " group by t.city order by " + getAccesslogIpByAreaRequest.getIndicator() + " desc ";
        }
        String getListSql = getListSqlByProvince;
        if ("country".equals(getAccesslogIpByAreaRequest.getSummaryOptions())) {
            getListSql = getListSqlByCountry;
        } else if ("city".equals(getAccesslogIpByAreaRequest.getSummaryOptions())) {
            getListSql = getListSqlByCity;
        }
        List<Accesslogbydate> accesslogbydateList = clickHouseJdbcTemplate.query(getListSql, paramMap,
                new BeanPropertyRowMapper<Accesslogbydate>(Accesslogbydate.class));
        Accesslogbydate totalAccessLogbydate = new Accesslogbydate();
        totalAccessLogbydate.setPv(BigDecimal.ZERO);
        totalAccessLogbydate.setUv(BigDecimal.ZERO);
        totalAccessLogbydate.setIpCount(BigDecimal.ZERO);
        accesslogbydateList.forEach(accessLogbydate -> {
            accessLogbydate.setIpCount(accessLogbydate.getUv());
            totalAccessLogbydate.setPv(accessLogbydate.getPv().add(totalAccessLogbydate.getPv()));
            totalAccessLogbydate.setUv(accessLogbydate.getUv().add(totalAccessLogbydate.getUv()));
        });
        totalAccessLogbydate.setIpCount(totalAccessLogbydate.getUv());
        List<AccesslogFlowDetail> flowDetaiList = new ArrayList<AccesslogFlowDetail>();
        for (Accesslogbydate accessLogbydate : accesslogbydateList) {
            AccesslogFlowDetail flowDetail = assemblyFlowDetail(accessLogbydate, totalAccessLogbydate);
            flowDetail.setProvince(accessLogbydate.getProvince());
            flowDetail.setCountry(accessLogbydate.getCountry());
            flowDetail.setCity(accessLogbydate.getCity());
            flowDetaiList.add(flowDetail);
        }
        GetAccesslogFlowTrendResponse response = new GetAccesslogFlowTrendResponse();
        response.setData(flowDetaiList);
        return response;
    }

    @Override
    public GetAccesslogFlowTrendResponse getIpTop10(BaseAccesslogRequest baseAccesslogRequest) {
        MapSqlParameterSource paramMap = new MapSqlParameterSource();
        String getListSql = "select remote_addr as remote_addr,count(1) as pv from gp_nginx_access t ";
        String where = "";
        where = buildApplicationCodeListFilter(baseAccesslogRequest.getApplicationCode(), paramMap, where);
        where = buildHttpHostFilter(baseAccesslogRequest.getHttpHost(), paramMap, where);
        where = buildStatDateStartFilter(baseAccesslogRequest.getStartTime(), paramMap, where);
        where = buildStatDateEndFilter(baseAccesslogRequest.getEndTime(), paramMap, where);
        if (StringUtils.isNotBlank(where)) {
            getListSql += " where " + where.substring(4);
        }
        getListSql += " group by t.remote_addr order by pv desc limit 10";

        List<Accesslogbydate> accesslogbydateList = clickHouseJdbcTemplate.query(getListSql, paramMap,
                new BeanPropertyRowMapper<Accesslogbydate>(Accesslogbydate.class));

        List<AccesslogFlowDetail> flowDetaiList = new ArrayList<AccesslogFlowDetail>();
        for (Accesslogbydate accessLogbydate : accesslogbydateList) {
            AccesslogFlowDetail flowDetail = assemblyFlowDetail(accessLogbydate, null);
            flowDetail.setIp(accessLogbydate.getRemoteAddr());
            flowDetaiList.add(flowDetail);
        }
        GetAccesslogFlowTrendResponse response = new GetAccesslogFlowTrendResponse();
        response.setData(flowDetaiList);
        return response;
    }

    @Override
    public GetAccesslogFlowTrendResponse getRequestTimeTop10(BaseAccesslogRequest baseAccesslogRequest) {
        return getUriTop10OrderBySortName(baseAccesslogRequest, "max_visit_time");
    }

    @Override
    public GetAccesslogFlowTrendResponse getUriTop10(BaseAccesslogRequest baseAccesslogRequest) {
        return getUriTop10OrderBySortName(baseAccesslogRequest, "uri_count");
    }

    private GetAccesslogFlowTrendResponse getUriTop10OrderBySortName(BaseAccesslogRequest baseAccesslogRequest, String sortName) {
        MapSqlParameterSource paramMap = new MapSqlParameterSource();
        String getListSql = "select uri as uri,max(request_time) AS max_visit_time,count(1) as uri_count,avg(request_time) as avg_visit_time from gp_nginx_access t ";
        String where = "";
        where = buildApplicationCodeListFilter(baseAccesslogRequest.getApplicationCode(), paramMap, where);
        where = buildHttpHostFilter(baseAccesslogRequest.getHttpHost(), paramMap, where);
        where = buildStatDateStartFilter(baseAccesslogRequest.getStartTime(), paramMap, where);
        where = buildStatDateEndFilter(baseAccesslogRequest.getEndTime(), paramMap, where);
        if (StringUtils.isNotBlank(where)) {
            getListSql += " where " + where.substring(4);
        }
        getListSql += " group by t.uri ";
        if ("max_visit_time".equals(sortName)) {
            getListSql += " order by max_visit_time desc limit 10";
        }
        if ("uri_count".equals(sortName)) {
            getListSql += " order by uri_count desc limit 10";
        }
        List<Accesslogbydate> accesslogbydateList = clickHouseJdbcTemplate.query(getListSql, paramMap,
                new BeanPropertyRowMapper<Accesslogbydate>(Accesslogbydate.class));

        List<AccesslogFlowDetail> flowDetaiList = new ArrayList<AccesslogFlowDetail>();
        for (Accesslogbydate accessLogbydate : accesslogbydateList) {
            AccesslogFlowDetail flowDetail = assemblyFlowDetail(accessLogbydate, null);
            flowDetail.setUri(accessLogbydate.getUri());
            flowDetail.setMaxVisitTime(accessLogbydate.getMaxVisitTime() != null ? accessLogbydate.getMaxVisitTime().setScale(4, RoundingMode.DOWN) : BigDecimal.ZERO);
            flowDetaiList.add(flowDetail);
        }
        GetAccesslogFlowTrendResponse response = new GetAccesslogFlowTrendResponse();
        response.setData(flowDetaiList);
        return response;
    }

    @Override
    public GetAccesslogFlowTrendResponse getStatus(BaseAccesslogRequest baseAccesslogRequest) {
        MapSqlParameterSource paramMap = new MapSqlParameterSource();
        String getListSql = "select status as status,count(1) as pv from gp_nginx_access t ";
        String where = "";
        where = buildApplicationCodeListFilter(baseAccesslogRequest.getApplicationCode(), paramMap, where);
        where = buildHttpHostFilter(baseAccesslogRequest.getHttpHost(), paramMap, where);
        where = buildStatDateStartFilter(baseAccesslogRequest.getStartTime(), paramMap, where);
        where = buildStatDateEndFilter(baseAccesslogRequest.getEndTime(), paramMap, where);
        if (StringUtils.isNotBlank(where)) {
            getListSql += " where " + where.substring(4);
        }
        getListSql += " group by t.status order by pv desc ";
        List<Accesslogbydate> accesslogbydateList = clickHouseJdbcTemplate.query(getListSql, paramMap,
                new BeanPropertyRowMapper<Accesslogbydate>(Accesslogbydate.class));

        Accesslogbydate totalAccessLogbydate = new Accesslogbydate();
        totalAccessLogbydate.setPv(BigDecimal.ZERO);
        for (Accesslogbydate accessLogbydate : accesslogbydateList) {
            totalAccessLogbydate.setPv(accessLogbydate.getPv().add(totalAccessLogbydate.getPv()));
        }
        List<AccesslogFlowDetail> flowDetaiList = new ArrayList<AccesslogFlowDetail>();
        for (Accesslogbydate accessLogbydate : accesslogbydateList) {
            AccesslogFlowDetail flowDetail = assemblyFlowDetail(accessLogbydate, totalAccessLogbydate);
            flowDetail.setStatus(accessLogbydate.getStatus());
            flowDetaiList.add(flowDetail);
        }
        GetAccesslogFlowTrendResponse response = new GetAccesslogFlowTrendResponse();
        response.setData(flowDetaiList);
        return response;
    }

    @Override
    public BaseStringResponse getStatusList(BaseAccesslogRequest baseAccesslogRequest) {
        MapSqlParameterSource paramMap = new MapSqlParameterSource();
        String getListSql = "select status as status from gp_nginx_access t ";
        String where = "";
        where = buildApplicationCodeListFilter(baseAccesslogRequest.getApplicationCode(), paramMap, where);
        where = buildHttpHostFilter(baseAccesslogRequest.getHttpHost(), paramMap, where);
        where = buildStatDateStartFilter(baseAccesslogRequest.getStartTime(), paramMap, where);
        where = buildStatDateEndFilter(baseAccesslogRequest.getEndTime(), paramMap, where);
        if (StringUtils.isNotBlank(where)) {
            getListSql += " where " + where.substring(4);
        }
        getListSql += " group by status order by status asc ";
        List<Accesslogbydate> accesslogbydateList = clickHouseJdbcTemplate.query(getListSql, paramMap,
                new BeanPropertyRowMapper<Accesslogbydate>(Accesslogbydate.class));

        BaseStringResponse response = new BaseStringResponse();
        List<String> statusList = new ArrayList<String>();
        for (Accesslogbydate accesslogbydate : accesslogbydateList) {
            statusList.add(accesslogbydate.getStatus());
        }
        response.setData(statusList);
        return response;
    }

    @Override
    public GetAccesslogStatusFlowTrendResponse getStatusFlowTrend(BaseAccesslogRequest baseAccesslogRequest) {
        MapSqlParameterSource paramMap = new MapSqlParameterSource();
        String getListSql = "select http_host as http_host,status as status,count(1) pv from gp_nginx_access t ";
        String where = "";
        where = buildApplicationCodeListFilter(baseAccesslogRequest.getApplicationCode(), paramMap, where);
        where = buildHttpHostFilter(baseAccesslogRequest.getHttpHost(), paramMap, where);
        where = buildStatDateStartFilter(baseAccesslogRequest.getStartTime(), paramMap, where);
        where = buildStatDateEndFilter(baseAccesslogRequest.getEndTime(), paramMap, where);
        if (StringUtils.isNotBlank(where)) {
            getListSql += " where " + where.substring(4);
        }
        getListSql += " group by http_host,status order by status asc ";
        List<Accesslogbydate> accesslogbydateList = clickHouseJdbcTemplate.query(getListSql, paramMap,
                new BeanPropertyRowMapper<Accesslogbydate>(Accesslogbydate.class));

        GetAccesslogStatusFlowTrendResponse response = new GetAccesslogStatusFlowTrendResponse();
        List<GetAccesslogStatusFlowTrendResponseData> getAccesslogStatusFlowTrendResponseDataList = new ArrayList<GetAccesslogStatusFlowTrendResponseData>();
        for (Accesslogbydate accesslogbydate : accesslogbydateList) {
            AccesslogFlowDetail flowDetail = new AccesslogFlowDetail();
            flowDetail.setStatus(accesslogbydate.getStatus());
            flowDetail.setPv(accesslogbydate.getPv());

            GetAccesslogStatusFlowTrendResponseData getAccesslogStatusFlowTrendResponseData = null;
            List<AccesslogFlowDetail> flowList = null;
            Optional<GetAccesslogStatusFlowTrendResponseData> optionalOld = getAccesslogStatusFlowTrendResponseDataList.stream().filter(f -> f.getHttpHost().equalsIgnoreCase(accesslogbydate.getHttpHost())).findAny();
            if (optionalOld.isPresent()) {
                getAccesslogStatusFlowTrendResponseData = optionalOld.get();
                flowList = getAccesslogStatusFlowTrendResponseData.getRows();
            } else {
                getAccesslogStatusFlowTrendResponseData = new GetAccesslogStatusFlowTrendResponseData();
                getAccesslogStatusFlowTrendResponseData.setHttpHost(accesslogbydate.getHttpHost());
                flowList = new ArrayList<AccesslogFlowDetail>();
                getAccesslogStatusFlowTrendResponseDataList.add(getAccesslogStatusFlowTrendResponseData);
            }
            flowList.add(flowDetail);
            getAccesslogStatusFlowTrendResponseData.setRows(flowList);
        }
        response.setData(getAccesslogStatusFlowTrendResponseDataList);
        return response;
    }

    @Override
    public GetAccesslogPageResponse getStatusDetail(GetAccesslogStatusPageRequest getAccesslogStatusPageRequest) {
        MapSqlParameterSource paramMap = new MapSqlParameterSource();
        String getListSql = "SELECT t2.http_host as http_host,t2.uri as uri, t2.pv as pv, t2.slow_pv as slow_pv, t2.max_visit_time as max_visit_time, t2.avg_visit_time as avg_visit_time FROM ( ";
        String getSonSql1 = "SELECT http_host as http_host,uri as uri, sum(if(request_time >= 1000, 1, 0)) AS slow_pv,count(1) AS pv,avg(request_time) AS avg_visit_time , max(request_time) AS max_visit_time FROM gp_nginx_access t ";
        String getCountSql = "SELECT count(1) FROM ( ";
        String where = "";
        where = buildApplicationCodeListFilter(getAccesslogStatusPageRequest.getApplicationCode(), paramMap, where);
        where = buildHttpHostFilter(getAccesslogStatusPageRequest.getHttpHost(), paramMap, where);
        where = buildStatDateStartFilter(getAccesslogStatusPageRequest.getStartTime(), paramMap, where);
        where = buildStatDateEndFilter(getAccesslogStatusPageRequest.getEndTime(), paramMap, where);
        where = buildStatusFilter(getAccesslogStatusPageRequest.getStatus(), paramMap, where);
        where = buildLimitFilter(getAccesslogStatusPageRequest.getLimits(), paramMap, where);
        if (StringUtils.isNotBlank(where)) {
            getSonSql1 += " where " + where.substring(4);
        }
        getSonSql1 += " GROUP BY http_host,uri " + getSortSqlFormat(getAccesslogStatusPageRequest.getSortName(), getAccesslogStatusPageRequest.getSortOrder(), "pv");
        getListSql += getSonSql1 + ") t2 ";
        getCountSql += getSonSql1 + ") t2 ";
        getListSql += " limit " + (getAccesslogStatusPageRequest.getPageNum() - 1) * getAccesslogStatusPageRequest.getPageSize() + "," + getAccesslogStatusPageRequest.getPageSize();
        List<Accesslogbydate> accesslogbydateList = clickHouseJdbcTemplate.query(getListSql, paramMap,
                new BeanPropertyRowMapper<Accesslogbydate>(Accesslogbydate.class));
        Integer total = clickHouseJdbcTemplate.queryForObject(getCountSql, paramMap, Integer.class);
        List<AccesslogFlowDetail> accesslogDetailList = new ArrayList<>();

        GetAccesslogPageResponse response = new GetAccesslogPageResponse();
        GetAccesslogPageResponseData responseData = new GetAccesslogPageResponseData();
        for (Accesslogbydate accesslogbydate : accesslogbydateList) {
            AccesslogFlowDetail accesslogFlowDetail = assemblyFlowDetail(accesslogbydate, null);
            accesslogFlowDetail.setHttpHost(accesslogbydate.getHttpHost());
            accesslogFlowDetail.setUri(accesslogbydate.getUri());
            accesslogFlowDetail.setSlowPv(accesslogbydate.getSlowPv());
            accesslogFlowDetail.setMaxVisitTime(accesslogbydate.getMaxVisitTime() != null ? accesslogbydate.getMaxVisitTime().setScale(4, RoundingMode.DOWN) : BigDecimal.ZERO);
            if (accesslogbydate.getPv() != null && accesslogbydate.getPv().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal pvRate = accesslogbydate.getSlowPv().divide(accesslogbydate.getPv(), 5, RoundingMode.DOWN);
                accesslogFlowDetail.setPvRate(pvRate);
            }
            accesslogDetailList.add(accesslogFlowDetail);
        }
        responseData.setRows(accesslogDetailList);
        responseData.setTotal(total);
        response.setData(responseData);
        return response;
    }

    @Override
    public GetAccesslogFlowTrendResponse getUa(BaseAccesslogRequest baseAccesslogRequest) {
        MapSqlParameterSource paramMap = new MapSqlParameterSource();
        String getListSql = "select browser as browser,countDistinct(remote_addr) as uv from gp_nginx_access t ";
        String where = "";
        where = buildApplicationCodeListFilter(baseAccesslogRequest.getApplicationCode(), paramMap, where);
        where = buildHttpHostFilter(baseAccesslogRequest.getHttpHost(), paramMap, where);
        where = buildStatDateStartFilter(baseAccesslogRequest.getStartTime(), paramMap, where);
        where = buildStatDateEndFilter(baseAccesslogRequest.getEndTime(), paramMap, where);
        if (StringUtils.isNotBlank(where)) {
            getListSql += " where " + where.substring(4);
        }
        getListSql += " group by t.browser order by uv desc ";
        List<Accesslogbydate> accesslogbydateList = clickHouseJdbcTemplate.query(getListSql, paramMap,
                new BeanPropertyRowMapper<Accesslogbydate>(Accesslogbydate.class));

        List<AccesslogFlowDetail> flowDetaiList = new ArrayList<AccesslogFlowDetail>();
        for (Accesslogbydate accessLogbydate : accesslogbydateList) {
            AccesslogFlowDetail flowDetail = assemblyFlowDetail(accessLogbydate, null);
            flowDetail.setBrowser(accessLogbydate.getBrowser());
            flowDetaiList.add(flowDetail);
        }
        GetAccesslogFlowTrendResponse response = new GetAccesslogFlowTrendResponse();
        response.setData(flowDetaiList);
        return response;
    }

    @Override
    public GetAccesslogFlowTrendResponse getRequestMethod(BaseAccesslogRequest baseAccesslogRequest) {
        MapSqlParameterSource paramMap = new MapSqlParameterSource();
        String getListSql = "select request_method as request_method,count(1) as pv from gp_nginx_access t ";
        String where = "";
        where = buildApplicationCodeListFilter(baseAccesslogRequest.getApplicationCode(), paramMap, where);
        where = buildHttpHostFilter(baseAccesslogRequest.getHttpHost(), paramMap, where);
        where = buildStatDateStartFilter(baseAccesslogRequest.getStartTime(), paramMap, where);
        where = buildStatDateEndFilter(baseAccesslogRequest.getEndTime(), paramMap, where);
        if (StringUtils.isNotBlank(where)) {
            getListSql += " where " + where.substring(4);
        }
        getListSql += " group by t.request_method order by pv desc ";
        List<Accesslogbydate> accesslogbydateList = clickHouseJdbcTemplate.query(getListSql, paramMap,
                new BeanPropertyRowMapper<Accesslogbydate>(Accesslogbydate.class));
        Accesslogbydate totalAccessLogbydate = new Accesslogbydate();
        totalAccessLogbydate.setPv(BigDecimal.ZERO);
        for (Accesslogbydate accessLogbydate : accesslogbydateList) {
            totalAccessLogbydate.setPv(accessLogbydate.getPv().add(totalAccessLogbydate.getPv()));
        }
        List<AccesslogFlowDetail> flowDetaiList = new ArrayList<AccesslogFlowDetail>();
        for (Accesslogbydate accessLogbydate : accesslogbydateList) {
            AccesslogFlowDetail flowDetail = assemblyFlowDetail(accessLogbydate, totalAccessLogbydate);
            flowDetail.setRequestMethod(accessLogbydate.getRequestMethod());
            flowDetaiList.add(flowDetail);
        }
        GetAccesslogFlowTrendResponse response = new GetAccesslogFlowTrendResponse();
        response.setData(flowDetaiList);
        return response;
    }

    @Override
    public GetAccesslogFlowTrendResponse getReferrerTop10(BaseAccesslogRequest baseAccesslogRequest) {
        MapSqlParameterSource paramMap = new MapSqlParameterSource();
        String getListSql = "select http_referrer as http_referrer,count(1) as pv from gp_nginx_access t ";
        String getSummarySql = "select count(1) as pv from gp_nginx_access t ";
        String where = "";
        where = buildApplicationCodeListFilter(baseAccesslogRequest.getApplicationCode(), paramMap, where);
        where = buildHttpHostFilter(baseAccesslogRequest.getHttpHost(), paramMap, where);
        where = buildStatDateStartFilter(baseAccesslogRequest.getStartTime(), paramMap, where);
        where = buildStatDateEndFilter(baseAccesslogRequest.getEndTime(), paramMap, where);
        if (StringUtils.isNotBlank(where)) {
            getListSql += " where " + where.substring(4);
            getSummarySql += " where " + where.substring(4);
        }
        getListSql += " group by t.http_referrer order by pv desc limit 10";

        List<Accesslogbydate> accesslogbydateList = clickHouseJdbcTemplate.query(getListSql, paramMap,
                new BeanPropertyRowMapper<Accesslogbydate>(Accesslogbydate.class));
        Accesslogbydate totalAccesslogbydate = clickHouseJdbcTemplate.queryForObject(getSummarySql, paramMap,
                new BeanPropertyRowMapper<Accesslogbydate>(Accesslogbydate.class));
        List<AccesslogFlowDetail> flowDetaiList = new ArrayList<AccesslogFlowDetail>();
        for (Accesslogbydate accessLogbydate : accesslogbydateList) {
            AccesslogFlowDetail flowDetail = assemblyFlowDetail(accessLogbydate, totalAccesslogbydate);
            flowDetail.setHttpReferrer(accessLogbydate.getHttpReferrer());
            flowDetaiList.add(flowDetail);
        }
        GetAccesslogFlowTrendResponse response = new GetAccesslogFlowTrendResponse();
        response.setData(flowDetaiList);
        return response;
    }

    @Override
    public GetAccesslogFlowTrendResponse getFlowTrend(GetAccesslogFlowTrendRequest getFlowTrendRequest) {
        MapSqlParameterSource paramMap = new MapSqlParameterSource();
        String where = "";
        where = buildApplicationCodeListFilter(getFlowTrendRequest.getApplicationCode(), paramMap, where);
        where = buildHttpHostFilter(getFlowTrendRequest.getHttpHost(), paramMap, where);
        where = buildStatDateStartFilter(getFlowTrendRequest.getStartTime(), paramMap, where);
        where = buildStatDateEndFilter(getFlowTrendRequest.getEndTime(), paramMap, where);
        List<AccesslogFlowDetail> accesslogFlowDetailList = null;
        if (StringUtils.isEmpty(getFlowTrendRequest.getTimeType()) || "hour".equals(getFlowTrendRequest.getTimeType())) {
            accesslogFlowDetailList = getFlowTrendByHour(paramMap, where);
        } else {
            accesslogFlowDetailList = getFlowTrendByTimeType(paramMap, where, getFlowTrendRequest);
        }
        GetAccesslogFlowTrendResponse response = new GetAccesslogFlowTrendResponse();
        response.setData(accesslogFlowDetailList);
        return response;
    }

    private List<AccesslogFlowDetail> getFlowTrendByTimeType(MapSqlParameterSource paramMap,
                                                             String where, GetAccesslogFlowTrendRequest getFlowTrendRequest) {
        String getListSql = "select stat_date as stat_date,count(1) as pv,countDistinct(remote_addr) as uv,sum(if(body_sent_bytes ='', 0, toFloat32(body_sent_bytes))) as body_sent_bytes from gp_nginx_access t  where " + where.substring(4) + " group by stat_date order by stat_date asc";
        List<Accesslogbydate> accesslogbydateList = clickHouseJdbcTemplate.query(getListSql, paramMap,
                new BeanPropertyRowMapper<Accesslogbydate>(Accesslogbydate.class));
        Accesslogbydate totalAccesslogbydate = new Accesslogbydate();
        totalAccesslogbydate.setPv(BigDecimal.ZERO);
        totalAccesslogbydate.setUv(BigDecimal.ZERO);
        for (Accesslogbydate accesslogbydate : accesslogbydateList) {
            totalAccesslogbydate.setPv(totalAccesslogbydate.getPv().add(accesslogbydate.getPv()));
//			totalAccesslogbydate.setRequestLength(totalAccesslogbydate.getRequestLength().add(accesslogbydate.getRequestLength()));
//			totalAccesslogbydate.setBodyBytesSent(totalAccesslogbydate.getBodyBytesSent().add(accesslogbydate.getBodyBytesSent()));
            totalAccesslogbydate.setUv(totalAccesslogbydate.getUv().add(accesslogbydate.getUv()));
        }
        List<AccesslogFlowDetail> accesslogDetailList = new ArrayList<AccesslogFlowDetail>();
        Timestamp startTime = transformFilterTime(getFlowTrendRequest.getStartTime(), true, getFlowTrendRequest.getTimeType());
        Timestamp endTime = transformFilterTime(getFlowTrendRequest.getEndTime(), false, getFlowTrendRequest.getTimeType());
        if ("day".equalsIgnoreCase(getFlowTrendRequest.getTimeType())) {
            accesslogDetailList = getFlowTrendByDate(accesslogbydateList, totalAccesslogbydate, startTime, endTime);
        } else if ("week".equalsIgnoreCase(getFlowTrendRequest.getTimeType())) {
            accesslogDetailList = getFlowTrendByWeek(accesslogbydateList, totalAccesslogbydate, startTime, endTime);
        } else if ("month".equalsIgnoreCase(getFlowTrendRequest.getTimeType())) {
            accesslogDetailList = getFlowTrendByMonth(accesslogbydateList, totalAccesslogbydate, startTime, endTime);
        }
        return accesslogDetailList;
    }

    private List<AccesslogFlowDetail> getFlowTrendByHour(MapSqlParameterSource paramMap,
                                                         String where) {
        List<AccesslogFlowDetail> flowDetailList = new ArrayList<>();

        String getHourListSql = "select stat_hour,count(1) as pv,countDistinct(remote_addr) as uv,sum(if(body_sent_bytes ='', 0, toFloat32(body_sent_bytes))) as body_sent_bytes from gp_nginx_access t where " + where.substring(4) + " group by stat_hour order by stat_hour";
        List<Accesslogbydate> flowTrendbyhourList = clickHouseJdbcTemplate.query(getHourListSql, paramMap,
                new BeanPropertyRowMapper<Accesslogbydate>(Accesslogbydate.class));

        for (int i = 0; i < 24; i++) {
            String hour = String.valueOf(100 + i).substring(1);
            AccesslogFlowDetail flowDetail = new AccesslogFlowDetail();
            flowDetail.setStatTime(hour);
            Optional<Accesslogbydate> opFlowTrendbyhour = flowTrendbyhourList.stream()
                    .filter(f -> f.getStatHour().equalsIgnoreCase(hour)).findAny();
            if (opFlowTrendbyhour.isPresent()) {
                Accesslogbydate flowTrendbyhour = opFlowTrendbyhour.get();
                flowDetail.setPv(flowTrendbyhour.getPv());
                flowDetail.setUv(flowTrendbyhour.getUv());
                flowDetail.setBodySentBytes(flowTrendbyhour.getBodySentBytes());
            }
            flowDetailList.add(flowDetail);
        }
        return flowDetailList;
    }

    private List<AccesslogFlowDetail> getFlowTrendByDate(List<Accesslogbydate> flowTrendbydateList,
                                                         Accesslogbydate totalFlowDetail, Timestamp startTime, Timestamp endTime) {
        List<AccesslogFlowDetail> flowDetailList = new ArrayList<>();

        Timestamp tmpTime = startTime;
        do {
            AccesslogFlowDetail flowDetail = new AccesslogFlowDetail();
            flowDetail.setStatTime(this.yMdFORMAT.get().format(tmpTime));
            Timestamp statDate = tmpTime;

            Optional<Accesslogbydate> optionalFlowTrendbydate = flowTrendbydateList.stream()
                    .filter(f -> f.getStatDate().equals(statDate)).findAny();
            if (optionalFlowTrendbydate.isPresent()) {
                Accesslogbydate flowTrendbydate = optionalFlowTrendbydate.get();
                flowDetail.setPv(flowTrendbydate.getPv());
                flowDetail.setUv(flowTrendbydate.getUv());
                flowDetail.setBodySentBytes(flowTrendbydate.getBodySentBytes());
            }
            flowDetailList.add(flowDetail);
            tmpTime = new Timestamp(tmpTime.getTime() + DateUtils.MILLIS_PER_DAY);
        } while (tmpTime.getTime() <= endTime.getTime());
        return flowDetailList;
    }

    private List<AccesslogFlowDetail> getFlowTrendByWeek(List<Accesslogbydate> flowTrendbydateList,
                                                         Accesslogbydate totalFlowDetail, Timestamp startTime, Timestamp endTime) {
        List<AccesslogFlowDetail> flowDetailList = new ArrayList<>();

        Timestamp tmpTime = startTime;
        int j = 0;
        do {
            long[] weekframe = TimeUtils.getCurrentWeekTimeFrame(tmpTime);
            if (weekframe[0] < startTime.getTime()) {
                weekframe[0] = startTime.getTime();
            }
            if (weekframe[1] > endTime.getTime()) {
                weekframe[1] = endTime.getTime();
            }

            AccesslogFlowDetail weekFlowDetail = new AccesslogFlowDetail();
            String statTime = this.yMdFORMAT.get().format(new Timestamp(weekframe[0]));
            if (weekframe[1] != weekframe[0]) {
                statTime += " ~ " + this.yMdFORMAT.get().format(new Timestamp(weekframe[1]));
            }
            weekFlowDetail.setStatTime(statTime);
            weekFlowDetail.setPv(BigDecimal.ZERO);
            weekFlowDetail.setUv(BigDecimal.ZERO);
            weekFlowDetail.setBodySentBytes(BigDecimal.ZERO);
            for (int i = j; i < flowTrendbydateList.size(); i++) {
                Accesslogbydate flowTrendbydate = flowTrendbydateList.get(i);
                Timestamp statDate = flowTrendbydate.getStatDate();
                long lStatDate = statDate.getTime();
                if (lStatDate >= weekframe[0] && lStatDate <= weekframe[1]) {
                    weekFlowDetail.setPv(flowTrendbydate.getPv().add(weekFlowDetail.getPv()));
                    weekFlowDetail.setUv(flowTrendbydate.getUv().add(weekFlowDetail.getUv()));
                    weekFlowDetail.setBodySentBytes(flowTrendbydate.getBodySentBytes().add(weekFlowDetail.getBodySentBytes()));
                    j++;
                }
                if (lStatDate > weekframe[1]) {
                    break;
                }
            }
            flowDetailList.add(weekFlowDetail);
            tmpTime = new Timestamp(weekframe[1] + DateUtils.MILLIS_PER_DAY);
        } while (tmpTime.getTime() <= endTime.getTime());
        return flowDetailList;
    }

    private List<AccesslogFlowDetail> getFlowTrendByMonth(List<Accesslogbydate> flowTrendbydateList,
                                                          Accesslogbydate totalFlowDetail, Timestamp startTime, Timestamp endTime) {
        List<AccesslogFlowDetail> flowDetailList = new ArrayList<>();

        Timestamp tmpTime = startTime;
        int j = 0;
        do {
            long[] monthframe = TimeUtils.getCurrentMonthTimeFrame(tmpTime);
            if (monthframe[0] < startTime.getTime()) {
                monthframe[0] = startTime.getTime();
            }
            if (monthframe[1] > endTime.getTime()) {
                monthframe[1] = endTime.getTime();
            }

            AccesslogFlowDetail monthlowDetail = new AccesslogFlowDetail();
            String statTime = this.yMdFORMAT.get().format(new Timestamp(monthframe[0]));
            if (monthframe[1] != monthframe[0]) {
                statTime += " ~ " + this.yMdFORMAT.get().format(new Timestamp(monthframe[1]));
            }
            monthlowDetail.setStatTime(statTime);
            monthlowDetail.setPv(BigDecimal.ZERO);
            monthlowDetail.setUv(BigDecimal.ZERO);
            monthlowDetail.setBodySentBytes(BigDecimal.ZERO);
            for (int i = j; i < flowTrendbydateList.size(); i++) {
                Accesslogbydate flowTrendbydate = flowTrendbydateList.get(i);
                Timestamp statDate = flowTrendbydate.getStatDate();
                long lStatDate = statDate.getTime();
                if (lStatDate >= monthframe[0] && lStatDate <= monthframe[1]) {
                    monthlowDetail.setPv(flowTrendbydate.getPv().add(monthlowDetail.getPv()));
                    monthlowDetail.setUv(flowTrendbydate.getUv().add(monthlowDetail.getUv()));
                    monthlowDetail.setBodySentBytes(flowTrendbydate.getBodySentBytes().add(monthlowDetail.getBodySentBytes()));
                    j++;
                }
                if (lStatDate > monthframe[1]) {
                    break;
                }
            }
            flowDetailList.add(monthlowDetail);
            tmpTime = new Timestamp(monthframe[1] + DateUtils.MILLIS_PER_DAY);
        } while (tmpTime.getTime() <= endTime.getTime());
        return flowDetailList;
    }


    @Override
    public GetAccesslogPageResponse getExceptionPv(GetAccesslogExceptionPvPageRequest getAccesslogExceptionPvPageRequest) {
        MapSqlParameterSource paramMap = new MapSqlParameterSource();
        String getListSql = "select t2.remote_addr as remote_addr,t2.pv as pv from (SELECT remote_addr as remote_addr,count(1) as pv FROM gp_nginx_access t ";
        String getSummarySql = "select count(1) as pv,countDistinct(remote_addr) as uv from gp_nginx_access t";
        String getCountSql = "SELECT countDistinct(t2.remote_addr) from (SELECT remote_addr as remote_addr,count(1) as pv FROM gp_nginx_access t ";

        String where = "";
        where = buildApplicationCodeListFilter(getAccesslogExceptionPvPageRequest.getApplicationCode(), paramMap, where);
        where = buildHttpHostFilter(getAccesslogExceptionPvPageRequest.getHttpHost(), paramMap, where);
        where = buildStatDateStartFilter(getAccesslogExceptionPvPageRequest.getStartTime(), paramMap, where);
        where = buildStatDateEndFilter(getAccesslogExceptionPvPageRequest.getEndTime(), paramMap, where);
        if (StringUtils.isNotBlank(where)) {
            getSummarySql += " where " + where.substring(4);
            getCountSql += " where " + where.substring(4);
            getListSql += " where " + where.substring(4);
        }
        Accesslogbydate totalAccesslogbydate = clickHouseJdbcTemplate.queryForObject(getSummarySql, paramMap,
                new BeanPropertyRowMapper<Accesslogbydate>(Accesslogbydate.class));
        BigDecimal exceptionOffsetValue = BigDecimal.ZERO;
        BigDecimal exceptionOffset = getAccesslogExceptionPvPageRequest.getExceptionOffset() != null ? getAccesslogExceptionPvPageRequest.getExceptionOffset().divide(new BigDecimal(100)) : new BigDecimal(Constants.DEFAULT_EXCEPTION_PV_OFFSET).divide(new BigDecimal(100));
        if (totalAccesslogbydate != null && totalAccesslogbydate.getPv() != null && totalAccesslogbydate.getPv().compareTo(BigDecimal.ZERO) > 0) {
            exceptionOffsetValue = totalAccesslogbydate.getPv().divide(totalAccesslogbydate.getUv(), 5, RoundingMode.DOWN);
            exceptionOffsetValue = exceptionOffsetValue.add(exceptionOffsetValue.multiply(exceptionOffset));
        }
        getListSql += " GROUP BY remote_addr) t2 ";
        getCountSql += " GROUP BY remote_addr) t2 ";
        if (exceptionOffset.compareTo(BigDecimal.ZERO) > 0) {
            getListSql += " where t2.pv > :exception_offset";
            getCountSql += " where t2.pv > :exception_offset";
            paramMap.addValue("exception_offset", exceptionOffsetValue);
        }
        getListSql += " order by t2.pv desc limit " + (getAccesslogExceptionPvPageRequest.getPageNum() - 1) * getAccesslogExceptionPvPageRequest.getPageSize() + "," + getAccesslogExceptionPvPageRequest.getPageSize();
        List<Accesslogbydate> accesslogbydateList = clickHouseJdbcTemplate.query(getListSql, paramMap,
                new BeanPropertyRowMapper<Accesslogbydate>(Accesslogbydate.class));

        Integer total = clickHouseJdbcTemplate.queryForObject(getCountSql, paramMap, Integer.class);
        List<AccesslogFlowDetail> accesslogDetailList = new ArrayList<>();

        GetAccesslogPageResponse response = new GetAccesslogPageResponse();
        GetAccesslogPageResponseData responseData = new GetAccesslogPageResponseData();
        for (Accesslogbydate accesslogbydate : accesslogbydateList) {
            AccesslogFlowDetail accesslogFlowDetail = assemblyFlowDetail(accesslogbydate, null);
            accesslogFlowDetail.setIp(accesslogbydate.getRemoteAddr());
            accesslogDetailList.add(accesslogFlowDetail);
        }
        responseData.setRows(accesslogDetailList);
        responseData.setTotal(total);
        response.setData(responseData);
        return response;
    }

    @Override
    public GetAccesslogPageResponse getExceptionIp(GetAccesslogExceptionPvPageRequest getAccesslogExceptionPvPageRequest) {
        MapSqlParameterSource paramMap = new MapSqlParameterSource();
        String getListSql = "select t2.province as province,t2.remote_addr as remote_addr,t2.pv as pv from (SELECT province as province,remote_addr as remote_addr,count(1) as pv FROM gp_nginx_access t ";
        String getSummarySql = "select count(1) as pv,countDistinct(remote_addr) as uv from gp_nginx_access t";
        String getCountSql = "SELECT countDistinct(t2.remote_addr) from (SELECT province as province,remote_addr as remote_addr,count(1) as pv FROM gp_nginx_access t ";

        String where = "";
        where = buildApplicationCodeListFilter(getAccesslogExceptionPvPageRequest.getApplicationCode(), paramMap, where);
        where = buildHttpHostFilter(getAccesslogExceptionPvPageRequest.getHttpHost(), paramMap, where);
        where = buildStatDateStartFilter(getAccesslogExceptionPvPageRequest.getStartTime(), paramMap, where);
        where = buildStatDateEndFilter(getAccesslogExceptionPvPageRequest.getEndTime(), paramMap, where);
        if (StringUtils.isNotBlank(where)) {
            getSummarySql += " where t.province='" + Constants.DEFAULT_EXCEPTION_PROVINCE_OFFSET + "' " + where;
            getCountSql += " where t.province <> '" + Constants.DEFAULT_EXCEPTION_PROVINCE_OFFSET + "'" + where;
            getListSql += " where t.province <> '" + Constants.DEFAULT_EXCEPTION_PROVINCE_OFFSET + "'" + where;
        }
        Accesslogbydate totalAccesslogbydate = clickHouseJdbcTemplate.queryForObject(getSummarySql, paramMap,
                new BeanPropertyRowMapper<Accesslogbydate>(Accesslogbydate.class));
        BigDecimal exceptionOffsetValue = BigDecimal.ZERO;
        BigDecimal exceptionOffset = getAccesslogExceptionPvPageRequest.getExceptionOffset() != null ? getAccesslogExceptionPvPageRequest.getExceptionOffset().divide(new BigDecimal(100)) : new BigDecimal(Constants.DEFAULT_EXCEPTION_IP_PV_OFFSET).divide(new BigDecimal(100));
        if (totalAccesslogbydate != null && totalAccesslogbydate.getPv() != null && totalAccesslogbydate.getPv().compareTo(BigDecimal.ZERO) > 0) {
            exceptionOffsetValue = totalAccesslogbydate.getPv().multiply(exceptionOffset);
        }
        getListSql += " GROUP BY province,remote_addr) t2 ";
        getCountSql += " GROUP BY province,remote_addr) t2 ";
        if (exceptionOffsetValue.compareTo(BigDecimal.ZERO) > 0) {
            getListSql += " where t2.pv > :exception_offset";
            getCountSql += " where t2.pv > :exception_offset";
            paramMap.addValue("exception_offset", exceptionOffsetValue);
        }
        getListSql += " order by t2.pv desc limit " + (getAccesslogExceptionPvPageRequest.getPageNum() - 1) * getAccesslogExceptionPvPageRequest.getPageSize() + "," + getAccesslogExceptionPvPageRequest.getPageSize();
        List<Accesslogbydate> accesslogbydateList = clickHouseJdbcTemplate.query(getListSql, paramMap,
                new BeanPropertyRowMapper<Accesslogbydate>(Accesslogbydate.class));

        Integer total = clickHouseJdbcTemplate.queryForObject(getCountSql, paramMap, Integer.class);
        List<AccesslogFlowDetail> accesslogDetailList = new ArrayList<>();

        GetAccesslogPageResponse response = new GetAccesslogPageResponse();
        GetAccesslogPageResponseData responseData = new GetAccesslogPageResponseData();
        for (Accesslogbydate accesslogbydate : accesslogbydateList) {
            AccesslogFlowDetail accesslogFlowDetail = assemblyFlowDetail(accesslogbydate, null);
            accesslogFlowDetail.setIp(accesslogbydate.getRemoteAddr());
            accesslogFlowDetail.setProvince(accesslogbydate.getProvince());
            accesslogDetailList.add(accesslogFlowDetail);
        }
        responseData.setRows(accesslogDetailList);
        responseData.setTotal(total);
        response.setData(responseData);
        return response;
    }


    @Override
    public GetAccesslogPageResponse getExceptionStatus(GetAccesslogStatusPageRequest getAccesslogStatusPageRequest) {
        MapSqlParameterSource paramMap = new MapSqlParameterSource();
        String getListSql = "SELECT uri as uri,count(1) as pv FROM gp_nginx_access t";
        String getCountSql = "SELECT countDistinct(uri) FROM gp_nginx_access t ";

        String where = "";
        where = buildApplicationCodeListFilter(getAccesslogStatusPageRequest.getApplicationCode(), paramMap, where);
        where = buildStatusFilter(getAccesslogStatusPageRequest.getStatus(), paramMap, where);
        where = buildHttpHostFilter(getAccesslogStatusPageRequest.getHttpHost(), paramMap, where);
        where = buildStatDateStartFilter(getAccesslogStatusPageRequest.getStartTime(), paramMap, where);
        where = buildStatDateEndFilter(getAccesslogStatusPageRequest.getEndTime(), paramMap, where);
        if (StringUtils.isNotBlank(where)) {
            getCountSql += " where " + where.substring(4);
            getListSql += " where " + where.substring(4);
        }

        getListSql += " GROUP BY uri ";
        getListSql += " order by pv desc limit " + (getAccesslogStatusPageRequest.getPageNum() - 1) * getAccesslogStatusPageRequest.getPageSize() + "," + getAccesslogStatusPageRequest.getPageSize();
        List<Accesslogbydate> accesslogbydateList = clickHouseJdbcTemplate.query(getListSql, paramMap,
                new BeanPropertyRowMapper<Accesslogbydate>(Accesslogbydate.class));

        Integer total = clickHouseJdbcTemplate.queryForObject(getCountSql, paramMap, Integer.class);
        List<AccesslogFlowDetail> accesslogDetailList = new ArrayList<>();

        GetAccesslogPageResponse response = new GetAccesslogPageResponse();
        GetAccesslogPageResponseData responseData = new GetAccesslogPageResponseData();
        for (Accesslogbydate accesslogbydate : accesslogbydateList) {
            AccesslogFlowDetail accesslogFlowDetail = assemblyFlowDetail(accesslogbydate, null);
            accesslogFlowDetail.setUri(accesslogbydate.getUri());
            accesslogDetailList.add(accesslogFlowDetail);
        }
        responseData.setRows(accesslogDetailList);
        responseData.setTotal(total);
        response.setData(responseData);
        return response;
    }


    @Override
    public GetAccesslogPageResponse getPerformanceDetail(GetAccesslogStatusPageRequest getAccesslogStatusPageRequest) {
        MapSqlParameterSource paramMap = new MapSqlParameterSource();
        String getListSql = "SELECT t2.http_host as http_host,t2.uri as uri, t2.pv as pv, t2.slow_pv as slow_pv, t2.max_visit_time as max_visit_time, t2.avg_visit_time as avg_visit_time FROM ( ";
        String getSonSql1 = "SELECT http_host as http_host,uri as uri, sum(if(request_time >= 1000, 1, 0)) AS slow_pv,count(1) AS pv,avg(request_time) AS avg_visit_time , max(request_time) AS max_visit_time FROM gp_nginx_access t ";
        String getCountSql = "SELECT count(1) FROM ( ";
        String where = "";
        where = buildApplicationCodeListFilter(getAccesslogStatusPageRequest.getApplicationCode(), paramMap, where);
        where = buildHttpHostFilter(getAccesslogStatusPageRequest.getHttpHost(), paramMap, where);
        where = buildStatDateStartFilter(getAccesslogStatusPageRequest.getStartTime(), paramMap, where);
        where = buildStatDateEndFilter(getAccesslogStatusPageRequest.getEndTime(), paramMap, where);
        where = buildStatusFilter(getAccesslogStatusPageRequest.getStatus(), paramMap, where);
        where = buildLimitFilter(getAccesslogStatusPageRequest.getLimits(), paramMap, where);
        if (StringUtils.isNotBlank(where)) {
            getSonSql1 += " where " + where.substring(4);
        }
        getSonSql1 += " GROUP BY http_host,uri " + getSortSqlFormat(getAccesslogStatusPageRequest.getSortName(), getAccesslogStatusPageRequest.getSortOrder(), "slow_pv");
        getListSql += getSonSql1 + ") t2 ";
        getCountSql += getSonSql1 + ") t2 ";
        if (getAccesslogStatusPageRequest.getIsOverOneSecond() != null && getAccesslogStatusPageRequest.getIsOverOneSecond()) {
            getListSql += "where t2.slow_pv > 0";
            getCountSql += "where t2.slow_pv > 0";
        }
        getListSql += " limit " + (getAccesslogStatusPageRequest.getPageNum() - 1) * getAccesslogStatusPageRequest.getPageSize() + "," + getAccesslogStatusPageRequest.getPageSize();
        List<Accesslogbydate> accesslogbydateList = clickHouseJdbcTemplate.query(getListSql, paramMap,
                new BeanPropertyRowMapper<Accesslogbydate>(Accesslogbydate.class));
        Integer total = clickHouseJdbcTemplate.queryForObject(getCountSql, paramMap, Integer.class);
        List<AccesslogFlowDetail> accesslogDetailList = new ArrayList<>();

        GetAccesslogPageResponse response = new GetAccesslogPageResponse();
        GetAccesslogPageResponseData responseData = new GetAccesslogPageResponseData();
        for (Accesslogbydate accesslogbydate : accesslogbydateList) {
            AccesslogFlowDetail accesslogFlowDetail = assemblyFlowDetail(accesslogbydate, null);
            accesslogFlowDetail.setHttpHost(accesslogbydate.getHttpHost());
            accesslogFlowDetail.setUri(accesslogbydate.getUri());
            accesslogFlowDetail.setSlowPv(accesslogbydate.getSlowPv());
            accesslogFlowDetail.setMaxVisitTime(accesslogbydate.getMaxVisitTime() != null ? accesslogbydate.getMaxVisitTime().setScale(4, RoundingMode.DOWN) : BigDecimal.ZERO);
            if (accesslogbydate.getPv() != null && accesslogbydate.getPv().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal pvRate = accesslogbydate.getSlowPv().divide(accesslogbydate.getPv(), 5, RoundingMode.DOWN);
                accesslogFlowDetail.setPvRate(pvRate);
            }
            accesslogDetailList.add(accesslogFlowDetail);
        }
        responseData.setRows(accesslogDetailList);
        responseData.setTotal(total);
        response.setData(responseData);
        return response;
    }

    @Override
    public GetAccesslogRequestimeGt100msResponse getRequestimeGt100ms(GetAccesslogRequestimeGt100msRequest getAccesslogRequestimeGt100msRequest) {
        if (StringUtils.equalsIgnoreCase(getAccesslogRequestimeGt100msRequest.getTimeType(), "min")) {
            return getRequestimeGt100msByMin(getAccesslogRequestimeGt100msRequest);
        }
        MapSqlParameterSource paramMap = new MapSqlParameterSource();
        String getListSql = "select uri as uri,count(1) as pv from gp_nginx_access t ";
        String getStatHourListSql = "select stat_hour as stat_hour,uri as uri,count(1) as pv from gp_nginx_access t ";
        String where = "";
        where = buildApplicationCodeListFilter(getAccesslogRequestimeGt100msRequest.getApplicationCode(), paramMap, where);
        where = buildHttpHostFilter(getAccesslogRequestimeGt100msRequest.getHttpHost(), paramMap, where);
        where = buildStatDateStartFilter(getAccesslogRequestimeGt100msRequest.getStartTime(), paramMap, where);
        where = buildStatDateEndFilter(getAccesslogRequestimeGt100msRequest.getEndTime(), paramMap, where);
        if (StringUtils.isNotBlank(where)) {
            getListSql += " where request_time>100 " + where;
            getStatHourListSql += " where request_time>100 " + where;
        }
        getListSql += " group by t.uri order by pv desc limit 19";
        getStatHourListSql += " group by t.stat_hour,t.uri order by stat_hour desc";
        List<Accesslogbydate> accesslogbydateList = clickHouseJdbcTemplate.query(getListSql, paramMap,
                new BeanPropertyRowMapper<Accesslogbydate>(Accesslogbydate.class));

        List<Accesslogbydate> accesslogbydateStatHourList = clickHouseJdbcTemplate.query(getStatHourListSql, paramMap,
                new BeanPropertyRowMapper<Accesslogbydate>(Accesslogbydate.class));
        List<GetAccesslogRequestimeGt100msData> rows = new ArrayList<GetAccesslogRequestimeGt100msData>();

        List<String> uris = new ArrayList<String>();
        for (int i = 0; i < accesslogbydateList.size(); i++) {
            Accesslogbydate accesslogbydate = accesslogbydateList.get(i);
            uris.add(accesslogbydate.getUri());
        }
        List<BigDecimal> otherPvList = new ArrayList<BigDecimal>();
        int i = 0;
        List<String> statHours = new ArrayList<String>();
        for (String uri : uris) {
            GetAccesslogRequestimeGt100msData getAccesslogRequestimeGt100msData = new GetAccesslogRequestimeGt100msData();
            getAccesslogRequestimeGt100msData.setUri(uri);
            List<BigDecimal> pvList = new ArrayList<BigDecimal>();
            for (int h = 0; h < 24; h++) {
                BigDecimal pv = BigDecimal.ZERO;
                BigDecimal otherPv = BigDecimal.ZERO;
                String hour = String.valueOf(100 + h).substring(1);
                if (i == 0) {
                    statHours.add(hour);
                }
                for (Accesslogbydate opFlowTrendbyhour : accesslogbydateStatHourList) {
                    if (!StringUtils.equalsIgnoreCase(opFlowTrendbyhour.getStatHour(), hour)) {
                        continue;
                    }
                    if (StringUtils.equalsIgnoreCase(opFlowTrendbyhour.getUri(), uris.get(i))) {
                        pv = pv.add(opFlowTrendbyhour.getPv());
                    }
                    if (i == 0 && !uris.contains(opFlowTrendbyhour.getUri())) {
                        otherPv = otherPv.add(opFlowTrendbyhour.getPv());
                    }
                }
                pvList.add(pv);
                if (i == 0) {
                    otherPvList.add(otherPv);
                }
            }
            i++;
            getAccesslogRequestimeGt100msData.setPv(pvList);
            rows.add(getAccesslogRequestimeGt100msData);
        }
        GetAccesslogRequestimeGt100msData getAccesslogRequestimeGt100msData = new GetAccesslogRequestimeGt100msData();
        getAccesslogRequestimeGt100msData.setUri("Other");
        getAccesslogRequestimeGt100msData.setPv(otherPvList);
        rows.add(getAccesslogRequestimeGt100msData);

        GetAccesslogRequestimeGt100msResponse response = new GetAccesslogRequestimeGt100msResponse();
        GetAccesslogRequestimeGt100msResponseData responseData = new GetAccesslogRequestimeGt100msResponseData();
        responseData.setRows(rows);
        responseData.setTimes(statHours);
        response.setData(responseData);
        return response;
    }


    public GetAccesslogRequestimeGt100msResponse getRequestimeGt100msByMin(GetAccesslogRequestimeGt100msRequest getAccesslogRequestimeGt100msRequest) {
        MapSqlParameterSource paramMap = new MapSqlParameterSource();
        String getListSql = "select uri as uri,count(1) as pv from gp_nginx_access t ";
        String getStatMinListSql = "select right(stat_min,2) as stat_min,uri as uri,count(1) as pv from gp_nginx_access t ";
        String where = "";
        where = buildApplicationCodeListFilter(getAccesslogRequestimeGt100msRequest.getApplicationCode(), paramMap, where);
        where = buildHttpHostFilter(getAccesslogRequestimeGt100msRequest.getHttpHost(), paramMap, where);
        where = buildStatDateStartFilter(getAccesslogRequestimeGt100msRequest.getStartTime(), paramMap, where);
        where = buildStatDateEndFilter(getAccesslogRequestimeGt100msRequest.getEndTime(), paramMap, where);
        where = buildStatHourFilter(getAccesslogRequestimeGt100msRequest.getHour(), paramMap, where);
        if (StringUtils.isNotBlank(where)) {
            getListSql += " where request_time>100  " + where;
            getStatMinListSql += " where request_time>100 " + where;
        }
        getListSql += " group by t.uri order by pv desc limit 19";
        getStatMinListSql += " group by stat_min,t.uri order by stat_min desc";
        List<Accesslogbydate> accesslogbydateList = clickHouseJdbcTemplate.query(getListSql, paramMap,
                new BeanPropertyRowMapper<Accesslogbydate>(Accesslogbydate.class));
        List<Accesslogbydate> accesslogbydateStatHourList = clickHouseJdbcTemplate.query(getStatMinListSql, paramMap,
                new BeanPropertyRowMapper<Accesslogbydate>(Accesslogbydate.class));
        List<GetAccesslogRequestimeGt100msData> rows = new ArrayList<GetAccesslogRequestimeGt100msData>();

        List<String> uris = new ArrayList<String>();
        for (int i = 0; i < accesslogbydateList.size(); i++) {
            Accesslogbydate accesslogbydate = accesslogbydateList.get(i);
            uris.add(accesslogbydate.getUri());
        }
        List<BigDecimal> otherPvList = new ArrayList<BigDecimal>();
        int i = 0;
        List<String> statMins = new ArrayList<String>();
        for (String uri : uris) {
            GetAccesslogRequestimeGt100msData getAccesslogRequestimeGt100msData = new GetAccesslogRequestimeGt100msData();
            getAccesslogRequestimeGt100msData.setUri(uri);
            List<BigDecimal> pvList = new ArrayList<BigDecimal>();
            for (int m = 0; m < 60; m++) {
                BigDecimal pv = BigDecimal.ZERO;
                BigDecimal otherPv = BigDecimal.ZERO;
                String statMin = String.valueOf(100 + m).substring(1);
                if (i == 0) {
                    statMins.add(statMin);
                }
                for (Accesslogbydate opFlowTrendbyhour : accesslogbydateStatHourList) {
                    String minute = opFlowTrendbyhour.getStatMin();
                    if (!StringUtils.equalsIgnoreCase(statMin, minute)) {
                        continue;
                    }
                    if (StringUtils.equalsIgnoreCase(opFlowTrendbyhour.getUri(), uris.get(i))) {
                        pv = pv.add(opFlowTrendbyhour.getPv());
                    }
                    if (i == 0 && !uris.contains(opFlowTrendbyhour.getUri())) {
                        otherPv = otherPv.add(opFlowTrendbyhour.getPv());
                    }
                }
                pvList.add(pv);
                if (i == 0) {
                    otherPvList.add(otherPv);
                }
            }
            i++;
            getAccesslogRequestimeGt100msData.setPv(pvList);
            rows.add(getAccesslogRequestimeGt100msData);
        }
        GetAccesslogRequestimeGt100msData getAccesslogRequestimeGt100msData = new GetAccesslogRequestimeGt100msData();
        getAccesslogRequestimeGt100msData.setUri("Other");
        getAccesslogRequestimeGt100msData.setPv(otherPvList);
        rows.add(getAccesslogRequestimeGt100msData);

        GetAccesslogRequestimeGt100msResponse response = new GetAccesslogRequestimeGt100msResponse();
        GetAccesslogRequestimeGt100msResponseData responseData = new GetAccesslogRequestimeGt100msResponseData();
        responseData.setRows(rows);
        responseData.setTimes(statMins);
        response.setData(responseData);
        return response;
    }

    @Override
    public GetAccesslogPageResponse getUriList(GetAccesslogStatusPageRequest getAccesslogStatusPageRequest) {
        MapSqlParameterSource paramMap = new MapSqlParameterSource();
        String getListSql = "SELECT t2.http_host as http_host,t2.uri as uri, t2.pv as pv, t2.slow_pv as slow_pv, t2.max_visit_time as max_visit_time, t2.avg_visit_time as avg_visit_time FROM ( ";
        String getSonSql1 = "SELECT http_host as http_host,uri as uri, sum(if(request_time >= 1000, 1, 0)) AS slow_pv,count(1) AS pv,avg(request_time) AS avg_visit_time , max(request_time) AS max_visit_time FROM gp_nginx_access t ";
        String getCountSql = "SELECT count(1) FROM ( ";
        String where = "";
        where = buildApplicationCodeListFilter(getAccesslogStatusPageRequest.getApplicationCode(), paramMap, where);
        where = buildHttpHostFilter(getAccesslogStatusPageRequest.getHttpHost(), paramMap, where);
        where = buildStatDateStartFilter(getAccesslogStatusPageRequest.getStartTime(), paramMap, where);
        where = buildStatDateEndFilter(getAccesslogStatusPageRequest.getEndTime(), paramMap, where);
        where = buildStatusFilter(getAccesslogStatusPageRequest.getStatus(), paramMap, where);
        where = buildLimitFilter(getAccesslogStatusPageRequest.getLimits(), paramMap, where);
        if (StringUtils.isNotBlank(where)) {
            getSonSql1 += " where " + where.substring(4);
        }
        getSonSql1 += " GROUP BY http_host,uri " + getSortSqlFormat(getAccesslogStatusPageRequest.getSortName(), getAccesslogStatusPageRequest.getSortOrder(), "slow_pv");
        getListSql += getSonSql1 + ") t2 ";
        getCountSql += getSonSql1 + ") t2 ";
        if (getAccesslogStatusPageRequest.getIsOverOneSecond() != null && getAccesslogStatusPageRequest.getIsOverOneSecond()) {
            getListSql += "where t2.slow_pv > 0";
            getCountSql += "where t2.slow_pv > 0";
        }
        getListSql += " limit " + (getAccesslogStatusPageRequest.getPageNum() - 1) * getAccesslogStatusPageRequest.getPageSize() + "," + getAccesslogStatusPageRequest.getPageSize();
        List<Accesslogbydate> accesslogbydateList = clickHouseJdbcTemplate.query(getListSql, paramMap,
                new BeanPropertyRowMapper<Accesslogbydate>(Accesslogbydate.class));
        Integer total = clickHouseJdbcTemplate.queryForObject(getCountSql, paramMap, Integer.class);
        List<AccesslogFlowDetail> accesslogDetailList = new ArrayList<>();

        GetAccesslogPageResponse response = new GetAccesslogPageResponse();
        GetAccesslogPageResponseData responseData = new GetAccesslogPageResponseData();
        for (Accesslogbydate accesslogbydate : accesslogbydateList) {
            AccesslogFlowDetail accesslogFlowDetail = assemblyFlowDetail(accesslogbydate, null);
            accesslogFlowDetail.setHttpHost(accesslogbydate.getHttpHost());
            accesslogFlowDetail.setUri(accesslogbydate.getUri());
            accesslogFlowDetail.setSlowPv(accesslogbydate.getSlowPv());
            accesslogFlowDetail.setMaxVisitTime(accesslogbydate.getMaxVisitTime() != null ? accesslogbydate.getMaxVisitTime().setScale(4, RoundingMode.DOWN) : BigDecimal.ZERO);
            if (accesslogbydate.getPv() != null && accesslogbydate.getPv().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal pvRate = accesslogbydate.getSlowPv().divide(accesslogbydate.getPv(), 5, RoundingMode.DOWN);
                accesslogFlowDetail.setPvRate(pvRate);
            }
            accesslogDetailList.add(accesslogFlowDetail);
        }
        responseData.setRows(accesslogDetailList);
        responseData.setTotal(total);
        response.setData(responseData);
        return response;
    }

    @Override
    public GetAccesslogPageResponse getIpList(GetAccesslogIpPageRequest getAccesslogIpPageRequest) {
        MapSqlParameterSource paramMap = new MapSqlParameterSource();
//        String getListSql = "select t.remote_addr as remote_addr,count(1) as pv,t.country as country,t.province as province,t.city as city," +
//                " groupArrayDistinct(3)(t.http_user_agent) AS http_user_agent," +
//                " groupArrayDistinct(3)(t.uri) AS uri" +
//                " from gp_nginx_access t ";
        String getListSql = "select t.remote_addr as remote_addr,count(1) as pv,t.country as country,t.province as province,t.city as city" +
                " from gp_nginx_access t ";
        String getCountSql = "SELECT countDistinct(t.remote_addr,t.country,t.province,t.city) FROM gp_nginx_access t ";

        String where = "";
        where = buildApplicationCodeListFilter(getAccesslogIpPageRequest.getApplicationCode(), paramMap, where);
        where = buildHttpHostFilter(getAccesslogIpPageRequest.getHttpHost(), paramMap, where);
        where = buildStatDateStartFilter(getAccesslogIpPageRequest.getStartTime(), paramMap, where);
        where = buildStatDateEndFilter(getAccesslogIpPageRequest.getEndTime(), paramMap, where);
        where = buildCountryFilter(getAccesslogIpPageRequest.getCountry(), paramMap, where);
        where = buildProvinceFilter(getAccesslogIpPageRequest.getProvince(), paramMap, where);
        where = buildCityFilter(getAccesslogIpPageRequest.getCity(), paramMap, where);
        where = buildRemoteAddrFilter(getAccesslogIpPageRequest.getIp(), paramMap, where);

        if (StringUtils.isNotBlank(where)) {
            getListSql += " where " + where.substring(4);
            getCountSql += " where " + where.substring(4);
        }
        getListSql += " group by t.remote_addr,t.country,t.province,t.city " + getSortSqlFormat(getAccesslogIpPageRequest.getSortName(), getAccesslogIpPageRequest.getSortOrder(), "pv");
        getListSql += " limit " + (getAccesslogIpPageRequest.getPageNum() - 1) * getAccesslogIpPageRequest.getPageSize() + "," + getAccesslogIpPageRequest.getPageSize();

        List<Accesslogbydate> accesslogbydateList = clickHouseJdbcTemplate.query(getListSql, paramMap,
                new BeanPropertyRowMapper<Accesslogbydate>(Accesslogbydate.class));
        Integer total = clickHouseJdbcTemplate.queryForObject(getCountSql, paramMap, Integer.class);
        List<AccesslogFlowDetail> accesslogDetailList = new ArrayList<>();

        GetAccesslogPageResponse response = new GetAccesslogPageResponse();
        GetAccesslogPageResponseData responseData = new GetAccesslogPageResponseData();
        for (Accesslogbydate accesslogbydate : accesslogbydateList) {
            AccesslogFlowDetail accesslogFlowDetail = assemblyFlowDetail(accesslogbydate, null);
            accesslogFlowDetail.setCountry(accesslogbydate.getCountry());
            accesslogFlowDetail.setProvince(accesslogbydate.getProvince());
            accesslogFlowDetail.setCity(accesslogbydate.getCity());
            accesslogFlowDetail.setIp(accesslogbydate.getRemoteAddr());
            accesslogFlowDetail.setHttpUserAgent(accesslogbydate.getHttpUserAgent());
            accesslogFlowDetail.setUri(accesslogbydate.getUri());
            accesslogDetailList.add(accesslogFlowDetail);
        }
        responseData.setRows(accesslogDetailList);
        responseData.setTotal(total);
        response.setData(responseData);
        return response;
    }

    @Override
    public GetAccesslogIpDetailPageResponse getIpDetailList(GetAccesslogIpDetailPageRequest getAccesslogIpDetailPageRequest) {
        MapSqlParameterSource paramMap = new MapSqlParameterSource();
        String getListSql = "SELECT t2.remote_addr as remote_addr,t2.uri as uri, t2.pv as pv, t2.slow_pv as slow_pv, t2.max_visit_time as max_visit_time, t2.avg_visit_time as avg_visit_time FROM ( ";
        String getSonSql1 = "SELECT remote_addr as remote_addr,uri as uri, sum(if(request_time >= 1000, 1, 0)) AS slow_pv,count(1) AS pv,avg(request_time) AS avg_visit_time , max(request_time) AS max_visit_time FROM gp_nginx_access t ";
        String getCountSql = "SELECT count(1) FROM ( ";
        String where = "";
        where = buildApplicationCodeListFilter(getAccesslogIpDetailPageRequest.getApplicationCode(), paramMap, where);
        where = buildHttpHostFilter(getAccesslogIpDetailPageRequest.getHttpHost(), paramMap, where);
        where = buildStatDateStartFilter(getAccesslogIpDetailPageRequest.getStartTime(), paramMap, where);
        where = buildStatDateEndFilter(getAccesslogIpDetailPageRequest.getEndTime(), paramMap, where);
        where = buildStatusFilter(getAccesslogIpDetailPageRequest.getStatus(), paramMap, where);
        where = buildLimitFilter(getAccesslogIpDetailPageRequest.getLimits(), paramMap, where);
        where = buildRemoteAddrFilter(getAccesslogIpDetailPageRequest.getIp(), paramMap, where);
        if (StringUtils.isNotBlank(where)) {
            getSonSql1 += " where " + where.substring(4);
        }
        getSonSql1 += " GROUP BY remote_addr,uri " + getSortSqlFormat(getAccesslogIpDetailPageRequest.getSortName(), getAccesslogIpDetailPageRequest.getSortOrder(), "pv");
        getListSql += getSonSql1 + ") t2 ";
        getCountSql += getSonSql1 + ") t2 ";
        if (getAccesslogIpDetailPageRequest.getIsOverOneSecond() != null && getAccesslogIpDetailPageRequest.getIsOverOneSecond()) {
            getListSql += "where t2.slow_pv > 0";
            getCountSql += "where t2.slow_pv > 0";
        }
        getListSql += " limit " + (getAccesslogIpDetailPageRequest.getPageNum() - 1) * getAccesslogIpDetailPageRequest.getPageSize() + "," + getAccesslogIpDetailPageRequest.getPageSize();
        List<Accesslogbydate> accesslogbydateList = clickHouseJdbcTemplate.query(getListSql, paramMap,
                new BeanPropertyRowMapper<Accesslogbydate>(Accesslogbydate.class));
        Integer total = clickHouseJdbcTemplate.queryForObject(getCountSql, paramMap, Integer.class);
        List<AccesslogFlowDetail> accesslogDetailList = new ArrayList<>();

        GetAccesslogIpDetailPageResponse response = new GetAccesslogIpDetailPageResponse();
        GetAccesslogIpDetailPageResponseData responseData = new GetAccesslogIpDetailPageResponseData();
        for (Accesslogbydate accesslogbydate : accesslogbydateList) {
            AccesslogFlowDetail accesslogFlowDetail = assemblyFlowDetail(accesslogbydate, null);
            accesslogFlowDetail.setHttpHost(accesslogbydate.getHttpHost());
            accesslogFlowDetail.setUri(accesslogbydate.getUri());
            accesslogFlowDetail.setHttpUserAgent(accesslogbydate.getHttpUserAgent());
            accesslogFlowDetail.setSlowPv(accesslogbydate.getSlowPv());
            accesslogFlowDetail.setMaxVisitTime(accesslogbydate.getMaxVisitTime() != null ? accesslogbydate.getMaxVisitTime().setScale(4, RoundingMode.DOWN) : BigDecimal.ZERO);
            if (accesslogbydate.getPv() != null && accesslogbydate.getPv().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal pvRate = accesslogbydate.getSlowPv().divide(accesslogbydate.getPv(), 5, RoundingMode.DOWN);
                accesslogFlowDetail.setPvRate(pvRate);
            }
            accesslogDetailList.add(accesslogFlowDetail);
        }
        responseData.setRows(accesslogDetailList);
        responseData.setTotal(total);
        response.setData(responseData);
        return response;
    }

    private AccesslogFlowDetail assemblyFlowDetail(Accesslogbydate baseDetailbydate, Accesslogbydate totalBaseDetailbydate) {
        AccesslogFlowDetail accesslogFlowDetail = new AccesslogFlowDetail();
//		accesslogFlowDetail.setVisitTime(baseDetailbydate.getVisitTime() != null ? baseDetailbydate.getVisitTime().setScale(5,RoundingMode.DOWN) : BigDecimal.ZERO);
        accesslogFlowDetail.setPv(baseDetailbydate.getPv());
        accesslogFlowDetail.setUv(baseDetailbydate.getUv());
        accesslogFlowDetail.setIpCount(baseDetailbydate.getUv());
        accesslogFlowDetail.setBodySentBytes(baseDetailbydate.getBodySentBytes());
        accesslogFlowDetail.setUriCount(baseDetailbydate.getUriCount());
        accesslogFlowDetail.setAvgVisitTime((baseDetailbydate.getAvgVisitTime() != null && !StringUtils.equalsIgnoreCase("nan", baseDetailbydate.getAvgVisitTime())) ? new BigDecimal(baseDetailbydate.getAvgVisitTime()).setScale(3, RoundingMode.DOWN) : BigDecimal.ZERO);
        if (totalBaseDetailbydate != null) {
            if (totalBaseDetailbydate.getPv() != null && totalBaseDetailbydate.getPv().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal pvRate = baseDetailbydate.getPv() == null ? BigDecimal.ZERO : baseDetailbydate.getPv().divide(totalBaseDetailbydate.getPv(), 5, RoundingMode.DOWN);
                accesslogFlowDetail.setPvRate(pvRate);
            }
            if (totalBaseDetailbydate.getUv() != null && totalBaseDetailbydate.getUv().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal uvRate = baseDetailbydate.getUv() == null ? BigDecimal.ZERO : baseDetailbydate.getUv().divide(totalBaseDetailbydate.getUv(), 5, RoundingMode.DOWN);
                accesslogFlowDetail.setUvRate(uvRate);
            }
            if (totalBaseDetailbydate.getIpCount() != null && totalBaseDetailbydate.getIpCount().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal ipCountRate = baseDetailbydate.getIpCount() == null ? BigDecimal.ZERO : baseDetailbydate.getIpCount().divide(totalBaseDetailbydate.getIpCount(), 5, RoundingMode.DOWN);
                accesslogFlowDetail.setIpCountRate(ipCountRate);
            }
            if (totalBaseDetailbydate.getBodySentBytes() != null && totalBaseDetailbydate.getBodySentBytes().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal bodySentBytesRate = baseDetailbydate.getBodySentBytes() == null ? BigDecimal.ZERO : baseDetailbydate.getBodySentBytes().divide(totalBaseDetailbydate.getBodySentBytes(), 5, RoundingMode.DOWN);
                accesslogFlowDetail.setBodySentBytesRate(bodySentBytesRate);
            }
        }
        return accesslogFlowDetail;
    }

    private static String getSortSqlFormat(String sortName, String sortOrder, String defalutSortName) {

        if ("pv".equals(sortName)) {
            return " ORDER BY pv " + sortOrder;
        }
        if ("bodySentBytes".equals(sortName)) {
            return " ORDER BY body_sent_bytes " + sortOrder;
        }
        if ("uv".equals(sortName)) {
            return " ORDER BY uv " + sortOrder;
        }
        if ("slowPv".equals(sortName)) {
            return " ORDER BY slow_pv " + sortOrder;
        }
        if ("maxVisitTime".equals(sortName)) {
            return " ORDER BY max_visit_time " + sortOrder;
        }
        if ("avgVisitTime".equals(sortName)) {
            return " ORDER BY avg_visit_time " + sortOrder;
        }
        if ("uriCount".equals(sortName)) {
            return " ORDER BY uri_count " + sortOrder;
        }
        if ("pvRate".equals(sortName)) {
            return " ORDER BY if(pv == 0 , 0 ,slow_pv/pv) " + sortOrder;
        }
        if ("country".equals(sortName)) {
            return " ORDER BY country " + sortOrder;
        }
        if ("province".equals(sortName)) {
            return " ORDER BY province " + sortOrder;
        }
        if ("city".equals(sortName)) {
            return " ORDER BY city " + sortOrder;
        }
        if ("ip".equals(sortName)) {
            return " ORDER BY remote_addr " + sortOrder;
        }
        return " ORDER BY " + defalutSortName + " desc ";
    }

    private String buildCountryFilter(List<String> countryList, MapSqlParameterSource paramMap, String where) {
        if (countryList != null && !countryList.isEmpty()) {
            where += " and t.country in (:country)";
            paramMap.addValue("country", countryList);
        }
        return where;
    }

    private String buildProvinceFilter(List<String> provinceList, MapSqlParameterSource paramMap, String where) {
        if (provinceList != null && !provinceList.isEmpty()) {
            where += " and t.province in (:province)";
            paramMap.addValue("province", provinceList);
        }
        return where;
    }

    private String buildCityFilter(List<String> cityList, MapSqlParameterSource paramMap, String where) {
        if (cityList != null && !cityList.isEmpty()) {
            where += " and t.city in (:city)";
            paramMap.addValue("city", cityList);
        }
        return where;
    }

    private String buildApplicationCodeListFilter(String applicationCode, MapSqlParameterSource paramMap, String where) {
        logger.info("buildApplicationCodeListFilter.applicationCode:{}", applicationCode);
        if (StringUtils.isNotEmpty(applicationCode)) {
            where += " and t.application_code = :application_code";
            paramMap.addValue("application_code", applicationCode);
        }
        if (StringUtils.equalsIgnoreCase(Constants.DEFAULT_SERVER_NAME_HQQ, applicationCode)) {
            where = buildDefaultHostFilter(paramMap, where);
        }
        return where;
    }

    private String buildRemoteAddrFilter(String remoteAddr, MapSqlParameterSource paramMap, String where) {
        logger.info("buildRemoteAddrFilter.remoteAddr:{}", remoteAddr);
        if (StringUtils.isNotEmpty(remoteAddr)) {
            where += " and t.remote_addr = :remote_addr";
            paramMap.addValue("remote_addr", remoteAddr);
        }
        return where;
    }

    private String buildHttpHostFilter(String httpHost, MapSqlParameterSource paramMap, String where) {
        logger.info("buildHttpHostFilter.httpHost:{}", httpHost);
        if (!StringUtils.equalsIgnoreCase(httpHost, Constants.DEFAULT_ALL)) {
            where += " and t.http_host = :http_host";
            paramMap.addValue("http_host", httpHost);
        }
        return where;
    }

    private String buildDefaultHostFilter(MapSqlParameterSource paramMap, String where) {
        where += " and t.http_host in (:http_host)";
        paramMap.addValue("http_host", hostList);

        return where;
    }

    private String buildStatusFilter(String status, MapSqlParameterSource paramMap, String where) {
        logger.info("buildStatusFilter.status:{}", status);
        if (StringUtils.isNotEmpty(status)) {
            where += " and t.status = :status";
            paramMap.addValue("status", status);
        }
        return where;
    }

    private String buildStatHourFilter(String statHour, MapSqlParameterSource paramMap, String where) {
        logger.info("buildHourFilter.statHour:{}", statHour);
        if (StringUtils.isNotEmpty(statHour)) {
            where += " and t.stat_hour = :stat_hour";
            paramMap.addValue("stat_hour", statHour);
        }
        return where;
    }

    private String buildStatDateEndFilter(String _endTime, MapSqlParameterSource paramMap, String where) {
        logger.info("buildStatDateEndFilter._endTime:{}", _endTime);
        return buildStatDateEndFilter(_endTime, paramMap, where, "day");
    }

    private String buildStatDateEndFilter(String _endTime, MapSqlParameterSource paramMap, String where, String timeType) {
        Timestamp endTime = transformFilterTime(_endTime, false, timeType);
        where += " and t.stat_date <=:endtime";
        paramMap.addValue("endtime", this.yMdFORMAT.get().format(endTime));
        return where;
    }

    private String buildStatDateStartFilter(String _startTime, MapSqlParameterSource paramMap, String where) {
        logger.info("buildStatDateStartFilter._startTime:{}", _startTime);
        return buildStatDateStartFilter(_startTime, paramMap, where, "day");
    }

    private String buildStatDateStartFilter(String _startTime, MapSqlParameterSource paramMap, String where, String timeType) {
        Timestamp startTime = transformFilterTime(_startTime, true, timeType);
        where += " and t.stat_date >=:starttime";
        paramMap.addValue("starttime", this.yMdFORMAT.get().format(startTime));
        return where;
    }

    private String buildLimitFilter(List<String> limits, MapSqlParameterSource paramMap, String where) {
        if (limits != null) {
            String paramKey = "paramKey";
            for (int i = 0; i < limits.size(); i++) {
                String lm = limits.get(i);
                paramKey = paramKey + i;
                where += " and notLike(uri,:" + paramKey + ")=1 ";
                paramMap.addValue(paramKey, "%" + lm);
//        		if("css".equals(lm)) {
//        			where += " and notLike(url2,'%.css')=1 ";
//        		}
//        		if("js".equals(lm)) {
//        			where += " and endsWith(url2,'.js')=1 ";
//        		}
            }
        }
        return where;
    }


    private Timestamp transformFilterTime(String time, boolean isStart, String timeType) {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        now = Timestamp.valueOf(this.yMdFORMAT.get().format(now) + " 00:00:00");
        Timestamp timestamp = now;
        if ("week".equalsIgnoreCase(timeType)) {
            long[] weekframe = TimeUtils.getCurrentWeekTimeFrame(now);
            if (isStart) {
                timestamp = new Timestamp(weekframe[0]);
            } else {
                timestamp = new Timestamp(weekframe[1]);
            }
        } else if ("month".equalsIgnoreCase(timeType)) {

            long[] monthframe = TimeUtils.getCurrentMonthTimeFrame(now);
            if (isStart) {
                timestamp = new Timestamp(monthframe[0]);
            } else {
                timestamp = new Timestamp(monthframe[1]);
            }
        } else if ("year".equalsIgnoreCase(timeType)) {
            long[] monthframe = TimeUtils.getCurrentYearTimeFrame(now);
            if (isStart) {
                timestamp = new Timestamp(monthframe[0]);
            } else {
                timestamp = new Timestamp(monthframe[1]);
            }
        }

        if (timestamp.getTime() > now.getTime()) {
            timestamp = now;
        }

        if (StringUtils.isNotBlank(time)) {
            timestamp = Timestamp.valueOf(time + " 00:00:00");
            if (timestamp.getTime() > now.getTime()) {
                timestamp = now;
            }
//            if (timestamp.getTime() <= constsDataHolder.getStartStatDate().getTime()) {
//                timestamp = constsDataHolder.getStartStatDate();
//            }
        }
        return timestamp;
    }

    private TimeFrame getPreviousTimeframe(String startTime, String timeType) {
        Timestamp start = transformFilterTime(startTime, true, timeType);
        start = new Timestamp(start.getTime() - DateUtils.MILLIS_PER_DAY);
        Timestamp end = null;
        if ("day".equalsIgnoreCase(timeType)) {
            end = start;
        } else if ("week".equalsIgnoreCase(timeType)) {
            long[] prev = TimeUtils.getCurrentWeekTimeFrame(start);
            start = new Timestamp(prev[0]);
            end = new Timestamp(prev[1]);
        } else if ("month".equalsIgnoreCase(timeType)) {
            long[] prev = TimeUtils.getCurrentMonthTimeFrame(start);
            start = new Timestamp(prev[0]);
            end = new Timestamp(prev[1]);
        } else if ("year".equalsIgnoreCase(timeType)) {
            long[] prev = TimeUtils.getCurrentYearTimeFrame(start);
            start = new Timestamp(prev[0]);
            end = new Timestamp(prev[1]);
        }

        TimeFrame timeFrame = new TimeFrame();
        timeFrame.setStartTime(start);
        timeFrame.setEndTime(end);

        return timeFrame;
    }

    private TimeFrame getSamePeriodTimeframe(String startTime, String timeType) {
        TimeFrame timeFrame = null;
        Timestamp start = transformFilterTime(startTime, true, timeType);
        Timestamp end = null;
        if ("day".equalsIgnoreCase(timeType)) {
            start = new Timestamp(start.getTime() - DateUtils.MILLIS_PER_DAY * 7);
            end = start;
        } else if ("month".equalsIgnoreCase(timeType)) {
            Calendar month = Calendar.getInstance();
            month.setTime(start);
            Calendar first = Calendar.getInstance();
            first.set(Calendar.YEAR, month.get(Calendar.YEAR) - 1);
            first.set(Calendar.MONTH, month.get(Calendar.MONTH));
            first.set(Calendar.DATE, 1);
            first.set(Calendar.HOUR_OF_DAY, 0);
            first.set(Calendar.MINUTE, 0);
            first.set(Calendar.SECOND, 0);
            first.set(Calendar.MILLISECOND, 0);
            long[] prev = TimeUtils.getCurrentMonthTimeFrame(new Timestamp(first.getTimeInMillis()));
            start = new Timestamp(prev[0]);
            end = new Timestamp(prev[1]);
        }
        if (end != null) {
            timeFrame = new TimeFrame();
            timeFrame.setStartTime(start);
            timeFrame.setEndTime(end);
        }
        return timeFrame;
    }
}
