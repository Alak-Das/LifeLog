package com.al.lifelog.repository;

import com.al.lifelog.model.MongoResourceHistory;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface HistoryRepository extends MongoRepository<MongoResourceHistory, String> {
    List<MongoResourceHistory> findByResourceIdAndResourceTypeOrderByVersionIdDesc(String resourceId,
            String resourceType);
}
