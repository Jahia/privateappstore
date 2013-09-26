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
<template:addResources type="css" resources="ui.stars.css"/>
<template:addResources type="javascript" resources="jquery.min.js,jquery.validate.js,jquery-ui.min.js,ui.stars.js"/>

<c:set var="isDeveloper" value="${jcr:hasPermission(currentNode, 'jcr:write')}"/>
<c:if test="${isDeveloper}">
    <c:set var="viewAsUser" value="${not empty param['viewAs'] && param['viewAs'] eq 'user'}" />
</c:if>

<c:set var="id" value="${currentNode.identifier}"/>
<c:set var="title" value="${currentNode.properties['jcr:title'].string}"/>
<c:set var="icon" value="${currentNode.properties['icon'].node}"/>
<c:set var="authorIsOrganisation" value="${currentNode.properties['authorNameDisplayedAs'].string eq 'organisation'}"/>
<c:set var="authorURL" value="${currentNode.properties['authorURL'].string}"/>
<c:set var="authorEmail" value="${currentNode.properties['authorEmail'].string}"/>
<c:set var="authorUsername" value="${currentNode.properties['jcr:createdBy'].string}"/>
<c:set var="author" value="${user:lookupUser(authorUsername)}"/>
<c:set var="userEmail" value="${author.properties['j:email']}"/>
<c:set var="description" value="${currentNode.properties['description'].string}"/>
<c:set var="category" value="${currentNode.properties['category']}"/>
<c:set var="nbOfVotes"
       value="${not empty currentNode.properties['j:nbOfVotes'] ? currentNode.properties['j:nbOfVotes'].long : null}"/>
<c:set var="avgRating"
       value="${not empty nbOfVotes && not empty currentNode.properties['j:sumOfVotes'] ?
       currentNode.properties['j:sumOfVotes'].long / nbOfVotes : null}"/>
<jcr:nodeProperty node="${currentNode}" name="j:tags" var="assignedTags"/>
<jcr:node var="videoNode" path="${currentNode.path}/video"/>

<template:addCacheDependency path="${author.localPath}"/>
<template:include view="hidden.sql">
    <template:param name="getActiveVersion" value="true"/>
</template:include>
<c:set value="${moduleMap.activeVersion}" var="activeVersion"/>

<c:if test="${isDeveloper && not viewAsUser}">

    <c:url var="postURL" value="${url.base}${currentNode.path}"/>
    <fmt:message var="labelEmpty" key="jnt_forgeModule.label.empty"/>

    <template:addResources type="inlinejavascript">

        <script type="text/javascript">

            $(document).ready(function() {

                $('#description-${id}').editable({
                    <jsp:include page="../../commons/bootstrap-editable-options-wysihtml5.jsp">
                        <jsp:param name="postURL" value="${postURL}"/>
                    </jsp:include>
                });

                $('#toggle-description-${id}').click(function(e) {
                    e.stopPropagation();
                    e.preventDefault();
                    $('#description-${id}').editable('toggle');
                });

                $('#authorURL-${id}').editable({
                    value: '${not empty authorURL ? authorURL : ''}',
                    <jsp:include page="../../commons/bootstrap-editable-options.jsp">
                        <jsp:param name="postURL" value="${postURL}"/>
                        <jsp:param name="validate" value="url"/>
                    </jsp:include>
                });

                <c:choose>
                    <c:when test="${authorIsOrganisation}">
                        $('#authorEmail-${id}').editable({
                            value: '${not empty authorEmail ? authorEmail : ''}',
                            <jsp:include page="../../commons/bootstrap-editable-options.jsp">
                                <jsp:param name="postURL" value="${postURL}"/>
                                <jsp:param name="validate" value="email"/>
                            </jsp:include>
                        });
                    </c:when>
                    <c:otherwise>
                        $('#authorEmail-${id}').popover({
                            title: '<fmt:message key="jnt_forgeModule.label.editAuthorEmail"/>',
                            <c:choose>
                                <c:when test="${not empty userEmail}">
                                    content: '<fmt:message key="jnt_forgeModule.label.uneditableAuthorEmail"/>',
                                </c:when>
                                <c:otherwise>
                                    content: '<fmt:message key="jnt_forgeModule.label.emptyAuthorEmail"/>',
                                </c:otherwise>
                            </c:choose>
                            placement: 'top',
                            trigger: 'hover'
                        });
                    </c:otherwise>
                </c:choose>
                
                <jcr:sql var="tags" sql="SELECT * FROM [jnt:tag] WHERE ischildnode(['${renderContext.site.path}/tags'])"/>

                <c:set var="tagsList" value=""/>
                <c:forEach items="${tags.nodes}" var="tag" varStatus="status">
                    <%--<c:set var="tagsList" value="${tagsList} {id: '${tag.identifier}', text:'${tag.name}'} ${not status.last ? ',': ''}"/>--%>
                    <c:set var="tagsList" value="${tagsList} '${tag.name}'${not status.last ? ',': ''}"/>
                </c:forEach>

                $('#tags-${id}').editable({
                    inputclass: 'input-large',
                    select2: {
                        tags: [${tagsList}],
                        tokenSeparators: [",", " "]
                    },
                    source: [${tagsList}],
                    <jsp:include page="../../commons/bootstrap-editable-options.jsp">
                        <jsp:param name="postURL" value="${postURL}.editTags.do"/>
                        <jsp:param name="jcrMethodToCall" value="post"/>
                    </jsp:include>
                });

                <%--$.fn.addPopover = function() {
                    this.popover({
                        content: '<fmt:message key="jnt_forgeModule.label.uneditableAuthorEmail"/>',
                        placement: 'top'
                    });
                };

                <c:if test="${not authorIsOrganisation}">

                    $('#authorEmail-${id}').editable('disable').addPopover();

                </c:if>--%>

            });

        </script>

    </template:addResources>
