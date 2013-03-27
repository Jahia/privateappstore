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
<template:addResources type="css" resources="modulesForge.css"/>

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

<jcr:sql
        var="moduleVersions"
        sql="SELECT * FROM [comnt:moduleVersion] WHERE isdescendantnode(['${currentNode.path}'])
              AND lastVersion = true"
        limit= '1' />

<section id="moduleOverview" itemtype="http://schema.org/SoftwareApplication">

    <c:forEach items="${moduleVersions.nodes}" var="lastVersion">

        <jcr:nodeProperty node="${lastVersion}" name="date" var="date"/>
        <jcr:nodeProperty node="${lastVersion}" name="version" var="version"/>
        <jcr:nodeProperty node="${lastVersion}" name="relatedJahiaVersion" var="relatedJahiaVersion"/>
        <jcr:nodeProperty node="${lastVersion}" name="moduleBinary" var="moduleBinary"/>
        <jcr:nodeProperty node="${fileMimeType}" name="fileMimeType" var="fileMimeType"/>

        <article class="moduleDescription">

            <h2>Description</h2>

            ${bigDescription}

            <footer>

                <a href="${authorURL}">Visit developer's website</a>
                <a href="mailto:${authorEmail}?Subject=${title}%20-%20Version:%20${version.string}">Email developer</a>

                <h4>Tags</h4>
                <ul class="modulesTags">
                    <c:forEach items="${assignedTags}" var="tag" varStatus="status">
                        <li>${tag.node.name}</li>
                    </c:forEach>
                </ul>

                <template:module node="${currentNode}" />

            </footer>

        </article>

        <aside class="moduleInformation">

                <dl class="moduleMetaData">

                    <h4>Information</h4>

                    <span content="${title}" itemprop="name"></span>
                    <span content="${icon.url}" itemprop="image"></span>
                    <span content="${fileMimeType}" itemprop="fileFormat"></span>
                    <span content="${moduleBinary.node.url}" itemprop="downloadUrl"></span>
                    <c:forEach items="${assignedTags}" var="tag" varStatus="status">
                        <span content="${tag.node.name}" itemprop="keywords"></span>
                    </c:forEach>

                    <dt>Updated:</dt>
                        <dd>
                            <time itemprop="datePublished">
                                <fmt:formatDate value="${date.date.time}" pattern="yyyy-MM-dd" />
                            </time>
                        </dd>

                    <dt>Current version:</dt>
                        <dd itemprop="softwareVersion">${version.string}</dd>

                    <dt>Requires Jahia:</dt>
                        <dd>${relatedJahiaVersion.node.displayableName}</dd>

                    <dt>Size:</dt>
                        <dd itemprop="fileSize">${jcr:humanReadableFileLength(moduleBinary.node)}</dd>

                    <div itemtype="http://schema.org/Organization" itemscope="" itemprop="author">

                        <dt>Author:</dt>
                            <dd itemprop="name">${authorName}</dd>

                        <span content="${authorURL}" itemprop="url"></span>
                        <span content="${authorEmail}" itemprop="email"></span>

                    </div>

                    <dt><h4>Category</h4></dt>
                        <dd itemprop="applicationCategory">${category.node.displayableName}</dd>

                    <c:if test="${jcr:isNodeType(currentNode, 'jmix:rating')}">
                    <dt><h4>Rating:</h4></dt>
                        <dd itemtype="http://schema.org/AggregateRating" itemscope="" itemprop="aggregateRating">
                            <div itemprop="ratingValue" content="${avgRating}">
                                <template:include view="hidden.average.readonly" />
                            </div>
                            (<span itemprop="ratingCount" content="${nbOfVotes}">${nbOfVotes}</span>)

                        </dd>
                    </c:if>

                </dl>



        </aside>

    </c:forEach>

</section>