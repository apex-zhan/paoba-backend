package com.zxw.paoba.service;

import com.zxw.paoba.model.domain.User;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;

/**
 * 用户服务测试
 *
 */
@SpringBootTest
public class UserServiceTest {

    @Resource
    private UserService userService;

    @Test
    public void testAddUser() {
        User user = new User();
        user.setUserName("本项目_所属 [zxw]()\n");
        user.setUserAccount("123");
        user.setUserAvatar("");
        user.setGender(0);
        user.setUserPassword("xxx");
        user.setPhone("123");
        user.setEmail("456");
        boolean result = userService.save(user);
        System.out.println(user.getId());
        Assertions.assertTrue(result);
    }

    @Test
    public void testUpdateUser() {
        User user = new User();
        user.setId(1L);
        user.setUserName("dog");
        user.setUserAccount("123");
        user.setUserAvatar("");
        user.setGender(0);
        user.setUserPassword("xxx");
        user.setPhone("123");
        user.setEmail("456");
        boolean result = userService.updateById(user);
        Assertions.assertTrue(result);
    }

    @Test
    public void testDeleteUser() {
        boolean result = userService.removeById(1L);
        Assertions.assertTrue(result);
    }

    @Test
    public void testGetUser() {
        User user = userService.getById(1L);
        Assertions.assertNotNull(user);
    }

    @Test
    void userRegister() {
        String userAccount = "zxw";
        String userPassword = "12345678";
        String checkPassword = "12345678";
        String planetCode = "1";
        long result = userService.userRegister(userAccount, userPassword, checkPassword);
        Assertions.assertEquals(-1, result);
        userAccount = "zx";
        result = userService.userRegister(userAccount, userPassword, checkPassword);
        Assertions.assertEquals(-1, result);
        userAccount = "zxw";
        userPassword = "123456";
        checkPassword = "123456";
        result = userService.userRegister(userAccount, userPassword, checkPassword);
        Assertions.assertEquals(-1, result);
        userAccount = "this zxw";
        userPassword = "12345678";
        result = userService.userRegister(userAccount, userPassword, checkPassword);
        Assertions.assertEquals(-1, result);
        checkPassword = "123456789";
        result = userService.userRegister(userAccount, userPassword, checkPassword);
        Assertions.assertEquals(-1, result);
        userAccount = "thisZxw";
        checkPassword = "12345678";
        result = userService.userRegister(userAccount, userPassword, checkPassword);
        Assertions.assertEquals(-1, result);
        userAccount = "zxw";
        result = userService.userRegister(userAccount, userPassword, checkPassword);
        Assertions.assertEquals(-1, result);
    }

    @Test
    public void testSearchUsersByTags() {
        List<User> tagList = userService.searchUsersByTags(Arrays.asList("java", "python"));
        Assertions.assertNotNull(tagList);
    }


}