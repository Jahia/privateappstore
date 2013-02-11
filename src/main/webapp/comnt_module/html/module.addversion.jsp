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
<uiComponents:ckeditor selector="jahia-moduleVersion-desc-${currentNode.UUID}"/>

<template:tokenizedForm>
    <form action="<c:url value='${url.base}${currentNode.path}.addModuleRelease.do'/>" method="post" id="newModuleForm" enctype="multipart/form-data"  accept="application/json">
        <fieldset>

            <div class="control-group">
                <label class="control-label" for="title"><fmt:message key="comnt_module.title"/></label>
                <div class="controls">
                    <input placeholder="<fmt:message key="comnt_module.title" />" class="span16" type="text"
                           name="title" id="title" value="${sessionScope.formDatas['title'][0]}"/>
                </div>
            </div>

            <div class="control-group">
                <label class="control-label" for="version"><fmt:message key="comnt_module.version"/></label>
                <div class="controls">
                    <input placeholder="<fmt:message key="comnt_module.version" />" class="span16" type="text"
                           name="version" id="version" value="${sessionScope.formDatas['version'][0]}"/>
                </div>
            </div>

            <div class="control-group">
                <label class="control-label" for="binaryFile"><fmt:message key="comnt_moduleVersion.moduleFile"/></label>
                <div class="controls">
                    <input placeholder="<fmt:message key="comnt_moduleVersion.moduleFile" />" class="span16" type="file"
                           name="binaryFile" id="binaryFile" value="${sessionScope.formDatas['binaryFile'][0]}"/>
                </div>
            </div>

            <div class="control-group">
                <label class="control-label" for="jahia-moduleVersion-desc-${currentNode.UUID}"><fmt:message key="comnt_moduleVersion.desc"/></label>
                <div class="controls">
                    <textarea rows="7" cols="35" id="jahia-moduleVersion-desc-${currentNode.UUID}"
                              placeholder="<fmt:message key="comnt_moduleVersion.desc" />" class="jahia-ckeditor span16"
                           name="desc" value="${sessionScope.formDatas['desc'][0]}">
                        <c:if test="${not empty sessionScope.formDatas['desc']}">
                            ${fn:escapeXml(sessionScope.formDatas['desc'][0])}
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