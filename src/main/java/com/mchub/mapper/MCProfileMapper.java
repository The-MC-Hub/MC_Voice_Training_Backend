package com.mchub.mapper;

import com.mchub.dto.MCProfileResponseDTO;
import com.mchub.models.MCProfile;
import com.mchub.models.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface MCProfileMapper {

    @Mapping(target = "id", source = "profile.id")
    @Mapping(target = "userId", source = "profile.user")
    @Mapping(target = "experience", source = "profile.experience")
    @Mapping(target = "styles", source = "profile.styles")
    @Mapping(target = "biography", source = "profile.biography")

    @Mapping(target = "ratesMin", expression = "java(profile.getRates() != null ? profile.getRates().getMin() : 0)")
    @Mapping(target = "ratesMax", expression = "java(profile.getRates() != null ? profile.getRates().getMax() : 0)")
    @Mapping(target = "eventTypes", source = "profile.eventTypes")
    @Mapping(target = "status", source = "profile.status")
    @Mapping(target = "rating", source = "profile.rating")
    @Mapping(target = "reviewsCount", source = "profile.reviewsCount")
    @Mapping(target = "regions", source = "profile.regions")
    @Mapping(target = "personality", source = "profile.personality")
    @Mapping(target = "hostingStyle", source = "profile.hostingStyle")
    @Mapping(target = "notableEvents", source = "profile.notableEvents")
    @Mapping(target = "languages", source = "profile.languages")

    @Mapping(target = "searchCount", source = "profile.searchCount")
    @Mapping(target = "lastActive", expression = "java(profile.getLastActive() != null ? profile.getLastActive().toString() : null)")
    @Mapping(target = "name", source = "user.name", defaultValue = "Unknown MC")
    @Mapping(target = "avatar", source = "user.avatar", defaultValue = "default-avatar.png")
    @Mapping(target = "verified", source = "user.verified")
    @Mapping(target = "socialLinks", expression = "java(profile.getSocialLinks() != null ? new com.mchub.dto.MCProfileResponseDTO.SocialLinksDTO(profile.getSocialLinks().getYoutube(), profile.getSocialLinks().getTiktok(), profile.getSocialLinks().getFacebook(), profile.getSocialLinks().getZalo()) : null)")
    @Mapping(target = "portfolio", source = "profile.portfolio")
    @Mapping(target = "responseTime", source = "profile.responseTime")
    @Mapping(target = "totalEvents", source = "profile.totalEvents")
    @Mapping(target = "achievements", source = "profile.achievements")
    @Mapping(target = "preferredContact", source = "profile.preferredContact")
    @Mapping(target = "visibleFields", source = "profile.visibleFields")
    @Mapping(target = "events", expression = "java(toEventEntryDTOs(profile.getEvents()))")
    MCProfileResponseDTO toResponseDTO(MCProfile profile, User user);

    default java.util.List<MCProfileResponseDTO.EventEntryDTO> toEventEntryDTOs(java.util.List<MCProfile.EventEntry> events) {
        if (events == null) return null;
        return events.stream().map(e -> new MCProfileResponseDTO.EventEntryDTO(
                e.getId(), e.getTitle(), e.getDescription(), e.getEventType(), e.getLocation(),
                e.getDate() != null ? e.getDate().toString() : null,
                e.getSkillsLearned(), e.getPhotos()
        )).toList();
    }
}
