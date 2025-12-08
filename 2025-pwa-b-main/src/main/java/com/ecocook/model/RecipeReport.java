package com.ecocook.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Représente un signalement de recette effectué par un utilisateur.
 * L'équipe d'administration s'en sert pour suivre la modération.
 */
@Entity
@Table(name = "recipe_reports")
public class RecipeReport {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "recipe_id", nullable = false)
    private Recipe recipe;
    
    @Column(name = "reported_by", nullable = false)
    private String reportedBy; // identifiant de la personne qui a signalé
    
    @Column(name = "report_reason", length = 1000)
    private String reportReason; // explication fournie par l'utilisateur
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private ReportStatus status = ReportStatus.PENDING;
    
    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;
    
    @Column(name = "reviewed_by")
    private String reviewedBy; // identifiant de l'admin qui a traité le signalement
    
    @Column(name = "review_note", length = 1000)
    private String reviewNote; // justification côté admin
    
    public RecipeReport() {}
    
    public RecipeReport(Recipe recipe, String reportedBy, String reportReason) {
        this.recipe = recipe;
        this.reportedBy = reportedBy;
        this.reportReason = reportReason;
    }
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Recipe getRecipe() { return recipe; }
    public void setRecipe(Recipe recipe) { this.recipe = recipe; }
    
    public String getReportedBy() { return reportedBy; }
    public void setReportedBy(String reportedBy) { this.reportedBy = reportedBy; }
    
    public String getReportReason() { return reportReason; }
    public void setReportReason(String reportReason) { this.reportReason = reportReason; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public ReportStatus getStatus() { return status; }
    public void setStatus(ReportStatus status) { this.status = status; }
    
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }
    
    public String getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(String reviewedBy) { this.reviewedBy = reviewedBy; }
    
    public String getReviewNote() { return reviewNote; }
    public void setReviewNote(String reviewNote) { this.reviewNote = reviewNote; }
    
    public enum ReportStatus {
        PENDING,    // En attente
        RESOLVED,   // Résolu (recette supprimée ou signalement rejeté)
        DISMISSED   // Rejeté (signalement sans fondement)
    }
}

