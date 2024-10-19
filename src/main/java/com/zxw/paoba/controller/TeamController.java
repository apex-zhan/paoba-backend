package com.zxw.paoba.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zxw.paoba.common.BaseResponse;
import com.zxw.paoba.common.ErrorCode;
import com.zxw.paoba.common.ResultUtils;
import com.zxw.paoba.exception.BusinessException;
import com.zxw.paoba.model.domain.Team;
import com.zxw.paoba.model.domain.User;
import com.zxw.paoba.model.domain.UserTeam;
import com.zxw.paoba.model.dto.TeamQuery;
import com.zxw.paoba.model.request.TeamAddRequest;
import com.zxw.paoba.model.request.TeamJoinRequest;
import com.zxw.paoba.model.request.TeamQuitRequest;
import com.zxw.paoba.model.request.TeamUpdateRequest;
import com.zxw.paoba.model.vo.TeamUserVO;
import com.zxw.paoba.service.TeamService;
import com.zxw.paoba.service.UserService;
import com.zxw.paoba.service.UserTeamService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/team")
@CrossOrigin()
@Slf4j
public class TeamController {
    @Autowired
    private UserService userService;
    @Autowired
    private TeamService teamService;
    @Autowired
    private UserTeamService userTeamService;

    /**
     * 添加队伍
     */
    @PostMapping("/add")
    public BaseResponse<Long> addTeam(@RequestBody TeamAddRequest teamAddRequest, HttpServletRequest request) {
        if (teamAddRequest == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        //插入
        User loginUser = userService.getLoginUser(request);
        Team team = new Team();
        BeanUtils.copyProperties(teamAddRequest, team);
        long teamId = teamService.addTeam(team, loginUser);
        return ResultUtils.success(teamId);
    }

    @PostMapping("/update")
    public BaseResponse<Boolean> updateTeam(@RequestBody TeamUpdateRequest TeamUpdateRequest, HttpServletRequest request) {
        //校验
        if (TeamUpdateRequest == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        boolean updateById = teamService.updateTeams(TeamUpdateRequest, loginUser);

        if (!updateById) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍更新失败");
        }
        return ResultUtils.success(true);
    }

    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteTeam(@RequestBody long id, HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        boolean removeById = teamService.dissolveTeam(id, loginUser);
        if (!removeById) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "队伍删除失败");
        }
        return ResultUtils.success(true);
    }

    @GetMapping("/get")
    public BaseResponse<Team> getTeamById(long id, HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Team team = teamService.getById(id);
        if (team == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        return ResultUtils.success(team);
    }

    @GetMapping("/list")
    public BaseResponse<List<TeamUserVO>> listTeams(TeamQuery teamQuery, HttpServletRequest request) {
        if (teamQuery == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean admin = userService.isAdmin(request);
        List<TeamUserVO> teamList = teamService.listTeams(teamQuery, admin);
        return ResultUtils.success(teamList);
    }

    @GetMapping("/list/page")
    public BaseResponse<Page<Team>> listTeamsByPage(TeamQuery teamQuery, HttpServletRequest request) {
        if (teamQuery == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Team team = new Team();
        //把teamQuery的属性值赋给team
        BeanUtils.copyProperties(teamQuery, team);
        team.setName(teamQuery.getName());
        Page<Team> page = new Page<>(teamQuery.getPageNum(), teamQuery.getPageSize());
        QueryWrapper<Team> queryWrapper = new QueryWrapper<>();
        Page<Team> teamPage = teamService.page(page, queryWrapper);
        return ResultUtils.success(teamPage);
    }

    @PostMapping("/join")
    public BaseResponse<Boolean> joinTeam(@RequestBody TeamJoinRequest teamJoinRequest, HttpServletRequest request) {
        if (teamJoinRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        boolean joinTeam = teamService.joinTeam(teamJoinRequest, loginUser);
        return ResultUtils.success(joinTeam);
    }

    @PostMapping("/quit")
    public BaseResponse<Boolean> quitTeam(@RequestBody TeamQuitRequest teamQuitRequest, HttpServletRequest request) {
        if (teamQuitRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        boolean quitTeam = teamService.quitTeam(teamQuitRequest, loginUser);
        return ResultUtils.success(quitTeam);
    }

    /**
     * 获取我创建的队伍
     *
     * @param teamQuery
     * @param request
     * @return
     */
    @GetMapping("/list/my/create")
    public BaseResponse<List<TeamUserVO>> listMyTeams(TeamQuery teamQuery, HttpServletRequest request) {
        if (teamQuery == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        // 只查询登录用户id创建队伍
        teamQuery.setUserId(loginUser.getId());
        //直接把管理员参数改成true，自己也能看见自己创建的队伍了
        List<TeamUserVO> teamList = teamService.listTeams(teamQuery, true);
        return ResultUtils.success(teamList);
    }

    /**
     * 获取我加入的队伍
     *
     * @param teamQuery
     * @param request
     * @return
     */
    @GetMapping("/list/my/join")
    public BaseResponse<List<TeamUserVO>> listMyJoinTeams(TeamQuery teamQuery, HttpServletRequest request) {
        if (teamQuery == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        QueryWrapper<UserTeam> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", loginUser.getId());
        List<UserTeam> userTeamList = userTeamService.list(queryWrapper);
        //取出不重复的队伍id
        //teamId userId
        //1,2
        //1,3
        //2,3
        //result
        //1 => 2,3
        //2 => 3
        //从userTeamList中提取了所有唯一的团队ID
        List<Long> list = userTeamList.stream().map(UserTeam::getTeamId).distinct().collect(Collectors.toList());
        teamQuery.setIdList(list);
        List<TeamUserVO> teamList = teamService.listTeams(teamQuery, true);
        return ResultUtils.success(teamList);
    }
}