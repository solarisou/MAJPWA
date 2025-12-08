package com.ecocook.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Représente une réponse à un avis, avec la possibilité de créer des fils de discussion.
 * Le double stockage (content / legacyComment) garantit la compatibilité avec l'ancien schéma.
 */
@Entity
@Table(name = "review_replies")
public class ReviewReply {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false)
    private RecipeReview review;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_reply_id")
    private ReviewReply parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("createdAt ASC")
    private List<ReviewReply> children = new ArrayList<>();

    @Column(name = "author", nullable = false)
    private String author;

    @Column(name = "user_name", nullable = false)
    private String userName;

    @Column(name = "content", nullable = false, length = 2000)
    private String content;

    @Column(name = "comment", nullable = false, length = 2000)
    private String legacyComment;

    @Column(name = "reported", nullable = false)
    private Boolean reported = false;

    @Column(name = "report_reason", length = 1000)
    private String reportReason;

    @Column(name = "reported_by")
    private String reportedBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        syncFields();
    }

    @PrePersist
    public void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        syncFields();
    }

    /**
     * Maintient les colonnes historiques synchronisées avec les nouvelles propriétés.
     */
    private void syncFields() {
        if (this.content == null && this.legacyComment != null) {
            this.content = this.legacyComment;
        }
        if (this.legacyComment == null && this.content != null) {
            this.legacyComment = this.content;
        }
        if (this.userName == null && this.author != null) {
            this.userName = this.author;
        }
        if (this.author == null && this.userName != null) {
            this.author = this.userName;
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public RecipeReview getReview() { return review; }
    public void setReview(RecipeReview review) { this.review = review; }

    public ReviewReply getParent() { return parent; }
    public void setParent(ReviewReply parent) { this.parent = parent; }

    public List<ReviewReply> getChildren() { return children; }
    public void setChildren(List<ReviewReply> children) { this.children = children; }

    public void addChild(ReviewReply child) {
        if (!children.contains(child)) {
            children.add(child);
            child.setParent(this);
        }
    }

    public void removeChild(ReviewReply child) {
        if (children.remove(child)) {
            child.setParent(null);
        }
    }

    public String getUserName() { return userName; }
    public void setUserName(String userName) {
        this.userName = userName;
        this.author = userName;
    }

    public String getAuthor() { return author; }
    public void setAuthor(String author) {
        this.author = author;
        this.userName = author;
    }

    public String getContent() { return content; }
    public void setContent(String content) {
        this.content = content;
        this.legacyComment = content;
    }

    public String getComment() { return content; }
    public void setComment(String comment) {
        this.content = comment;
        this.legacyComment = comment;
    }

    public String getLegacyComment() { return legacyComment; }
    public void setLegacyComment(String legacyComment) {
        this.legacyComment = legacyComment;
        this.content = legacyComment;
    }

    public Boolean getReported() { return reported; }
    public void setReported(Boolean reported) { this.reported = reported; }

    public String getReportReason() { return reportReason; }
    public void setReportReason(String reportReason) { this.reportReason = reportReason; }

    public String getReportedBy() { return reportedBy; }
    public void setReportedBy(String reportedBy) { this.reportedBy = reportedBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}