</c:if>

<article id="moduleOverview">

    <jcr:nodeProperty node="${activeVersion}" name="versionNumber" var="versionNumber"/>
    <jcr:nodeProperty node="${activeVersion}" name="relatedJahiaVersion" var="requiredVersion"/>

    <div class="mainContentWrapper">

        <section class="moduleDescription">

            <h2><fmt:message key="jnt_forgeModule.label.description"/></h2>

            <c:if test="${isDeveloper && not viewAsUser}">

            <p class="editable-toggle">
                <a id="toggle-description-${id}" href="#"><i class="icon-pencil"></i>&nbsp;<fmt:message key="jnt_forgeModule.label.edit"/></a>
            </p>

            <div data-original-title="<fmt:message key="jnt_forgeModule.label.description"/>" data-toggle="manual" data-name="description" data-type="wysihtml5"
                 data-pk="1" id="description-${id}" class="editable" tabindex="-1">

                </c:if>

                <c:if test="${not empty description}">
                    ${description}
                </c:if>

                <c:if test="${isDeveloper && not viewAsUser}">
            </div>
            </c:if>

            <footer>

                <c:choose>

                    <c:when test="${isDeveloper && not viewAsUser}">
                        <a id="authorURL-${id}" class="btn btn-small btn-primary editable"
                           data-original-title="<fmt:message key="jnt_forgeModule.label.editAuthorURL"/>" data-pk="1"
                           data-type="text" data-name="authorURL" href="#" ><fmt:message key="jnt_forgeModule.label.editAuthorURL"/></a>

                        <c:choose>
                            <c:when test="${authorIsOrganisation}">
                                <a id="authorEmail-${id}" class="btn btn-small btn-primary editable"
                                   data-original-title="<fmt:message key="jnt_forgeModule.label.editAuthorEmail"/>" data-pk="1"
                                   data-type="text" data-name="authorEmail" href="#"><fmt:message key="jnt_forgeModule.label.editAuthorEmail"/></a>
                            </c:when>
                            <c:otherwise>
                                <a id="authorEmail-${id}" class="btn btn-small btn-primary" href="#">${not empty userEmail ? userEmail : labelEmpty}</a>
                            </c:otherwise>
                        </c:choose>

                        <section class="moduleTags">

                            <h5><fmt:message key="jnt_forgeModule.label.tags"/></h5>
                            <a href="#" id="tags-${id}" class="editable editable-click" data-type="select2" data-pk="1" data-original-title="<fmt:message key="jnt_forgeModule.label.developer.addTag"/>">
                                <c:forEach items="${assignedTags}" var="tag" varStatus="status">${tag.node.name}${not status.last ? ', ' : ''}</c:forEach>
                            </a>

                        </section>

                    </c:when>

                    <c:otherwise>
                        <c:if test="${not empty authorURL}">
                            <a class="btn btn-small btn-primary" target="_blank" href="${authorURL}"><fmt:message key="jnt_forgeModule.label.authorURL"/></a>
                        </c:if>
                        <c:choose>
                            <c:when test="${authorIsOrganisation && not empty authorEmail}">
                                <a class="btn btn-small btn-primary" href="mailto:${authorEmail}?Subject=${fn:replace(title, " ","%20")}%20-%20Version:%20${versionNumber.string}"><fmt:message key="jnt_forgeModule.label.authorEmail"/></a>
                            </c:when>
                            <c:when test="${not authorIsOrganisation && not empty userEmail}">
                                <a class="btn btn-small btn-primary" href="mailto:${userEmail}?Subject=${fn:replace(title, " ","%20")}%20-%20Version:%20${versionNumber.string}"><fmt:message key="jnt_forgeModule.label.authorEmail"/></a>
                            </c:when>
                        </c:choose>
                        <c:if test="${not empty authorEmail}">
                        </c:if>

                        <c:if test="${not empty assignedTags}">

                            <section class="moduleTags">

                                <h5><fmt:message key="jnt_forgeModule.label.tags"/></h5>
                                <ul class="inline unstyled">
                                    <c:forEach items="${assignedTags}" var="tag" varStatus="status">
                                        <li class="tag">${tag.node.name}</li>
                                    </c:forEach>
                                </ul>

                            </section>

                        </c:if>

                    </c:otherwise>

                </c:choose>

            </footer>

        </section>

        <c:if test="${not empty videoNode}">

            <section class="moduleVideo">

                <h2><fmt:message key="jnt_forgeModule.label.video"/></h2>
                <template:module path="${videoNode.path}" view="forge"/>

            </section>

        </c:if>

    </div>


</article>