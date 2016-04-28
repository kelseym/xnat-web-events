<%@ page session="true" contentType="text/html" pageEncoding="UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="pg" tagdir="/WEB-INF/tags/page" %>
<%--<%@ taglib prefix="sp" tagdir="/WEB-INF/tags/spawner" %>--%>

<c:if test="${empty hasInit}">
    <pg:init>
        <c:if test="${empty hasVars}">
            <pg:jsvars/>
        </c:if>
    </pg:init>
</c:if>

<c:set var="_msg">
    No spawning allowed.
</c:set>

<pg:restricted msg="${_msg}">

    <c:set var="_siteRoot" value="${sessionScope.siteRoot}"/>

    <c:import url="/xapi/spawner/resolve/siteAdmin/siteAdmin" var="siteAdmin"/>

    <%--<button type="button" id="view-json">View JSON</button>--%>
    <div class="hidden">${siteAdmin}</div>

    <!-- button element will be rendered in this span -->
    <span id="view-json"></span>

    <script>
        (function(){

            function showJSON(json){
                return xmodal.message({
                    title: 'Site Admin JSON',
                    maximize: true,
                    width: '90%',
                    height: '90%',
                    content: spawn('pre.json', JSON.stringify(json, null, 2)).outerHTML
                })
            }

            spawn('button|type=button', {
                html: 'View JSON',
                onclick: function(){
                    XNAT.xhr.get('${siteRoot}/xapi/spawner/resolve/siteAdmin/siteAdmin', function(data){
                        showJSON(data);
                    });
                },
                $: {
                    appendTo: '#view-json'
                }
            });

            //$('#view-json').click(function(){});

        })();
    </script>
</pg:restricted>
