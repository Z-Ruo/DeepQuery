package back.model;

import lombok.Data;

@Data
public class CollectionCreateRequest {
    // 集合名称
    private String name;
    // 向量维度
    private int dimension = 384;
}
