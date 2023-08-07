package com.homeofthewizard.dinerlib;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DinerSpringConfiguration {

    @Bean
    public Diner getDinerPlace(){
        return new Pizza();
    }
}
