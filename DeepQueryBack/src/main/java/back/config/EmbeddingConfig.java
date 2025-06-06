package back.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.milvus.MilvusEmbeddingStore;
// 移除未使用的导入

@Configuration
public class EmbeddingConfig {

    @Value("${milvus.host}")
    private String milvusHost;
    
    @Value("${milvus.port}")
    private int milvusPort;
    
    @Value("${milvus.username}")
    private String milvusUsername;
    
    @Value("${milvus.password}")
    private String milvusPassword;
    
    @Value("${milvus.collection}")
    private String collectionName;
    
    @Value("${milvus.dimension}")
    private int dimension;

    @Bean
    public EmbeddingModel embeddingModel() {
        return new AllMiniLmL6V2EmbeddingModel();
    }

    @Bean
    public EmbeddingStore<TextSegment> embeddingStore() {
        return MilvusEmbeddingStore.builder()
                .uri("http://" + milvusHost + ":" + milvusPort) // 从配置文件读取 Milvus 服务的 URI
                .username(milvusUsername) // 从配置文件读取 Milvus 用户名
                .password(milvusPassword) // 从配置文件读取 Milvus 密码
                .collectionName(collectionName) // 从配置文件读取 Milvus 集合名称
                .dimension(dimension) // 从配置文件读取向量维度
                .build();
    }
}
