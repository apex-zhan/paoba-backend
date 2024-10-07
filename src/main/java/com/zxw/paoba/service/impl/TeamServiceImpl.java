package com.zxw.paoba.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zxw.paoba.common.ErrorCode;
import com.zxw.paoba.exception.BusinessException;
import com.zxw.paoba.mapper.TeamMapper;
import com.zxw.paoba.model.domain.Team;

import com.zxw.paoba.model.domain.User;
import com.zxw.paoba.model.domain.UserTeam;
import com.zxw.paoba.model.dto.TeamQuery;
import com.zxw.paoba.model.enums.TeamStatusEnum;
import com.zxw.paoba.model.request.TeamUpdateRequest;
import com.zxw.paoba.model.vo.TeamUserVO;
import com.zxw.paoba.model.vo.UserVO;
import com.zxw.paoba.service.TeamService;
import com.zxw.paoba.service.UserService;
import com.zxw.paoba.service.UserTeamService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * @author zxw
 * @description 针对表【team(队伍)】的数据库操作Service实现,使用@Transactional来确保线程安全和sql的原子性
 * @createDate 2024-10-02 15:18:51
 */
@Service
public class TeamServiceImpl extends ServiceImpl<TeamMapper, Team>
        implements TeamService {
    @Autowired
    private UserTeamService userTeamService;
    @Autowired
    private UserService userService;
    @Autowired
    private TeamService teamService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addTeam(Team team, User loginUser) {
//        请求参数允许为空？
        if (team == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
//        是否登录，未登录不允许创建？
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        Long userId = loginUser.getId();
//        1. 校验信息
//          ● 校验用户最多创建队伍 5 个
//        todo bug:用户如果点击100次就创了100个,解决方法就是加锁
        QueryWrapper<Team> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId);
        long count = this.count(queryWrapper);
        if (count >= 5) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户最多创建队伍 5 个");
        }
//          ● 队伍人数>1 且<=500
        int teamMaxNum = Optional.ofNullable(team.getMaxNum()).orElse(0);
        if (teamMaxNum < 1 || teamMaxNum > 500) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍人数不合法");
        }
//          ● 队伍标题<=24
        String name = team.getName();
        if (StringUtils.isBlank(name) || name.length() > 24) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍标题不合法");
        }
//          ● 队伍描述简介<512
        String description = team.getDescription();
        if (StringUtils.isNotBlank(description) && description.length() > 512) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍描述简介不合法");
        }
//          ● 是否公开 int，不传默认公开为 0
        int status = Optional.ofNullable(team.getStatus()).orElse(0);
        TeamStatusEnum teamStatusEnum = TeamStatusEnum.getEnumByValue(status);
        if (teamStatusEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍状态不合法");
        }
//          ● 如果加密，一定要有密码<=32 位
        String password = team.getPassword();
        if (StringUtils.isNotBlank(password) && password.length() > 32) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍密码不合法");
        }
//          ● 超时时间>当前时间
        Date expireTime = team.getExpireTime();
//        判断当前时间是否已超过给定的expireTime
        if (new Date().after(expireTime)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍过期时间不合法");
        }
        //2. 插入队伍信息到队伍表
        team.setId(null);
        team.setUserId(userId);
        boolean res = this.save(team);
        Long teamId = team.getId();
        if (!res || teamId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "创建队伍失败");
        }
