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
<template:addResources type="css" resources="forge.css"/>

<c:set var="id" value="${currentNode.identifier}"/>
<c:set var="title" value="${currentNode.properties['jcr:title'].string}"/>
<jcr:node var="iconFolder" path="${renderContext.mainResource.node.path}/icon" />
<c:forEach var="iconItem" items="${iconFolder.nodes}">
    <c:set var="icon" value="${iconItem}"/>
</c:forEach>
<c:set var="description" value="${currentNode.properties['description'].string}"/>

<%@include file="../../commons/authorName.jspf"%>

<section class="forgeModule">

    <header>
        <a href="<c:url value="${currentNode.url}"/>">
            <c:url var="iconUrl" value="${url.currentModule}/img/icon.png"/>
            <img class="moduleIcon media-object" src="${not empty icon.url ? icon.url : iconUrl}"
                 alt="<fmt:message key="jnt_forgeModule.label.moduleIcon"><fmt:param value="${title}"/></fmt:message>"/>
        </a>
        <a href="<c:url value="${currentNode.url}"/>"><h4>${title}</h4></a>
        <a class="moduleAuthor">${authorName}</a>
    </header>

    <p>${functions:abbreviate(functions:removeHtmlTags(description), 100,120,'...')}</p>

</section>
