package com.xxsword.xitem.pc;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xxsword.xitem.admin.domain.exam.convert.PaperConvert;
import com.xxsword.xitem.admin.domain.exam.entity.*;
import com.xxsword.xitem.admin.domain.exam.vo.PaperVO;
import com.xxsword.xitem.admin.domain.exam.vo.QuestionVO;
import com.xxsword.xitem.admin.domain.exam.vo.UserPaperVO;
import com.xxsword.xitem.admin.domain.system.entity.UserInfo;
import com.xxsword.xitem.admin.model.RestResult;
import com.xxsword.xitem.admin.service.exam.*;
import com.xxsword.xitem.admin.utils.DateUtil;
import com.xxsword.xitem.admin.utils.ExamUtil;
import com.xxsword.xitem.admin.utils.Utils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("pc/exam")
public class PcExamController {
    @Autowired
    private ExamService examService;
    @Autowired
    private UserPaperService userPaperService;
    @Autowired
    private QuestionService questionService;
    @Autowired
    private UserPaperQuestionService userPaperQuestionService;
    @Autowired
    private QuestionRuleService questionRuleService;
    @Autowired
    private PaperService paperService;

    @RequestMapping("index")
    public String index(Model model) {
        LambdaQueryWrapper<Exam> examQ = Wrappers.lambdaQuery();
        examQ.eq(Exam::getStatus, 1);
        Page<Exam> examPage = examService.page(new Page<>(1, 50), examQ);
        model.addAttribute("examList", examPage.getRecords());
        return "/pc/exam/examindex";
    }

    /**
     * 考试简介页
     *
     * @param eid
     * @param model
     * @return
     */
    @RequestMapping("{eid}")
    public String examid(HttpServletRequest request, @PathVariable String eid, Model model) {
        UserInfo userInfo = Utils.getUserInfo(request);
        Exam exam = examService.getById(eid);
        List<UserPaper> userPaperList = userPaperService.listUserPaper(userInfo.getId(), exam.getPaperid(), exam.getId(), 1);
        List<UserPaperVO> userPaperVOList = userPaperService.listUserPaperVOByUserPaper(userPaperList);
        model.addAttribute("exam", exam);
        model.addAttribute("paperScore", questionRuleService.getPaperScore(exam.getPaperid()));
        model.addAttribute("examStatus", ExamUtil.getExamStatus(exam));
        model.addAttribute("listUserPaper", userPaperVOList);
        return "/pc/exam/exam";
    }

    /**
     * 进入考试
     *
     * @param request
     * @param eid
     * @return
     */
    @RequestMapping("examPageShow")
    public String examPageShow(HttpServletRequest request, String eid, Model model) {
        UserInfo userInfo = Utils.getUserInfo(request);
        Exam exam = examService.getById(eid);
        if (!examExamStatusCheck(exam, model)) {
            return "/pc/exam/examerror";
        }
        Long countUserPaper = userPaperService.countUserPaper(userInfo.getId(), exam.getPaperid(), exam.getId(), 1);
        if (countUserPaper >= exam.getMaxnum() && exam.getMaxnum() > 0) {
            model.addAttribute("exStatus", "已超过最大考试次数");
            return "/pc/exam/examerror";
        }
        UserPaper userPaper = userPaperService.getUserPaper(userInfo, exam.getPaperid(), exam.getId(), 1);
        if (!examCheckUserPaper(exam, userPaper, model)) {
            return "/pc/exam/examerror";
        }
        List<String> userPaperQuestionIds = userPaperQuestionService.getPaperQ(userPaper).stream().map(UserPaperQuestion::getId).collect(Collectors.toList());
        model.addAttribute("examTitle", exam.getTitle());
        model.addAttribute("userPaperQuestionIds", String.join(",", userPaperQuestionIds));
        model.addAttribute("endTime", ExamUtil.examEndTime(exam, userPaper));
        model.addAttribute("userPaperId", userPaper.getId());
        return "pc/exam/paperquestion";
    }

    /**
     * 考试状态检查
     *
     * @param exam
     * @param model
     * @return true-可以考 false-不能考
     */
    private boolean examExamStatusCheck(Exam exam, Model model) {
        int exStatus = ExamUtil.getExamStatus(exam);
        if (exStatus != 1) {// 考试时间
            if (exStatus == 0) {
                model.addAttribute("exStatus", "考试未开始");
            }
            if (exStatus == 2) {
                model.addAttribute("exStatus", "考试已结束");
            }
            return false;
        }
        return true;
    }

