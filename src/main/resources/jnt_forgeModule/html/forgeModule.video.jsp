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

<template:addResources type="javascript" resources="html5shiv.js"/>

<c:set var="id" value="${currentNode.identifier}"/>
<c:set var="isDeveloper" value="${jcr:hasPermission(currentNode, 'jcr:write')}"/>

<c:if test="${isDeveloper}">
    <c:set var="viewAsUser" value="${not empty param['viewAs'] && param['viewAs'] eq 'user'}" />
</c:if>
<c:set var="hasVideoNode" value="${jcr:hasChildrenOfType(currentNode, 'jnt:videostreaming')}"/>
<c:set var="isEmptyTab" value="false"/>

<c:if test="${hasVideoNode}">
    <jcr:node var="videoNode" path="${currentNode.path}/video"/>
    <c:set var="videoProvider" value="${videoNode.properties['provider'].string}"/>
    <c:set var="videoIdentifier" value="${videoNode.properties['identifier'].string}"/>
    <c:set var="videoHeight" value="${videoNode.properties['height'].string}"/>
    <c:set var="videoWidth" value="${videoNode.properties['width'].string}"/>
    <c:set var="videoAllowfullscreen" value="${videoNode.properties['allowfullscreen'].string}"/>
</c:if>

<c:if test="${(not isDeveloper || viewAsUser)
                    && (not hasVideoNode
                        || (empty videoProvider || fn:trim(videoProvider) eq 0)
                        || (empty videoIdentifier || fn:trim(videoIdentifier) eq 0))}">

    <c:set var="isEmptyTab" value="true"/>
    <template:addResources type="inlinejavascript">
        <script type="text/javascript">

            $(document).ready(function() {

                var tabID = $('#moduleVideo').parent('.tab-pane').attr("id");
                var navTabSelector = "a[href='#" + tabID + "']";

                $(".jnt_bootstrapTabularList").find(navTabSelector).parent().remove();
            });
        </script>
    </template:addResources>
</c:if>

<c:if test="${isDeveloper && not viewAsUser}">
    <template:addResources type="inlinejavascript">

        <script type="text/javascript">

            $(document).ready(function() {

                $('#forgeModuleVideoForm-${id}').hide();

                $('#toggle-video-${id}').click(function() {

                    <c:choose>
                        <c:when test="${hasVideoNode}">

                            if ($(this).hasClass('active')) {
                                $('#forgeModuleVideoForm-${id}').slideUp(function() {
                                    $('#forgeModuleVideoWrapper-${id}').slideDown();
                                });
                                $(this).removeClass('active');
                            }
                            else {
                                $('#forgeModuleVideoWrapper-${id}').slideUp(function() {
                                    $('#forgeModuleVideoForm-${id}').slideDown();
                                });
                                $(this).addClass('active');
                            }

                        </c:when>
                        <c:otherwise>
                            $('#forgeModuleVideoForm-${id}').slideToggle();
                        </c:otherwise>
                    </c:choose>
                });

                <c:if test="${hasVideoNode}">

                    $('#remove-video-${id}').click(function() {

                        var boostrapTab = $(this).parents('.tab-pane').attr('id');

                        $.post('<c:url value="${url.base}${videoNode.path}"/>',{jcrMethodToCall: 'delete'}, function() {
                            window.location = '${currentNode.url}' + "?bootstrapTab=" + boostrapTab;
                        }, "json");
                    });

                </c:if>

                jQuery.validator.addMethod("integer", function(value, element) {
                    return this.optional(element) || /^-?\d+$/.test(value);
                });

                $("#forgeModuleVideoForm-${id}").validate({

                    rules: {
                        'provider': {
                            required: true
                        },
                        'identifier': {
                            required: true
                        },
                        width: {
                            integer: true
                        },
                        height: {
                            integer: true
                        }
                    },
                    messages: {
                        provider: {
                            required: "<fmt:message key='jnt_forgeModule.label.askVideoProvider'/>"
                        },
                        identifier: {
                            required: "<fmt:message key='jnt_forgeModule.label.askVideoIdentifier'/>"
                        },
                        width: {
                            integer: "<fmt:message key='jnt_forgeModule.label.askInteger'/>"
                        },
                        height: {
                            integer: "<fmt:message key='jnt_forgeModule.label.askInteger'/>"
                        }
                    },
                    submitHandler: function(form) {

                        var boostrapTab = $(form).parents('.tab-pane').attr('id');
                        $.post('<c:url value='${url.base}${currentNode.path}.${hasVideoNode ? "editVideo" : "addVideo"}.do'/>',
                                $(form).serialize(), function() {
                            window.location = '${currentNode.url}' + "?bootstrapTab=" + boostrapTab;
                        }, "json");
                    },
                    highlight: function(element, errorClass, validClass) {
                        $(element).addClass("error").removeClass(validClass).parents('.control-group').addClass("error");
                    },
                    unhighlight: function(element, errorClass, validClass) {
                        $(element).removeClass("error").addClass(validClass).parents('.control-group').removeClass("error");
                    }
                });
            });

        </script>

    </template:addResources>
