package com.mchub.services;

import com.mchub.exception.AppException;
import com.mchub.exception.ErrorCode;
import com.mchub.models.Competition;
import com.mchub.repositories.CompetitionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CompetitionService {

    private final CompetitionRepository competitionRepository;

    public List<Competition> getAllCompetitions() {
        return competitionRepository.findAll();
    }

    public Competition createCompetition(Competition competition) {
        if (competition.getStartDate() == null) {
            competition.setStartDate(Instant.now());
        }
        if (competition.getEndDate() == null) {
            competition.setEndDate(Instant.now().plus(7, ChronoUnit.DAYS));
        }
        return competitionRepository.save(competition);
    }

    public Competition updateCompetition(String id, Competition competitionDetails) {
        Competition competition = competitionRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Competition not found: " + id));

        competition.setTitle(competitionDetails.getTitle());
        competition.setDescription(competitionDetails.getDescription());
        competition.setType(competitionDetails.getType());
        competition.setChallengeScriptId(competitionDetails.getChallengeScriptId());
        competition.setStartDate(competitionDetails.getStartDate());
        competition.setEndDate(competitionDetails.getEndDate());
        competition.setActive(competitionDetails.isActive());

        return competitionRepository.save(competition);
    }

    public void deleteCompetition(String id) {
        if (!competitionRepository.existsById(id)) {
            throw new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Competition not found: " + id);
        }
        competitionRepository.deleteById(id);
    }
}
