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
<c:set var="title" value="${currentNode.properties['jcr:title'].string}"/>
<c:set var="icon" value="${currentNode.properties['icon'].node}"/>
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

<%@include file="../../commons/authorName.jspf"%>

<template:include view="hidden.sql">
    <template:param name="getActiveVersion" value="true"/>
</template:include>
<c:set value="${moduleMap.activeVersion}" var="activeVersion"/>
<c:set value="${moduleMap.activeVersionBinary}" var="activeVersionBinary"/>
<template:addCacheDependency node="${activeVersion}"/>


<jcr:nodeProperty node="${activeVersion}" name="versionNumber" var="versionNumber"/>
<jcr:nodeProperty node="${activeVersion}" name="relatedJahiaVersion" var="relatedJahiaVersion"/>


<c:set var="isDeveloper" value="${renderContext.loggedIn && jcr:hasPermission(currentNode, 'jcr:all_live')
    && not jcr:hasPermission(currentNode.parent, 'jcr:all_live')}"/>
<c:if test="${isDeveloper}">
    <c:set var="viewAsUser" value="${not empty param['viewAs'] && param['viewAs'] eq 'user'}" />
</c:if>

<c:if test="${isDeveloper && not viewAsUser}">

    <fmt:message var="labelNotSelected" key="jnt_forgeModule.label.notSelected"/>
    <fmt:message var="labelEmpty" key="jnt_forgeModule.label.empty"/>
    <fmt:message var="labelEmptyOrganisation" key="jnt_forgeModule.label.developer.emptyOrganisation"/>
    <fmt:message var="labelEmptyFullName" key="jnt_forgeModule.label.developer.emptyFullName"/>
    <c:url var="postURL" value="${url.base}${currentNode.path}"/>

    <jcr:node var="moduleCategories" path="/sites/systemsite/categories/forge-categories/module-categories"/>

    <template:addResources type="inlinejavascript">
        <script type="text/javascript">

            var categories = [];
            <c:forEach items="${jcr:getNodes(moduleCategories, 'jnt:category')}" var="moduleCategory">
                categories.push({value: '${moduleCategory.identifier}', text: '${moduleCategory.displayableName}'});
            </c:forEach>

            $(document).ready(function() {

                <c:if test="${empty authorOrganisation}">
                $('#authorName-information-${id}').on('shown', function(e, editable) {
                    $(this).next('.editable-container').find('.editable-input select option[value="organisation"]').attr("disabled","true");
                });
                </c:if>

                <c:if test="${empty authorFullName || authorFullName eq authorUsername}">
                $('#authorName-information-${id}').on('shown', function(e, editable) {
                    $(this).next('.editable-container').find('.editable-input select option[value="fullName"]').attr("disabled","true");
                });
                </c:if>

                $('#category-${id}').editable({
                    source: categories,
                    <%-- TODO pre selected value --%>
                    value: '${category.node.identifier}',
                    <jsp:include page="../../commons/bootstrap-editable-options.jsp">
                        <jsp:param name="postURL" value='${postURL}'/>
                    </jsp:include>
                });

                $('#authorName-information-${id}').editable({
                    source: [{value:'username', text:'${authorUsername}'},
                        {value:'fullName', text:'${not empty authorFullName &&  authorFullName ne authorUsername ? authorFullName : labelEmptyFullName}'},
                        {value:'organisation', text: '${not empty authorOrganisation ? authorOrganisation : labelEmptyOrganisation}'}],
                    value: '${authorNameDisplayedAs}',
                    <jsp:include page="../../commons/bootstrap-editable-options.jsp">
                        <jsp:param name="postURL" value="${postURL}"/>
                        <jsp:param name="customSuccess" value="
                            if (${authorNameDisplayedAs eq 'organisation'} || newValue == 'organisation')
                                document.location = '${currentNode.url}';
                            else {
                                var newAuthorName = $(this).next('.editable-container').find('option[value='+newValue+']').html();
                                $('#authorName-header-${id}').html(newAuthorName).editable('setValue', newValue);
                            }"/>
                    </jsp:include>
                });

            });

        </script>
    </template:addResources>

