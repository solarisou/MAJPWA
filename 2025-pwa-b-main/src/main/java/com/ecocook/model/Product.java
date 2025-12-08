package com.ecocook.model;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "products")
public class Product {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String name;
    private int quantity;
    private String unit;
    private LocalDate expiryDate;
    
    @Column(name = "storage_type")
    private String storageType;
    
    @Column(name = "user_name")
    private String userName;
    
    // Constructeurs
    public Product() {}
    
    public Product(String name, int quantity, LocalDate expiryDate, String userName) {
        this.name = name;
        this.quantity = quantity;
        this.expiryDate = expiryDate;
        this.userName = userName;
        this.storageType = "placard";
    }
    
    public Product(String name, int quantity, LocalDate expiryDate, String storageType, String userName) {
        this.name = name;
        this.quantity = quantity;
        this.expiryDate = expiryDate;
        this.storageType = storageType;
        this.userName = userName;
    }
    
    public Product(String name, int quantity, String unit, LocalDate expiryDate, String storageType, String userName) {
        this.name = name;
        this.quantity = quantity;
        this.unit = unit;
        this.expiryDate = expiryDate;
        this.storageType = storageType;
        this.userName = userName;
    }
    
    // Getters et Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    
    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }
    
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    
    public String getStorageType() { return storageType; }
    public void setStorageType(String storageType) { this.storageType = storageType; }
    
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    
    public boolean isExpiringSoon() {
        if (expiryDate == null) return false;
        return expiryDate.isBefore(LocalDate.now().plusDays(7)) && expiryDate.isAfter(LocalDate.now());
    }
    
    public boolean isExpired() {
        if (expiryDate == null) return false;
        return expiryDate.isBefore(LocalDate.now());
    }
}