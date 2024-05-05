<!DOCTYPE html>
<html lang="zh">
<head>
    <#include "../commons/head.ftl"/>
</head>
<body>
<div class="lyear-layout-web">
    <div class="lyear-layout-container">
        <!--左侧导航-->
        <#include "../commons/aside.ftl"/>
        <!--End 左侧导航-->

        <!--头部信息-->
        <#include "../commons/header.ftl"/>
        <!--End 头部信息-->

        <!--页面主要内容-->
        <main class="lyear-layout-content">
            <div class="container-fluid">
                <div class="row">
                    <div class="col-12">
                        <div class="card">
                            <div class="card-header">
                                <form class="form-inline" method="post" action="#!" role="form" id="searchform">
                                    <div class="input-group">
                                        <div class="input-group-btn">
                                            <button class="btn btn-default" id="search-btn" type="button">
                                                角色名
                                            </button>
                                        </div>
                                        <input type="text" class="form-control" value="" name="keyinfo"
                                               placeholder="角色名">
                                    </div>

                                    <button type="button" id="searchBtn" class="btn btn-primary">搜索</button>
                                </form>
                            </div>

                            <div class="card-body">
                                <div id="custom-toolbar">
                                    <div class="form-inline" role="form">
                                        <button id="add" class="btn btn-primary m-r-5">
                                            新增
                                        </button>
                                        <button id="del" class="btn btn-primary m-r-5">
                                            删除
                                        </button>
                                    </div>
                                </div>
                                <div class="table-responsive">
                                    <table id="table-pagination"
                                           data-toolbar="#custom-toolbar"
                                           data-toggle="table"
                                           data-pagination="true"
                                           data-page-list="[10, 20, 50, 100, 200]"
                                           data-click-to-select="true"
                                           data-url="${ctx.contextPath}/admin/system/roleListData"
                                           data-side-pagination="server">
                                        <thead>
                                        <tr>
                                            <th data-checkbox="true"></th>
                                            <th data-field="name">角色名</th>
                                            <th data-field="cdate" data-formatter="cdate">创建时间</th>
                                            <th data-field="id" data-formatter="caozuo">操作</th>
                                        </tr>
                                        </thead>
                                    </table>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </main>
        <!--End 页面主要内容-->
    </div>
</div>
<#include "../commons/js.ftl"/>
<script type="text/javascript">
    function cdate(value, row) {
        if (value == '') {
            return '';
        }
        return value.substring(0, 10);
    }

    function caozuo(value, row) {
        let htm = '';
        htm += '<div class="btn-group">';
        htm += '<a class="btn btn-sm btn-default m-r-5" href="#" onclick="edit(\'' + value + '\')" title="编辑">编辑</a>';
        htm += '<a class="btn btn-sm btn-default m-r-5" href="${ctx.contextPath}/admin/system/roleFunctions.do?roleid=' + value + '" title="权限设置">权限设置</a>';
        htm += '<a class="btn btn-sm btn-default" href="#" onclick="userListByRole(\'' + value + '\')" title="分配权限">分配权限</a>';
        htm += '</div>';
        return htm;
    }

    $('#add').click(function () {
        window.location.href = '${ctx.contextPath}/admin/system/roleEdit';
    })

    function edit(id) {
        window.location.href = '${ctx.contextPath}/admin/system/roleEdit?roleId=' + id;
    }

    function userListByRole(id) {
        layer_show('分配权限', '${ctx.contextPath}/admin/system/userListByRole?roleId=' + id);
    }

    $('#del').click(function () {
        if (getSelectionIds() != false) {
            $.confirm({
                title: '提示',
                content: '是否要删除？',
                buttons: {
                    confirm: {
                        text: '确认',
                        action: function () {
                            $.ajax({
                                url: "${ctx.contextPath}/admin/system/roleDelete",
                                data: {
                                    'roleIds': getSelectionIds().join(',')
                                },
                                success: function (data) {
                                    if (data.result) {
                                        layer.msg(data.msg);
                                        $("#table-pagination").bootstrapTable('refresh');
                                    } else {
                                        alert(data.msg);
                                    }
                                }
                            });
                        }
                    },
                    cancel: {
                        text: '取消',
                        action: function () {
                        }
                    }
                }
            });
        }
    });

    $('#searchBtn').click(function () {
        $("#table-pagination").bootstrapTable('refresh', {
            url: "${ctx.contextPath}/admin/system/roleListData?" + $("#searchform").serialize()
        });
    });

</script>
</body>
</html>
