package org.jahia.modules.forge.flow;

import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.httpclient.HttpClient;
import org.apache.xerces.impl.dv.util.Base64;
import org.jahia.utils.i18n.Messages;
import org.springframework.binding.message.MessageBuilder;
import org.springframework.binding.validation.ValidationContext;
import org.springframework.context.i18n.LocaleContextHolder;

import java.io.IOException;
import java.io.Serializable;

/**
 * Bean to handle forge settings flow.
 */
public class ForgeSettings implements Serializable {

    private static final long serialVersionUID = 33235900427979718L;
    private String password;
    private String url;
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
        this.url = url;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public void validateView(ValidationContext context) {

        // try basic http connexion
        PostMethod httpMethod = new PostMethod(StringUtils.substringBeforeLast(url,"/content/repositories/") + "/service/local/artifact/maven/content");
        httpMethod.addRequestHeader("Authorization", "Basic " + Base64.encode((user + ":" + password).getBytes()));
        HttpClient httpClient = new HttpClient();
        try {
            int i = httpClient.executeMethod(httpMethod);
            if (i != 400) {
                context.getMessageContext().addMessage(new MessageBuilder()
                        .error()
                        .source("testUrl")
                        .defaultText(
                                Messages.getWithArgs("resources.Jahia_Forge",
                                        "jahiaForge.errors.url.not.working", LocaleContextHolder.getLocale(), url + "/service -> " + i))
                        .build());
            }
        } catch (IOException e) {
            context.getMessageContext().addMessage(new MessageBuilder()
                    .error()
                    .source("testUrl")
                    .defaultText(
                            Messages.get("resources.JahiaForge",
                                    "url.not.working", LocaleContextHolder.getLocale()))
                    .build());
        }
    }


}
