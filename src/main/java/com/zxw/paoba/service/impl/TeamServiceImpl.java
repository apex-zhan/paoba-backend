package com.zxw.paoba.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zxw.paoba.common.ErrorCode;
import com.zxw.paoba.exception.BusinessException;
import com.zxw.paoba.mapper.TeamMapper;
import com.zxw.paoba.model.domain.Team;

import com.zxw.paoba.model.domain.User;
import com.zxw.paoba.model.domain.UserTeam;
import com.zxw.paoba.model.enums.TeamStatusEnum;
import com.zxw.paoba.service.TeamService;
import com.zxw.paoba.service.UserTeamService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jta.atomikos.AtomikosDependsOnBeanFactoryPostProcessor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
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
//        todo bug用户如果点击100次就创了100个,解决方法就是加锁
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
}




