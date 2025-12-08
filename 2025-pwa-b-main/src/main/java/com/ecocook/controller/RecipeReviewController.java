package com.ecocook.controller;

import com.ecocook.model.Recipe;
import com.ecocook.model.RecipeReview;
import com.ecocook.model.RecipeReview.ReviewStatus;
import com.ecocook.model.ReviewPhoto;
import com.ecocook.model.ReviewReply;
import com.ecocook.repository.RecipeReviewRepository;
import com.ecocook.repository.ReviewReplyRepository;
import com.ecocook.service.RecipeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.UUID;

/**
 * Gestion des avis et réponses sur les recettes
 */
@Controller
@RequestMapping("/recipes")
public class RecipeReviewController {

    @Autowired
    private RecipeReviewRepository reviewRepository;
    
    @Autowired
    private RecipeService recipeService;

    @Autowired
    private ReviewReplyRepository reviewReplyRepository;
    
    private static final String UPLOAD_DIR = "src/main/resources/static/uploads/reviews/";
    
    @PostMapping("/{recipeId}/review")
    public String addReview(@PathVariable Long recipeId,
                          @RequestParam Integer rating,
                          @RequestParam(required = false) String comment,
                          @RequestParam(required = false) MultipartFile[] photos,
                          Authentication authentication,
                          RedirectAttributes redirectAttributes) {
        if (!isUserAuthenticated(authentication)) {
            redirectAttributes.addFlashAttribute("error", "Vous devez être connecté pour laisser un avis.");
            return "redirect:/login";
        }

        try {
            String userName = authentication.getName();
            
            Optional<RecipeReview> existingReview = reviewRepository.findByRecipeIdAndUserName(recipeId, userName);
            if (existingReview.isPresent()) {
                redirectAttributes.addFlashAttribute("error", "Vous avez déjà laissé un avis pour cette recette.");
                return "redirect:/recipes/" + recipeId;
            }
            
            Optional<Recipe> recipeOpt = recipeService.getRecipeById(recipeId);
            if (recipeOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Recette non trouvée.");
                return "redirect:/recipes";
            }
            
            RecipeReview review = new RecipeReview();
            review.setRecipe(recipeOpt.get());
            review.setUserName(userName);
            review.setRating(rating);
            review.setComment(comment);
            review.setStatus(ReviewStatus.APPROVED);
            
            if (photos != null && photos.length > 0) {
                for (MultipartFile photo : photos) {
                    if (!photo.isEmpty()) {
                        String fileName = savePhoto(photo);
                        if (fileName != null) {
                            ReviewPhoto reviewPhoto = new ReviewPhoto("/uploads/reviews/" + fileName, photo.getOriginalFilename());
                            review.addPhoto(reviewPhoto);
                        }
                    }
                }
            }
            
            reviewRepository.save(review);
            redirectAttributes.addFlashAttribute("success", "Votre avis a été ajouté avec succès !");
            
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Erreur lors de l'ajout de l'avis.");
        }
        
        return "redirect:/recipes/" + recipeId;
    }
    
    @PostMapping("/reviews/{reviewId}/report")
    public String reportReview(@PathVariable long reviewId,
                              @RequestParam(required = false) String reason,
                              Authentication authentication,
                              RedirectAttributes redirectAttributes) {
        if (!isUserAuthenticated(authentication)) {
            redirectAttributes.addFlashAttribute("error", "Vous devez être connecté pour signaler un avis.");
            return "redirect:/login";
        }

        Optional<RecipeReview> reviewOpt = reviewRepository.findById(reviewId);
        if (reviewOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Avis non trouvé.");
            return "redirect:/recipes";
        }
        
        RecipeReview review = reviewOpt.get();
        String currentUser = authentication.getName();
        review.setReported(true);
        review.setReportReason(reason != null ? reason : "Avis signalé par un utilisateur");
        review.setReportedBy(currentUser);
        reviewRepository.save(review);
        
        redirectAttributes.addFlashAttribute("success", "Avis signalé. Un administrateur va l'examiner.");
        return "redirect:/recipes/" + review.getRecipe().getId();
    }

    @PostMapping("/reviews/{reviewId}/edit")
    public String editReview(@PathVariable long reviewId,
                             @RequestParam Integer rating,
                             @RequestParam(required = false) String comment,
                             Authentication authentication,
                             RedirectAttributes redirectAttributes) {
        if (!isUserAuthenticated(authentication)) {
            redirectAttributes.addFlashAttribute("error", "Vous devez être connecté pour modifier un avis.");
            return "redirect:/login";
        }

        Optional<RecipeReview> reviewOpt = reviewRepository.findById(reviewId);
        if (reviewOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Avis non trouvé.");
            return "redirect:/recipes";
        }

        RecipeReview review = reviewOpt.get();
        String currentUser = authentication.getName();

        if (!currentUser.equals(review.getUserName())) {
            redirectAttributes.addFlashAttribute("error", "Vous ne pouvez modifier que votre propre avis.");
            return "redirect:/recipes/" + review.getRecipe().getId();
        }

        review.setRating(rating);
        review.setComment(comment);
        reviewRepository.save(review);

        redirectAttributes.addFlashAttribute("success", "Votre avis a été mis à jour.");
        return "redirect:/recipes/" + review.getRecipe().getId();
    }

