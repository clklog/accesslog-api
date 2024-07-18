package com.zcunsoft.accesslog.api.entity.clickhouse;

import lombok.Data;

import java.math.BigDecimal;
import java.sql.Timestamp;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

@Entity(name = "")
@Data
public class Accesslogbydate  {
	@Id
    @Column
    Timestamp statDate;

	@Column
    String statHour;

	@Column
    String statMin;

	@Column
    Timestamp timeLocal;

	@Column
    String applicationCode;

    @Column
    String country;

    @Column
    String province;

    @Column
    String city;

    @Column
    String upstreamUri;

    @Column
    String upstreamAddr;

    @Column
    String uri;

    @Column
    String requestMethod;

    @Column
    String httpHost;

    @Column
    String httpUserAgent;

    @Column
    String browser;

    @Column
    String model;

    @Column
    String browserVersion;

    @Column
    String brand;

    @Column
    String remoteAddr;

    @Column
    String manufacturer;

    @Column
    String remoteUser;

    @Column
    String upstreamStatus;

    @Column
    BigDecimal requestTime;

    @Column
    String httpVersion;

    @Column
    String httpReferrer;

    @Column
	BigDecimal bodySentBytes;

	@Column
	BigDecimal pv;

	@Column
	BigDecimal uv;

    @Column
    BigDecimal ipCount;

	@Column
	BigDecimal slowPv;

	@Column
	BigDecimal maxVisitTime;

	@Column
	BigDecimal requestLength;

	@Column
	BigDecimal upstreamResponseTime;

	@Column
	String status;

	@Column
	BigDecimal visitCount;

	@Column
	BigDecimal visitTime;

	@Column
	String avgVisitTime;

	@Column
	BigDecimal uriCount;

	@Column
    Timestamp latestTime;

    @Column
    Timestamp createTime;
}
