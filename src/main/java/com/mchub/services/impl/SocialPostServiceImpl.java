package com.mchub.services.impl;

import com.mchub.dto.SaveSocialPostRequest;
import com.mchub.dto.SocialPostDTO;
import com.mchub.models.SocialPost;
import com.mchub.repositories.SocialPostRepository;
import com.mchub.services.SocialPostService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SocialPostServiceImpl implements SocialPostService {

    private final SocialPostRepository socialPostRepository;

    @Override
    public List<SocialPostDTO> getActivePosts() {
        return socialPostRepository.findByActiveTrueOrderBySortOrderDesc()
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public List<SocialPostDTO> getAllPostsAdmin() {
        return socialPostRepository.findAllByOrderBySortOrderDesc()
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public SocialPostDTO createPost(SaveSocialPostRequest request) {
        SocialPost post = SocialPost.builder()
                .image(request.getImage())
                .description(request.getDescription())
                .fbLink(request.getFbLink())
                .sortOrder(request.getSortOrder())
                .active(request.isActive())
                .build();
        return toDTO(socialPostRepository.save(post));
    }

    @Override
    public SocialPostDTO updatePost(String id, SaveSocialPostRequest request) {
        SocialPost post = socialPostRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Social post not found: " + id));
        post.setImage(request.getImage());
        post.setDescription(request.getDescription());
        post.setFbLink(request.getFbLink());
        post.setSortOrder(request.getSortOrder());
        post.setActive(request.isActive());
        return toDTO(socialPostRepository.save(post));
    }

    @Override
    public void deletePost(String id) {
        if (!socialPostRepository.existsById(id)) {
            throw new RuntimeException("Social post not found: " + id);
        }
        socialPostRepository.deleteById(id);
    }

    @Override
    public SocialPostDTO toggleActive(String id) {
        SocialPost post = socialPostRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Social post not found: " + id));
        post.setActive(!post.isActive());
        return toDTO(socialPostRepository.save(post));
    }

    @Override
    public SocialPostDTO recordClick(String id) {
        SocialPost post = socialPostRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Social post not found: " + id));
        post.setClickCount(post.getClickCount() + 1);
        return toDTO(socialPostRepository.save(post));
    }

    private SocialPostDTO toDTO(SocialPost post) {
        return SocialPostDTO.builder()
                .id(post.getId())
                .image(post.getImage())
                .description(post.getDescription())
                .fbLink(post.getFbLink())
                .sortOrder(post.getSortOrder())
                .active(post.isActive())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .clickCount(post.getClickCount())
                .build();
    }
}
