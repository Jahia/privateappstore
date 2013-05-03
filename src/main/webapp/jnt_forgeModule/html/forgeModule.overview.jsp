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
<template:addResources type="css" resources="ui.stars.css"/>
<template:addResources type="javascript" resources="jquery.min.js,jquery.validate.js,jquery-ui.min.js,ui.stars.js"/>

<c:set var="id" value="${currentNode.identifier}"/>
<c:set var="title" value="${currentNode.properties['jcr:title'].string}"/>
<c:set var="icon" value="${currentNode.properties['icon'].node}"/>
<c:set var="authorName" value="${currentNode.properties['authorName'].string}"/>
<c:set var="authorURL" value="${currentNode.properties['authorURL'].string}"/>
<c:set var="authorEmail" value="${currentNode.properties['authorEmail'].string}"/>
<c:set var="description" value="${currentNode.properties['description'].string}"/>
<c:set var="category" value="${currentNode.properties['category']}"/>
<c:set var="nbOfVotes"
       value="${not empty currentNode.properties['j:nbOfVotes'] ? currentNode.properties['j:nbOfVotes'].long : null}"/>
<c:set var="avgRating"
       value="${not empty nbOfVotes && not empty currentNode.properties['j:sumOfVotes'] ?
       currentNode.properties['j:sumOfVotes'].long / nbOfVotes : null}"/>
<jcr:nodeProperty node="${currentNode}" name="j:tags" var="assignedTags"/>
<jcr:node var="videoNode" path="${currentNode.path}/video"/>

<template:include view="hidden.sql">
    <template:param name="getActiveVersion" value="true"/>
</template:include>
<c:set value="${moduleMap.activeVersion}" var="activeVersion"/>
<c:set value="${moduleMap.activeVersionBinary}" var="activeVersionBinary"/>

<article id="moduleOverview" itemtype="http://schema.org/SoftwareApplication">

    <jcr:nodeProperty node="${activeVersion}" name="versionNumber" var="versionNumber"/>
    <jcr:nodeProperty node="${activeVersion}" name="relatedJahiaVersion" var="relatedJahiaVersion"/>

    <div class="mainContentWrapper">

        <section class="moduleDescription">

            <h2><fmt:message key="jnt_forgeModule.label.description"/></h2>

            ${description}

            <footer>

                <a class="btn btn-small btn-success" href="${authorURL}"><fmt:message key="jnt_forgeModule.label.authorURL"/></a>
                <a class="btn btn-small btn-success" href="mailto:${authorEmail}?Subject=${title}%20-%20Version:%20${versionNumber.string}"><fmt:message key="jnt_forgeModule.label.authorEmail"/></a>

                <c:if test="${not empty assignedTags}">

                    <section class="moduleTags">

                        <header><fmt:message key="jnt_forgeModule.label.tags"/></header>
                        <ul class="inline">
                            <c:forEach items="${assignedTags}" var="tag" varStatus="status">
                                <li class="tag">${tag.node.name}</li>
                            </c:forEach>
                        </ul>

                    </section>

                </c:if>

            </footer>

        </section>

        <c:if test="${not empty videoNode}">

            <section class="moduleVideo">

                <h2><fmt:message key="jnt_forgeModule.label.video"/></h2>

                <%-- TODO --%>
                <template:module node="${videoNode}"/>
                <template:module path="${videoNode.path}"/>

            </section>

        </c:if>

    </div>


</article>