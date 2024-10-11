package com.zxw.paoba.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.zxw.paoba.common.BaseResponse;
import com.zxw.paoba.model.domain.Team;
import com.zxw.paoba.model.domain.User;
import com.zxw.paoba.model.dto.TeamQuery;
import com.zxw.paoba.model.request.TeamJoinRequest;
import com.zxw.paoba.model.request.TeamQuitRequest;
import com.zxw.paoba.model.request.TeamUpdateRequest;
import com.zxw.paoba.model.vo.TeamUserVO;
import lombok.extern.java.Log;
import org.springframework.web.bind.annotation.RequestBody;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author zxw
 * @description 针对表【team(队伍)】的数据库操作Service
 * @createDate 2024-10-02 15:18:51
 */
public interface TeamService extends IService<Team> {

    /**
     * 创建队伍
     *
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

    List<TeamUserVO> listTeams(TeamQuery teamQuery, boolean isAdmin);

    /**
     * 更新队伍信息
     *
     * @param teamUpdateRequest
     * @param loginUser
     * @return
     **/
    boolean updateTeams(TeamUpdateRequest teamUpdateRequest, User loginUser);

    /**
     * 加入队伍
     * @param teamJoinRequest
     * @param loginUser
     * @return
     */
    boolean joinTeam(TeamJoinRequest teamJoinRequest, User loginUser);

    /**
     * 退出队伍
     * @param teamQuitRequest
     * @param loginUser
     * @return
     */
    boolean quitTeam(TeamQuitRequest teamQuitRequest, User loginUser);

    /**
     * 解散队伍
     * @param id
     * @param loginUser
     * @return
     */

    boolean dissolveTeam(Long id, User loginUser);
}