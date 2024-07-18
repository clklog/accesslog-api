package com.zcunsoft.accesslog.api.cfg;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.zcunsoft.accesslog.api.handlers.ConstsDataHolder;


@Configuration
@EnableConfigurationProperties({ ClklogApiSetting.class})
public class SpringConfiguration {

    @Bean
    public ConstsDataHolder constsDataHolder() {
        return new ConstsDataHolder();
    }
}
