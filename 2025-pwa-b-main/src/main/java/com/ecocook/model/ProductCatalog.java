package com.ecocook.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "product_catalog")
public class ProductCatalog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String name;
    private String displayNameFr;
    private String category;
    private String defaultStorageType;
    private Integer defaultShelfLife;
    private String defaultUnit;
    private String iconPath;
    
    @jakarta.persistence.PrePersist
    @jakarta.persistence.PreUpdate
    private void ensureDefaults() {
        if (this.displayNameFr == null || this.displayNameFr.isBlank()) {
            this.displayNameFr = this.name;
        }
    }
    
    public ProductCatalog() {}
    
    public ProductCatalog(String name, String category, String defaultStorageType, Integer defaultShelfLife, String defaultUnit) {
        this.name = name;
        this.category = category;
        this.defaultStorageType = defaultStorageType;
        this.defaultShelfLife = defaultShelfLife;
        this.defaultUnit = defaultUnit;
        this.displayNameFr = name;
    }
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDisplayNameFr() { return displayNameFr; }
    public void setDisplayNameFr(String displayNameFr) { this.displayNameFr = displayNameFr; }
    
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    
    public String getDefaultStorageType() { return defaultStorageType; }
    public void setDefaultStorageType(String defaultStorageType) { this.defaultStorageType = defaultStorageType; }
    
    public Integer getDefaultShelfLife() { return defaultShelfLife; }
    public void setDefaultShelfLife(Integer defaultShelfLife) { this.defaultShelfLife = defaultShelfLife; }
    
    public String getDefaultUnit() { return defaultUnit; }
    public void setDefaultUnit(String defaultUnit) { this.defaultUnit = defaultUnit; }
    
    public String getIconPath() { return iconPath; }
    public void setIconPath(String iconPath) { this.iconPath = iconPath; }
}


