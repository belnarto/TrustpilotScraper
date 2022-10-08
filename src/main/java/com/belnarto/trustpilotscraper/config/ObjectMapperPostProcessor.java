package com.belnarto.trustpilotscraper.config;

import com.belnarto.trustpilotscraper.serializer.RatingSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

@Component
public class ObjectMapperPostProcessor implements BeanPostProcessor {

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof ObjectMapper) {
            SimpleModule module = new SimpleModule();
            module.addSerializer(Double.class, new RatingSerializer());
            ((ObjectMapper) bean).registerModule(module);
        }
        return bean;
    }

}
