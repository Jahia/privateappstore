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

<c:set var="id" value="${currentNode.identifier}"/>
<c:set var="title" value="${currentNode.properties['jcr:title'].string}"/>
<c:set var="icon" value="${currentNode.properties['icon'].node}"/>
<c:set var="authorName" value="${currentNode.properties['authorName'].string}"/>

<jcr:sql
        var="moduleVersions"
        sql="SELECT * FROM [comnt:moduleVersion] WHERE isdescendantnode(['${currentNode.path}'])
              AND lastVersion = true"
        limit= '1' />

<section id="moduleHeader">

    <header>

        <h1>${title}</h1>
        <a class="moduleAuthor">${authorName}</a>

    </header>

    <figure>

        <img src="${icon.url}" alt="icon"/>

        <c:if test="${jcr:isNodeType(currentNode, 'jmix:rating')}">
            <template:include view="hidden.average.readonly" />
        </c:if>

    </figure>

    <div class="downloadLink">
        <c:forEach items="${moduleVersions.nodes}" var="lastVersion">
            <jcr:nodeProperty node="${lastVersion}" name="moduleBinary" var="moduleBinary"/>
            <jcr:nodeProperty node="${lastVersion}" name="version" var="version"/>
            <a href="${moduleBinary.node.url}">Download version ${version.string}</a>
        </c:forEach>

    </div>





</section>