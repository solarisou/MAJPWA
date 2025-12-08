package com.ecocook.repository;

import com.ecocook.model.ShoppingListItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface ShoppingListRepository extends JpaRepository<ShoppingListItem, Long> {
    
    List<ShoppingListItem> findByUserNameOrderByCreatedAtDesc(String userName);
    
    List<ShoppingListItem> findByUserNameAndCheckedOrderByCreatedAtDesc(String userName, boolean checked);
    
    @Transactional
    @Modifying
    void deleteByUserNameAndChecked(String userName, boolean checked);
}

