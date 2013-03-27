<%@ taglib prefix="jcr" uri="http://www.jahia.org/tags/jcr" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="utility" uri="http://www.jahia.org/tags/utilityLib" %>
<%@ taglib prefix="template" uri="http://www.jahia.org/tags/templateLib" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
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

<template:addResources type="javascript" resources="jquery.min.js,jquery-ui.min.js"/>
<template:addResources type="css" resources="jquery-ui.smoothness.css, modulesForge.css"/>

<c:set var="screenshotsNodes" value="${jcr:getNodes(currentNode, 'comnt:moduleScreenshot')}"/>

<template:addResources type="inlinejavascript">
    <script type="text/javascript">

    $(document).ready(function() {

        $('#moduleScreenshotList').sortable({
           revert: true
        });

        $('#moduleScreenshotList, #moduleScreenshotList li').disableSelection();
    });

    </script>
</template:addResources>

<ul id="moduleScreenshotList">
    <c:forEach var="moduleScreenshot" items="${screenshotsNodes}">

        <li class="${moduleScreenshot.displayableName}">
            <template:module node="${moduleScreenshot}"/>
        </li>

    </c:forEach>
</ul>