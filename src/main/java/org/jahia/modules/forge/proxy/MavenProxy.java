package org.jahia.modules.forge.proxy;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.xerces.impl.dv.util.Base64;
import org.jahia.data.templates.ModuleReleaseInfo;
import org.jahia.services.content.JCRCallback;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.JCRTemplate;
import org.jahia.services.content.decorator.JCRSiteNode;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class MavenProxy implements Controller {

//    private HttpClientService httpClientService;

    @Override
    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {

        if (request.getMethod().equals("GET")) {
            String pathInfo = StringUtils.substringAfter(request.getPathInfo(),"/mavenproxy/");
            String siteName = StringUtils.substringBefore(pathInfo, "/");
            String path = "/" + StringUtils.substringAfter(pathInfo, "/");

            ModuleReleaseInfo releaseInfo = getModuleReleaseInfo(siteName);

            String url = releaseInfo.getRepositoryUrl() + path;
            HttpClient client = new HttpClient();
            GetMethod getMethod = new GetMethod(url);
            getMethod.addRequestHeader("Authorization", "Basic " + Base64.encode((releaseInfo.getCatalogUsername() + ":" + releaseInfo.getCatalogPassword()).getBytes()));

            int res = client.executeMethod(null, getMethod);
            if (res == 200) {
                for (Header header : getMethod.getResponseHeaders()) {
                    response.setHeader(header.getName(), header.getValue());
                }
                IOUtils.copy(getMethod.getResponseBodyAsStream(), response.getOutputStream());
            } else {
                response.sendError(res);
            }
        }
        return null;
    }

    private ModuleReleaseInfo getModuleReleaseInfo(final String siteName) throws RepositoryException {
        return JCRTemplate.getInstance().doExecuteWithSystemSession(new JCRCallback<ModuleReleaseInfo>() {
            @Override
            public ModuleReleaseInfo doInJCR(JCRSessionWrapper session) throws RepositoryException {
                JCRSiteNode siteNode = (JCRSiteNode) session.getNode("/sites/"+siteName);
                String forgeSettingsUrl = siteNode.getProperty("forgeSettingsUrl").getString();
                String user = siteNode.getProperty("forgeSettingsUser").getString();
                String password = new String(Base64.decode(siteNode.getProperty("forgeSettingsPassword").getString()));
                ModuleReleaseInfo info = new ModuleReleaseInfo();
                info.setRepositoryUrl(forgeSettingsUrl);
                info.setCatalogUsername(user);
                info.setCatalogPassword(password);
                return info;
            }
        });
    }

//    public void setHttpClientService(HttpClientService httpClientService) {
//        this.httpClientService = httpClientService;
//    }

}
