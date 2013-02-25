<%@ taglib prefix="jcr" uri="http://www.jahia.org/tags/jcr" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="utility" uri="http://www.jahia.org/tags/utilityLib" %>
<%@ taglib prefix="template" uri="http://www.jahia.org/tags/templateLib" %>
<%@ taglib prefix="uiComponents" uri="http://www.jahia.org/tags/uiComponentsLib" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%--@elvariable id="currentNode" type="org.jahia.services.content.JCRNodeWrapper"--%>
<%--@elvariable id="out" type="java.io.PrintWriter"--%>
<%--@elvariable id="script" type="org.jahia.services.render.scripting.Script"--%>
<%--@elvariable id="scriptInfo" type="java.lang.String"--%>
<%--@elvariable id="workspace" type="java.lang.String"--%>
<%--@elvariable id="renderContext" type="org.jahia.services.render.RenderContext"--%>
<%--@elvariable id="currentResource" type="org.jahia.services.render.Resource"--%>
<%--@elvariable id="url" type="org.jahia.services.render.URLGenerator"--%>
<%--@elvariable id="acl" type="java.lang.String"--%>
<template:addResources type="javascript" resources="jquery.min.js,jquery.validate.js"/>

<script type="text/javascript">
    $(document).ready(function() {
         var form = $("#newModuleForm");
        form.attr("enctype", "multipart/form-data");
    })
</script>
<uiComponents:ckeditor selector="jahia-module-bigDescription-${currentNode.UUID}"/>
<jcr:nodeProperty node="${currentNode}" name='moduleRepository' var="moduleRepository"/>

<c:choose>
    <c:when test="${jcr:isNodeType(renderContext.mainResource.node,'comnt:module')}">
        <c:set var="targetNode" value="${url.base}${renderContext.mainResource.node.path}"/>
        <c:set var="currentModule" value="${url.base}${renderContext.mainResource.node}"/>
        <c:set var="edition" value="true"/>
        <jcr:nodeProperty node="${renderContext.mainResource.node}" name="jcr:title" var="title"/>
        <jcr:nodeProperty node="${renderContext.mainResource.node}" name="authorName" var="authorName"/>
        <jcr:nodeProperty node="${renderContext.mainResource.node}" name="authorURL" var="authorURL"/>
        <jcr:nodeProperty node="${renderContext.mainResource.node}" name="authorEmail" var="authorEmail"/>
        <jcr:nodeProperty node="${renderContext.mainResource.node}" name="codeRepository" var="codeRepository"/>
        <jcr:nodeProperty node="${renderContext.mainResource.node}" name="iconFile" var="iconFile"/>
        <jcr:nodeProperty node="${renderContext.mainResource.node}" name="quickDescription" var="quickDescription"/>
        <jcr:nodeProperty node="${renderContext.mainResource.node}" name="bigDescription" var="bigDescription"/>
        <jcr:nodeProperty node="${renderContext.mainResource.node}" name="screenshot1" var="screenshot1"/>
        <jcr:nodeProperty node="${renderContext.mainResource.node}" name="screenshot2" var="screenshot2"/>
        <jcr:nodeProperty node="${renderContext.mainResource.node}" name="screenshot3" var="screenshot3"/>
        <jcr:nodeProperty node="${renderContext.mainResource.node}" name="screenshot4" var="screenshot4"/>
    </c:when>
    <c:otherwise>
        <c:set var="targetNode" value="${url.base}${moduleRepository.node.path}"/>
        <c:set var="edition" value="false"/>
    </c:otherwise>
</c:choose>

