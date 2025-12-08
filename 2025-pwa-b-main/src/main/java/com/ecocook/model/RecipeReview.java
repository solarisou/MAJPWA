package com.ecocook.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Avis laissé sur une recette, pouvant contenir une note, un commentaire, des photos et des réponses.
 */
@Entity
@Table(name = "recipe_reviews")
public class RecipeReview {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "recipe_id", nullable = false)
    private Recipe recipe;
    
    @Column(name = "user_name", nullable = false)
    private String userName; // identifiant de l'utilisateur qui a laissé l'avis
    
    @Column(nullable = false)
    private Integer rating; // note sur 5
    
    @Column(length = 2000)
    private String comment; // texte facultatif
    
    @OneToMany(mappedBy = "review", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<ReviewPhoto> photos = new ArrayList<>();

    @OneToMany(mappedBy = "review", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("createdAt ASC")
    private List<ReviewReply> replies = new ArrayList<>();
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private ReviewStatus status = ReviewStatus.APPROVED;
    
    @Column(name = "reported", nullable = false)
    private Boolean reported = false; // l'avis a-t-il été signalé ?
    
    @Column(name = "report_reason", length = 1000)
    private String reportReason; // explication fournie lors du signalement
    
    @Column(name = "reported_by")
    private String reportedBy; // identifiant de la personne qui a signalé
    
    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;
    
    @Column(name = "reviewed_by")
    private String reviewedBy; // identifiant de l'admin qui a traité l'avis
    
    public RecipeReview() {}
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Recipe getRecipe() { return recipe; }
    public void setRecipe(Recipe recipe) { this.recipe = recipe; }
    
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    
    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }
    
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    
    public List<ReviewPhoto> getPhotos() { return photos; }
    public void setPhotos(List<ReviewPhoto> photos) { this.photos = photos; }

    public List<ReviewReply> getReplies() { return replies; }
    public void setReplies(List<ReviewReply> replies) { this.replies = replies; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public ReviewStatus getStatus() { return status; }
    public void setStatus(ReviewStatus status) { this.status = status; }
    
    public Boolean getReported() { return reported; }
    public void setReported(Boolean reported) { this.reported = reported; }
    
    public String getReportReason() { return reportReason; }
    public void setReportReason(String reportReason) { this.reportReason = reportReason; }
    
    public String getReportedBy() { return reportedBy; }
    public void setReportedBy(String reportedBy) { this.reportedBy = reportedBy; }
    
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }
    
    public String getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(String reviewedBy) { this.reviewedBy = reviewedBy; }
    
    public void addPhoto(ReviewPhoto photo) {
        photos.add(photo);
        photo.setReview(this);
    }
    
    public void removePhoto(ReviewPhoto photo) {
        photos.remove(photo);
        photo.setReview(null);
    }

    public void addReply(ReviewReply reply) {
        replies.add(reply);
        reply.setReview(this);
    }

    public void removeReply(ReviewReply reply) {
        replies.remove(reply);
        reply.setReview(null);
    }
    
    public enum ReviewStatus {
        APPROVED,   // Approuvé
        PENDING,    // En attente de modération
        REJECTED,   // Rejeté
        REPORTED    // Signalé
    }
}

