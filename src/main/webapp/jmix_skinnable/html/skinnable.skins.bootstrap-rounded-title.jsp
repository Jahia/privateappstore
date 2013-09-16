<%@ taglib uri="http://www.jahia.org/tags/jcr" prefix="jcr" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib prefix="template" uri="http://www.jahia.org/tags/templateLib" %>
<jcr:nodeProperty node="${currentNode}" name="jcr:title" var="title"/>
<div class="box ">
    <c:if test="${not empty title}">
        <h4>${fn:escapeXml(title.string)}</h4>
    </c:if>
    ${wrappedContent}
</div>