    @PostMapping("/reviews/{reviewId}/delete")
    public String deleteReview(@PathVariable long reviewId,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) {
        if (!isUserAuthenticated(authentication)) {
            redirectAttributes.addFlashAttribute("error", "Vous devez être connecté pour supprimer un avis.");
            return "redirect:/login";
        }

        Optional<RecipeReview> reviewOpt = reviewRepository.findById(reviewId);
        if (reviewOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Avis non trouvé.");
            return "redirect:/recipes";
        }

        RecipeReview review = reviewOpt.get();
        String currentUser = authentication.getName();
        boolean isAdmin = isAdmin(authentication);

        if (!currentUser.equals(review.getUserName()) && !isAdmin) {
            redirectAttributes.addFlashAttribute("error", "Vous ne pouvez pas supprimer cet avis.");
            return "redirect:/recipes/" + review.getRecipe().getId();
        }

        reviewRepository.delete(review);
        redirectAttributes.addFlashAttribute("success", "Avis supprimé.");
        return "redirect:/recipes/" + review.getRecipe().getId();
    }

    @PostMapping("/reviews/{reviewId}/reply")
    public String replyToReview(@PathVariable long reviewId,
                                @RequestParam String comment,
                                @RequestParam(name = "parentReplyId", required = false) Long parentReplyId,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        if (!isUserAuthenticated(authentication)) {
            redirectAttributes.addFlashAttribute("error", "Vous devez être connecté pour répondre.");
            return "redirect:/login";
        }

        Optional<RecipeReview> reviewOpt = reviewRepository.findById(reviewId);
        if (reviewOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Avis non trouvé.");
            return "redirect:/recipes";
        }

        RecipeReview review = reviewOpt.get();
        if (comment == null || comment.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "La réponse ne peut pas être vide.");
            return "redirect:/recipes/" + review.getRecipe().getId();
        }
        String currentUser = authentication.getName();
        boolean isAdmin = isAdmin(authentication);
        boolean isRecipeOwner = review.getRecipe().getCreatedBy() != null
                && review.getRecipe().getCreatedBy().equals(currentUser);

        if (!isAdmin && !isRecipeOwner) {
            redirectAttributes.addFlashAttribute("error", "Seuls l'auteur de la recette ou un administrateur peuvent répondre.");
            return "redirect:/recipes/" + review.getRecipe().getId();
        }

        if (parentReplyId != null) {
            Optional<ReviewReply> parentOpt = reviewReplyRepository.findById(parentReplyId);
            if (parentOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "La réponse parent n'existe pas.");
                return "redirect:/recipes/" + review.getRecipe().getId();
            }

            ReviewReply parent = parentOpt.get();
            if (!parent.getReview().getId().equals(reviewId)) {
                redirectAttributes.addFlashAttribute("error", "Vous ne pouvez répondre qu'à une réponse de cet avis.");
                return "redirect:/recipes/" + review.getRecipe().getId();
            }

            if (reviewReplyRepository.existsByParentIdAndUserName(parentReplyId, currentUser)) {
                redirectAttributes.addFlashAttribute("error", "Vous avez déjà répondu à ce commentaire.");
                return "redirect:/recipes/" + review.getRecipe().getId();
            }

            ReviewReply reply = new ReviewReply();
            reply.setUserName(currentUser);
            reply.setComment(comment.trim());

