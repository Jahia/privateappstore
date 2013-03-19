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
        <jcr:nodeProperty node="${renderContext.mainResource.node}" name="supportedByJahia" var="supportedByJahia"/>
        <jcr:nodeProperty node="${renderContext.mainResource.node}" name="reviewedByJahia" var="reviewedByJahia"/>
    </c:when>
    <c:otherwise>
        <c:set var="targetNode" value="${url.base}${moduleRepository.node.path}"/>
        <c:set var="edition" value="false"/>
    </c:otherwise>
</c:choose>

<c:set var="moduleCategoriesPath" value="/sites/systemsite/categories/forge-categories/module-categories"/>

<template:addResources type="inlinejavascript">
    <script type="text/javascript">

        $(document).ready(function() {

            jQuery.validator.addMethod("regexp", function(value, element, param) {
                          return this.optional(element) || param.test(value);
                        });

                $("#newModuleForm-${currentNode.UUID}").validate({
                rules: {
                    'title': {
                        required: true,
                        minlength: 2
                    },

                    'authorName': {
                        required: true,
                        minlength: 2
                    },

                    'authorEmail': {
                        required: true,
                        email: true
                    },

                    'screenshot1': {
                    <c:if test="${not edition}">
                        required: true,
                    </c:if>
                        regexp: /(\S+?)\.(jpg|png|gif|jpeg)$/i
                    },

                    'screenshot2': {
                        regexp: /(\S+?)\.(jpg|png|gif|jpeg)$/i
                    },

                    'screenshot3': {
                        regexp: /(\S+?)\.(jpg|png|gif|jpeg)$/i
                    },

                    'screenshot4': {
                        regexp: /(\S+?)\.(jpg|png|gif|jpeg)$/i
                    },

                    'quickDescription': {
                        required: true,
                        minlength: 50
                    },
                    'bigDescription': {
                        required: true,
                        minlength: 100
                    },
                    'moduleCategory': {
                        required: true
                    }
                },
                messages: {
                    'title': {
                        required: "<fmt:message key='forge.label.askTitle'/>",
                        minlength: "<fmt:message key='forge.label.titleSizeWarning'/>"
                    },
                    'authorName': {
                        required: "<fmt:message key='forge.label.askAuthorName'/>",
                        minlength: "<fmt:message key='forge.label.authorNameSizeWarning'/>"
                    },
                    'authorEmail': "<fmt:message key='forge.label.askValidEmail'/>",

                    'screenshot1': {
                         required: "<fmt:message key='forge.label.askScreenshot1'/>",
                         regexp:"<fmt:message key='forge.label.askValidImage'/>"
                     },
                    'screenshot2': {
                        regexp:"<fmt:message key='forge.label.askValidImage'/>"
                     },
                    'screenshot3': {
                        regexp:"<fmt:message key='forge.label.askValidImage'/>"
                     },
                    'screenshot4': {
                        regexp:"<fmt:message key='forge.label.askValidImage'/>"
                     },
                    'quickDescription': {
                        required: "<fmt:message key='forge.label.askQuickDescription'/>",
                        minlength: "<fmt:message key='forge.label.quickDescriptionSizeWarning'/>"
                    },
                    'bigDescription': {
                        required: "<fmt:message key='forge.label.askBigDescription'/>",
                        minlength: "<fmt:message key='forge.label.bigDescriptionSizeWarning'/>"
                    },
                    'moduleCategory': {
                        required: "<fmt:message key='forge.label.askModuleCategory'/>"
                    }
                }
            });

            var form = $("#newModuleForm");
            form.attr("enctype", "multipart/form-data");

        });

    </script>
</template:addResources>

