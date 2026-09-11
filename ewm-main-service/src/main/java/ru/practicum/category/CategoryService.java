package ru.practicum.category;

import java.util.List;

public interface CategoryService {

    CategoryDto createCategory(NewCategoryDto request);

    CategoryDto updateCategory(Long catId, CategoryDto request);

    void deleteCategory(Long catId);

    List<CategoryDto> getCategories(int from, int size);

    CategoryDto getCategory(Long catId);

    Category getCategoryOrThrow(Long catId);
}
