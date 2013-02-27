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
<uiComponents:ckeditor selector="jahia-moduleVersion-desc-${currentNode.UUID}"/>


<c:choose>
    <c:when test="${jcr:isNodeType(renderContext.mainResource.node,'comnt:moduleVersion')}">
        <c:set var="targetNode" value="${url.base}${renderContext.mainResource.node.path}"/>
        <c:set var="currentModule" value="${url.base}${renderContext.mainResource.node}"/>
        <c:set var="edition" value="true"/>
        <jcr:nodeProperty node="${renderContext.mainResource.node}" name="jcr:title" var="title"/>
        <jcr:nodeProperty node="${renderContext.mainResource.node}" name="moduleBinary" var="moduleBinary"/>
        <jcr:nodeProperty node="${renderContext.mainResource.node}" name="desc" var="desc"/>
        <jcr:nodeProperty node="${renderContext.mainResource.node}" name="relatedJahiaVersion" var="relatedJahiaVersion"/>
        <jcr:nodeProperty node="${renderContext.mainResource.node}" name="releaseType" var="releaseType"/>
        <jcr:nodeProperty node="${renderContext.mainResource.node}" name="status" var="status"/>
        <jcr:nodeProperty node="${renderContext.mainResource.node}" name="version" var="version"/>
    </c:when>
    <c:otherwise>
        <c:set var="targetNode" value="${url.base}${currentNode.path}"/>
        <c:set var="edition" value="false"/>
    </c:otherwise>
</c:choose>

<template:addResources type="inlinejavascript">
    <script type="text/javascript">

        $(document).ready(function() {

            jQuery.validator.addMethod("regexp", function(value, element, param) {
                          return this.optional(element) || param.test(value);
                        });

                $("#newVersionForm-${currentNode.UUID}").validate({
                rules: {
                    'title': {
                        required: true,
                        minlength: 2
                    },

                    'version': {
                        required: true,
                        regexp: /^(\d+\.){3}(\d+)$/i
                    },

                    'binaryFile': {
                        <c:if test="${not edition}">
                            required: true,
                        </c:if>
                        regexp: /(\S+?)\.(jar|war)$/i
                    },

                    'desc': {
                        required: true,
                        minlength: 50
                    }
                },
                messages: {
                    'title': {
                        required: "<fmt:message key='forge.label.askTitle'/>",
                        minlength: "<fmt:message key='forge.label.titleSizeWarning'/>"
                    },

                    'version': {
                        required: "<fmt:message key='forge.label.askVersion'/>",
                        regexp: "<fmt:message key='forge.label.askValidVersion'/>"
                    },

                    'binaryFile': {
                         required: "<fmt:message key='forge.label.askBinaryFile'/>",
                         regexp:"<fmt:message key='forge.label.askValidBinaryFile'/>"
                     },

                    'desc': {
                        required: "<fmt:message key='forge.label.askVersionDescription'/>",
                        minlength: "<fmt:message key='forge.label.versionDescriptionSizeWarning'/>"
                    }
                }
            });

            var form = $("#newModuleForm");
            form.attr("enctype", "multipart/form-data")
            $("#releaseType").val('${releaseType}');

        });

    </script>
</template:addResources>

