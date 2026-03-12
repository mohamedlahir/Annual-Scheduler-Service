package com.laby.annual.scheduler.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.laby.annual.scheduler.DTO.AuthCommanDataModel;
import com.laby.annual.scheduler.entity.Tutor;
import com.laby.annual.scheduler.repository.TutorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TutorCRUDService {

   private final TutorRepository tutorRepository;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    private PasswordEncoder encoder(){
        return new BCryptPasswordEncoder();
    }

    public ResponseEntity<String> createTutor(Tutor tutor) {
        Tutor saved = createTutorInternal(tutor);
        return ResponseEntity.ok("Tutor created successfully with ID: " + saved.getTutorId());
    }

    public Tutor createTutorInternal(Tutor tutor) {
        String profileIDStr = UUID.randomUUID().toString();
        String rawPassword = tutor.getPassword();
        String encodedPassword = encoder().encode(rawPassword);
        tutor.setTutorId(profileIDStr); // set tutorId from auth-service
        tutor.setPassword(encodedPassword); // hash before save
        Tutor saved = tutorRepository.save(tutor);

        AuthCommanDataModel authData = new AuthCommanDataModel();
        authData.setEmail(tutor.getEmail());
        authData.setPassword(rawPassword); // send raw password for authentication service to hash and store
        authData.setRole(tutor.getRole());
        authData.setProfileID(tutor.getTutorId());
        authData.setSchoolId(tutor.getSchoolId());

        try {
            ObjectMapper om = new ObjectMapper();
            String payload = om.writeValueAsString(authData);
            kafkaTemplate.send("tutor-profile-creation", payload);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize user data for Kafka: " + e.getMessage(), e);
        }

        return saved;
    }

    public ResponseEntity<String> createTutorSingle(Tutor tutor) {
        String profileIDStr = UUID.randomUUID().toString();
        String rawPassword = tutor.getPassword();
        String encodedPassword = encoder().encode(rawPassword);
        tutor.setTutorId(profileIDStr); // set tutorId from auth-service
        tutor.setPassword(encodedPassword); // set default password (should be changed by tutor later)
        tutorRepository.save(tutor); // save tutor to DB

        AuthCommanDataModel authData = new AuthCommanDataModel();
        authData.setEmail(tutor.getEmail());
        authData.setPassword(rawPassword); // send raw password
        authData.setRole(tutor.getRole());
        authData.setProfileID(tutor.getTutorId());
        authData.setSchoolId(tutor.getSchoolId());


        // Serialize Users to JSON and send as String to avoid ClassCastException with StringSerializer

        try {
            ObjectMapper om = new ObjectMapper();
            String payload = om.writeValueAsString(authData);
            kafkaTemplate.send("tutor-profile-creation", payload);

        } catch (JsonProcessingException e) {
            return ResponseEntity.status(500).body("Failed to serialize user data for Kafka: " + e.getMessage());
        }
        return ResponseEntity.ok("Tutor created successfully with ID: " + profileIDStr);

    }

}
