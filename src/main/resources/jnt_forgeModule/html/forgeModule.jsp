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

<template:addResources type="javascript" resources="html5shiv.js"/>
<template:addResources type="css" resources="forge.css,bootstrap-wysihtml5.css,bootstrap-editable.css"/>

<c:set var="id" value="${currentNode.identifier}"/>
<c:set var="title" value="${currentNode.properties['jcr:title'].string}"/>
<jcr:node var="iconFolder" path="${currentNode.path}/icon" />
<c:forEach var="iconItem" items="${iconFolder.nodes}">
    <c:set var="icon" value="${iconItem}"/>
</c:forEach>
<c:set var="description" value="${currentNode.properties['description'].string}"/>

<%@include file="../../commons/authorName.jspf"%>

<section class="forgeModule thumbnail">

    <header>
        <a href="<c:url value="${currentNode.url}" context="/"/>">
            <c:url var="iconUrl" value="${url.currentModule}/img/icon.png"/>
            <img class="moduleIcon" src="${not empty icon.url ? icon.url : iconUrl}"
                 alt="<fmt:message key="jnt_forgeEntry.label.moduleIcon"><fmt:param value="${title}"/></fmt:message>"/>
        </a>
        <a href="<c:url value="${currentNode.url}" context="/"/>"><h4>${title}</h4></a>
        <p class="moduleAuthor">${authorName}</p>
    </header>

    <p>${functions:abbreviate(functions:removeHtmlTags(description), 100,120,'...')}</p>

    <footer class="badges">
        <c:if test="${currentNode.properties['reviewedByJahia'].boolean}">
            <span class="badge badge-success badge-reviewedByJahia" data-toggle="tooltip" title="<fmt:message key="jnt_forgeEntry.label.admin.reviewedByJahia"/>"><i class="icon-ok icon-white"></i></span>
        </c:if>
        <c:if test="${currentNode.properties['supportedByJahia'].boolean}">
            <span class="badge badge-warning badge-supportedByJahia" data-toggle="tooltip" title="<fmt:message key="jnt_forgeEntry.label.admin.supportedByJahia"/>"><i class="icon-wrench icon-white"></i></span>
        </c:if>
    </footer>

</section>
