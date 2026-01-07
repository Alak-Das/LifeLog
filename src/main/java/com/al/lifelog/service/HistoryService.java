package com.al.lifelog.service;

import com.al.lifelog.model.MongoResourceHistory;
import com.al.lifelog.repository.HistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.Date;
import java.util.List;

@Service
public class HistoryService {

    private final HistoryRepository historyRepository;

    @Autowired
    public HistoryService(HistoryRepository historyRepository) {
        this.historyRepository = historyRepository;
    }

    public void saveHistory(String resourceId, String type, String json, Long version, Date lastUpdated) {
        MongoResourceHistory history = new MongoResourceHistory();
        history.setResourceId(resourceId);
        history.setResourceType(type);
        history.setFhirJson(json);
        history.setVersionId(version);
        history.setLastUpdated(lastUpdated);
        historyRepository.save(history);
    }

    public List<MongoResourceHistory> getHistory(String resourceId, String type) {
        return historyRepository.findByResourceIdAndResourceTypeOrderByVersionIdDesc(resourceId, type);
    }
}
