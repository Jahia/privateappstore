<%@ taglib prefix="jcr" uri="http://www.jahia.org/tags/jcr" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="utility" uri="http://www.jahia.org/tags/utilityLib" %>
<%@ taglib prefix="template" uri="http://www.jahia.org/tags/templateLib" %>
<%@ taglib prefix="uiComponents" uri="http://www.jahia.org/tags/uiComponentsLib" %>
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

<template:addResources type="javascript" resources="html5shiv.js"/>

<c:set var="boundComponent"
       value="${uiComponents:getBindedComponent(currentNode, renderContext, 'j:bindedComponent')}"/>

<section id="reviewsList">

    <c:if test="${renderContext.editMode}">
        <fmt:message key="jnt_reviewsList.label.reviewsList"/>
    </c:if>

    <c:if test="${not empty boundComponent}">
        <jcr:node var="reviewsNode" path="${boundComponent.path}/reviews"/>
        <c:choose>
            <c:when test="${not empty reviewsNode}">
                <template:addCacheDependency node="${reviewsNode}"/>

                <jcr:sql
                        var="reviews"
                        sql="SELECT * FROM [jnt:review] WHERE isdescendantnode(['${reviewsNode.path}']) ORDER BY [jcr:lastModified] DESC" />

                <c:forEach items="${reviews.nodes}" var="review">

                    <c:set var="content" value="${review.properties['content'].string}" />

                    <c:if test="${fn:length(fn:trim(content)) gt 0}">

                        <div class="reviewListItem">
                            <template:module node="${review}"/>
                        </div>

                    </c:if>

                </c:forEach>
            </c:when>
            <c:otherwise>
                <template:addCacheDependency node="${boundComponent}"/>
            </c:otherwise>
        </c:choose>
    </c:if>

</section>
