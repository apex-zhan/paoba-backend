package com.zxw.paoba.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zxw.paoba.mapper.TeamMapper;
import com.zxw.paoba.model.domain.Team;

import com.zxw.paoba.service.TeamService;
import org.springframework.stereotype.Service;

/**
* @author zxw
* @description 针对表【team(队伍)】的数据库操作Service实现
* @createDate 2024-10-02 15:18:51
*/
@Service
public class TeamServiceImpl extends ServiceImpl<TeamMapper, Team>
    implements TeamService {

}




