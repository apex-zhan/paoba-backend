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
import com.zxw.paoba.model.request.TeamJoinRequest;
import com.zxw.paoba.model.request.TeamQuitRequest;
import com.zxw.paoba.model.request.TeamUpdateRequest;
import com.zxw.paoba.model.vo.TeamUserVO;
import com.zxw.paoba.model.vo.UserVO;
import com.zxw.paoba.service.TeamService;
import com.zxw.paoba.service.UserService;
import com.zxw.paoba.service.UserTeamService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;

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
public class TeamServiceImpl extends ServiceImpl<TeamMapper, Team> implements TeamService {
    @Autowired
    private UserTeamService userTeamService;
    @Autowired
    private UserService userService;

    /**
     * 创建队伍
     *
     * @param team      队伍信息
     * @param loginUser
     * @return
     */

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
        synchronized (userId.toString().intern()) {
//            1. 校验信息
//          ● 校验用户最多创建队伍 5 个
//            用户如果点击100次就创了100个,解决方法就是加锁
            QueryWrapper<Team> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("user_id", userId);
            long count = this.count(queryWrapper);
            if (count >= 5) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户最多创建队伍 5 个");
            }
            //2. 插入队伍信息到队伍表
            team.setId(null);
            team.setUserId(userId);
            boolean res = this.save(team);
            Long teamId = team.getId();
            if (!res || teamId == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "创建队伍失败");
            }
