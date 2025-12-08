package com.ecocook.repository;

import com.ecocook.model.ReviewReply;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReviewReplyRepository extends JpaRepository<ReviewReply, Long> {
    boolean existsByReviewIdAndParentIsNullAndUserName(Long reviewId, String userName);
    boolean existsByParentIdAndUserName(Long parentId, String userName);
}


