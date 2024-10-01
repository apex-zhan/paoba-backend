package com.zxw.paoba.config;

import lombok.Data;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * @author zxw
 * redisisson 配置
 */
@Configuration
@ConfigurationProperties(prefix = "spring.redis")
@Data
public class RedissonConfig {
    private String host;
    private String port;

    @Bean
    public RedissonClient redissonClient() {
        // 1. Create config object
        Config config = new Config();
        String redis = String.format("redis://%s:%s", host, port);
        // useClusterServers集群
        // useSingleServer单机
        config.useSingleServer().setAddress(redis).setDatabase(3);
        // Sync and Async API
        RedissonClient redisson = Redisson.create(config);
        return redisson;
    }
}