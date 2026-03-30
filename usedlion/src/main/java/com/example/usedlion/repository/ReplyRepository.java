package com.example.usedlion.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.example.usedlion.dto.ReplyDetailDto;
import com.example.usedlion.entity.Reply;

public interface ReplyRepository extends JpaRepository<Reply, Integer> {

    @Query("SELECT new com.example.usedlion.dto.ReplyDetailDto(r.id,r.userId,r.ref,r.level,r.postId,r.content,r.created_at,u.username,u.email,p.title) FROM Reply r JOIN UserInformation u on u.userId = r.userId JOIN Post p on p.postId = r.postId  WHERE r.postId = ?1 order by r.id,r.created_at")
    public List<ReplyDetailDto> getReplyByPostId(Integer postId);

    @Query("SELECT new com.example.usedlion.dto.ReplyDetailDto(r.id,r.userId,r.ref,r.level,r.postId,r.content,r.created_at,u.username,u.email,p.title) FROM Reply r JOIN UserInformation u on u.userId = r.userId JOIN Post p on p.postId = r.postId  WHERE r.userId = ?1 order by r.id,r.created_at")
    public List<ReplyDetailDto> getReplyByUserId(Integer userId);

    public List<Reply> getReplyByPostIdAndRef(Integer postId, Integer ref);
}