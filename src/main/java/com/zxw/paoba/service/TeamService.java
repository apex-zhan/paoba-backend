package com.zxw.paoba.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.zxw.paoba.model.domain.Team;
import com.zxw.paoba.model.domain.User;
import com.zxw.paoba.model.dto.TeamQuery;
import com.zxw.paoba.model.vo.TeamUserVO;

import java.util.List;

/**
* @author zxw
* @description 针对表【team(队伍)】的数据库操作Service
* @createDate 2024-10-02 15:18:51
*/
public interface TeamService extends IService<Team> {

    /**
     * 创建队伍
     * @param team 队伍信息
     * @return 队伍id
     */
    Long addTeam(Team team, User loginUser);

    /**
     * 查询队伍列表
     *
     * @param teamQuery
     * @return
     */
    List<TeamUserVO> listTeams(TeamQuery teamQuery);
}
