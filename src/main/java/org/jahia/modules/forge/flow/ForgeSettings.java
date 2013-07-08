package org.jahia.modules.forge.flow;

import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.httpclient.HttpClient;
import org.apache.xerces.impl.dv.util.Base64;
import org.jahia.utils.i18n.Messages;
import org.springframework.binding.message.MessageBuilder;
import org.springframework.binding.validation.ValidationContext;
import org.springframework.context.i18n.LocaleContextHolder;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Bean to handle forge settings flow.
 */
public class ForgeSettings implements Serializable {

    private static final long serialVersionUID = 33235900427979718L;
    private String password;
    private String url;
    private String user;
    private String passwordConfirm;
    private String originPassword;

    /**
     * default constructor
     */
    public ForgeSettings() {
        super();
    }

    public void setOriginPassword(String originPassword) {
        this.originPassword = originPassword;
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
        this.url = url;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPasswordConfirm() {
        return passwordConfirm;
    }

    public void setPasswordConfirm(String passwordConfirm) {
        this.passwordConfirm = passwordConfirm;
    }

    public void validateView(ValidationContext context) {
        // verify password
        if (StringUtils.isNotBlank(password)) {
            if (!passwordConfirm.equals(password)) {
                context.getMessageContext().addMessage(new MessageBuilder()
                        .error()
                        .source("passwordConfirm")
                        .defaultText(
                                Messages.get("resources.Jahia_Forge",
                                        "jahiaForge.errors.password.not.matching", LocaleContextHolder.getLocale()))
                        .build());
                return;
            }
            originPassword = password;
        }

        // try basic http connexion
        GetMethod httpMethod = new GetMethod(url);
        httpMethod.addRequestHeader("Authorization", "Basic " + Base64.encode((user + ":" + originPassword).getBytes()));
        HttpClient httpClient = new HttpClient();
        try {
            int i = httpClient.executeMethod(httpMethod);
            if (i != 200 ) {
                context.getMessageContext().addMessage(new MessageBuilder()
                        .error()
                        .source("testUrl")
                        .defaultText(
                                Messages.getWithArgs("resources.Jahia_Forge",
                                        "jahiaForge.errors.url.not.working", LocaleContextHolder.getLocale(),i))
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