            reply.setReview(review);
            parent.addChild(reply);
            review.addReply(reply);
            reviewReplyRepository.save(reply);
        } else {
            if (reviewReplyRepository.existsByReviewIdAndParentIsNullAndUserName(reviewId, currentUser)) {
                redirectAttributes.addFlashAttribute("error", "Vous avez déjà répondu à cet avis.");
                return "redirect:/recipes/" + review.getRecipe().getId();
            }

            ReviewReply reply = new ReviewReply();
            reply.setUserName(currentUser);
            reply.setComment(comment.trim());
            review.addReply(reply);
            reviewReplyRepository.save(reply);
        }

        redirectAttributes.addFlashAttribute("success", "Réponse ajoutée.");
        return "redirect:/recipes/" + review.getRecipe().getId();
    }

    @PostMapping("/reviews/replies/{replyId}/delete")
    public String deleteReply(@PathVariable long replyId,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) {
        if (!isUserAuthenticated(authentication)) {
            redirectAttributes.addFlashAttribute("error", "Vous devez être connecté pour supprimer une réponse.");
            return "redirect:/login";
        }

        Optional<ReviewReply> replyOpt = reviewReplyRepository.findById(replyId);
        if (replyOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Réponse non trouvée.");
            return "redirect:/recipes";
        }

        ReviewReply reply = replyOpt.get();
        String currentUser = authentication.getName();
        boolean isAdmin = isAdmin(authentication);

        RecipeReview review = reviewRepository.findById(reply.getReview().getId()).orElse(null);
        if (review == null) {
            redirectAttributes.addFlashAttribute("error", "Avis associé non trouvé.");
            return "redirect:/recipes";
        }
        Long recipeId = review.getRecipe().getId();

        if (!currentUser.equals(reply.getUserName()) && !isAdmin) {
            redirectAttributes.addFlashAttribute("error", "Vous ne pouvez pas supprimer cette réponse.");
            return "redirect:/recipes/" + recipeId;
        }

        reviewReplyRepository.delete(reply);
        redirectAttributes.addFlashAttribute("success", "Réponse supprimée.");
        return "redirect:/recipes/" + recipeId;
    }

    @PostMapping("/reviews/replies/{replyId}/edit")
    public String editReply(@PathVariable long replyId,
                             @RequestParam String comment,
                             Authentication authentication,
                             RedirectAttributes redirectAttributes) {
        if (!isUserAuthenticated(authentication)) {
            redirectAttributes.addFlashAttribute("error", "Vous devez être connecté pour modifier une réponse.");
            return "redirect:/login";
        }

        Optional<ReviewReply> replyOpt = reviewReplyRepository.findById(replyId);
        if (replyOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Réponse non trouvée.");
            return "redirect:/recipes";
        }

        ReviewReply reply = replyOpt.get();
        String currentUser = authentication.getName();
        boolean isAdmin = isAdmin(authentication);

        if (!currentUser.equals(reply.getUserName()) && !isAdmin) {
            redirectAttributes.addFlashAttribute("error", "Vous ne pouvez pas modifier cette réponse.");
            return "redirect:/recipes/" + reply.getReview().getRecipe().getId();
        }

        if (comment == null || comment.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "La réponse ne peut pas être vide.");
            return "redirect:/recipes/" + reply.getReview().getRecipe().getId();
        }

        reply.setComment(comment.trim());
        reviewReplyRepository.save(reply);

        redirectAttributes.addFlashAttribute("success", "Réponse mise à jour.");
        return "redirect:/recipes/" + reply.getReview().getRecipe().getId();
    }

    @PostMapping("/reviews/replies/{replyId}/report")
    public String reportReply(@PathVariable long replyId,
                               @RequestParam(required = false) String reason,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) {
        if (!isUserAuthenticated(authentication)) {
            redirectAttributes.addFlashAttribute("error", "Vous devez être connecté pour signaler une réponse.");
            return "redirect:/login";
        }

        Optional<ReviewReply> replyOpt = reviewReplyRepository.findById(replyId);
        if (replyOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Réponse non trouvée.");
            return "redirect:/recipes";
        }

        ReviewReply reply = replyOpt.get();
        String currentUser = authentication.getName();

        if (currentUser.equals(reply.getUserName())) {
            redirectAttributes.addFlashAttribute("error", "Vous ne pouvez pas signaler votre propre réponse.");
            return "redirect:/recipes/" + reply.getReview().getRecipe().getId();
        }

        reply.setReported(true);
        reply.setReportReason(reason != null ? reason : "Réponse signalée par un utilisateur");
        reply.setReportedBy(currentUser);
        reviewReplyRepository.save(reply);

        redirectAttributes.addFlashAttribute("success", "Réponse signalée. Un administrateur va l'examiner.");
        return "redirect:/recipes/" + reply.getReview().getRecipe().getId();
    }
    
    private String savePhoto(MultipartFile photo) {
        try {
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            
            String originalFileName = photo.getOriginalFilename();
            String extension = originalFileName != null && originalFileName.contains(".") 
                ? originalFileName.substring(originalFileName.lastIndexOf(".")) 
                : ".jpg";
            String fileName = UUID.randomUUID().toString() + extension;
            
            Path filePath = uploadPath.resolve(fileName);
            Files.copy(photo.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            
            return fileName;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .anyMatch(auth -> auth.equals("ROLE_ADMIN"));
    }

    private boolean isUserAuthenticated(Authentication authentication) {
        return authentication != null
            && !(authentication instanceof AnonymousAuthenticationToken)
            && authentication.isAuthenticated();
    }
}

