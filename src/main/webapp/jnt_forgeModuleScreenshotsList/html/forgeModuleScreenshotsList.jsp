<%@ taglib prefix="jcr" uri="http://www.jahia.org/tags/jcr" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="utility" uri="http://www.jahia.org/tags/utilityLib" %>
<%@ taglib prefix="template" uri="http://www.jahia.org/tags/templateLib" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="functions" uri="http://www.jahia.org/tags/functions" %>
<%@ taglib prefix="user" uri="http://www.jahia.org/tags/user" %>
<%@ taglib prefix="query" uri="http://www.jahia.org/tags/queryLib" %>
<%--@elvariable id="currentNode" type="org.jahia.services.content.JCRNodeWrapper"--%>
<%--@elvariable id="out" type="java.io.PrintWriter"--%>
<%--@elvariable id="script" type="org.jahia.services.render.scripting.Script"--%>
<%--@elvariable id="scriptInfo" type="java.lang.String"--%>
<%--@elvariable id="workspace" type="java.lang.String"--%>
<%--@elvariable id="renderContext" type="org.jahia.services.render.RenderContext"--%>
<%--@elvariable id="currentResource" type="org.jahia.services.render.Resource"--%>
<%--@elvariable id="currentUser" type="org.jahia.services.usermanager.JahiaUser"--%>
<%--@elvariable id="url" type="org.jahia.services.render.URLGenerator"--%>

<template:addResources type="javascript" resources="jquery.min.js,jquery-ui.min.js,bootstrap.js"/>
<template:addResources type="css" resources="jquery-ui.smoothness.css"/>

<c:set var="id" value="${currentNode.identifier}"/>
<c:set var="isDeveloper" value="${jcr:hasPermission(currentNode, 'jcr:write')}"/>
<c:if test="${isDeveloper}">
    <c:set var="viewAsUser" value="${not empty param['viewAs'] && param['viewAs'] eq 'user'}" />
</c:if>

<template:include view="hidden.header"/>

<c:if test="${empty moduleMap.currentList && (not isDeveloper || viewAsUser)}">

    <c:set var="isEmptyTab" value="true"/>
    <template:addResources type="inlinejavascript">
        <script type="text/javascript">

            $(document).ready(function() {

                var tabID = $('#moduleScreenshots').parents('.tab-pane').attr("id");
                var navTabSelector = "a[href='#" + tabID + "']";

                $(".jnt_bootstrapTabularList").find(navTabSelector).parent().remove();
            });
        </script>
    </template:addResources>
</c:if>

<c:if test="${not isEmptyTab}">

    <c:set var="columnsNumber" value="4"/>

    <template:addResources type="inlinejavascript">
        <script type="text/javascript">

        $(document).ready(function() {

            <c:choose>

                <c:when test="${isDeveloper && not viewAsUser}">

                    $('#moduleScreenshotsList').sortable({
                       revert: true
                    });

                    $('#moduleScreenshotsList').on('sortstop', function(event, ui) {

                        var movedScreenshot = $(ui.item[0]);
                        var folder = movedScreenshot.attr("data-parent-path");

                        var data = {};
                        data['source'] = folder + "/" + movedScreenshot.attr("data-name");

                        if (movedScreenshot.is(':last-child')) {
                            data['target'] = folder;
                            data['action'] = "moveAfter";
                        }
                        else {
                            data['target'] = folder + "/" + movedScreenshot.next().attr("data-name");
                            data['action'] = "moveBefore";
                        }

                        $.post('<c:url value="${url.base}${currentNode.path}.move.do"/>', data, function () {

                        }, "json");

                    });

                    $('.remove-screenshot').click(function() {
                        var listItem = $(this).parent('li.moduleScreenshot');
                        $.post($(this).attr('data-path'), {jcrMethodToCall: 'delete'}, function() {
                            if($('#moduleScreenshotsList li').length == 1)
                                $('#jnt_forge').triggerHandler('forgeModuleUpdated');
                            listItem.fadeOut('slow', function() {listItem.remove()});
                        }, "json");
                    });

                    $('#moduleScreenshotsList, #moduleScreenshotsList li').disableSelection();

                </c:when>

                <c:otherwise>

                    $('#screenshotsCarousel-${id}').carousel();

                </c:otherwise>

            </c:choose>

        });

        </script>
    </template:addResources>

    <c:choose>

        <c:when test="${isDeveloper && not viewAsUser}">
            <div class="row-fluid">
                <ul id="moduleScreenshotsList" class="thumbnails">
                    <c:forEach var="moduleScreenshot" items="${moduleMap.currentList}" varStatus="status">
                        <li class="moduleScreenshot span${functions:round(12/columnsNumber)}${status.index % columnsNumber eq 0 ? '' : ''}"
                            data-name="${moduleScreenshot.name}" data-parent-path="${moduleScreenshot.parent.path}">
                            <img class="move-screenshot" src="${moduleScreenshot.thumbnailUrls['thumbnail2']}"/>
                            <a class="remove-screenshot" data-path="<c:url value='${url.base}${moduleScreenshot.path}'/>"
                               href="#"><i class="icon-remove"></i>&nbsp;<fmt:message key="jnt_forgeModule.label.remove"/></a>
                        </li>
                    </c:forEach>
                </ul>
            </div>
        </c:when>

        <c:otherwise>
            <c:if test="${not empty moduleMap.currentList}">
                <div id="screenshotsCarousel-${id}" class="carousel slide">

                    <%--<ol class="carousel-indicators">--%>
                        <%--<c:forEach var="moduleScreenshot" items="${moduleMap.currentList}" varStatus="status">--%>
                            <%--<li data-target="#screenshotsCarousel-${id}" data-slide-to="${status.index}" class="${status.first ? 'active' : ''}"></li>--%>
                        <%--</c:forEach>--%>
                    <%--</ol>--%>

                    <div class="carousel-inner">
                        <c:forEach var="moduleScreenshot" items="${moduleMap.currentList}" varStatus="status">
                            <div class="${status.first ? 'active ' : ''}item">
                                <template:module node="${moduleScreenshot}" view="${moduleMap.subNodesView}" editable="${moduleMap.editable}"/>
                            </div>
                        </c:forEach>
                    </div>

                    <a class="carousel-control left" href="#screenshotsCarousel-${id}" data-slide="prev"></a>
                    <a class="carousel-control right" href="#screenshotsCarousel-${id}" data-slide="next"></a>

                </div>
            </c:if>
        </c:otherwise>

    </c:choose>

    <template:include view="hidden.footer"/>

</c:if>