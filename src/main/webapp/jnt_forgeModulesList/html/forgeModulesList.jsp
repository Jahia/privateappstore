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

<template:include view="hidden.header"/>

<c:set var="columnsNumber" value="${currentNode.properties['columnsNumber'].long}"/>

<template:addResources type="javascript" resources="bootstrap.js"/>

<template:addResources type="inlinejavascript">
    <script type="text/javascript">
        $(document).ready(function() {
            $('.badge-reviewedByJahia').tooltip();
            $('.badge-supportedByJahia').tooltip();
        });
    </script>
</template:addResources>

<c:choose>
    <c:when test="${renderContext.editMode}}">
        <fmt:message key="jnt_forgeModulesList.label.liveOnly"/>
    </c:when>
    <c:otherwise>
        <c:forEach items="${moduleMap.currentList}" var="module" varStatus="status" begin="${moduleMap.begin}" end="${moduleMap.end}">

            <c:if test="${status.index % columnsNumber eq 0}">
                <div class="row-fluid">
            </c:if>

            <div class="span${functions:round(12 / columnsNumber)}">
                <template:module node="${module}"/>
            </div>

            <c:if test="${status.index % columnsNumber eq columnsNumber - 1}">
                </div>
            </c:if>

        </c:forEach>
    </c:otherwise>
</c:choose>