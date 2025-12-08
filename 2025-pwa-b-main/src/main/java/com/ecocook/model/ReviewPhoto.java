package com.ecocook.model;

import jakarta.persistence.*;

/**
 * Photo jointe à un avis de recette.
 */
@Entity
@Table(name = "review_photos")
public class ReviewPhoto {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "review_id", nullable = false)
    private RecipeReview review;
    
    @Column(name = "photo_path", nullable = false, length = 500)
    private String photoPath; // chemin dans le dossier statique
    
    @Column(name = "photo_name", length = 255)
    private String photoName; // nom d'origine fourni par l'utilisateur
    
    public ReviewPhoto() {}
    
    public ReviewPhoto(String photoPath, String photoName) {
        this.photoPath = photoPath;
        this.photoName = photoName;
    }
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public RecipeReview getReview() { return review; }
    public void setReview(RecipeReview review) { this.review = review; }
    
    public String getPhotoPath() { return photoPath; }
    public void setPhotoPath(String photoPath) { this.photoPath = photoPath; }
    
    public String getPhotoName() { return photoName; }
    public void setPhotoName(String photoName) { this.photoName = photoName; }
}

