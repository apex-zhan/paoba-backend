package com.zxw.paoba;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zxw.paoba.model.domain.UserTeam;
import com.zxw.paoba.service.UserTeamService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.DigestUtils;

import java.security.NoSuchAlgorithmException;

/**
 * 测试类
 */
@SpringBootTest
class paobaApplicationTest {
    @Autowired
    UserTeamService userTeamService;

    @Test
    void testDigest() throws NoSuchAlgorithmException {
        String newPassword = DigestUtils.md5DigestAsHex(("abcd" + "mypassword").getBytes());
        System.out.println(newPassword);
    }

    @Test
    void contextLoads() {

    }
}
