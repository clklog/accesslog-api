package com.zcunsoft.accesslog.api.models.enums;



import org.apache.commons.lang3.StringUtils;


public enum SummaryOption {
	Status("Status", "状态"),
	Location("Location", "位置"),
	StatusLocation("StatusLocation", "状态位置"),
	LocationStatus("LocationStatus", "位置状态"),
	Default("Default", "缺省");
    private String code;
    private String name;

    private SummaryOption(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public static SummaryOption fromCode(String code) {
        if (StringUtils.isEmpty(code)) {
            return SummaryOption.Default;
        }
        for (SummaryOption value : SummaryOption.values()) {
            if (value.isEqual(code)) {
                return value;
            }
        }
        return SummaryOption.Default;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

   

    public boolean isEqual(String code) {
        return StringUtils.isNotEmpty(code) ? this.code.equalsIgnoreCase(code) : false;
    }

}
