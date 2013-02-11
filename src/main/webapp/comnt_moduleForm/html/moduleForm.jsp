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

<template:tokenizedForm>
    <form action="<c:url value='${url.base}${moduleRepository.node.path}.addModule.do'/>" method="post" id="newModuleForm" enctype="multipart/form-data"  accept="application/json">
        <fieldset>

            <div class="control-group">
                <label class="control-label" for="title"><fmt:message key="comnt_module.title"/></label>
                <div class="controls">
                    <input placeholder="<fmt:message key="comnt_module.title" />" class="span16" type="text"
                           name="title" id="title" value="${sessionScope.formDatas['title'][0]}"/>
                </div>
            </div>

            <div class="control-group">
                <label class="control-label" for="authorName"><fmt:message key="comnt_module.authorName"/></label>
                <div class="controls">
                    <input placeholder="<fmt:message key="comnt_module.authorName" />" class="span16" type="text"
                           name="authorName" id="authorName" value="${sessionScope.formDatas['authorName'][0]}"/>
                </div>
            </div>

            <div class="control-group">
                <label class="control-label" for="authorURL"><fmt:message key="comnt_module.authorURL"/></label>
                <div class="controls">
                    <input placeholder="<fmt:message key="comnt_module.authorURL" />" class="span16" type="text"
                           name="authorURL" id="authorURL" value="${sessionScope.formDatas['authorURL'][0]}"/>
                </div>
            </div>

            <div class="control-group">
                <label class="control-label" for="authorEmail"><fmt:message key="comnt_module.authorEmail"/></label>
                <div class="controls">
                    <input placeholder="<fmt:message key="comnt_module.authorEmail" />" class="span16" type="text"
                           name="authorEmail" id="authorEmail" value="${sessionScope.formDatas['authorEmail'][0]}"/>
                </div>
            </div>

            <div class="control-group">
                <label class="control-label" for="codeRepository"><fmt:message key="comnt_module.codeRepository"/></label>
                <div class="controls">
                    <input placeholder="<fmt:message key="comnt_module.codeRepository" />" class="span16" type="text"
                           name="codeRepository" id="codeRepository" value="${sessionScope.formDatas['codeRepository'][0]}"/>
                </div>
            </div>

            <c:forEach var="i" begin="1" end="4" step="1" varStatus ="status">
            <div class="control-group">
                <label class="control-label" for="screenshot${i}"><fmt:message key="comnt_module.screenshot${i}"/></label>
                <div class="controls">
                    <input placeholder="<fmt:message key="comnt_module.screenshot${i}" />" class="span16" type="file"
                           name="screenshot${i}" id="screenshot${i}" value="${sessionScope.formDatas['screenshot${i}'][0]}"/>
                </div>
            </div>
            </c:forEach>

            <div class="control-group">
                <label class="control-label" for="iconFile"><fmt:message key="comnt_module.iconFile"/></label>
                <div class="controls">
                    <input placeholder="<fmt:message key="comnt_module.iconFile" />" class="span16" type="file"
                           name="iconFile" id="iconFile" value="${sessionScope.formDatas['iconFile'][0]}"/>
                </div>
            </div>
            <div class="control-group">
                <label class="control-label" for="quickDescription"><fmt:message key="comnt_module.quickDescription"/></label>
                <div class="controls">
                    <input placeholder="<fmt:message key="comnt_module.quickDescription" />" class="span16" type="text"
                           name="quickDescription" id="quickDescription" value="${sessionScope.formDatas['quickDescription'][0]}"/>
                </div>
            </div>

            <div class="control-group">
                <label class="control-label" for="quickDescription"><fmt:message key="comnt_module.bigDescription"/></label>
                <div class="controls">
                    <textarea rows="7" cols="35" id="jahia-module-bigDescription-${currentNode.UUID}"
                              placeholder="<fmt:message key="comnt_module.bigDescription" />" class="jahia-ckeditor span16"
                           name="bigDescription" value="${sessionScope.formDatas['bigDescription'][0]}">
                        <c:if test="${not empty sessionScope.formDatas['bigDescription']}">
                            ${fn:escapeXml(sessionScope.formDatas['bigDescription'][0])}
                        </c:if>
                    </textarea>
                </div>
            </div>

            <div class="control-group">
                <label class="control-label" for="jahiAppLicense"><fmt:message key="comnt_module.jahiAppLicense"/></label>
                <div class="controls">
                    <input type="hidden" id="jahiAppLicense" name="jahiAppLicense" value=""/>
                    <input type="text" id="categoryFieldDisplay1" name="categoryFieldDisplay1" readonly="readonly" />
                    <uiComponents:treeItemSelector fieldId="jahiAppLicense" displayFieldId="categoryFieldDisplay1" nodeTypes="jnt:category"
                        selectableNodeTypes="jnt:category" root="/sites/systemsite/categories/forge-categories/license"
                        includeChildren="false" displayIncludeChildren="false"/>
                </div>
            </div>

            <div class="control-group">
                <label class="control-label" for="jahiAppStatus"><fmt:message key="comnt_module.jahiAppStatus"/></label>
                <div class="controls">
                    <input type="hidden" id="jahiAppStatus" name="jahiAppStatus" value=""/>
                    <input type="text" id="categoryFieldDisplay2" name="categoryFieldDisplay2" readonly="readonly" />
                    <uiComponents:treeItemSelector fieldId="jahiAppStatus" displayFieldId="categoryFieldDisplay2" nodeTypes="jnt:category"
                        selectableNodeTypes="jnt:category" root="/sites/systemsite/categories/forge-categories/status"
                        includeChildren="false" displayIncludeChildren="false"/>
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