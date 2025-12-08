package com.ecocook.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "shopping_list_items")
public class ShoppingListItem {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String name;
    private Integer quantity;
    private String unit;
    private boolean checked;
    
    @Column(name = "user_name")
    private String userName;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    public ShoppingListItem() {
        this.createdAt = LocalDateTime.now();
        this.checked = false;
    }
    
    public ShoppingListItem(String name, Integer quantity, String unit, String userName) {
        this.name = name;
        this.quantity = quantity;
        this.unit = unit;
        this.userName = userName;
        this.createdAt = LocalDateTime.now();
        this.checked = false;
    }
    
    public Long getId() { return id; }
    public void setId(Long id) {
        if (id != null) {
            this.id = id;
        }
    }
    
    public String getName() { return name; }
    public void setName(String name) {
        if (name != null && !name.isEmpty()) {
            this.name = name;
        }
    }
    
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    
    public boolean isChecked() { return checked; }
    public void setChecked(boolean checked) { this.checked = checked; }
    
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

