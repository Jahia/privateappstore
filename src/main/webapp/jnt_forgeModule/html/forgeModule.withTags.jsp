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

<template:addResources type="javascript" resources="html5shiv.js, bootstrap.js"/>
<template:addResources type="css" resources="forge.css"/>

<c:set var="id" value="${currentNode.identifier}"/>
<c:set var="title" value="${currentNode.properties['jcr:title'].string}"/>
<jcr:node var="iconFolder" path="${currentNode.path}/icon" />
<c:forEach var="iconItem" items="${iconFolder.nodes}">
    <c:set var="icon" value="${iconItem}"/>
</c:forEach>

<%@include file="../../commons/authorName.jspf"%>

<article>
    <div class="media-body">
        <a class="media-photo" href="<c:url value="${currentNode.url}"/>" context="/">
            <c:url var="iconUrl" value="${url.currentModule}/img/icon.png"/>
            <img class="moduleIcon" src="${not empty icon.url ? icon.url : iconUrl}"
                 alt="<fmt:message key="jnt_forgeModule.label.moduleIcon"><fmt:param value="${title}"/></fmt:message>"/>
        </a>
        <h3 class="media-heading"><a href="<c:url value="${currentNode.url}" context="/"/>">${title}&nbsp;<small>${authorName}</small></a></h3>
        <jsp:useBean id="filteredTags" class="java.util.LinkedHashMap"/>
        <c:forEach items="${currentNode.properties['j:tags']}" var="tag" varStatus="status">
            <c:if test="${not empty tag.node}">
                <c:set target="${filteredTags}" property="${tag.node.identifier}" value="${tag.node.name}"/>
            </c:if>
        </c:forEach>
        <c:if test="${not empty filteredTags}">
            <c:forEach items="${filteredTags}" var="tag" varStatus="status">
                <c:if test="status.first"><p class="media-info"></c:if>
                <span class="label label-media-info"><i class="fa fa-tag"></i> ${fn:escapeXml(tag.value)}</span>
                <c:if test="status.end"></p></c:if>
            </c:forEach>
        </c:if>
    </div>
</article>