<template:tokenizedForm>
    <form action="<c:url value='${targetNode}.addModuleRelease.do'/>" method="post" id="newVersionForm-${currentNode.UUID}" enctype="multipart/form-data"  accept="application/json">
        <fieldset>

            <div class="control-group">
                <label class="control-label" for="title"><fmt:message key="comnt_module.title"/></label>
                <div class="controls">
                    <input placeholder="<fmt:message key="comnt_module.title" />" class="span16" type="text"
                           name="title" id="title" value="${title.string}"/>
                </div>
            </div>

            <div class="control-group">
                <label class="control-label" for="version"><fmt:message key="comnt_module.version"/></label>
                <div class="controls">
                    <input placeholder="<fmt:message key="comnt_module.version" />" class="span16" type="text"
                           name="version" id="version" value="${version.string}"/>
                </div>
            </div>


            <div class="control-group">
                <label class="control-label" for="binaryFile"><fmt:message key="comnt_moduleVersion.moduleFile"/></label>
                <div class="controls">

                    <c:if test="${not empty moduleBinary.node.name}">
                        <p>${moduleBinary.node.name}&nbsp<a href="#" onclick="$('#binaryFile').show();">Update</a></p>
                        <c:set var="binaryFieldDisplay" value="style=\"display: none;\""/>
                    </c:if>
                    <input placeholder="<fmt:message key="comnt_moduleVersion.moduleFile" />" class="span16" type="file"
                           name="binaryFile" id="binaryFile" value="${moduleBinary.node.name}" ${binaryFieldDisplay}/>
                </div>
            </div>

            <div class="control-group">
                <label class="control-label" for="jahia-moduleVersion-desc-${currentNode.UUID}"><fmt:message key="comnt_moduleVersion.desc"/></label>
                <div class="controls">
                    <textarea rows="7" cols="35" id="jahia-moduleVersion-desc-${currentNode.UUID}"
                              placeholder="<fmt:message key="comnt_moduleVersion.desc" />" class="jahia-ckeditor span16"
                           name="desc" value="${desc.string}">
                        <c:if test="${not empty desc.string}">
                            ${fn:escapeXml(desc.string)}
                        </c:if>
                    </textarea>
                </div>
            </div>



            <div class="control-group">
                <label class="control-label" for="releaseType"><fmt:message key="comnt_module.releaseType"/></label>
                <div class="controls">
                    <select name="releaseType" id="releaseType" >
                        <option value="hotfix"> hotfix </option>
                        <option value="service-pack"> service-pack </option>
                        <option value="upgrade"> upgrade </option>
                    </select>
                </div>
            </div>

            <jcr:node var="jahiaVersionCategory" path="/sites/systemsite/categories/forge-categories/related-jahia-version"/>
            <c:if test="${not empty jahiaVersionCategory.node}">
                <div class="control-group">
                    <label class="control-label" for="relatedJahiaVersion"><fmt:message key="comnt_module.relatedJahiaVersion"/></label>
                    <div class="controls">
                        <input type="text" id="relatedJahiaVersion" name="relatedJahiaVersion" value=""/>
                        <input type="text" id="categoryFieldDisplay1" name="categoryFieldDisplay1" readonly="readonly" />
                        <uiComponents:treeItemSelector fieldId="relatedJahiaVersion" displayFieldId="categoryFieldDisplay1" nodeTypes="jnt:category"
                            selectableNodeTypes="jnt:category" root="/sites/systemsite/categories/forge-categories/related-jahia-version"
                            includeChildren="false" displayIncludeChildren="false" valueType="identifier" />
                    </div>
                </div>
            </c:if>

            <jcr:node var="jahiaVersionCategory" path="/sites/systemsite/categories/forge-categories/status"/>
            <c:if test="${not empty jahiaVersionCategory.node}">
            <div class="control-group">
                <label class="control-label" for="jahiAppStatus"><fmt:message key="comnt_module.jahiAppStatus"/></label>
                <div class="controls">
                    <input type="text" id="jahiAppStatus" name="jahiAppStatus" value=""/>
                    <input type="text" id="categoryFieldDisplay2" name="categoryFieldDisplay2" readonly="readonly" />
                    <uiComponents:treeItemSelector fieldId="jahiAppStatus" displayFieldId="categoryFieldDisplay2" nodeTypes="jnt:category"
                        selectableNodeTypes="jnt:category" root="/sites/systemsite/categories/forge-categories/status"
                        includeChildren="false" displayIncludeChildren="false" valueType="identifier" />
                </div>
            </div>
            </c:if>


            <div class="control-group">
                <div class="controls">
                    <input type="submit" class="btn" value="<fmt:message key="forge.submit" />"/>
                </div>
            </div>

        </fieldset>

    </form>
</template:tokenizedForm>
<c:if test="${edition}">
    <div class="edit"><a href="<c:url value='${targetNode}.html'/>">Back</a></div>
</c:if>
