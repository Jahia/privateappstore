<%@ taglib prefix="jcr" uri="http://www.jahia.org/tags/jcr" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="utility" uri="http://www.jahia.org/tags/utilityLib" %>
<%@ taglib prefix="template" uri="http://www.jahia.org/tags/templateLib" %>
<%@ taglib prefix="uiComponents" uri="http://www.jahia.org/tags/uiComponentsLib" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="query" uri="http://www.jahia.org/tags/queryLib" %>
<%@ taglib prefix="user" uri="http://www.jahia.org/tags/user" %>
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
<template:addResources type="javascript" resources="jquery.min.js, bootstrap-modal.js, modulesForge.js"/>

<c:set var="id" value="${currentNode.identifier}"/>
<c:set var="createdBy" value="${currentNode.properties['jcr:createdBy'].string}"/>
<c:set var="content" value="${currentNode.properties['content'].string}"/>
<c:set var="created" value="${currentNode.properties['jcr:created'].date.time}"/>

<c:if test="${createdBy ne 'guest'}">
    <jcr:node var="user" path="${user:lookupUser(createdBy).localPath}"/>
    <jcr:nodeProperty node="${user}" name="j:publicProperties" var="publicProperties" />

    <c:forEach items="${publicProperties}" var="value">
        <c:set var="publicPropertiesAsString" value="${value.string} ${publicPropertiesAsString}"/>
    </c:forEach>

    <jcr:nodeProperty var="picture" node="${user}" name="j:picture"/>

    <c:if test="${fn:contains(publicPropertiesAsString,'j:firstName')}">
        <c:set var="firstName" value="${user.properties['j:firstName'].string}"/>
    </c:if>
    <c:if test="${fn:contains(publicPropertiesAsString,'j:lastName')}">
        <c:set var="lastName" value="${user.properties['j:lastName'].string}"/>
    </c:if>
</c:if>

<article class="review media" itemscope itemtype="http://schema.org/Review">

    <div class="authorImage pull-left">

        <c:if test="${not empty picture}">
            <img
                    class="media-object"
                    src="${picture.node.thumbnailUrls['avatar_60']}"
                    <c:choose>
                        <c:when test="${not empty firstName || not empty lastName}">
                            alt="${fn:escapeXml(firstName)}<c:if test="${not empty firstName}">&nbsp;</c:if>${fn:escapeXml(lastName)}"
                        </c:when>
                        <c:otherwise>
                            alt="${createdBy}"
                        </c:otherwise>
                    </c:choose>
                    width="60"
                    height="60"
                    itemprop="image"/>
        </c:if>
        <c:if test="${empty picture}"><img alt="" src="<c:url value='/modules/default/images/userbig.png'/>"/></c:if>
    </div>

    <div class="media-body">

        <header>

            <div class="media-heading">

                <span itemprop="author" itemscope itemtype="http://schema.org/Person">
                    <c:choose>
                        <c:when test="${not empty user}">
                            <a class="authorName" href="<c:url value='${url.base}${user.path}.html'/>" itemprop="name">
                                <c:choose>
                                    <c:when test="${not empty firstName || not empty lastName}">
                                        ${fn:escapeXml(firstName)}<c:if test="${not empty firstName}">&nbsp;</c:if>${fn:escapeXml(lastName)}
                                    </c:when>
                                    <c:otherwise>
                                        ${createdBy}
                                    </c:otherwise>
                                </c:choose>
                            </a>
                        </c:when>
                        <c:otherwise>
                            <span class="authorName" itemprop="name">${fn:escapeXml(currentNode.properties.pseudo.string)}</span>
                        </c:otherwise>
                    </c:choose>
                </span>

                <time itemprop="datePublished" datetime="<fmt:formatDate value="${created}" pattern="yyyy-MM-dd" />">
                    <fmt:formatDate value="${created}" dateStyle="long" />
                </time>

                <%--- check if curent user is the owner of the module ---%>
                <c:if test="${renderContext.loggedIn && jcr:hasPermission(currentNode.parent.parent.parent, 'jcr:all_live')}">

                    <div class="pull-right">
                        <%@include file="../../commons/reportButton.jspf"%>
                    </div>

                </c:if>

            </div>

        </header>

        <div itemprop="reviewBody">
            ${fn:escapeXml(content)}
        </div>

    </div>

</article>