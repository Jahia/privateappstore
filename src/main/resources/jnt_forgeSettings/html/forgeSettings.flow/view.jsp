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
<c:forEach var="msg" items="${flowRequestContext.messageContext.allMessages}">
    <div class="${msg.severity == 'ERROR' ? 'validationError' : ''} alert ${msg.severity == 'ERROR' ? 'alert-error' : 'alert-success'}">
        <button type="button" class="close" data-dismiss="alert">&times;</button>
            ${fn:escapeXml(msg.text)}</div>
</c:forEach>
<c:url var="urlAction"
       value="${url.base}${renderContext.mainResource.path}?${fn:substringAfter(flowExecutionUrl,'?')}"/>
<h3><fmt:message key="jahiaForge.settings.title"/></h3>
<form:form modelAttribute="forgeSettings" cssClass="form-horizontal" autocomplete="off" action="${urlAction}">

    <div class="form-group">
        <label for="url" class="col-sm-2 control-label"><fmt:message key="jahiaForge.settings.url"/></label>
        <div class="col-md-10">
            <form:input class="form-control" type="text" id="url" path="url"/>
        </div>
    </div>
    <div class="form-group">
        <label for="id" class="col-sm-2 control-label"><fmt:message key="jahiaForge.settings.id"/></label>
        <div class="col-md-10">
            <form:input class="form-control" type="text" id="id" path="id"/>
        </div>
    </div>
    <div class="form-group">
        <label for="user" class="col-sm-2 control-label"><fmt:message key="jahiaForge.settings.user"/></label>
        <div class="col-md-10">
            <form:input class="form-control" type="text" id="user" path="user"/>
        </div>
    </div>
    <div class="form-group">
        <label for="password" class="col-sm-2 control-label"><fmt:message
                key="jahiaForge.settings.password"/></label>
        <div class="col-md-10">
            <form:password class="form-control" id="password" path="password"/>
        </div>
    </div>
    <div class="form-group">
        <div class="col-sm-offset-2 col-sm-10">
            <button class="btn btn-primary" id="submit" type="submit" name="_eventId_submit"><i
                    class="icon-ok icon-white"></i>&nbsp;<fmt:message key='label.save'/></button>
        </div>
    </div>
</form:form>