//      3. 插入用户=>队伍关系到关系表
        UserTeam userTeam = new UserTeam();
        userTeam.setUserId(userId);
        userTeam.setTeamId(teamId);
        userTeam.setJoinTime(new Date());
        boolean result = userTeamService.save(userTeam);
        if (!result) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "创建队伍失败");
        }
        return teamId;
    }


    /**
     * 查询队伍列表
     *
     * @param teamQuery
     * @return
     */
    @Override
    public List<TeamUserVO> listTeams(TeamQuery teamQuery, boolean isAdmin) {
        QueryWrapper<Team> queryWrapper = new QueryWrapper<>();
        // 组合查询条件
        if (teamQuery != null) {
            Long id = teamQuery.getId();
            if (id != null && id > 0) {
                queryWrapper.eq("id", id);
            }
            List<Long> idList = teamQuery.getIdList();
            if (CollectionUtils.isNotEmpty(idList)) {
                queryWrapper.in("id", idList);
            }
            String searchText = teamQuery.getSearchText();
            if (StringUtils.isNotBlank(searchText)) {
                queryWrapper.and(qw -> qw.like("name", searchText).or().like("description", searchText));
            }
            String name = teamQuery.getName();
            if (StringUtils.isNotBlank(name)) {
                queryWrapper.like("name", name);
            }
            String description = teamQuery.getDescription();
            if (StringUtils.isNotBlank(description)) {
                queryWrapper.like("description", description);
            }
            Integer maxNum = teamQuery.getMaxNum();
            // 查询最大人数相等的
            if (maxNum != null && maxNum > 0) {
                queryWrapper.eq("maxNum", maxNum);
            }
            Long userId = teamQuery.getUserId();
            // 根据创建人来查询
            if (userId != null && userId > 0) {
                queryWrapper.eq("userId", userId);
            }
            // 根据状态来查询
            Integer status = teamQuery.getStatus();
            TeamStatusEnum statusEnum = TeamStatusEnum.getEnumByValue(status);
            if (statusEnum == null) {
                statusEnum = TeamStatusEnum.PUBLIC;
            }
            if (!isAdmin && statusEnum.equals(TeamStatusEnum.PRIVATE)) {
                throw new BusinessException(ErrorCode.NO_AUTH);
            }
            queryWrapper.eq("status", statusEnum.getValue());
        }
        // 不展示已过期的队伍
        // expireTime is null or expireTime > now()
        queryWrapper.and(qw -> qw.gt("expireTime", new Date()).or().isNull("expireTime"));
        /**
         * 关联查询创建人的用户信息
         */
        //一. 自己写sql
        //1. 查询队伍和创建人的信息
        // select * from team t left join user u on t.userId = u.id
        //2. 查询队伍和已加入队伍成员的信息
        //select * from team t
        // left join user_team ut on t.id = ut.teamId
        // left join user u on u.id = ut.userId

        //二. 使用mybatis-plus提供的关联查询
        //查询队伍列表
        List<Team> teamList = this.list(queryWrapper);
        if (CollectionUtils.isEmpty(teamList)) {
            return new ArrayList<>();
        }
        ArrayList<TeamUserVO> teamUserVOList = new ArrayList<>();
        //遍历拿到队伍列表中的创建人用户id,通过他去查询他的用户信息
        for (Team team : teamList) {
            Long UserId = team.getUserId();
            if (UserId == null) {
                continue;
            }
            //查询用户信息
            User user = userService.getById(UserId);
            TeamUserVO teamUserVO = new TeamUserVO();
            BeanUtils.copyProperties(team, teamUserVO);
            //脱敏用户人信息
            if (user != null) {
                UserVO userVO = new UserVO();
                BeanUtils.copyProperties(user, userVO);
                teamUserVO.setCreateUser(userVO);
            }
            teamUserVOList.add(teamUserVO);
        }
        return teamUserVOList;
    }


    @Override
    public boolean updateTeams(TeamUpdateRequest teamUpdateRequest, User loginUser) {
        if (teamUpdateRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long teamId = teamUpdateRequest.getId();
        if (teamId == null || teamId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Team oldTeam = this.getById(teamId);
        if (oldTeam == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "队伍不存在");
        }
        // 只有管理员和创建者才能修改
        boolean isAdmin = userService.isAdmin(loginUser);
        if (!isAdmin && !oldTeam.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        //队伍状态只能修改为加密或公开或者私有
        Integer status = teamUpdateRequest.getStatus();
        TeamStatusEnum statusEnum = TeamStatusEnum.getEnumByValue(status);
        if (statusEnum.equals(TeamStatusEnum.SECRET)) {
            if (StringUtils.isBlank(teamUpdateRequest.getPassword())) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "加密房间必须设置密码");
            }
        }
        //如果用户更新的数据新老值一样，就不更新
        if (teamUpdateRequest.equals(oldTeam)) {
            return true;
        }
        //更新队伍信息
        Team team = new Team();
        BeanUtils.copyProperties(teamUpdateRequest, team);
        team.setUpdateTime(new Date());
        boolean updatedById = this.updateById(team);
        return updatedById;
    }
}



