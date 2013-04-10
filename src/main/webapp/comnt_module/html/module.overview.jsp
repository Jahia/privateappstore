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

<template:addResources type="javascript" resources="html5shiv.js"/>
<template:addResources type="css" resources="modulesForge.css,review.css"/>
<template:addResources type="css" resources="commentable.css,ui.stars.css"/>
<template:addResources type="javascript" resources="jquery.min.js,jquery.validate.js,jquery-ui.min.js,ui.stars.js"/>

<c:set var="id" value="${currentNode.identifier}"/>
<c:set var="title" value="${currentNode.properties['jcr:title'].string}"/>
<c:set var="icon" value="${currentNode.properties['icon'].node}"/>
<c:set var="authorName" value="${currentNode.properties['authorName'].string}"/>
<c:set var="authorURL" value="${currentNode.properties['authorURL'].string}"/>
<c:set var="authorEmail" value="${currentNode.properties['authorEmail'].string}"/>
<c:set var="bigDescription" value="${currentNode.properties['bigDescription'].string}"/>
<c:set var="category" value="${currentNode.properties['category']}"/>
<c:set var="nbOfVotes"
       value="${not empty currentNode.properties['j:nbOfVotes'] ? currentNode.properties['j:nbOfVotes'].long : null}"/>
<c:set var="avgRating"
       value="${not empty nbOfVotes && not empty currentNode.properties['j:sumOfVotes'] ?
       currentNode.properties['j:sumOfVotes'].long / nbOfVotes : null}"/>
<jcr:nodeProperty node="${currentNode}" name="j:tags" var="assignedTags"/>
<jcr:node var="videoNode" path="${currentNode.path}/video"/>

<template:include view="sql">
    <template:param name="getActiveVersion" value="true"/>
</template:include>
<c:set value="${moduleMap.activeVersion}" var="activeVersion"/>
<c:set value="${moduleMap.activeVersionBinary}" var="activeVersionBinary"/>

<article id="moduleOverview" itemtype="http://schema.org/SoftwareApplication">

    <jcr:nodeProperty node="${activeVersion}" name="version" var="version"/>
    <jcr:nodeProperty node="${activeVersion}" name="relatedJahiaVersion" var="relatedJahiaVersion"/>

    <div class="mainContentWrapper">

        <section class="moduleDescription">

            <h2><fmt:message key="comnt_module.label.description"/></h2>

            ${bigDescription}

            <footer>

                <a href="${authorURL}"><fmt:message key="comnt_module.label.authorURL"/></a>
                <a href="mailto:${authorEmail}?Subject=${title}%20-%20Version:%20${version.string}"><fmt:message key="comnt_module.label.authorEmail"/></a>

                <c:if test="${not empty assignedTags}">

                    <section class="moduleTags">

                        <header><fmt:message key="comnt_module.label.tags"/></header>
                        <ul>
                            <c:forEach items="${assignedTags}" var="tag" varStatus="status">
                                <li>${tag.node.name}</li>
                            </c:forEach>
                        </ul>

                    </section>

                </c:if>

            </footer>

        </section>

        <c:if test="${not empty videoNode}">

            <section class="moduleVideo">

                <h2><fmt:message key="comnt_module.label.video"/></h2>

                <%-- TODO --%>
                <template:module node="${videoNode}"/>
                <template:module path="${videoNode.path}"/>

            </section>

        </c:if>

    </div>

    <aside class="moduleInformation">

        <dl class="moduleMetaData">

            <h4><fmt:message key="comnt_module.label.information"/></h4>

            <span content="${title}" itemprop="name"></span>
            <span content="${icon.url}" itemprop="image"></span>
            <span content="${activeVersionBinary.fileContent.contentType}" itemprop="fileFormat"></span>
            <span content="${activeVersionBinary.url}" itemprop="downloadUrl"></span>
            <c:forEach items="${assignedTags}" var="tag" varStatus="status">
                <span content="${tag.node.name}" itemprop="keywords"></span>
            </c:forEach>

            <dt><fmt:message key="comnt_module.label.updated"/></dt>
                <dd>
                    <time itemprop="datePublished">
                        <fmt:formatDate value="${activeVersionBinary.contentLastModifiedAsDate}" pattern="yyyy-MM-dd" />
                    </time>
                </dd>

            <dt><fmt:message key="comnt_module.label.version"/></dt>
                <dd itemprop="softwareVersion">${version.string}</dd>

            <dt><fmt:message key="comnt_module.label.relatedJahiaVersion"/></dt>
                <dd>${relatedJahiaVersion.node.displayableName}</dd>

            <dt><fmt:message key="comnt_module.label.fileSize"/></dt>
                <dd itemprop="fileSize">${jcr:humanReadableFileLength(activeVersionBinary)}</dd>

            <div itemtype="http://schema.org/Organization" itemscope="" itemprop="author">

                <dt><fmt:message key="comnt_module.label.authorName"/></dt>
                    <dd itemprop="name">${authorName}</dd>

                <span content="${authorURL}" itemprop="url"></span>
                <span content="${authorEmail}" itemprop="email"></span>

            </div>

            <c:if test="${jcr:isNodeType(currentNode, 'jmix:rating')}">
                <dt><fmt:message key="comnt_module.label.rating"/></dt>
                <dd itemtype="http://schema.org/AggregateRating" itemscope="" itemprop="aggregateRating">
                    <span itemprop="worstRating" content="1"></span>
                    <span itemprop="bestRating" content="5"></span>
                    <div itemprop="ratingValue" content="${avgRating}">
                        <template:include view="hidden.average.readonly" />
                    </div>
                    <span itemprop="ratingCount" content="${nbOfVotes}">(${nbOfVotes})</span>

                </dd>
            </c:if>

            <dt><h4><fmt:message key="comnt_module.label.category"/></h4></dt>
                <dd itemprop="applicationCategory">${category.node.displayableName}</dd>

        </dl>

    </aside>

</article>