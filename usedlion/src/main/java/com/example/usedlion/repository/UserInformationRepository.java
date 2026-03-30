package com.example.usedlion.repository;

import com.example.usedlion.dto.UserInformation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;

@Repository
public class UserInformationRepository {
    @Autowired
    private JdbcTemplate jdbc;

    // 생성자 주입 G1
    public UserInformationRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public UserInformation findByEmail(String email) {
        String sql = "SELECT * FROM user_information WHERE email = ?"; // G1:테이블명 변경
        return jdbc.query(sql, ps -> ps.setString(1, email), rs -> {
            if (rs.next()) {
                UserInformation u = new UserInformation();
                u.setId(rs.getLong("id"));
                u.setPassword(rs.getString("password"));
                u.setEmail(rs.getString("email"));
                u.setUsername(rs.getString("username"));
                u.setNickname(rs.getString("nickname"));
                u.setProvider(rs.getString("provider"));
                u.setProviderId(rs.getString("provider_id"));
                u.setRole(rs.getString("role"));
                u.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                u.setRegion(rs.getString("region"));
                u.setProfileComplete(rs.getBoolean("is_profile_complete"));
                return u;
            }
            return null;
        });
    }

    // 새 회원 저장 --------//
    public void save(UserInformation u) {
        String sql = """
                INSERT INTO user_information
                (email, password, username, nickname, provider, provider_id, role, created_at, region)
                VALUES (?,?,?,?,?,?,?,?,?)
                """;
        ; // G1: 테이블명 변경
        jdbc.update(sql,
                u.getEmail(),
                u.getPassword(), // 비밀번호
                u.getUsername(), // 이름
                u.getNickname(), // 닉네임
                u.getProvider(), // 'local'
                u.getProviderId(), // null
                u.getRole(), // 'USER'
                Timestamp.valueOf(u.getCreatedAt()), // 가입일시
                u.getRegion());
    }

    public void updateProfileFields(UserInformation user) {
        String sql = """
                    UPDATE user_information
                    SET nickname = ?, region = ?, is_profile_complete = ?
                    WHERE email = ?
                """;
        jdbc.update(sql,
                user.getNickname(),
                user.getRegion(),
                user.isProfileComplete(),
                user.getEmail());
    }

    // ID -> Nickname 변환
    public String findNicknameById(Long id) {
        String sql = "SELECT nickname FROM user_information WHERE id = ?";
        return jdbc.query(sql, ps -> ps.setLong(1, id), rs -> {
            if (rs.next()) {
                return rs.getString("nickname");
            }
            return null;
        });
    }

}

