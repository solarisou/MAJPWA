package com.ecocook.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.ecocook.model.ProductCatalog;

@Repository
public interface ProductCatalogRepository extends JpaRepository<ProductCatalog, Long> {
    
    Optional<ProductCatalog> findByNameIgnoreCase(String name);
    
    List<ProductCatalog> findByCategory(String category);
    
    @Query("SELECT DISTINCT p.category FROM ProductCatalog p ORDER BY p.category")
    List<String> findAllCategories();
    
    List<ProductCatalog> findByNameContainingIgnoreCaseOrderByName(String search);
    
    List<ProductCatalog> findByDisplayNameFrContainingIgnoreCaseOrderByDisplayNameFr(String search);
}

