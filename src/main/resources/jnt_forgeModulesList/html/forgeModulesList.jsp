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
<c:set var="count" value="0"/>
<c:forEach items="${moduleMap.currentList}" var="module" varStatus="status" begin="${moduleMap.begin}" end="${moduleMap.end}">
    <c:choose>
        <c:when test="${status.index % columnsNumber eq 0}">
            <div class="row-fluid">
        </c:when>
        <c:otherwise>
            <c:set var="count" value="${count + 1}"/>
        </c:otherwise>
    </c:choose>

    <div class="span${functions:round(12 / columnsNumber)}">
        <template:module node="${module}"/>
    </div>  <!-- end span -->

    <c:if test="${(count + 1) eq columnsNumber || status.last}">
        <c:set var="count" value="0"/>
        </div> <!-- end row fluid -->
    </c:if>

</c:forEach>