//          3. 插入用户=>队伍关系到关系表
            UserTeam userTeam = new UserTeam();
            userTeam.setUserId(userId);
            userTeam.setTeamId(teamId);
            userTeam.setJoinTime(new Date());
            boolean result = userTeamService.save(userTeam);
            if (!result) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "创建队伍失败");
            }
        }
        return team.getId();

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


    /**
     * 更新队伍
     *
     * @param teamUpdateRequest
     * @param loginUser
     * @return
     */
    @Override
    public boolean updateTeams(@RequestBody TeamUpdateRequest teamUpdateRequest, User loginUser) {
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
        if (statusEnum.equals(TeamStatusEnum.SECRET) || StringUtils.isBlank(teamUpdateRequest.getPassword())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "加密房间必须设置密码");
        }
        // todo 如果用户更新的数据新老值一样，就不更新

        //更新队伍信息
        Team team = new Team();
        BeanUtils.copyProperties(teamUpdateRequest, team);
        team.setUpdateTime(new Date());
        boolean updatedById = this.updateById(team);
        return updatedById;
    }

    /**
     * 加入队伍
     * todo 考虑是否会重复加入，加锁
     *
     * @param teamJoinRequest
     * @param loginUser
     * @return
     */
    @Override
    public boolean joinTeam(@RequestBody TeamJoinRequest teamJoinRequest, User loginUser) {
        if (teamJoinRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long teamId = teamJoinRequest.getTeamId();
        Team team = this.getById(teamId);
        if (ObjectUtils.isEmpty(teamId)) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "队伍不存在");
        }
        Date expireTime = team.getExpireTime();
        if (expireTime != null && expireTime.before(new Date())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍已过期");
        }
        Integer teamStatus = team.getStatus();
        TeamStatusEnum teamStatusEnum = TeamStatusEnum.getEnumByValue(teamStatus);
        if (TeamStatusEnum.PRIVATE.equals(teamStatusEnum)) {
            throw new BusinessException(ErrorCode.NO_AUTH, "队伍是私有状态，无法加入");
        }
        String password = teamJoinRequest.getPassword();
        if (TeamStatusEnum.SECRET.equals(teamStatusEnum)) {
            if (StringUtils.isBlank(password) || !password.equals(team.getPassword())) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码错误");
            }
        }
        // 该用户已加入的队伍，不能重复加入已加入的队伍
        long userId = loginUser.getId();
        long count = userTeamService.count(new QueryWrapper<UserTeam>().eq("userId", userId).eq("teamId", teamId));
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户已加入该队伍");
        }
        //已加入队伍的数量
        long hasUserJoinTeamNum = userTeamService.count(new QueryWrapper<UserTeam>().eq("userId", userId));
        if (hasUserJoinTeamNum > 5) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "已加入队伍数量已达上限");
        }
        //已加入队伍的人数
        long hasJoinTeamNum = userTeamService.count(new QueryWrapper<UserTeam>().eq("teamId", teamId));
        if (hasJoinTeamNum >= team.getMaxNum()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍已满");
        }
        //新增队伍用户信息
        UserTeam userTeam = new UserTeam();
        userTeam.setUserId(userId);
        userTeam.setTeamId(teamId);
        userTeam.setJoinTime(new Date());
        return userTeamService.save(userTeam);
    }


    /**
     * 退出队伍
     *
     * @param teamQuitRequest
     * @param loginUser
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean quitTeam(@RequestBody TeamQuitRequest teamQuitRequest, User loginUser) {
        if (teamQuitRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long teamId = teamQuitRequest.getTeamId();
        Team team = getTeamById(teamId);
        long loginUserId = loginUser.getId();
        // 校验是否已加入队伍
        UserTeam userTeam = new UserTeam();
        userTeam.setTeamId(teamId);
        userTeam.setUserId(loginUserId);
        QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>(userTeam);
        long teamUserCount = userTeamService.count(userTeamQueryWrapper);
        if (teamUserCount == 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "未加入该队伍");
        }
        // 通过队伍id查询关联的当前队伍人数
        long countUserTeamByTeamId = this.countUserTeamByTeamId(teamId);
        //队伍剩一个人，直接解散队伍
        if (countUserTeamByTeamId == 1) {
            // 删除队伍和所有加入当前队伍的关系
            this.removeById(teamId);
        } else {
            //队伍至少还剩两个人
            //队长（创建人和队长不一样，我们这里先约定用户id也是队长id）
            if (team.getUserId() == loginUserId) {
                //把队伍转交给最早加入队伍的人,先查询已加入队伍的所有用户的加入时间
                QueryWrapper<UserTeam> queryWrapper = new QueryWrapper<>();
                //加入队伍的用户
                queryWrapper.eq("teamId", teamId);
                //查询队伍用户的加入时间（或者id）并升序排序，取加入队伍的用户id
                queryWrapper.last("order by id asc limit 2");
                List<UserTeam> userTeamsList = userTeamService.list(queryWrapper);
                if (CollectionUtils.isEmpty(userTeamsList) || userTeamsList.size() <= 1) {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR);
                }
                //获取最早加入队伍的用户id除现任队长
                UserTeam nextUserTeam = userTeamsList.get(1);
                Long nextTeamLeaderId = nextUserTeam.getUserId();
                //更新他为队长
                Team updateTeam = new Team();
                updateTeam.setId(teamId);
                updateTeam.setUserId(nextTeamLeaderId);
                boolean updateLeaderById = this.updateById(updateTeam);
                if (!updateLeaderById) {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新队长失败");
                }
            }
        }
        //移除之前队伍用户关系
        boolean remove = userTeamService.remove(userTeamQueryWrapper);
        return remove;
    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean dissolveTeam(Long id, User loginUser) {
        Team team = getTeamById(id);
        Long teamId = team.getId();
        if (teamId == null || teamId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍不存在");
        }
        // 校验是否为队伍创建人
        if(team.getUserId() != loginUser.getId()){
            throw new BusinessException(ErrorCode.NO_AUTH, "无操作权限");
        }
        // 移除所有加入队伍的关联信息
        boolean result = userTeamService.remove(new QueryWrapper<UserTeam>().eq("teamId", teamId));
        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "删除队伍失败");
        }
        return this.removeById(teamId);
    };

    /**
     * 根据 id 获取队伍信息
     *
     * @param teamId
     * @return
     */
    private Team getTeamById(Long teamId) {
        if (teamId == null || teamId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Team team = this.getById(teamId);
        if (team == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "队伍不存在");
        }
        return team;
    }

    /**
     * 通过队伍id查询当前队伍人数
     *
     * @param teamId
     * @return
     */
    public long countUserTeamByTeamId(Long teamId) {
        QueryWrapper<UserTeam> userTeamWrapper = new QueryWrapper<>();
        userTeamWrapper.eq("teamId", teamId);
        long teamUserCount = userTeamService.count(userTeamWrapper);
        return teamUserCount;
    }
}

