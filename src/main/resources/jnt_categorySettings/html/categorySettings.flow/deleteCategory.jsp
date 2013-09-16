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
<jcr:node var="category" uuid="${flowHandler.currentCategory}"/>
<fieldset>
    <legend><fmt:message key="jahiaForge.settings.deleteCategory"/></legend>
    <p><fmt:message key="jahiaForge.settings.deleteCategory.confirm">
        <fmt:param value="<strong>${category.displayableName}<strong>"/>
    </fmt:message>
    </p>
    <c:if test="${!empty flowHandler.categoryUsages}">
        <c:out value="${category.displayableName}"/> is used by : <br>
        <ul>
            <c:forEach items="${flowHandler.categoryUsages}" var="usage">
                <li>${usage}</li>
            </c:forEach>
        </ul>
    </c:if>
    <form:form>
        <button class="btn" type="submit" name="_eventId_cancel"><i class="icon-step-backward"></i>&nbsp;<fmt:message key='label.cancel'/></button>
        <button class="btn btn-primary" type="submit" name="_eventId_submit"><i class="icon-trash icon-white"></i>&nbsp;<fmt:message key='label.remove'/></button>
    </form:form>
</fieldset>
