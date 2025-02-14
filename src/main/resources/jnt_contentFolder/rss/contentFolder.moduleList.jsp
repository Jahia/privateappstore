<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="functions" uri="http://www.jahia.org/tags/functions" %>
<%@ taglib prefix="jcr" uri="http://www.jahia.org/tags/jcr" %>
<%@ taglib prefix="template" uri="http://www.jahia.org/tags/templateLib" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>

<template:addCacheDependency flushOnPathMatchingRegexp="\Q${currentNode.path}\E/.*"/>
<?xml version="1.0" encoding="UTF-8"?>
<rss version="2.0" xmlns:atom="http://www.w3.org/2005/Atom">
    <channel>
        <title>Jahia modules RSS feed</title>
        <link>${url.server}${url.context}</link>
        <c:set var="language" value="${currentResource.locale.language}"/>
        <language>${language}</language>
            <jsp:useBean id="now" class="java.util.Date"/>
        <lastBuildDate><fmt:formatDate value="${now}" pattern="EEE, dd MMM yyyy HH:mm:ss Z"/></lastBuildDate>
            <atom:link href="${url.server}${url.context}/feed.xml" rel="self" type="application/rss+xml"/>
        <description>New Jahia modules.</description>
            <jcr:jqom var="result" statement="SELECT * FROM [jnt:forgeModuleVersion] as module WHERE ISDESCENDANTNODE(module,'${currentNode.path}') ORDER BY module.[jcr:lastModified] DESC"/>
            <c:forEach items="${result.nodes}" var="version">
                <c:if test="${version.properties.published.boolean}">
                    <c:set var="downloadUrl" value="${version.properties.url.string}"/>
                <item>
                    <title>${version.name}, ${version.properties['versionNumber']}</title>
                    <link>${fn:escapeXml(downloadUrl)}</link>
                    <guid>${fn:escapeXml(downloadUrl)}</guid>
                    <description>${version.displayableName}, version ${version.properties['versionNumber']}.</description>
                        <jcr:nodeProperty node="${version}" name="jcr:lastModified" var="modified"/>
                    <pubDate><fmt:formatDate value="${modified.time}" pattern="EEE, dd MMM yyyy HH:mm:ss Z" /></pubDate>
                </item>
            </c:if>
        </c:forEach>
    </channel>
</rss>