<template:tokenizedForm>
    <form action="<c:url value='${targetNode}.addModule.do'/>" method="post" id="newModuleForm-${currentNode.UUID}" enctype="multipart/form-data"  accept="application/json">
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
                    <c:set var="screenshot1Display" value="style=\"display: none;\""/>
                </c:if>

                <div class="controls" id="screenshot1container" ${screenshot1Display}>
                    <input placeholder="<fmt:message key="comnt_module.screenshot1" />" class="span16" type="file"
                           name="screenshot1" id="screenshot1" />
                </div>

                <c:if test="${not empty screenshot1.node}">
                    <div>
                        <img src="${screenshot1.node.thumbnailUrls['thumbnail']}" alt="${screenshot1.node.name}"/>
                    </div>
                    <p>
                        <a id="screenshot1update" onclick="$('#screenshot1container').show();$('#screenshot1update').hide();">Update</a>
                    </p>
                </c:if>
            </div>
            
            <div class="control-group">
                <label class="control-label" for="screenshot2"><fmt:message key="comnt_module.screenshot2"/></label>

                <c:if test="${not empty screenshot2.node}">
                    <c:set var="screenshot2Display" value="style=\"display: none;\""/>
                </c:if>

                <div class="controls" id="screenshot2container" ${screenshot2Display}>
                    <input placeholder="<fmt:message key="comnt_module.screenshot2" />" class="span16" type="file"
                           name="screenshot2" id="screenshot2"/>
                </div>

                <c:if test="${not empty screenshot2.node}">
                    <div>
                        <img src="${screenshot2.node.thumbnailUrls['thumbnail']}" alt="${screenshot2.node.name}"/>
                    </div>
                    <p>
                        <a id="screenshot2update" onclick="$('#screenshot2container').show();$('#screenshot2update').hide();">Update</a>
                    </p>
                </c:if>
            </div>
            
            <div class="control-group">
                <label class="control-label" for="screenshot3"><fmt:message key="comnt_module.screenshot3"/></label>

                <c:if test="${not empty screenshot3.node}">
                    <c:set var="screenshot3Display" value="style=\"display: none;\""/>
                </c:if>

                <div class="controls" id="screenshot3container" ${screenshot3Display}>
                    <input placeholder="<fmt:message key="comnt_module.screenshot3" />" class="span16" type="file"
                           name="screenshot3" id="screenshot3"/>
                </div>

                <c:if test="${not empty screenshot3.node}">
                    <div>
                        <img src="${screenshot3.node.thumbnailUrls['thumbnail']}" alt="${screenshot3.node.name}"/>
                    </div>
                    <p>
                        <a id="screenshot3update" onclick="$('#screenshot3container').show();$('#screenshot3update').hide();">Update</a>
                    </p>
                </c:if>
            </div>
            
            <div class="control-group">
                <label class="control-label" for="screenshot4"><fmt:message key="comnt_module.screenshot4"/></label>

                <c:if test="${not empty screenshot4.node}">
                    <c:set var="screenshot4Display" value="style=\"display: none;\""/>
                </c:if>

                <div class="controls" id="screenshot4container" ${screenshot4Display}>
                    <input placeholder="<fmt:message key="comnt_module.screenshot4" />" class="span16" type="file"
                           name="screenshot4" id="screenshot4"/>
                </div>

                <c:if test="${not empty screenshot4.node}">
                    <div>
                        <img src="${screenshot4.node.thumbnailUrls['thumbnail']}" alt="${screenshot4.node.name}"/>
                    </div>
                    <p>
                        <a id="screenshot4update" onclick="$('#screenshot4container').show();$('#screenshot4update').hide();">Update</a>
                    </p>
                </c:if>
            </div>

            <div class="control-group">
                <label class="control-label" for="videoProvider"><fmt:message key="comnt_module.videoProvider"/></label>
                <div class="controls">
                    <select name="videoProvider" id="videoProvider" >
                        <option value="youtube"> youtube </option>
                        <option value="dailymotion"> dailymotion </option>
                        <option value="vimeo"> vimeo </option>
                        <option value="watt"> watt </option>
                    </select>
                </div>
            </div>

            <div class="control-group">
                <label class="control-label" for="videoIdentifier"><fmt:message key="comnt_module.videoIdentifier"/></label>
                <div class="controls">
                    <input placeholder="<fmt:message key="comnt_module.videoIdentifier" />" class="span16" type="text"
                           name="videoIdentifier" id="videoIdentifier" value="${videoIdentifier.string}"/>
                </div>
            </div>

            <div class="control-group">
                <label class="control-label" for="videoWidth"><fmt:message key="comnt_module.videoWidth"/></label>
                <div class="controls">
                    <input placeholder="<fmt:message key="comnt_module.videoWidth" />" class="span16" type="text"
                           name="videoWidth" id="videoWidth" value="${videoWidth.string}"/>
                </div>
            </div>


            <div class="control-group">
                <label class="control-label" for="videoHeight"><fmt:message key="comnt_module.videoHeight"/></label>
                <div class="controls">
                    <input placeholder="<fmt:message key="comnt_module.videoHeight" />" class="span16" type="text"
                           name="videoHeight" id="videoHeight" value="${videoHeight.string}"/>
                </div>
            </div>

            <div class="control-group">
                <label class="control-label" for="videoAllowfullscreen"><fmt:message key="comnt_module.videoAllowfullscreen"/></label>
                <div class="controls">
                    <input placeholder="<fmt:message key="comnt_module.videoAllowfullscreen" />" class="span16" type="checkbox"
                           name="videoAllowfullscreen" id="videoAllowfullscreen" checked="${videoAllowfullscreen.string}"/>
                </div>
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
                    <textarea placeholder="<fmt:message key="comnt_module.quickDescription" />" class="span16"
                              name="quickDescription" id="quickDescription">${quickDescription.string}</textarea>
                </div>
            </div>

            <div class="control-group">
                <label class="control-label" for="bigDescription"><fmt:message key="comnt_module.bigDescription"/></label>
                <div class="controls">
                    <textarea rows="7" cols="35" id="jahia-module-bigDescription-${currentNode.UUID}"
                              placeholder="<fmt:message key="comnt_module.bigDescription" />" class="jahia-ckeditor span16"
                              name="bigDescription" >
                        <c:if test="${not empty bigDescription.string}">
                            ${fn:escapeXml(bigDescription.string)}
                        </c:if>
                    </textarea>
                </div>
            </div>

            <jcr:node var="licenseCategory" path="/sites/systemsite/categories/forge-categories/license"/>
            <c:if test="${not empty licenseCategory.node}">
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
            </c:if>

            <jcr:node var="moduleCategories" path="${moduleCategoriesPath}"/>
            <c:if test="${not empty moduleCategories}">

                <select id="moduleCategory" name="moduleCategory">
                    <option value=""><fmt:message key="comnt_module.category"/></option>
                    <c:forEach items="${jcr:getChildrenOfType(moduleCategories, 'jnt:category')}" var="category">
                        <option value="${category.identifier}">${category.displayableName}</option>
                    </c:forEach>
                </select>

            </c:if>

            <div class="control-group">
                <label class="control-label" for="tags"><fmt:message key="comnt_module.tags"/></label>
                <div class="controls">
                    <input placeholder="<fmt:message key="comnt_module.tags" />" type="text"
                           name="j:newTag" id="tags" />
                </div>
            </div>

            <c:if test="${jcr:hasPermission(currentNode, 'reviewModule')}">
                <div class="control-group">
                    <label class="control-label" for="supportedByJahia"><fmt:message key="comnt_module.supportedByJahia"/></label>
                    <div class="controls">
                        <input placeholder="<fmt:message key="comnt_module.supportedByJahia" />" class="span16" type="checkbox"
                               name="supportedByJahia" id="supportedByJahia" value="true" <c:if test="${supportedByJahia.boolean}">checked="true"</c:if>"/>
                    </div>
                </div>
                <div class="control-group">
                    <label class="control-label" for="reviewedByJahia"><fmt:message key="comnt_module.reviewedByJahia"/></label>
                    <div class="controls">
                        <input placeholder="<fmt:message key="comnt_module.reviewedByJahia" />" class="span16" type="checkbox"
                               name="reviewedByJahia" id="reviewedByJahia" value="true" <c:if test="${reviewedByJahia.boolean}">checked="true"</c:if>"/>
                    </div>
                </div>
            </c:if>

            <div class="control-group">
                <div class="controls">
                    <input type="submit" class="btn" onclick="CKEDITOR.instances['jahia-module-bigDescription-${currentNode.UUID}'].updateElement();" value="<fmt:message key="forge.submit" />"/>
                </div>
            </div>
        </fieldset>
    </form>
</template:tokenizedForm>
<c:if test="${edition}">
    <div class="edit"><a href="<c:url value='${targetNode}.html'/>">Back</a></div>
</c:if>