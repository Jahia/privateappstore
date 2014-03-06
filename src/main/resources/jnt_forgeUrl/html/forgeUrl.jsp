<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<%@ page language="java" contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="template" uri="http://www.jahia.org/tags/templateLib" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="jcr" uri="http://www.jahia.org/tags/jcr" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%--@elvariable id="currentNode" type="org.jahia.services.content.JCRNodeWrapper"--%>
<%--@elvariable id="out" type="java.io.PrintWriter"--%>
<%--@elvariable id="script" type="org.jahia.services.render.scripting.Script"--%>
<%--@elvariable id="scriptInfo" type="java.lang.String"--%>
<%--@elvariable id="workspace" type="java.lang.String"--%>
<%--@elvariable id="renderContext" type="org.jahia.services.render.RenderContext"--%>
<%--@elvariable id="currentResource" type="org.jahia.services.render.Resource"--%>
<%--@elvariable id="url" type="org.jahia.services.render.URLGenerator"--%>
<template:addResources type="javascript" resources="ZeroClipboard.min.js" />
<c:url var="moviePath" value="${url.currentModule}/javascript/ZeroClipboard.swf"/>
<c:set var="site" value="${renderContext.site}"/>
<c:url var="siteUrl" value="${url.server}${url.context}${url.base}${site.path}" />
<fmt:message var="copyToClipboard" key="forgeUrl.copyToClipboard"/>
<fmt:message var="copied" key="forgeUrl.copied"/>
<div class="input-append">
    <input class="span12" type="text" value="${siteUrl}" readonly />
    <button id="copyForgeUrl${currentNode.identifier}" class="btn" type="button" data-clipboard-text="${siteUrl}" title="${copyToClipboard}"><i class="icon-screenshot"></i></button>
</div>

<script>
    var client = new ZeroClipboard( document.getElementById("copyForgeUrl${currentNode.identifier}"), {
        moviePath: "${moviePath}"
    } );

    client.on( "load", function(client) {
        client.on( "complete", function(client, args) {
            alert("${copied}");
        } );
    } );
</script>
