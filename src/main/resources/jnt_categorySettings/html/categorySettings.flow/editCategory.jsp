<%@ page import="org.springframework.context.i18n.LocaleContextHolder" %>
<%@ page import="java.util.Locale" %>
<%@ taglib prefix="jcr" uri="http://www.jahia.org/tags/jcr" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="utility" uri="http://www.jahia.org/tags/utilityLib" %>
<%@ taglib prefix="template" uri="http://www.jahia.org/tags/templateLib" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="functions" uri="http://www.jahia.org/tags/functions" %>
<%@ taglib prefix="user" uri="http://www.jahia.org/tags/user" %>
<%--@elvariable id="currentNode" type="org.jahia.services.content.JCRNodeWrapper"--%>
<%--@elvariable id="out" type="java.io.PrintWriter"--%>
<%--@elvariable id="script" type="org.jahia.services.render.scripting.Script"--%>
<%--@elvariable id="scriptInfo" type="java.lang.String"--%>
<%--@elvariable id="workspace" type="java.lang.String"--%>
<%--@elvariable id="renderContext" type="org.jahia.services.render.RenderContext"--%>
<%--@elvariable id="currentResource" type="org.jahia.services.render.Resource"--%>
<%--@elvariable id="url" type="org.jahia.services.render.URLGenerator"--%>
<%--@elvariable id="flowHandler" type="org.jahia.modules.forge.flow.CategorySettingsHandler"--%>
<script>
    function addI18nRow() {
        var languageCode = $("#newLanguage").val();
        if (languageCode != null) {
            var $selectedOption = $("#newLanguage option[value='" + languageCode + "']");
            var languageName = $selectedOption.text();
            $('#roleI18nPropsTable > tbody:last').append("<tr><td>" + languageName +
                    "</td><td><input type=\"text\" name=\"lang_" + languageCode + "\" onchange=\"$('.submitButton').addClass('btn-danger')\" /></td></tr>");
            $selectedOption.remove();
        }
    }
</script>
<jcr:node var="category" uuid="${flowHandler.currentCategory}"/>
<form:form>
<fieldset>
    <legend><fmt:message key="jahiaForge.settings.editCategory"/></legend>
    <h2><fmt:message key="label.edit"/>&nbsp;${category.name}</h2>
    <table class="table table-bordered table-striped table-hover" id="roleI18nPropsTable">
        <thead>
        <tr>
            <th width="10%"><fmt:message key="label.language"/></th>
            <th width="20%"><fmt:message key="label.title"/></th>
        </tr>
        </thead>

        <tbody>
        <c:forEach items="${flowHandler.categoryI18NTitles}" var="entry" varStatus="loopStatus">
            <c:if test="${not empty entry.value}">
                <tr>
                    <td>
                        <c:set var="locale" value="${entry.key.language}"/>
                        <%=new Locale((String) pageContext.getAttribute("locale")).getDisplayName(LocaleContextHolder.getLocale())%>
                    </td>
                    <td>
                        <input type="text" name="lang_${entry.key.language}" value="${entry.value}" onchange="$('.submitButton').addClass('btn-danger')" />
                    </td>
                </tr>
            </c:if>
        </c:forEach>
        </tbody>
    </table>
    <c:if test="${!empty flowHandler.availableLanguages}">
        <fieldset>
        <label><fmt:message key="jahiaForge.settings.availableLanguages"/></label>
        <select id="newLanguage">
            <c:forEach items="${flowHandler.availableLanguages}" var="entry">
                <option value="${entry}"><%=new Locale((String) pageContext.getAttribute("entry")).getDisplayName(LocaleContextHolder.getLocale())%></option>
            </c:forEach>
        </select>
        <button class="btn" onclick="addI18nRow(); return false;"><i class="icon-plus"></i></button>
        </fieldset>
    </c:if>
    <button class="btn" type="submit" name="_eventId_cancel"><i class="icon-step-backward"></i>&nbsp;<fmt:message key='label.cancel'/></button>
    <button class="btn btn-primary" type="submit" name="_eventId_submit"><i class="icon-ok icon-white"></i>&nbsp;<fmt:message key='label.save'/></button>
</form:form>
</fieldset>