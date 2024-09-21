package com.zxw.paoba.service;

import com.zxw.paoba.mapper.UserMapper;
import com.zxw.paoba.model.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.StopWatch;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static net.sf.jsqlparser.util.validation.metadata.NamedObject.user;

/**
 * 导入用户测试
 */
@SpringBootTest
public class InsertUsersTest {

    @Resource
    private UserService userService;
    @Autowired
    private UserMapper userMapper;
    /**
     * 自定义线程池
      */
    private ExecutorService executorService = new ThreadPoolExecutor(40, 1000, 10000, TimeUnit.MINUTES, new ArrayBlockingQueue<>(10000));

    /**
     * 批量插入用户
     */
    @Test
    public void doInsertUsers() {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        final int num = 100000;
        List<User> list = new ArrayList<>();
        for (int i = 0; i < num; i++) {
            User user = new User();
            user.setUserName("zxw");
            user.setUserAccount("zzxw");
            user.setUserAvatar("https://api.dicebear.com/7.x/miniavs/svg?seed=2");
            user.setGender(0);
            user.setUserPassword("12345678");
            user.setPhone("123456789");
            user.setEmail("999999");
            user.setTags("[java]");
            user.setUserStatus(0);
            user.setUserRole(0);
            list.add(user);
//            userMapper.insert(user);
            //1千6秒
        }
        userService.saveBatch(list, 1000);
        //10万数据插入要20秒
        stopWatch.stop();
        long watchTime = stopWatch.getTotalTimeMillis();
        System.out.println(watchTime);
    }

    /**
     * 并发批量插入用户
     */
    @Test
    public void doConcurrencyInsertUsers() {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        final int num = 100000;
        // 分十组
        int batchSize = 10000;
        int j = 0;
        List<CompletableFuture<Void>> futureList = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            List<User> userList = new ArrayList<>();
            while (true) {
                j++;
                User user = new User();
                user.setUserName("");
                user.setUserAccount("");
                user.setUserAvatar("https://api.dicebear.com/7.x/miniavs/svg?seed=2");
                user.setGender(0);
                user.setUserPassword("12345678");
                user.setPhone("123");
                user.setEmail("123@qq.com");
                user.setTags("[]");
                user.setUserStatus(0);
                user.setUserRole(0);
                userList.add(user);
                if (j % batchSize == 0) {
                    break;
                }
            }
            // 异步执行
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                System.out.println("threadName: " + Thread.currentThread().getName());
                userService.saveBatch(userList, batchSize);
            }, executorService);
            futureList.add(future);
        }
        CompletableFuture.allOf(futureList.toArray(new CompletableFuture[]{})).join();
        // 20 秒 10 万条
        //使用异步后只要几秒
        stopWatch.stop();
        System.out.println(stopWatch.getTotalTimeMillis());
    }

    //更新数据
    @Test
    public void updateData() {
    }
}
