package com.mchub.repositories;

import com.mchub.models.Conversation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends MongoRepository<Conversation, String> {

    List<Conversation> findByParticipantsContaining(String userId);

    Optional<Conversation> findByBookingId(String bookingId);

    @Query("{ 'participants': { $all: [?0, ?1] }, 'bookingId': ?2 }")
    Optional<Conversation> findExisting(String user1, String user2, String bookingId);
}
