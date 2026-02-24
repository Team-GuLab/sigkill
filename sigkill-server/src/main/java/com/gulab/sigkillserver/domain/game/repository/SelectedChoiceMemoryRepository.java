package com.gulab.sigkillserver.domain.game.repository;

import com.gulab.sigkillserver.domain.game.model.SelectedChoice;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class SelectedChoiceMemoryRepository implements SelectedChoiceRepository {

    private final Map<Long, SelectedChoice> store = new ConcurrentHashMap<>();

    @Override public SelectedChoice save(SelectedChoice selectedChoice) {
        store.put(selectedChoice.getGameId(), selectedChoice);
        return selectedChoice;
    }
}
