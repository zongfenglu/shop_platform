package com.shopplatform.domain.ssl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class AcmeProperties {

    private final boolean enabled;
    private final String directoryUrl;
    private final String email;
    private final String certDir;
    private final String reloadCommand;

    public AcmeProperties(
            @Value("${shop.acme.enabled:false}") boolean enabled,
            @Value("${shop.acme.directory-url:https://acme-staging-v02.api.letsencrypt.org/directory}") String directoryUrl,
            @Value("${shop.acme.email:}") String email,
            @Value("${shop.acme.cert-dir:./data/certs}") String certDir,
            @Value("${shop.acme.reload-command:}") String reloadCommand) {
        this.enabled = enabled;
        this.directoryUrl = directoryUrl == null ? "" : directoryUrl.trim();
        this.email = email == null ? "" : email.trim();
        this.certDir = certDir == null ? "./data/certs" : certDir.trim();
        this.reloadCommand = reloadCommand == null ? "" : reloadCommand.trim();
    }

    public boolean enabled() {
        return enabled;
    }

    public boolean ready() {
        return enabled && StringUtils.hasText(directoryUrl) && StringUtils.hasText(email);
    }

    public String directoryUrl() {
        return directoryUrl;
    }

    public String email() {
        return email;
    }

    public String certDir() {
        return certDir;
    }

    public String reloadCommand() {
        return reloadCommand;
    }
}