</c:if>

<section id="moduleVideo">

    <c:if test="${not isEmptyTab}">

        <h2><fmt:message key="jnt_forgeModule.label.video"/></h2>

        <c:if test="${isDeveloper && not viewAsUser}">

            <c:choose>
                <c:when test="${hasVideoNode}">
                    <p>
                        <a id="toggle-video-${id}" href="#"><i class="icon-pencil"></i>&nbsp;<fmt:message key="jnt_forgeModule.label.edit"/></a>
                        <a id="remove-video-${id}" href="#"><i class="icon-remove"></i>&nbsp;<fmt:message key="jnt_forgeModule.label.remove"/></a>
                    </p>
                </c:when>
                <c:otherwise>
                    <p>
                        <a id="toggle-video-${id}" href="#"><i class="icon-plus"></i>&nbsp;<fmt:message key="jnt_forgeModule.label.add"/></a>
                    </p>
                </c:otherwise>
            </c:choose>

            <template:tokenizedForm>
                <form id="forgeModuleVideoForm-${id}" action="<c:url value='${url.base}${currentNode.path}.${hasVideoNode ? "editVideo" : "addVideo"}.do'/>" method="post">
                    <fieldset>

                        <div class="control-group">
                            <label class="control-label" for="provider"><fmt:message key="jnt_forgeModule.label.videoProvider"/></label>
                            <div class="controls">
                                <select name="provider" id="provider" >
                                    <option value="youtube" ${videoProvider eq 'youtube' ? 'selected' : ''}>youtube</option>
                                    <option value="dailymotion" ${videoProvider eq 'dailymotion' ? 'selected' : ''}>dailymotion</option>
                                    <option value="vimeo" ${videoProvider eq 'vimeo' ? 'selected' : ''}>vimeo</option>
                                    <option value="watt" ${videoProvider eq 'watt' ? 'selected' : ''}>watt</option>
                                </select>
                            </div>
                        </div>

                        <div class="control-group">
                            <label class="control-label" for="identifier"><fmt:message key="jnt_forgeModule.label.videoIdentifier"/></label>
                            <div class="controls">
                                <input placeholder="<fmt:message key="jnt_forgeModule.label.videoIdentifier" />" type="text"
                                       name="identifier" id="identifier" value="${videoIdentifier}"/>
                            </div>
                        </div>

                        <div class="control-group">
                            <label class="control-label" for="width"><fmt:message key="jnt_forgeModule.label.videoWidth"/></label>
                            <div class="controls">
                                <input placeholder="<fmt:message key="jnt_forgeModule.label.videoWidth" />" type="text"
                                       name="width" id="width" value="${videoWidth}"/>
                            </div>
                        </div>

                        <div class="control-group">
                            <label class="control-label" for="height"><fmt:message key="jnt_forgeModule.label.videoHeight"/></label>
                            <div class="controls">
                                <input placeholder="<fmt:message key="jnt_forgeModule.label.videoHeight" />" type="text"
                                       name="height" id="height" value="${videoHeight}"/>
                            </div>
                        </div>

                        <div class="control-group">
                            <div class="controls">
                                <label class="checkbox">
                                    <input type="checkbox" name="allowfullscreen" id="allowfullscreen"
                                           ${empty videoAllowfullscreen || not empty videoAllowfullscreen && videoAllowfullscreen ? 'checked' : ''}/>
                                    <fmt:message key="jnt_forgeModule.label.videoAllowfullscreen"/>
                                </label>
                            </div>
                        </div>

                        <div class="control-group">
                            <div class="controls">
                                <input type="submit" class="btn btn-primary" value="<fmt:message key="jnt_forgeModule.label.submit"/>"/>
                            </div>
                        </div>

                    </fieldset>
                </form>
            </template:tokenizedForm>

        </c:if>

        <c:if test="${hasVideoNode}">

            <div id="forgeModuleVideoWrapper-${id}">
                    <%-- TODO --%>
                <template:module node="${videoNode}"/>
                <template:module node="${videoNode}" view="default" nodeTypes="jnt:videostreaming"/>
                <template:module node="${videoNode}" view="default"/>

                <template:module path="${videoNode.path}"/>
            </div>

        </c:if>

    </c:if>

</section>