package com.ecocook.repository;

import com.ecocook.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * Récupère tous les produits d'un utilisateur, triés par date de péremption
     */
    List<Product> findByUserNameOrderByExpiryDateAsc(String userName);
    
    /**
     * Récupère les produits expirés d'un utilisateur
     */
    List<Product> findByUserNameAndExpiryDateBefore(String userName, LocalDate date);
    
    /**
     * Récupère les produits qui expirent bientôt
     */
    List<Product> findByUserNameAndExpiryDateBetween(String userName, LocalDate start, LocalDate end);
}