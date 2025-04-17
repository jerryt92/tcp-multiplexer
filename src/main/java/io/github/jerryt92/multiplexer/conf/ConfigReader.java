
package io.github.jerryt92.multiplexer.conf;

import io.github.jerryt92.multiplexer.TcpMultiplexer;
import io.github.jerryt92.multiplexer.protocol.ProtocolType;
import lombok.Data;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.plugins.convert.TypeConverters;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.net.InetAddress;
import java.util.HashSet;
import java.util.Map;

public class ConfigReader {
    private static final Logger log = LogManager.getLogger(ConfigReader.class);
    public static final ConfigReader INSTANCE = new ConfigReader();

    @Getter
    private final AppConfig appConfig = new AppConfig();


    private ConfigReader() {
        ClassLoader classLoader = TcpMultiplexer.class.getClassLoader();
        Yaml yaml = new Yaml();
        try (InputStream inputStream = classLoader.getResourceAsStream("conf.yaml")) {
            Map<String, Object> obj = yaml.load(inputStream);
            Map<String, Object> serverConfigMap = (Map<String, Object>) obj.get("server");
            ServerConfig serverConfig = new ServerConfig();
            String host = (String) serverConfigMap.get("host");
            serverConfig.setHost(InetAddress.getByName(StringUtils.defaultIfBlank(host, "0.0.0.0")));
            serverConfig.setPort((Integer) serverConfigMap.get("port"));
            appConfig.setServer(serverConfig);
            Map<String, Object> forwardConfigMap = (Map<String, Object>) obj.get("forward");
            ForwardConfig forwardConfig = new ForwardConfig();
            forwardConfig.setDefault((String) forwardConfigMap.get("default"));
            forwardConfig.setSsl((String) forwardConfigMap.get("ssl"));
            forwardConfig.setHttp((String) forwardConfigMap.get("http"));
            forwardConfig.setSsh((String) forwardConfigMap.get("ssh"));
            forwardConfig.setMqtt((String) forwardConfigMap.get("mqtt"));
            forwardConfig.setEnableProtocols(new HashSet<>());
            for (String protocol : ((String) forwardConfigMap.get("enabled")).split(",")) {
                protocol = protocol.trim();
                switch (protocol) {
                    case "ssl":
                        forwardConfig.getEnableProtocols().add(ProtocolType.SSL_TLS);
                        break;
                    case "http":
                        forwardConfig.getEnableProtocols().add(ProtocolType.HTTP);
                        break;
                    case "mqtt":
                        forwardConfig.getEnableProtocols().add(ProtocolType.MQTT);
                        break;
                    case "ssh":
                        forwardConfig.getEnableProtocols().add(ProtocolType.SSH);
                        break;
                }
            }
            appConfig.setForward(forwardConfig);
        } catch (Exception e) {
            log.error("Failed to read configuration file", e);
            throw new RuntimeException("Failed to read configuration file", e);
        }
    }

    @Data
    public static class AppConfig {
        private ServerConfig server;
        private ForwardConfig forward;

        public void setServer(ServerConfig server) {
            this.server = server;
        }

        public void setForward(ForwardConfig forward) {
            this.forward = forward;
        }
    }

    @Data
    public static class ServerConfig {
        private InetAddress host;
        private int port;

    }

    @Data
    public static class ForwardConfig {
        private HashSet<ProtocolType> enableProtocols;
        private String defaultAddress;
        private String ssl;
        private String http;
        private String ssh;
        private String mqtt;

        public String getDefault() {
            return defaultAddress;
        }

        public void setDefault(String defaultAddress) {
            this.defaultAddress = defaultAddress;
        }
    }
}