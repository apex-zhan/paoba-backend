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
public class RedissionConfig {
    private String host;
    private String port;
    @Bean
    public RedissonClient redissonClient() {
        // 1. Create config object
        Config config = new Config();
        String redis = String.format("redis://%s:%s", host, port);
        // useClusterServers集群
        // useSingleServer单机
        config.useSingleServer().setAddress(redis).setDatabase(2);
        // use "redis://" for SSL connection

        // or read config from file
        // config = Config.fromYAML(new File("config-file.yaml"));
        // 2. Create Redisson instance

        // Sync and Async API
        RedissonClient redisson = Redisson.create(config);

        // Reactive API
        // RedissonReactiveClient redissonReactive = redisson.reactive();

        // RxJava3 API
        // RedissonRxClient redissonRx = redisson.rxJava();
        return redisson;
    }
}