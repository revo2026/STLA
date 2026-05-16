package com.stla.domain.interfaces;

import com.stla.domain.models.Category;
import java.util.List;
import java.util.Optional;

public interface CategoryRepository {
    Optional<Category> findById(String id);
    List<Category> findAll();
    List<Category> findActive();
    void save(Category category);
    void update(Category category);
    void deactivate(String id);
}
