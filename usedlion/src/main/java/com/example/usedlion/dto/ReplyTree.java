package com.example.usedlion.dto;

import java.util.ArrayList;
import java.util.List;

public class ReplyTree {
    private ReplyDetailDto reply;
    private List<ReplyTree> children;

    public ReplyTree(ReplyDetailDto reply) {
        this.reply = reply;
        this.children = new ArrayList<>();
    }

    public ReplyDetailDto getReply() {
        return reply;
    }

    public List<ReplyTree> getChildren() {
        return children;
    }

}
