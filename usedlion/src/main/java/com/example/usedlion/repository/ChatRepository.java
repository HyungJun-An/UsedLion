package com.example.usedlion.repository;

import com.example.usedlion.entity.Chat;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatRepository extends JpaRepository<Chat, Integer> {

    /**
     * 게시글의 모든 채팅 메시지 조회
     */
    List<Chat> findByPostIdOrderByIdDesc(Integer postId);

    /**
     *  게시글의 채팅 내역 페이징 조회
     */
    Page<Chat> findByPostIdOrderByIdDesc(Integer postId, Pageable pageable);

    /**
     * 1대1 채팅 내역 페이징 조회
     */
    Page<Chat> findByRoomIdOrderByIdDesc(String roomId, Pageable pageable);

    /**
     * 유저이메일로 userId찾기
     */
    @Query(value = "SELECT id FROM user_information WHERE email = :email", nativeQuery = true)
    Long findUserIdByEmail(@Param("email") String email);

}