package com.belnarto.trustpilotscraper.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import redis.embedded.RedisServer;

@TestConfiguration
public class TestRedisConfiguration {

    @Bean(initMethod = "start", destroyMethod = "stop")
    RedisServer redisMockServer(@Value("${spring.redis.port}") int redisPort) {
        return RedisServer.builder()
            .port(redisPort)
            .setting("maxmemory 128M")
            .build();
    }

}
