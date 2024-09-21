package com.zxw.paoba.once;

import com.zxw.paoba.mapper.UserMapper;
import com.zxw.paoba.model.domain.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

/**
 * 一次性任务
 * 模拟批量插入用户数据
 */
@Component
@Deprecated
public class InsertUsers {
    @Autowired
    private UserMapper userMapper;

    public void insertUsers() {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        final int num = 1000;
        for (int i = 0; i < num; i++) {
            User user = new User();
            user.setUserName("zxw");
            user.setUserAccount("zzxw");
            user.setUserAvatar("");
            user.setGender(0);
            user.setUserPassword("12345678");
            user.setPhone("123456789");
            user.setEmail("999999");
            user.setTags("[java]");
            user.setUserStatus(0);
            user.setUserRole(0);
            userMapper.insert(user);

        }
        stopWatch.stop();
        long watchTime = stopWatch.getTotalTimeMillis();
        System.out.println(watchTime);
    }
}
