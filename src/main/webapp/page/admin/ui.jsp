<%@ page session="true" contentType="text/html" pageEncoding="UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="pg" tagdir="/WEB-INF/tags/page" %>

<c:if test="${empty hasInit}">
    <pg:init>
        <c:if test="${empty hasVars}">
            <pg:jsvars/>
        </c:if>
    </pg:init>
</c:if>

<pg:restricted>
    <div id="admin-page">
        <header id="content-header">
            <h2 class="pull-left">Site Administration</h2>
            <div class="clearfix"></div>
        </header>

        <!-- Admin tab container -->
        <div id="admin-config-tabs" class="content-tabs xnat-tab-container">
            <div class="xnat-nav-tabs side pull-left">
                <!-- ================== -->
                <!-- Admin tab flippers -->
                <!-- ================== -->
            </div>
            <div class="xnat-tab-content side pull-right">
                <!-- ================== -->
                <!-- Admin tab panes    -->
                <!-- ================== -->
            </div>
        </div>

        <script>
            (function(){
                // get the JSON and do the setup
                var jsonUrl = XNAT.url.rootUrl('/page/admin/data/config/site-admin.json');
                $.getJSON(jsonUrl).done(function(data){
                    var adminTabs =
                                XNAT.ui.tabs
                                    .init(data.Result)
                                    .render('#admin-config-tabs');

                    console.log(adminTabs);
                });
            })();
        </script>

    </div>
</pg:restricted>