<template:tokenizedForm>
    <form action="<c:url value='${targetNode}.addModule.do'/>" method="post" id="newModuleForm" enctype="multipart/form-data"  accept="application/json">
        <fieldset>

            <div class="control-group">
                <label class="control-label" for="title"><fmt:message key="comnt_module.title"/></label>
                <div class="controls">
                    <input placeholder="<fmt:message key="comnt_module.title" />" class="span16" type="text"
                           name="title" id="title" value="${title.string}"/>
                </div>
            </div>

            <div class="control-group">
                <label class="control-label" for="authorName"><fmt:message key="comnt_module.authorName"/></label>
                <div class="controls">
                    <input placeholder="<fmt:message key="comnt_module.authorName" />" class="span16" type="text"
                           name="authorName" id="authorName" value="${authorName.string}"/>
                </div>
            </div>

            <div class="control-group">
                <label class="control-label" for="authorURL"><fmt:message key="comnt_module.authorURL"/></label>
                <div class="controls">
                    <input placeholder="<fmt:message key="comnt_module.authorURL" />" class="span16" type="text"
                           name="authorURL" id="authorURL" value="${authorURL.string}"/>
                </div>
            </div>

            <div class="control-group">
                <label class="control-label" for="authorEmail"><fmt:message key="comnt_module.authorEmail"/></label>
                <div class="controls">
                    <input placeholder="<fmt:message key="comnt_module.authorEmail" />" class="span16" type="text"
                           name="authorEmail" id="authorEmail" value="${authorEmail.string}"/>
                </div>
            </div>

            <div class="control-group">
                <label class="control-label" for="codeRepository"><fmt:message key="comnt_module.codeRepository"/></label>
                <div class="controls">
                    <input placeholder="<fmt:message key="comnt_module.codeRepository" />" class="span16" type="text"
                           name="codeRepository" id="codeRepository" value="${codeRepository.string}"/>
                </div>
            </div>

            <div class="control-group">
                <label class="control-label" for="screenshot1"><fmt:message key="comnt_module.screenshot1"/></label>
                <c:if test="${not empty screenshot1.node}">
                    ${screenshot1.node.name}
                    <img src="${screenshot1.node.thumbnailUrls['thumbnail']}" alt=""/>
                    <p><a href="#" onclick="$('#screenshot1container').show();">Update</a></p>
                    <c:set var="screenshot1Display" value="style=\"display: none;\""/>
                </c:if>
                <div class="controls" id="screenshot1container" ${screenshot1Display}>
                    <input placeholder="<fmt:message key="comnt_module.screenshot1" />" class="span16" type="file"
                           name="screenshot1" id="screenshot1"/>
                </div>

            </div>
            <div class="control-group">
                <label class="control-label" for="screenshot2"><fmt:message key="comnt_module.screenshot2"/></label>
                <div class="controls">
                    <input placeholder="<fmt:message key="comnt_module.screenshot2" />" class="span16" type="file"
                           name="screenshot2" id="screenshot2"/>
                </div>
                <img src="${screenshot2.node.thumbnailUrls['thumbnail']}" alt=""/>
            </div>
            <div class="control-group">
                <label class="control-label" for="screenshot3"><fmt:message key="comnt_module.screenshot3"/></label>
                <div class="controls">
                    <input placeholder="<fmt:message key="comnt_module.screenshot3" />" class="span16" type="file"
                           name="screenshot3" id="screenshot3"/>
                </div>
                <img src="${screenshot3.node.thumbnailUrls['thumbnail']}" alt=""/>
            </div>
            <div class="control-group">
                <label class="control-label" for="screenshot1"><fmt:message key="comnt_module.screenshot4"/></label>
                <div class="controls">
                    <input placeholder="<fmt:message key="comnt_module.screenshot4" />" class="span16" type="file"
                           name="screenshot4" id="screenshot4"/>
                </div>
                <img src="${screenshot4.node.thumbnailUrls['thumbnail']}" alt=""/>
            </div>

            <div class="control-group">
                <label class="control-label" for="iconFile"><fmt:message key="comnt_module.iconFile"/></label>
                <div class="controls">
                    <input placeholder="<fmt:message key="comnt_module.iconFile" />" class="span16" type="file"
                           name="iconFile" id="iconFile" value="${iconFile.path}"/>
                </div>
            </div>
            <div class="control-group">
                <label class="control-label" for="quickDescription"><fmt:message key="comnt_module.quickDescription"/></label>
                <div class="controls">
                    <input placeholder="<fmt:message key="comnt_module.quickDescription" />" class="span16" type="text"
                           name="quickDescription" id="quickDescription" value="${quickDescription.string}"/>
                </div>
            </div>

            <div class="control-group">
                <label class="control-label" for="quickDescription"><fmt:message key="comnt_module.bigDescription"/></label>
                <div class="controls">
                    <textarea rows="7" cols="35" id="jahia-module-bigDescription-${currentNode.UUID}"
                              placeholder="<fmt:message key="comnt_module.bigDescription" />" class="jahia-ckeditor span16"
                           name="bigDescription" value="${bigDescription.string}">
                        <c:if test="${not empty bigDescription.string}">
                            ${fn:escapeXml(bigDescription.string)}
                        </c:if>
                    </textarea>
                </div>
            </div>

            <div class="control-group">
                <label class="control-label" for="jahiAppLicense"><fmt:message key="comnt_module.jahiAppLicense"/></label>
                <div class="controls">
                    <input type="text" id="jahiAppLicense" name="jahiAppLicense" value=""/>
                    <input type="text" id="categoryFieldDisplay1" name="categoryFieldDisplay1" readonly="readonly" />
                    <uiComponents:treeItemSelector fieldId="jahiAppLicense" displayFieldId="categoryFieldDisplay1" nodeTypes="jnt:category"
                        selectableNodeTypes="jnt:category" root="/sites/systemsite/categories/forge-categories/license"
                        includeChildren="false" displayIncludeChildren="false" valueType="identifier" />
                </div>
            </div>

            <div class="control-group">
                <div class="controls">
                    <input type="submit" class="btn" value="<fmt:message key="forge.submit" />"/>
                </div>
            </div>
        </fieldset>
    </form>
</template:tokenizedForm>