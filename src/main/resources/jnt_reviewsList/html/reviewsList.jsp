<%@ page import="java.util.Calendar" %>
<%@ taglib prefix="jcr" uri="http://www.jahia.org/tags/jcr" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="utility" uri="http://www.jahia.org/tags/utilityLib" %>
<%@ taglib prefix="template" uri="http://www.jahia.org/tags/templateLib" %>
<%@ taglib prefix="uiComponents" uri="http://www.jahia.org/tags/uiComponentsLib" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="functions" uri="http://www.jahia.org/tags/functions" %>
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
<template:addResources type="javascript" resources="jquery.min.js, bootstrap-alert.js"/>

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

                <c:set var="isForgeAdmin" value="${jcr:hasPermission(boundComponent.parent, 'jcr:all_live')}"/>

                <c:if test="${isForgeAdmin}">

                    <jcr:sql
                            var="reviews"
                            sql="SELECT * FROM [jnt:review] WHERE isdescendantnode(['${reviewsNode.path}']) AND reported = true AND (unjustifiedReport IS null OR unjustifiedReport = false) ORDER BY [jcr:created] DESC" />

                    <jcr:sql
                            var="replies"
                            sql="SELECT * FROM [jnt:review] AS review INNER JOIN [jnt:post] AS reply ON ischildnode(reply,review) WHERE isdescendantnode(review,['${reviewsNode.path}']) AND (review.reported IS null OR review.reported = false) AND reply.reported = true AND (reply.unjustifiedReport IS null OR reply.unjustifiedReport = false) ORDER BY review.[jcr:created] DESC" />

                    <c:set var="reportedOverallNbr" value="${functions:length(reviews.nodes) + functions:length(replies.nodes)}"/>
                    <c:set var="isReportedOverall" value="${reportedOverallNbr gt 0}"/>
                </c:if>

                <c:choose>
                    <c:when test="${isForgeAdmin && isReportedOverall}">

                        <div class="alert">
                            <button type="button" class="close" data-dismiss="alert">&times;</button>
                            <c:choose>
                                <c:when test="${reportedOverallNbr eq 1}">
                                    <fmt:message key="jnt_review.label.admin.alert.reportedReview"/>
                                </c:when>
                                <c:otherwise>
                                    <fmt:message key="jnt_review.label.admin.alert.reportedReviews">
                                        <fmt:param value="${reportedOverallNbr}"/>
                                    </fmt:message>
                                </c:otherwise>
                            </c:choose>
                        </div>

                    </c:when>
                    <c:otherwise>

                        <c:if test="${isForgeAdmin}">
                            <div class="alert alert-success">
                                <button type="button" class="close" data-dismiss="alert">&times;</button>
                                <fmt:message key="jnt_review.label.admin.alert.reportedReviewsEmpty"/>
                            </div>
                        </c:if>

                        <jcr:sql
                                var="reviews"
                                sql="SELECT * FROM [jnt:review] WHERE isdescendantnode(['${reviewsNode.path}']) AND content IS NOT null ORDER BY [jcr:created] DESC" />

                    </c:otherwise>
                </c:choose>
                
                <c:forEach items="${reviews.nodes}" var="review">

                    <c:set var="content" value="${review.properties['content'].string}" />

                    <c:if test="${fn:length(fn:trim(content)) gt 0}">

                        <c:if test="${empty reviewsListHeader}">
                            <h2><fmt:message key="jnt_reviewsList.label.reviewsList"/></h2>
                            <c:set var="reviewsListHeader" value="true"/>
                        </c:if>

                        <div class="reviewListItem">
                            <template:module node="${review}">
                                <%--<template:param name="module.cache.additional.key" value="${review.identifier}"/>
                                <template:param name="isForgeAdmin" value="${isForgeAdmin}"/>
                                <template:param name="isReportedOverall" value="${isReportedOverall}"/>--%>
                            </template:module>
                        </div>

                    </c:if>

                </c:forEach>



                <c:if test="${isForgeAdmin && isReportedOverall}">

                    <c:forEach items="${replies.rows}" var="row" varStatus="status">

                        <c:set var="review" value="${row.nodes['review']}"/>

                        <c:if test="${review.identifier ne lastReviewID}">

                            <c:set var="lastReviewID" value="${review.identifier}"/>
                            <c:set var="content" value="${review.properties['content'].string}" />

                            <c:if test="${fn:length(fn:trim(content)) gt 0}">

                                <c:if test="${empty reviewsListHeader}">
                                    <h2><fmt:message key="jnt_reviewsList.label.reviewsList"/></h2>
                                    <c:set var="reviewsListHeader" value="true"/>
                                </c:if>

                                <div class="reviewListItem">
                                    <template:module node="${review}">
                                       <%-- <template:param name="module.cache.additional.key" value="${review.identifier}"/>
                                        <template:param name="cache.mainResource.flushParent" value="true"/>
                                        <template:param name="isForgeAdmin" value="${isForgeAdmin}"/>
                                        <template:param name="isReportedOverall" value="${isReportedOverall}"/>--%>
                                    </template:module>
                                </div>

                            </c:if>

                        </c:if>

                    </c:forEach>

                </c:if>




            </c:when>
            <c:otherwise>
                <template:addCacheDependency node="${boundComponent}"/>
            </c:otherwise>
        </c:choose>
    </c:if>

</section>
