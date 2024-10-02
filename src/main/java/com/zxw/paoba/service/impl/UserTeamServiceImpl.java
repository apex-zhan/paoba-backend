package com.zxw.paoba.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;


import com.zxw.paoba.mapper.UserTeamMapper;
import com.zxw.paoba.model.domain.UserTeam;

import com.zxw.paoba.service.UserTeamService;
import org.springframework.stereotype.Service;

/**
 * @author zxw
 * @description 针对表【user_team(用户队伍关系)】的数据库操作Service实现
 * @createDate 2024-10-02 15:21:12
 */
@Service
public class UserTeamServiceImpl extends ServiceImpl<UserTeamMapper, UserTeam>
        implements UserTeamService {

}