</c:if>



<aside class="moduleInformation" itemtype="http://schema.org/SoftwareApplication">

    <dl class="moduleMetaData dl-small">

        <h4><fmt:message key="jnt_forgeModule.label.information"/></h4>

        <span content="${title}" itemprop="name"></span>
        <span content="${icon.url}" itemprop="image"></span>
        <span content="${activeVersionBinary.fileContent.contentType}" itemprop="fileFormat"></span>
        <span content="${activeVersionBinary.url}" itemprop="downloadUrl"></span>
        <c:forEach items="${assignedTags}" var="tag" varStatus="status">
            <span content="${tag.node.name}" itemprop="keywords"></span>
        </c:forEach>

        <dt><fmt:message key="jnt_forgeModule.label.updated"/></dt>
        <dd>
            <time itemprop="datePublished">
                <fmt:formatDate value="${activeVersionBinary.contentLastModifiedAsDate}" pattern="yyyy-MM-dd" />
            </time>
        </dd>

        <dt><fmt:message key="jnt_forgeModule.label.version"/></dt>
        <dd itemprop="softwareVersion">${versionNumber.string}</dd>

        <dt><fmt:message key="jnt_forgeModule.label.relatedJahiaVersion"/></dt>
        <dd>${relatedJahiaVersion.node.displayableName}</dd>


        <dt><fmt:message key="jnt_forgeModule.label.fileSize"/></dt>
        <dd itemprop="fileSize">${not empty activeVersion ? jcr:humanReadableFileLength(activeVersionBinary) : ''}</dd>

        <span itemtype="http://schema.org/Organization" itemscope="" itemprop="author">

            <dt><fmt:message key="jnt_forgeModule.label.authorName"/></dt>
            <dd itemprop="name">
                <c:if test="${isDeveloper && not viewAsUser}">
                    <a data-original-title="<fmt:message key="jnt_forgeModule.label.askAuthorNameDisplayedAs"/>" data-name="authorNameDisplayedAs" data-pk="1" data-type="select"
                       id="authorName-information-${id}" href="#" class="editable editable-click">
                </c:if>
                ${authorName}
                <c:if test="${isDeveloper && not viewAsUser}">
                    </a>
                </c:if>
            </dd>

            <span content="${authorURL}" itemprop="url"></span>
            <span content="${authorEmail}" itemprop="email"></span>

        </span>

        <c:if test="${jcr:isNodeType(currentNode, 'jmix:rating') && nbOfVotes gt 0}">
            <dt><fmt:message key="jnt_forgeModule.label.rating"/></dt>
            <dd itemtype="http://schema.org/AggregateRating" itemscope="" itemprop="aggregateRating">
                <span itemprop="worstRating" content="1"></span>
                <span itemprop="bestRating" content="5"></span>
                <div itemprop="ratingValue" content="${avgRating}">
                    <template:include view="hidden.average.readonly" />
                </div>
                <span itemprop="ratingCount" content="${nbOfVotes}">(${nbOfVotes})</span>

            </dd>
        </c:if>

        <dt><h4><fmt:message key="jnt_forgeModule.label.category"/></h4></dt>
        <dd itemprop="applicationCategory">
            <c:if test="${isDeveloper && not viewAsUser}">
                <a data-original-title="<fmt:message key="jnt_forgeModule.label.askCategory"/>" data-name="category" data-pk="1" data-type="select"
                   id="category-${id}" href="#" class="editable editable-click">
            </c:if>
            ${not empty category ? category.node.displayableName : labelNotSelected}
            <c:if test="${isDeveloper && not viewAsUser}">
                </a>
            </c:if>
        </dd>

    </dl>

</aside>