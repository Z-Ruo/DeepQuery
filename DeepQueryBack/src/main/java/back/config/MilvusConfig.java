package back.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.milvus.v2.client.ConnectConfig;
import io.milvus.v2.client.MilvusClientV2;

/**
 * Milvus向量数据库配置类
 * 负责创建和管理Milvus客户端连接
 */
@Configuration
public class MilvusConfig {

    @Value("${milvus.host}")
    private String host;
    
    @Value("${milvus.port}")
    private int port;
    
    @Value("${milvus.username}")
    private String username;
    
    @Value("${milvus.password}")
    private String password;

    /**
     * 创建Milvus客户端Bean
     * 
     * @return MilvusClientV2实例
     */
    @Bean
    public MilvusClientV2 milvusClient() {
        ConnectConfig connectConfig = ConnectConfig.builder()
                .uri("http://" + host + ":" + port) // 从配置文件读取 Milvus 服务的 URI
                .username(username) // 从配置文件读取 Milvus 用户名
                .password(password) // 从配置文件读取 Milvus 密码
                .build();
                
        return new MilvusClientV2(connectConfig);
    }
}