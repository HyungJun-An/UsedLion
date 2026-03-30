package com.example.usedlion.repository;

import com.example.usedlion.dto.Member;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class MemberRepository {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    public Member findByEmail(String email) {
        String sql = "SELECT * FROM member WHERE email = ?";
        return jdbcTemplate.query(sql, new Object[]{email}, rs -> {
            if (rs.next()) {
                Member member = new Member();
                member.setId(rs.getLong("id"));
                member.setEmail(rs.getString("email"));
                member.setName(rs.getString("name"));
                member.setProvider(rs.getString("provider"));
                member.setProviderId(rs.getString("provider_id"));
                member.setRole(rs.getString("role"));
                member.setCreatedAt(rs.getString("created_at"));
                return member;
            }
            return null;
        });
    }

    public void save(Member member) {
        String sql = "INSERT INTO member (email, name, provider, provider_id, role) VALUES (?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql, member.getEmail(), member.getName(), member.getProvider(), member.getProviderId(), member.getRole());
    }
}
