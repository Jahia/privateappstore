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
<c:set var="FAQ" value="${currentNode.properties['FAQ'].string}"/>
<c:set var="isDeveloper" value="${renderContext.loggedIn && jcr:hasPermission(currentNode, 'jcr:all_live')}"/>
<c:if test="${isDeveloper}">
    <c:set var="viewAsUser" value="${not empty param['viewAs'] && param['viewAs'] eq 'user'}" />
</c:if>
<c:set var="isEmptyTab" value="false"/>

<c:if test="${isDeveloper && not viewAsUser}">

    <c:url var="postURL" value="${url.base}${renderContext.mainResource.node.path}"/>
    <template:addResources type="inlinejavascript">

        <script type="text/javascript">

            $(document).ready(function() {

                $('#FAQ-${id}').editable({
                    <jsp:include page="../../commons/bootstrap-editable-options-wysihtml5.jsp">
                        <jsp:param name="postURL" value="${postURL}"/>
                    </jsp:include>
                });

                $('#toggle-FAQ-${id}').click(function(e) {
                    e.stopPropagation();
                    e.preventDefault();
                    $('#FAQ-${id}').editable('toggle');
                });

            });

        </script>

    </template:addResources>

</c:if>

<%--<jsp:include page="../../commons/checkEmptyTab.jsp">
    <jsp:param name="propertyName" value="FAQ"/>
    <jsp:param name="wrapperSelector" value="#moduleFAQ"/>
</jsp:include>--%>

<c:if test="${fn:length( fn:trim( functions:removeHtmlTags( fn:replace(FAQ, '&nbsp;', ' ') ))) eq 0
                && (not isDeveloper || viewAsUser)}">

    <c:set var="isEmptyTab" value="true"/>
    <template:addResources type="inlinejavascript">
        <script type="text/javascript">

            $(document).ready(function() {

                var tabID = $('#moduleFAQ').parent('.tab-pane').attr("id");
                var navTabSelector = "a[href='#" + tabID + "']";

                $(".jnt_bootstrapTabularList").find(navTabSelector).parent().remove();
            });
        </script>
    </template:addResources>
</c:if>

<section id="moduleFAQ">

    <c:if test="${not isEmptyTab}">

        <h2><fmt:message key="jnt_forgeModule.label.FAQ"/></h2>

        <c:if test="${isDeveloper && not viewAsUser}">

            <p class="editable-toggle">
                <a id="toggle-FAQ-${id}" href="#"><i class="icon-pencil"></i>&nbsp;<fmt:message key="jnt_forgeModule.label.edit"/></a>
            </p>

            <div data-original-title="<fmt:message key="jnt_forgeModule.label.FAQ"/>" data-toggle="manual" data-name="FAQ" data-type="wysihtml5"
                 data-pk="1" id="FAQ-${id}" class="editable" tabindex="-1">

        </c:if>

        <c:if test="${not empty FAQ}">
            ${FAQ}
        </c:if>

        <c:if test="${isDeveloper && not viewAsUser}">
            </div>
        </c:if>

    </c:if>

</section>