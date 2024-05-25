package com.xxsword.xitem.admin.service.exam.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xxsword.xitem.admin.domain.exam.entity.UserPaper;
import com.xxsword.xitem.admin.domain.exam.entity.UserPaperQuestion;
import com.xxsword.xitem.admin.domain.system.entity.UserInfo;
import com.xxsword.xitem.admin.mapper.exam.UserPaperMapper;
import com.xxsword.xitem.admin.service.exam.UserPaperQuestionService;
import com.xxsword.xitem.admin.service.exam.UserPaperService;
import com.xxsword.xitem.admin.utils.DateUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class UserPaperServiceImpl extends ServiceImpl<UserPaperMapper, UserPaper> implements UserPaperService {
    @Autowired
    private UserPaperQuestionService userPaperQuestionService;

    @Override
    public void upLastInfo(UserInfo doUserInfo, String ids) {
        String[] idsS = ids.split(",");
        List<UserPaper> listUp = new ArrayList<>();
        for (String id : idsS) {
            UserPaper itemUp = new UserPaper();
            itemUp.setId(id);
            itemUp.setBaseInfo(doUserInfo);
            listUp.add(itemUp);
        }
        updateBatchById(listUp);
    }

    @Override
    @Transactional
    public UserPaper getUserPaper(UserInfo userInfo, String paperId, String examId, Integer type) {
        if (type == null) {
            type = 1;
        }
        UserPaper userPaper = null;
        if (type.equals(1)) {
            List<UserPaper> listUserPaper = this.listUserPaper(userInfo.getId(), paperId, examId, 0);// 获取其未提交的答题记录
            if (listUserPaper == null || listUserPaper.size() == 0) {
                userPaper = this.newUserPaper(userInfo, paperId, examId);
            } else {
                userPaper = listUserPaper.get(0);
                if (listUserPaper.size() > 1) {
                    this.clearUserPaper(listUserPaper);// 一场考试中，一个用户对一张试卷只能有一条substatus为0的数据
                }
            }
        }
        if (type.equals(2)) {
            userPaper = this.newUserPaper(userInfo, paperId, examId);
        }
        return userPaper;
    }

    @Override
    public List<UserPaper> listUserPaper(String userId, String paperId, String examId, Integer subStatus) {
        return list(getUserPaperLambdaQueryWrapper(userId, paperId, examId, subStatus));
    }

    @Override
    public Long countUserPaper(String userId, String paperId, String examId, Integer subStatus) {
        return count(getUserPaperLambdaQueryWrapper(userId, paperId, examId, subStatus));
    }

    @Override
    @Transactional
    public UserPaper userPaperSub(UserInfo userInfo, String userPaperId) {
        UserPaper userPaper = getById(userPaperId);
        if (userPaper.getSubstatus().equals(1)) {// 已提交则无需重复提交
            return userPaper;
        }
        UserPaper userPaperUp = new UserPaper();
        userPaperUp.setId(userPaperId);
        userPaperUp.setBaseInfo(userInfo);
        userPaperUp.setSubstatus(1);
        userPaperUp.setSubdate(DateUtil.now());
        userPaperUp.setScore(this.sumScore(userPaper));
        updateById(userPaperUp);
        return userPaperUp;
    }

    /**
     * 算分
     */
    private double sumScore(UserPaper userPaper) {
        List<UserPaperQuestion> list = userPaperQuestionService.getPaperQ(userPaper);
        return list.stream().filter(item -> item.getScore() != null).map(item -> BigDecimal.valueOf(item.getScore())).reduce(BigDecimal.ZERO, BigDecimal::add).doubleValue();
    }

    private LambdaQueryWrapper<UserPaper> getUserPaperLambdaQueryWrapper(String userId, String paperId, String examId, Integer subStatus) {
        LambdaQueryWrapper<UserPaper> q = Wrappers.lambdaQuery();
        q.eq(UserPaper::getStatus, 1);
        q.eq(UserPaper::getUserid, userId);
        q.eq(UserPaper::getPaperid, paperId);
        q.eq(UserPaper::getExamid, examId);
        if (subStatus != null) {
            q.eq(UserPaper::getSubstatus, subStatus);
        }
        q.orderByDesc(UserPaper::getCdate, UserPaper::getId);
        return q;
    }

    private void clearUserPaper(List<UserPaper> listUserPaper) {
        if (listUserPaper.size() == 0) {
            return;
        }
        if (listUserPaper.size() == 1) {
            return;
        }
        List<UserPaper> userPaperListUp = new ArrayList<>();
        for (int i = 1; i < listUserPaper.size(); i++) {
            UserPaper up = new UserPaper();
            up.setId(listUserPaper.get(i).getId());
            up.setStatus(0);
            userPaperListUp.add(up);
        }
        updateBatchById(userPaperListUp);
    }

    /**
     * 新的试卷
     *
     * @param userInfo
     * @param paperId
     * @param examId
     * @return
     */
    private UserPaper newUserPaper(UserInfo userInfo, String paperId, String examId) {
        UserPaper userPaper = new UserPaper();
        userPaper.setBaseInfo(userInfo);
        userPaper.setSubstatus(0);
        userPaper.setExamid(examId);
        userPaper.setPaperid(paperId);
        userPaper.setUserid(userInfo.getId());
        save(userPaper);
        userPaperQuestionService.newPaperQ(userPaper, userInfo);// 给新的试卷生成新的题目
        return userPaper;
    }
}
