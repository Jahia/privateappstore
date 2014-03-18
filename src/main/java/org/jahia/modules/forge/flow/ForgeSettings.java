package org.jahia.modules.forge.flow;

import java.io.Serializable;

import org.apache.commons.lang.StringUtils;

/**
 * Bean to handle Private App Store settings flow.
 */
public class ForgeSettings implements Serializable {

    private static final long serialVersionUID = 33235900427979718L;
    private String password;
    private String url;
    private String id;
    private String user;

    /**
     * default constructor
     */
    public ForgeSettings() {
        super();
    }


    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        // set password only if not empty
        if (StringUtils.isNotEmpty(password)) {
            this.password = password;
        }
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        if (StringUtils.endsWith(url,"/")) {
            url = StringUtils.substringBeforeLast(url,"/");
        }
        this.url = StringUtils.trim(url);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

}
