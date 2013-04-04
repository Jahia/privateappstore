<%@ taglib prefix="jcr" uri="http://www.jahia.org/tags/jcr" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="utility" uri="http://www.jahia.org/tags/utilityLib" %>
<%@ taglib prefix="template" uri="http://www.jahia.org/tags/templateLib" %>
<%@ taglib prefix="uiComponents" uri="http://www.jahia.org/tags/uiComponentsLib" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="functions" uri="http://www.jahia.org/tags/functions" %>
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
<template:addResources type="css" resources="modulesForge.css"/>
<template:addResources type="css" resources="jquery.autocomplete.css" />
<template:addResources type="css" resources="thickbox.css" />
<template:addResources type="javascript" resources="jquery.autocomplete.js" />
<template:addResources type="javascript" resources="jquery.bgiframe.min.js" />
<template:addResources type="javascript" resources="thickbox-compressed.js" />

<c:set var="id" value="${currentNode.identifier}"/>
<jcr:nodeProperty node="${currentNode}" name='moduleRepository' var="moduleRepository"/>
<c:set var="screenshotsMaxNbr" value="${currentNode.properties.screenshotsMaxNbr.long}"/>
<c:set var="screenshotInputsDefaultNbr" value="${currentNode.properties.screenshotInputsDefaultNbr.long}"/>
<uiComponents:ckeditor selector="jahia-module-bigDescription-${id}"/>
<uiComponents:ckeditor selector="jahia-module-howToInstall-${id}"/>
<uiComponents:ckeditor selector="jahia-module-FAQ-${id}"/>

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
        <jcr:nodeProperty node="${renderContext.mainResource.node}" name="icon" var="icon"/>
        <jcr:nodeProperty node="${renderContext.mainResource.node}" name="quickDescription" var="quickDescription"/>
        <jcr:nodeProperty node="${renderContext.mainResource.node}" name="bigDescription" var="bigDescription"/>
        <jcr:nodeProperty node="${renderContext.mainResource.node}" name="category" var="categoryNode"/>
        <jcr:nodeProperty node="${renderContext.mainResource.node}" name="howToInstall" var="howToInstall"/>
        <jcr:nodeProperty node="${renderContext.mainResource.node}" name="FAQ" var="FAQ"/>

        <jcr:node path="${renderContext.mainResource.node.path}/screenshots" var="screenshots"/>
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
<c:set var="separator" value="${functions:default(currentResource.moduleParams.separator, ', ')}"/>

