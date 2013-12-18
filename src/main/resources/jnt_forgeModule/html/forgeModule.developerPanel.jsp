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

<c:set var="isDeveloper" value="${jcr:hasPermission(currentNode, 'jcr:write')}"/>

<c:if test="${isDeveloper}">
    <template:addResources type="css" resources="bootstrap-wysihtml5.css,bootstrap-editable.css"/>

    <c:set var="id" value="${currentNode.identifier}"/>
    <c:set var="viewAsUser" value="${not empty param['viewAs'] && param['viewAs'] eq 'user'}" />
    <c:set var="published" value="${currentNode.properties['published'].boolean}" />

    <template:addResources type="inlinejavascript">

        <script type="text/javascript">

            $(document).ready(function() {

                function updateCompletionStatus() {

                    $.get('<c:url value='${url.base}${currentNode.path}.calculateCompletion.do'/>', function(data) {

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
                            $('#publishModule-${id}').addClass('disabled').removeClass("btn-danger");

                        var todoList = $('#todoList-${id}');
                        var todoListWrapper = $('#todoListWrapper-${id}');

                        if (completion == 100) {
                            todoListWrapper.slideUp();
                            todoList.empty().addClass('completed');
                        }
                        else {
                            var items = [];
                            var hasMandatoryLeft = false;
                            $.each(data['todoList'], function(key, val) {
                                if (!hasMandatoryLeft && val['mandatory']) {
                                    hasMandatoryLeft = true;
                                }
                                items.push('<li'+ (val['mandatory'] ? ' class="text-error"' : '' ) + '>' + val['name'] + '</li>');
                            });

                            if (!hasMandatoryLeft) {
                                $('span#mandatoryTodoList').hide();
                            }
                            else {
                                $('span#mandatoryTodoList').show();
                            }

                            todoList.empty().append(items.join(''));

                            if (todoList.hasClass('completed')) {
                                todoListWrapper.slideDown();
                                todoList.removeClass('completed');
                            }
                        }

                    }, "json");
                }

                updateCompletionStatus();

                $('#moduleDeveloperPanel').on('forgeModuleUpdated', function() {
                    updateCompletionStatus();
                });

                $('#publishModule-${id}').click(function() {

                    var btn = $(this);

                    if (!btn.hasClass('disabled')) {

                        var data = {};
                        data['publish'] = btn.attr("data-value") == "false";

                        $.post('<c:url value='${url.base}${currentNode.path}.publishModule.do'/>', data, function(result) {

                            var published = result['published'];
                            if (result['published'] != null) {
                                btn.toggleClass('btn-success btn-danger');
                                btn.attr("data-value", result['published']);

                                if (published)
                                    btn.text('<fmt:message key="jnt_forgeModule.label.developer.unpublish"/>');
                                else
                                    btn.text('<fmt:message key="jnt_forgeModule.label.developer.publish"/>');
                            }

                        }, "json");
                    }
                });

                $('#viewAsUserBtn-${id}').tooltip();

            });

        </script>

    </template:addResources>

    <section id="moduleDeveloperPanel" ${viewAsUser ? 'class="viewAs"' : ''}>

        <h4><fmt:message key="jnt_forgeModule.label.developer.title"/></h4>

        <h6><fmt:message key="jnt_forgeModule.label.developer.modulePageCompletion"/></h6>
        <div class="progress">
            <div id="completion-${id}" class="bar"><span class="ratingCount"></span></div>
        </div>

        <div id="todoListWrapper-${id}">
            <h6>
                <fmt:message key="jnt_forgeModule.label.developer.todoList"/>&nbsp;
                <span id="mandatoryTodoList"><fmt:message key="jnt_forgeModule.label.developer.todoListMandatory"/></span>
            </h6>
            <ul id="todoList-${id}">
            </ul>
        </div>

        <div class="btn-group">
            <a class="btn btn-small ${viewAsUser ? 'btn-primary' : ''}" id="viewAsUserBtn-${id}"
                href="<c:url value="${url.base}${currentNode.path}.html${viewAsUser ? '' : '?viewAs=user'}"/>"
                data-toggle="tooltip" title="<fmt:message key="jnt_forgeModule.label.developer.viewAs.tooltip"/>">
                <fmt:message key="jnt_forgeModule.label.developer.viewAs"/>
            </a>
            <button id="publishModule-${id}" class="btn btn-small ${published ? 'btn-success': 'btn-danger'} disabled" data-value="${published}">
                <c:choose>
                    <c:when test="${published}"><fmt:message key="jnt_forgeModule.label.developer.unpublish"/></c:when>
                    <c:otherwise><fmt:message key="jnt_forgeModule.label.developer.publish"/></c:otherwise>
                </c:choose>
            </button>
        </div>

    </section>

</c:if>