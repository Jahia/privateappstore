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
<jcr:node var="iconFolder" path="${renderContext.mainResource.node.path}/icon" />
<c:forEach var="iconItem" items="${iconFolder.nodes}">
    <c:set var="icon" value="${iconItem}"/>
</c:forEach>
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


<jcr:nodeProperty node="${activeVersion}" name="versionNumber" var="versionNumber"/>
<jcr:nodeProperty node="${activeVersion}" name="requiredVersion" var="requiredVersion"/>


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

    <jcr:node var="moduleCategories" path="${renderContext.site.path}/contents/forge-modules-categories"/>

    <template:addResources type="inlinejavascript">
        <script type="text/javascript">

            var categories = [];
            <c:if test="${jcr:hasChildrenOfType(moduleCategories, 'jnt:text')}">
            <c:forEach items="${jcr:getNodes(moduleCategories, 'jnt:text')}" var="moduleCategory">
            categories.push({value: '${moduleCategory.identifier}', text: '${moduleCategory.properties['text'].string}'});
            </c:forEach>
            </c:if>

            $(document).ready(function() {


                $('#forgeContestBtnInformation').click(function() {

                    var btn = $(this);
                    var dataName = btn.attr('data-name');
                    var dataValue = !(btn.attr('data-value') === 'true');
                    var data = {};

                    data[dataName] = dataValue;
                    data['jcrMethodToCall'] = 'put';

                    $.post('<c:url value='${url.base}${currentNode.path}'/>', data, function(result) {
                        btn.toggleClass('btn-success btn-danger');
                        btn.attr('data-value', result[dataName]);
                        var text=btn.attr('data-value') == "true"?"<fmt:message key="jnt_forgeModule.label.contestModule.button.yes"/>":"<fmt:message key="jnt_forgeModule.label.contestModule.button.no"/>";
                        $("#forgeContestBtnInformation").html(text);



                    },"json")

                });

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
        <span content="${activeVersion.properties.url.string}" itemprop="downloadUrl"></span>
        <c:forEach items="${assignedTags}" var="tag" varStatus="status">
            <span content="${tag.node.name}" itemprop="keywords"></span>
        </c:forEach>

        <div class="term"><fmt:message key="jnt_forgeModule.label.updated"/></div>
        <div class="description">
            <time itemprop="datePublished">
                <fmt:formatDate value="${activeVersion.properties['jcr:lastModified'].date.time}" pattern="yyyy-MM-dd" />
            </time>
        </div>

        <div class="term"><fmt:message key="jnt_forgeModule.label.version"/></div>
        <div class="description" itemprop="softwareVersion">${versionNumber.string}</div>

        <c:if test="${not empty requiredVersion}">
            <div class="term"><fmt:message key="jnt_forgeModule.label.relatedJahiaVersion"/></div>
            <div class="description">${requiredVersion.node.properties['text'].string}</div>
        </c:if>
    </div>

    <div itemscope="" itemtype="http://schema.org/Organization">

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
        <div class="description" itemtype="http://schema.org/AggregateRating" itemscope="" >
            <span itemprop="worstRating" content="1"></span>
            <span itemprop="bestRating" content="5"></span>
            <div class="ratingValue" itemprop="ratingValue" content="${avgRating}">
                <template:include view="hidden.average.readonly" />
            </div>
            <span itemprop="ratingCount" content="${nbOfVotes}">(${nbOfVotes})</span>

        </div>
    </c:if>

    <div class="term"><h4><fmt:message key="jnt_forgeModule.label.category"/></h4></div>
    <div class="description">
        <c:if test="${isDeveloper && not viewAsUser}">
        <a data-original-title="<fmt:message key="jnt_forgeModule.label.askCategory"/>" data-name="category" data-pk="1" data-type="select"
           id="category-${id}" href="#" class="editable editable-click">
            </c:if>
            ${not empty category ? category.node.properties['text'].string : labelNotSelected}
            <c:if test="${isDeveloper && not viewAsUser}">
        </a>
        </c:if>
    </div>
    <c:if test="${isDeveloper && not viewAsUser}">
        <div class="term"><h4><fmt:message key="jnt_forgeModule.label.contestModule"/></h4></div>
        <div class="description">
            <c:set var="isContestModule" value="${currentNode.properties['isContestModule'].boolean}"/>
            <button id="forgeContestBtnInformation" class="forgeContestBtn btn btn-small ${isContestModule ? 'btn-success' : 'btn-danger'}"
                    data-value="${isContestModule}" data-name="isContestModule">
                <fmt:message key="jnt_forgeModule.label.contestModule.button.${isContestModule ? 'yes' : 'no'}"/>
            </button>
        </div>
    </c:if>


</aside>