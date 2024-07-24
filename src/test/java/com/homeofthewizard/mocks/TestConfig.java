package com.homeofthewizard.mocks;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TestConfig {

    @Bean
    public TestPojo pojo() {
        return new TestPojo();
    }

    @Bean
    public TestFilterPojo testPojo() {
        return new TestFilterPojo();
    }
}
