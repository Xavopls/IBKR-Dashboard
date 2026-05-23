package com.xavopls.ibkr_dashboard.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "ibkr")
public class IbkrProperties {

    private String accountId;
    private Tws tws = new Tws();
    private Flex flex = new Flex();

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public Tws getTws() {
        return tws;
    }

    public void setTws(Tws tws) {
        this.tws = tws;
    }

    public Flex getFlex() {
        return flex;
    }

    public void setFlex(Flex flex) {
        this.flex = flex;
    }

    public static class Tws {
        private String host = "127.0.0.1";
        private int port = 7497;
        private int clientId = 101;
        private int timeoutSeconds = 20;

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public int getClientId() {
            return clientId;
        }

        public void setClientId(int clientId) {
            this.clientId = clientId;
        }

        public int getTimeoutSeconds() {
            return timeoutSeconds;
        }

        public void setTimeoutSeconds(int timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
        }
    }

    public static class Flex {
        private String token;
        private String tradesQueryId;
        private String baseUrl = "https://ndcdyn.interactivebrokers.com/AccountManagement/FlexWebService";
        private int version = 3;

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }

        public String getTradesQueryId() {
            return tradesQueryId;
        }

        public void setTradesQueryId(String tradesQueryId) {
            this.tradesQueryId = tradesQueryId;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public int getVersion() {
            return version;
        }

        public void setVersion(int version) {
            this.version = version;
        }
    }
}
