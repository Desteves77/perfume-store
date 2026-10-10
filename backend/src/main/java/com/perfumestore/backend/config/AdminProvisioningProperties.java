package com.perfumestore.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.admin.provisioning")
public class AdminProvisioningProperties {

    private boolean enabled;
    private String name;
    private String email;
    private String password;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean value) { enabled = value; }
    public String getName() { return name; }
    public void setName(String value) { name = value; }
    public String getEmail() { return email; }
    public void setEmail(String value) { email = value; }
    public String getPassword() { return password; }
    public void setPassword(String value) { password = value; }
}