    /**
     * 考试时长超时状态检查
     *
     * @param exam
     * @return
     */
    private boolean examCheckUserPaper(Exam exam, UserPaper userPaper, Model model) {
        if (!ExamUtil.examDurationCheck(exam, userPaper)) {
            model.addAttribute("duration", exam.getDuration());
            model.addAttribute("cDate", userPaper.getCdate());
            model.addAttribute("nowDate", DateUtil.now());
            model.addAttribute("exStatus", "超过考试时长未交卷");
            model.addAttribute("userPaperId", userPaper.getId());
            return false;
        }
        return true;
    }

    /**
     * 考试页中的下一题
     *
     * @return
     */
    @RequestMapping("getQuestion")
    @ResponseBody
    public RestResult getQuestion(HttpServletRequest request, String nextQid) {
        UserPaperQuestion userPaperQuestion = userPaperQuestionService.getById(nextQid);
        UserPaper userPaper = userPaperService.getById(userPaperQuestion.getUserpaperid());
        if (userPaper == null) {
            return RestResult.Fail("参数异常");
        }
        if (!ExamUtil.examDurationCheck(examService.getById(userPaper.getExamid()), userPaper)) {
            return RestResult.Fail("已超过考试时长，请提交试卷");
        }
        if (userPaper.getStatus() == 1 && userPaper.getSubstatus() == 0) {
            QuestionVO questionVO = questionService.getQuestionVO(userPaperQuestion, true, false, true);
            return RestResult.OK(questionVO);
        } else {
            return RestResult.Fail("考试状态异常，请重新进入");
        }
    }

    @RequestMapping("saveAnswer")
    @ResponseBody
    public RestResult saveAnswer(HttpServletRequest request, String qid, String answers) {
        if (StringUtils.isBlank(answers)) {
            return RestResult.OK("未作答");
        }
        UserInfo userInfo = Utils.getUserInfo(request);
        userPaperQuestionService.upUserPaperQuestionAnswers(userInfo, qid, answers);
        return RestResult.OK();
    }

    /**
     * 考试提交
     *
     * @param userPaperId
     * @return
     */
    @RequestMapping("examPageSubmit")
    @ResponseBody
    public RestResult examPageSubmit(HttpServletRequest request, String userPaperId) {
        UserInfo userInfo = Utils.getUserInfo(request);
        userPaperService.userPaperSub(userInfo, userPaperId);
        return RestResult.OK();
    }

    /**
     * 考试提交成功页
     *
     * @return
     */
    @RequestMapping("examSubmitOk")
    public String examSubmitOk(String userPaperId, Model model) {
        UserPaper userPaper = userPaperService.getById(userPaperId);
        Exam exam = examService.getById(userPaper.getExamid());
        model.addAttribute("score", userPaper.getScore());// 得分
        model.addAttribute("maxscore", questionRuleService.getPaperScore(exam.getPaperid()));// 总分
        model.addAttribute("examtitle", exam.getTitle());
        model.addAttribute("paperduration", DateUtil.sToHHmmss(DateUtil.differSecond(userPaper.getCdate(), userPaper.getSubdate(), DateUtil.sdfA1)));// 考试用时
        return "/pc/exam/examok";
    }

    /**
     * 哪些题目未作答
     *
     * @param userPaperId
     * @return
     */
    @RequestMapping("checkBlankNum")
    @ResponseBody
    public RestResult checkBlankNum(String userPaperId) {
        UserPaper userPaper = userPaperService.getById(userPaperId);
        List<Integer> integerList = userPaperQuestionService.checkBlankNum(userPaper);
        return RestResult.OK(integerList);
    }

    /**
     * 答题卡显示
     */
    @RequestMapping("examSheet")
    public String examSheet(String userPaperId, Model model) {
        UserPaper userPaper = userPaperService.getById(userPaperId);
        List<UserPaperQuestion> userPaperQuestionList = userPaperQuestionService.getPaperQ(userPaper);
        model.addAttribute("userPaperQuestionList", userPaperQuestionList);
        return "/pc/exam/examsheet";
    }

    /**
     * 用户自己的查卷
     *
     * @param request
     * @return
     */
    @RequestMapping("userPaperPreview")
    public String userPaperPreview(HttpServletRequest request, String userPaperId, Model model) {
        UserPaper userPaper = userPaperService.getById(userPaperId);
        Paper paper = paperService.getById(userPaper.getPaperid());
        List<QuestionVO> questionVOList = userPaperQuestionService.listQuestionByUserPaper(userPaper, true, true, true);
        PaperVO paperVO = PaperConvert.INSTANCE.toPaperVO(paper);
        paperVO.setQuestionVOList(questionVOList);
        paperVO.setSnum(questionVOList.size());
        paperVO.setScore(Utils.sum(questionVOList.stream().map(QuestionVO::getQscore).collect(Collectors.toList())));

        model.addAttribute("paperVO", paperVO);
        return "/pc/exam/userpaperpreview";
    }
}
