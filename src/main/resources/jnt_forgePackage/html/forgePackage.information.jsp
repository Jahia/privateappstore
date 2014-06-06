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
    <c:forEach items="${currentNode.properties['j:defaultCategory']}" var="cat" varStatus="vs">
        <c:set var="category" value="${cat}"/>
    </c:forEach>
<c:set var="nbOfVotes"
       value="${not empty currentNode.properties['j:nbOfVotes'] ? currentNode.properties['j:nbOfVotes'].long : null}"/>
<c:set var="avgRating"
       value="${not empty nbOfVotes && not empty currentNode.properties['j:sumOfVotes'] ?
       currentNode.properties['j:sumOfVotes'].long / nbOfVotes : null}"/>
<jcr:nodeProperty node="${currentNode}" name="j:tags" var="assignedTags"/>
<jcr:node var="videoNode" path="${currentNode.path}/video"/>

<%@include file="../../commons/authorName.jspf"%>

<template:include view="hidden.sql">
    <template:param name="getLatestVersion" value="true"/>
</template:include>
<c:set value="${moduleMap.latestVersion}" var="latestVersion"/>
<template:addCacheDependency node="${latestVersion}"/>


<jcr:nodeProperty node="${latestVersion}" name="versionNumber" var="versionNumber"/>
<jcr:nodeProperty node="${latestVersion}" name="requiredVersion" var="requiredVersion"/>


<c:set var="isDeveloper" value="${jcr:hasPermission(currentNode, 'jcr:write')}"/>
<c:if test="${isDeveloper}">
    <c:set var="viewAsUser" value="${not empty param['viewAs'] && param['viewAs'] eq 'user'}" />
</c:if>

<c:if test="${isDeveloper && not viewAsUser}">

    <fmt:message var="labelNotSelected" key="jnt_forgeModule.label.notSelected"/>
    <fmt:message var="labelEmpty" key="jnt_forgeModule.label.empty"/>
    <fmt:message var="labelEmptyOrganisation" key="jnt_forgeModule.label.developer.emptyOrganisation"/>
    <fmt:message var="labelEmptyFullName" key="jnt_forgeModule.label.developer.emptyFullName"/>
    <c:url var="postURL" value="${url.base}${currentNode.path}"/>

    <c:set var="moduleCategories" value="${renderContext.site.properties['rootCategory'].node}"/>
    <template:addCacheDependency node="${renderContext.site}"/>
    <template:addCacheDependency node="${moduleCategories}"/>

    <template:addResources type="inlinejavascript">
        <script type="text/javascript">

            var categories = [];
            <c:if test="${! empty moduleCategories && jcr:hasChildrenOfType(moduleCategories, 'jnt:category')}">
                <c:forEach items="${jcr:getNodes(moduleCategories, 'jnt:category')}" var="moduleCategory">
                    categories.push({value: '${moduleCategory.identifier}', text: '${moduleCategory.displayableName}'});
                </c:forEach>
            </c:if>

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
                        <jsp:param name="customSuccess" value="document.location = '${currentNode.url}';"/>
                    </jsp:include>
                });

            });

        </script>
    </template:addResources>

</c:if>



<aside class="moduleInformation moduleMetaData dl-small">

    <c:if test="${currentNode.properties['reviewedByJahia'].boolean || currentNode.properties['supportedByJahia'].boolean}">
        <div class="labels">
            <c:if test="${currentNode.properties['reviewedByJahia'].boolean}">
                <span class="label label-success">
                    <i class="icon-ok icon-white"></i>
                    <fmt:message key="jnt_forgeModule.label.admin.reviewedByJahia"/>
                </span>
            </c:if>
            <c:if test="${currentNode.properties['supportedByJahia'].boolean}">
                <span class="label label-warning">
                    <i class="icon-wrench icon-white"></i>
                    <fmt:message key="jnt_forgeModule.label.admin.supportedByJahia"/>
                </span>
            </c:if>
        </div>
    </c:if>

    <h4><fmt:message key="jnt_forgeModule.label.information"/></h4>

    <div itemscope="" itemtype="http://schema.org/SoftwareApplication">
        <span content="${title}" itemprop="name"></span>
        <c:if test="${not empty icon}"><span content="${icon.url}" itemprop="image"></span></c:if>
        <span content="${latestVersion.properties.url.string}" itemprop="downloadUrl"></span>
        <c:forEach items="${assignedTags}" var="tag" varStatus="status">
            <span content="${tag.node.name}" itemprop="keywords"></span>
        </c:forEach>

        <div class="term"><fmt:message key="jnt_forgeModule.label.moduleId"/></div>
        <div class="description">
            ${currentNode.name}
        </div>

        <div class="term"><fmt:message key="jnt_forgeModule.label.groupId"/></div>
        <div class="description">
            ${currentNode.properties['groupId'].string}
        </div>

        <div class="term"><fmt:message key="jnt_forgeModule.label.updated"/></div>
        <div class="description">
            <time itemprop="datePublished">
                <fmt:formatDate value="${latestVersion.properties['jcr:lastModified'].date.time}" pattern="yyyy-MM-dd" />
            </time>
        </div>

        <div class="term"><fmt:message key="jnt_forgeModule.label.version"/></div>
        <div class="description" itemprop="softwareVersion">${versionNumber.string}</div>

        <c:if test="${not empty requiredVersion}">
            <div class="term"><fmt:message key="jnt_forgeModule.label.relatedJahiaVersion"/></div>
            <div class="description">${requiredVersion.node.displayableName}</div>
        </c:if>


        <div itemtype="http://schema.org/Organization" itemscope="" itemprop="author">

            <div class="term"><fmt:message key="jnt_forgeModule.label.authorName"/></div>
            <div class="description" itemprop="name">
                <c:if test="${isDeveloper && not viewAsUser}">
                    <a data-original-title="<fmt:message key="jnt_forgeModule.label.askAuthorNameDisplayedAs"/>" data-name="authorNameDisplayedAs" data-pk="1" data-type="select"
                       id="authorName-information-${id}" href="#" class="editable editable-click">
                </c:if>
                ${authorName}
                <c:if test="${isDeveloper && not viewAsUser}">
                    </a>
                </c:if>
            </div>

            <span content="${authorURL}" itemprop="url"></span>
            <span content="${authorEmail}" itemprop="email"></span>

        </div>

        <c:if test="${jcr:isNodeType(currentNode, 'jmix:rating') && nbOfVotes gt 0}">
            <div class="term"><fmt:message key="jnt_forgeModule.label.rating"/></div>
            <div class="description" itemtype="http://schema.org/AggregateRating" itemscope="" itemprop="aggregateRating">
                <span itemprop="worstRating" content="1"></span>
                <span itemprop="bestRating" content="5"></span>
                <div class="ratingValue" itemprop="ratingValue" content="${avgRating}">
                    <template:include view="hidden.average.readonly" />
                </div>
                <span itemprop="ratingCount" content="${nbOfVotes}">(${nbOfVotes})</span>

            </div>
        </c:if>

        <div class="term"><h4><fmt:message key="jnt_forgeModule.label.category"/></h4></div>
        <div class="description" itemprop="applicationCategory">
            <c:if test="${isDeveloper && not viewAsUser}">
                <a data-original-title="<fmt:message key="jnt_forgeModule.label.askCategory"/>" data-name="j:defaultCategory" data-pk="1" data-type="select"
                   id="category-${id}" href="#" class="editable editable-click">
            </c:if>
            ${not empty category ? category.node.displayableName : labelNotSelected}
            <c:if test="${isDeveloper && not viewAsUser}">
                </a>
            </c:if>
        </div>

    </div>

</aside>