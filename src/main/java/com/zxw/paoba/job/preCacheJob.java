package com.zxw.paoba.job;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zxw.paoba.model.domain.User;
import com.zxw.paoba.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;


@Component
@Slf4j
public class preCacheJob {

    @Autowired
    private UserService userService;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    //重点用户
    private List<Long> mainUserList = Arrays.asList(1L);
    /**
     * 每天凌晨一点执行缓存推荐用户
     */
@Scheduled(cron = "0 0 1 * * ?")
    synchronized void doCacheRecommendUser(){
    for (Long userId : mainUserList) {
        QueryWrapper<User> QueryWrapper = new QueryWrapper<>();
        Page<User> userPage = userService.page(new Page<>(1, 10), QueryWrapper);
        //缓存到redis
        String redisKey = String.format("paoba:user:recommend:%s",  userId);
        ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();
        try {
            valueOperations.set(redisKey, userPage, 30000, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            log.error("redis set key error", e);
        }
    }
        
    }
}
