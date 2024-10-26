package com.zxw.paoba.job;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zxw.paoba.model.domain.User;
import com.zxw.paoba.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.log4j.Logger;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 每天凌晨一点执行缓存推荐用户
 */
@Component
@Slf4j
public class preCacheJob {
    // 获取日志记录器实例
    private static final Logger logger = Logger.getLogger(preCacheJob.class);
    @Resource
    private UserService userService;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private RedissonClient redissonClient;


    //重点用户
    private List<Long> mainUserList = Arrays.asList(1L);

    @Scheduled(cron = "0 0 1 * * ?")
    public void doCacheRecommendUser() {
        RLock lock = redissonClient.getLock("paoba:job:cacheRecommendUser:lock");
        try {
            if (lock.tryLock(0, 30000L, TimeUnit.MILLISECONDS)) {
                for (Long userId : mainUserList) {
                    QueryWrapper<User> QueryWrapper = new QueryWrapper<>();
                    Page<User> userPage = userService.page(new Page<>(1, 10), QueryWrapper);
                    //缓存到redis
                    String redisKey = String.format("paoba:user:recommend:%s", userId);
                    ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();
                    try {
                        valueOperations.set(redisKey, userPage, 30000, TimeUnit.MILLISECONDS);
                    } catch (Exception e) {
                        logger.error("redis set key error", e);
                    }
                }
            }
        } catch (InterruptedException e) {
            logger.error("redis lock error", e);
        } finally {
            //只有自己能释放自己的锁
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }

    }
}
