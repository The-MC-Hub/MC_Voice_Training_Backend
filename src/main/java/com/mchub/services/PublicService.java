package com.mchub.services;

import com.mchub.dto.EnumOptionDTO;
import com.mchub.dto.MCSearchResultDTO;
import com.mchub.dto.MCProfileResponseDTO;
import com.mchub.dto.MCTrainingStatsDTO;
import com.mchub.dto.SearchMCRequest;

import java.util.List;
import java.util.Map;

public interface PublicService {

        Map<String, Object> getLandingData();

        List<MCProfileResponseDTO> discoverMCs(String category);

    MCProfileResponseDTO getMCProfile(String id);

    

    List<EnumOptionDTO> getUserRoles();


    List<EnumOptionDTO> getReportReasons();
    List<MCTrainingStatsDTO> getFeaturedMCTrainingStats();

    List<MCSearchResultDTO> searchMCs(SearchMCRequest req);
}
