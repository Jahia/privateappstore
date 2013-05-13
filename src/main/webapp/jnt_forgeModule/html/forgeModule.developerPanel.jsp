<%@ taglib prefix="jcr" uri="http://www.jahia.org/tags/jcr" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="utility" uri="http://www.jahia.org/tags/utilityLib" %>
<%@ taglib prefix="template" uri="http://www.jahia.org/tags/templateLib" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="functions" uri="http://www.jahia.org/tags/functions" %>
<%@ taglib prefix="user" uri="http://www.jahia.org/tags/user" %>
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

<template:addResources type="javascript" resources="jquery.js, html5shiv.js, forge.js"/>
<template:addResources type="css" resources="forge.css"/>

<c:set var="isDeveloper" value="${renderContext.loggedIn && jcr:hasPermission(currentNode, 'jcr:all_live')}"/>

<c:if test="${isDeveloper}">

    <c:set var="id" value="${currentNode.identifier}"/>
    <c:set var="viewAsUser" value="${not empty param['viewAs'] && param['viewAs'] eq 'user'}" />

    <template:addResources type="inlinejavascript">

        <script type="text/javascript">

            $(document).ready(function() {

                function updateCompletionStatus() {

                    $.post('<c:url value='${url.base}${currentNode.path}.calculateCompletion.do'/>', function(data) {

                        var completion = data['completion'];
                        var canBePublished = data['canBePublished'];

                        var bar = $('#completion-${id}').css('width', completion+"%");
                        bar.children('.ratingCount').html(completion+"%");

                        if (completion < 60)
                            bar.addClass('bar-danger');
                        else if (!canBePublished)
                            bar.addClass('bar-warning');
                        else
                            bar.addClass('bar-success');

                        if (canBePublished)
                            $('#publishModule-${id}').removeClass('disabled');
                        else
                            $('#publishModule-${id}').addClass('disabled');

                        var items = [];
                        $.each(data['todoList'], function(key, val) {
                            items.push('<li'+ (val['mandatory'] ? ' class="text-error"' : '' ) + '>' + val['name'] + '</li>');
                        });

                        $('#todoList-${id}').empty().append(items.join(''));

                    }, "json");



                }

                updateCompletionStatus();

                $('#jnt_forge').on('forgeModuleUpdated', function() {
                    updateCompletionStatus();
                });

            });

        </script>

    </template:addResources>

    <section id="moduleDeveloperPanel">


        <div class="progress">
            <div id="completion-${id}" class="bar"><span class="ratingCount"></span></div>
        </div>

        <ul id="todoList-${id}" class="incomplete">
        </ul>

        <div class="btn-group">
            <a class="btn btn-small" href="<c:url value="${url.base}${currentNode.path}.forge-module-add-version.html"/>"><fmt:message key="jnt_forgeModule.label.developer.addVersion"/></a>
            <a class="btn btn-small ${viewAsUser ? 'btn-primary' : ''}" href="<c:url value="${url.base}${currentNode.path}.html${viewAsUser ? '' : '?viewAs=user'}"/>"><fmt:message key="jnt_forgeModule.label.developer.viewAs"/></a>
            <button id="publishModule-${id}" class="btn btn-small disabled"><fmt:message key="jnt_forgeModule.label.developer.publish"/></button>
        </div>

    </section>

</c:if>