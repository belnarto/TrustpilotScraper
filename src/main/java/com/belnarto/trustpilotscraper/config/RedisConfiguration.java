package com.belnarto.trustpilotscraper.config;

import com.belnarto.trustpilotscraper.dto.ReviewDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfiguration {

    @Bean
    ReactiveRedisTemplate<String, ReviewDto> reactiveRedisTemplate(ReactiveRedisConnectionFactory factory,
        ObjectMapper objectMapper) {

        StringRedisSerializer keySerializer = new StringRedisSerializer();
        RedisSerializationContext.RedisSerializationContextBuilder<String, ReviewDto> builder =
            RedisSerializationContext.newSerializationContext(keySerializer);

        Jackson2JsonRedisSerializer<ReviewDto> valueSerializer = new Jackson2JsonRedisSerializer<>(ReviewDto.class);
        valueSerializer.setObjectMapper(objectMapper);

        return new ReactiveRedisTemplate<>(factory, builder.value(valueSerializer).build());
    }

}
