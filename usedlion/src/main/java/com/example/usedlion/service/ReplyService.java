package com.example.usedlion.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.example.usedlion.dto.ReplyDetailDto;
import com.example.usedlion.dto.ReplyTree;
import com.example.usedlion.entity.Reply;
import com.example.usedlion.repository.ReplyRepository;

@Service
public class ReplyService {
    private final ReplyRepository replyRepository;

    public ReplyService(ReplyRepository replyRepository) {
        this.replyRepository = replyRepository;
    }

    public List<ReplyDetailDto> getReplyByPostId(Integer postId) {
        return replyRepository.getReplyByPostId(postId);
    }

    public List<ReplyDetailDto> getReplyByUserId(Integer userId) {
        return replyRepository.getReplyByUserId(userId);
    }

    public Reply getReplyByReplyId(Integer replyId) {
        return replyRepository.findById(replyId).orElse(null);
    }

    public void createReply(Reply reply) {
        replyRepository.save(reply);
    }

    public void updateReply(Integer replyId, Reply updatedReply) {
        Reply existing = replyRepository.findById(replyId)
                .orElseThrow(() -> new RuntimeException("댓글이 존재하지 않습니다."));
        existing.setContent(updatedReply.getContent());
        existing.setCreated_at(updatedReply.getCreated_at()); // 수정 시 시간도 갱신
        replyRepository.save(existing);
    }

    public void recursiveDelete(Reply reply) {
        Integer postId = reply.getPostId();
        Integer replyId = reply.getId();
        replyRepository.deleteById(reply.getId());
        List<Reply> replies = replyRepository.getReplyByPostIdAndRef(postId, replyId);
        for (Reply r : replies) {
            recursiveDelete(r);
        }

    }

    public List<ReplyTree> MakeTree(List<ReplyDetailDto> list) {
        Map<Integer, ReplyTree> replyMap = new HashMap<>();
        List<ReplyTree> root = new ArrayList<>();

        for (ReplyDetailDto reply : list) {
            replyMap.put(reply.getId(), new ReplyTree(reply));
        }
        for (ReplyDetailDto reply : list) {
            if (reply.getRef() == 0) {
                root.add(replyMap.get(reply.getId()));
            } else {
                ReplyTree parent = replyMap.get(reply.getRef());
                if (parent != null) {
                    parent.getChildren().add(replyMap.get(reply.getId()));
                }
            }
        }
        return root;
    }
}
