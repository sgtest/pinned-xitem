<!DOCTYPE html>
<html lang="zh">
<head>
    <#include "../commons/head.ftl"/>
    <link href="${ctx.contextPath}/static/pc/exam/css/style.css" rel="stylesheet">
</head>
<body style="background-color: #fff">
<div class="paper-desc">
    <div>总题数：${paperVO.snum!}&nbsp;题&nbsp;&nbsp;总分：${paperVO.score!}&nbsp;分</div>
</div>
<div class="paper-box">
    <div class="paper-title">${paperVO.title!}</div>
    <#list paperVO.questionVOList as item>
        <div class="q-item-box">
            <div class="q-item-title-box">
                <label class="q-item-title">${item_index+1}.&nbsp;${item.title!}</label>
                <label class="q-item-score">【${item.qscore!}分】</label>
            </div>
            <div class="q-item-op-box">
                <#list item.questionOptionList as option>
                    <div class="q-item-op">
                        <label class="q-item-op-label">
                            ${option.title!}
                        </label>
                    </div>
                </#list>
            </div>
            <div>
                正确答案：${item.answer!}
            </div>
            <div <#if item.answer==(item.useranswer!)> class="text-success" </#if> >
                用户答案：${item.useranswer!}
            </div>
        </div>
    </#list>
</div>
<div style="position: fixed;bottom: 0;background-color: #fff;text-align: center;width: 100%">
    <button type="button" class="btn btn-secondary" onclick="javascript:history.back(-1);return false;">返回</button>
</div>
<#include "../commons/js.ftl"/>
<script type="text/javascript">
</script>
</body>
</html>