<template:addResources type="inlinejavascript">
    <script type="text/javascript">

        $(document).ready(function() {

            // Set JQuery to traditional
            $.ajaxSetup({traditional: true, cache:false});

            jQuery.validator.addMethod("regexp", function(value, element, param) {
                          return this.optional(element) || param.test(value);
                        });

            $("#moduleForm-${id}").validate({

                rules: {
                    'jcr:title': {
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

                    <c:forEach var="i" begin="1" end="${screenshotsMaxNbr}">

                        'screenshot${i}' : {
                            <c:if test="${i == 1 && not edition}">
                            required: true,
                            </c:if>
                            regexp: /(\S+?)\.(jpg|png|gif|jpeg)$/i
                        },
                    </c:forEach>

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
                    'jcr:title': {
                        required: "<fmt:message key='forge.label.askTitle'/>",
                        minlength: "<fmt:message key='forge.label.titleSizeWarning'/>"
                    },
                    'authorName': {
                        required: "<fmt:message key='forge.label.askAuthorName'/>",
                        minlength: "<fmt:message key='forge.label.authorNameSizeWarning'/>"
                    },
                    'authorEmail': "<fmt:message key='forge.label.askValidEmail'/>",

                    <c:forEach var="i" begin="1" end="${screenshotsMaxNbr}">

                        'screenshot${i}' : {
                            <c:if test="${i == 1 && not edition}">
                            required: "<fmt:message key='forge.label.askScreenshot1'/>",
                            </c:if>
                            regexp: "<fmt:message key='forge.label.askValidImage'/>"
                        },
                    </c:forEach>

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

            $(".addScreenshot").click(function() {
                $(".control-group.hidden-control-group.screenshot").not(".set-control-group").first()
                        .slideDown(function(){$(this).removeClass("hidden-control-group");});
            });

            $(".updateScreenshot").click(function() {
                $(this).parents('.control-group').find('.controls').slideDown();
            });

            $(".updateScreenshot").parents('.control-group').addClass("set-control-group").removeClass("hidden-control-group");


            function getText(node) {
                return node["nodename"];
            }

            function format(result) {
                return getText(result["node"]);
            }

            $(".newTagInput").autocomplete("<c:url value='${url.find}'/>", {
                dataType: "json",
                cacheLength: 1,
                parse: function parse(data) {
                    return $.map(data, function(row) {
                        return {
                            data: row,
                            value: getText(row["node"]),
                            result: getText(row["node"])
                        }
                    });
                },
                formatItem: function(item) {
                    return format(item);
                },
                extraParams: {
                    query : "select * from [jnt:tag] as tags where isdescendantnode(tags,'${functions:sqlencode(renderContext.site.path)}/tags') and localname(tags) like '%{$q}%'",
                    escapeColon : "false",
                    propertyMatchRegexp : "{$q}.*",
                    removeDuplicatePropValues : "false"
                }
            });



        });

        function deleteTag(name) {

            var deletedTags = $('#deletedTag').val();
            var separator = "${separator}";

            if (deletedTags)
                deletedTags += separator + " " + name;
            else
                deletedTags = name;

            $('#deletedTag').val(deletedTags);
        }

    </script>
</template:addResources>

<template:tokenizedForm>
    <form action="<c:url value='${targetNode}.${edition ? "editModule" : "addModule"}.do'/>" method="post" id="moduleForm-${id}" class="moduleForm" enctype="multipart/form-data"  accept="application/json">
        <fieldset>

            <div class="control-group">
                <label class="control-label" for="jcr:title"><fmt:message key="comnt_module.title"/></label>
                <div class="controls">
                    <input placeholder="<fmt:message key="comnt_module.title" />" class="span16" type="text"
                           name="jcr:title" id="jcr:title" value="${title.string}"/>
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

            <c:forEach var="i" begin="1" end="${screenshotsMaxNbr}">

                <div class="control-group screenshot ${i > screenshotInputsDefaultNbr ? 'hidden-control-group' : '' }">
                    <label class="control-label" for="screenshot${i}"><fmt:message key="comnt_module.screenshot"/>&nbsp;${i}</label>
                    <div class="controls">
                        <input class="span16" type="file"
                               name="screenshot${i}" id="screenshot${i}" />
                    </div>

                    <div class="screenshotThumbnail">
                        <c:set var="screenshotsNodes" value="${not empty screenshots ? jcr:getNodes(screenshots, 'comnt:moduleScreenshot') : ''}"/>
                        <c:if test="${edition && not empty screenshotsNodes}">
                            <c:forEach var="moduleScreenshot" items="${screenshotsNodes}">

                                <c:set var="screenshotName" value="screenshot${i}"/>
                                <jcr:node var="screenshot" path="${moduleScreenshot.properties['screenshot'].node.path}"/>
                                <c:if test="${moduleScreenshot.displayableName == screenshotName}">
                                    <template:module node="${moduleScreenshot}"/>
                                </c:if>

                            </c:forEach>
                        </c:if>
                    </div>

                </div>

            </c:forEach>

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
                <label class="control-label" for="icon"><fmt:message key="comnt_module.icon"/></label>
                <div class="controls">
                    <input placeholder="<fmt:message key="comnt_module.icon" />" class="span16" type="file"
                           name="icon" id="icon" />
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
                    <textarea rows="7" cols="35" id="jahia-module-bigDescription-${id}"
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
                    <c:forEach items="${jcr:getNodes(moduleCategories, 'jnt:category')}" var="category">
                        <option value="${category.identifier}" ${category.identifier == categoryNode.node.identifier ? 'selected' : ''}>${category.displayableName}</option>
                    </c:forEach>
                </select>

            </c:if>

            <div class="control-group">
                <label class="control-label" for="newTag"><fmt:message key="comnt_module.tags"/></label>
                <div class="controls">
                    <input type="hidden" value="" name="deletedTag" id="deletedTag" />
                    <input placeholder="<fmt:message key="comnt_module.tags" />" type="text"
                           name="j:newTag" id="newTag" class="newTagInput" />
                </div>

                <c:if test="${jcr:isNodeType(renderContext.mainResource.node, 'jmix:tagged')}">

                    <jcr:nodeProperty node="${renderContext.mainResource.node}" name="j:tags" var="assignedTags"/>
                    <jsp:useBean id="filteredTags" class="java.util.LinkedHashMap"/>

                    <c:forEach items="${assignedTags}" var="tag">
                        <c:if test="${not empty tag.node}">
                            <c:set target="${filteredTags}" property="${tag.node.identifier}" value="${tag.node.name}"/>
                        </c:if>
                    </c:forEach>

                    <p>
                        <c:choose>

                            <c:when test="${not empty filteredTags}">
                                <c:forEach items="${filteredTags}" var="tag" varStatus="status">

                                    <span class="taggeditem">${fn:escapeXml(tag.value)}</span>
                                    <a href="#" onclick="deleteTag('${tag.value}')">delete</a>
                                    ${!status.last ? separator : ''}

                                </c:forEach>
                            </c:when>
                            <c:otherwise>
                                <span class="notaggeditem${boundComponent.identifier}"><fmt:message
                                    key="label.tags.notag"/></span>
                            </c:otherwise>

                        </c:choose>
                    </p>

                </c:if>

            </div>

            <div class="control-group">
                <label class="control-label" for="jahia-module-howToInstall-${id}"><fmt:message key="comnt_module.howToInstall"/></label>
                <div class="controls">
                    <textarea rows="7" cols="35" id="jahia-module-howToInstall-${id}"
                              placeholder="<fmt:message key="comnt_module.howToInstall" />" class="jahia-ckeditor span16"
                              name="howToInstall" >
                        <c:if test="${not empty howToInstall.string}">
                            ${fn:escapeXml(howToInstall.string)}
                        </c:if>
                    </textarea>
                </div>
            </div>

            <div class="control-group">
                <label class="control-label" for="jahia-module-FAQ-${id}"><fmt:message key="comnt_module.FAQ"/></label>
                <div class="controls">
                    <textarea rows="7" cols="35" id="jahia-module-FAQ-${id}"
                              placeholder="<fmt:message key="comnt_module.FAQ" />" class="jahia-ckeditor span16"
                              name="FAQ" >
                        <c:if test="${not empty FAQ.string}">
                            ${fn:escapeXml(FAQ.string)}
                        </c:if>
                    </textarea>
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
                    <input type="submit" class="btn" onclick="CKEDITOR.instances['jahia-module-bigDescription-${id}'].updateElement();" value="<fmt:message key="forge.submit" />"/>
                </div>
            </div>
        </fieldset>
    </form>
</template:tokenizedForm>
<c:if test="${edition}">
    <div class="edit"><a href="<c:url value='${targetNode}.html'/>">Back</a></div>
</c:if>