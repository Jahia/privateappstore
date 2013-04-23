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
<template:addResources type="css" resources="ui.stars.css"/>
<template:addResources type="javascript" resources="jquery.min.js,ui.stars.js"/>

<c:set var="id" value="${currentNode.identifier}"/>
<c:set var="createdBy" value="${currentNode.properties['jcr:createdBy'].string}"/>
<c:set var="rating" value="${currentNode.properties['rating'].long}"/>
<c:set var="title" value="${currentNode.properties['jcr:title'].string}"/>
<c:set var="content" value="${currentNode.properties['content'].string}"/>
<c:set var="created" value="${currentNode.properties['jcr:created'].date.time}"/>
<jcr:sql
        var="replies"
        sql="SELECT * FROM [jnt:post] WHERE isdescendantnode(['${currentNode.path}'])
              ORDER BY [jcr:lastModified] ASC" />

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

<script type="text/javascript">

    $(document).ready(function(){

        $("#reviewRating-${id}").stars();

        <c:if test="${renderContext.loggedIn && jcr:hasPermission(currentNode, 'jcr:all_live')}">

            $('#replyReviewToggle-${id}').click( function() {

                $(this).toggleClass('btn-inverse');
                $('#replyReview-${id}').slideToggle();
            });

        </c:if>

    });

</script>

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

                <c:if test="${renderContext.loggedIn && jcr:hasPermission(currentNode, 'jcr:all_live')}">

                    <span class="pull-right"><button id="replyReviewToggle-${id}" class="btn btn-small"><fmt:message key="jnt_review.label.reply"/></button></span>

                </c:if>

                <div itemprop="reviewRating" itemscope itemtype="http://schema.org/Rating">

                    <span itemprop="worstRating" content="1"></span>
                    <span itemprop="bestRating" content="5"></span>
                    <span itemprop="ratingValue" content="${rating}"></span>

                    <form id="reviewRating-${id}" class="reviewRating" style="width: 200px" action="">
                        <input type="radio" name="rate_avg" value="1" title="<fmt:message key="jnt_review.label.rating.poor"/>"
                               disabled="disabled" ${rating >= 1.0 ? 'checked="checked"' : ''}/>
                        <input type="radio" name="rate_avg" value="2" title="<fmt:message key="jnt_review.label.rating.fair"/>"
                               disabled="disabled" ${rating >= 2.0 ? 'checked="checked"' : ''}/>
                        <input type="radio" name="rate_avg" value="3" title="<fmt:message key="jnt_review.label.rating.average"/>"
                               disabled="disabled" ${rating >= 3.0 ? 'checked="checked"' : ''}/>
                        <input type="radio" name="rate_avg" value="4" title="<fmt:message key="jnt_review.label.rating.good"/>"
                               disabled="disabled" ${rating >= 4.0 ? 'checked="checked"' : ''}/>
                        <input type="radio" name="rate_avg" value="5" title="<fmt:message key="jnt_review.label.rating.excellent"/>"
                               disabled="disabled" ${rating >= 5.0 ? 'checked="checked"' : ''}/>
                    </form>

                </div>

                <h4 itemprop="headline">${fn:escapeXml(title)}</h4>

            </div>

        </header>

        <div itemprop="reviewBody">
            ${fn:escapeXml(content)}
        </div>

        <c:forEach items="${replies.nodes}" var="reply">

            <template:module node="${reply}" view="reply"/>

        </c:forEach>

    </div>

    <c:if test="${renderContext.loggedIn && jcr:hasPermission(currentNode, 'jcr:all_live')}">

        <div id="replyReview-${id}" class="replyReview box box-rounded box-tinted box-margin-top">

            <template:tokenizedForm>

                <form id="replyReviewForm-${id}" class="replyReviewForm form-horizontal" action="<c:url value='${url.base}${currentNode.path}.replyReview.do'/>" method="post">
                    <input type="hidden" name="jcrNodeType" value="jnt:review"/>

                    <fieldset>

                        <div class="control-group">
                            <label class="control-label" for="reviewReply-${id}">
                                <fmt:message key="jnt_review.label.reply"/>
                            </label>
                            <div class="controls">
                                <textarea rows="7" id="reviewReply-${id}"
                                          name="content"
                                        ><c:if test="${not empty sessionScope.formDatas['content']}">
                                    ${fn:escapeXml(sessionScope.formDatas['content'][0])}
                                </c:if></textarea>
                            </div>
                        </div>

                        <div class="control-group">
                            <div class="controls">
                                <input class="btn btn-primary" type="submit" value="<fmt:message key='jnt_review.label.submitReply'/>"/>
                            </div>
                        </div>

                    </fieldset>


                </form>

            </template:tokenizedForm>

        </div>

    </c:if>

</article>