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

import static net.sf.jsqlparser.util.validation.metadata.NamedObject.user;

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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public synchronized Long addTeam(Team team, User loginUser) {
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
    public List<TeamUserVO> listTeams(TeamQuery teamQuery) {
        QueryWrapper<Team> queryWrapper = new QueryWrapper<>();
        if (teamQuery != null) {
            queryWrapper.lambda().eq(Team::getId, teamQuery.getId());
            queryWrapper.lambda().like(Team::getName, teamQuery.getName());
            queryWrapper.lambda().like(Team::getDescription, teamQuery.getDescription());
            // 根据创建人的信息来查询
            queryWrapper.lambda().eq(Team::getUserId, teamQuery.getUserId());
            queryWrapper.lambda().eq(Team::getMaxNum, teamQuery.getMaxNum());
            Integer status = teamQuery.getStatus();
            TeamStatusEnum statusEnum = TeamStatusEnum.getEnumByValue(status);
            if (statusEnum == null) {
                //不传默认公开
                statusEnum = TeamStatusEnum.PUBLIC;
            }
            queryWrapper.lambda().eq(Team::getStatus, statusEnum.getValue());
        }
        //不展示已过期的队伍
        //expireTime is  null or expireTime > now()
        queryWrapper.lambda().gt(Team::getExpireTime, new Date()).or().isNull(Team::getExpireTime);
        //查询队伍列表
        List<Team> teamList = this.list();
        if (CollectionUtils.isEmpty(teamList)) {
            return new ArrayList<>();
        }
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
}




