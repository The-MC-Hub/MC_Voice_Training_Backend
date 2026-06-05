package com.mchub.services;

import com.mchub.dto.SaveSocialPostRequest;
import com.mchub.dto.SocialPostDTO;

import java.util.List;

public interface SocialPostService {
    List<SocialPostDTO> getActivePosts();
    List<SocialPostDTO> getAllPostsAdmin();
    SocialPostDTO createPost(SaveSocialPostRequest request);
    SocialPostDTO updatePost(String id, SaveSocialPostRequest request);
    void deletePost(String id);
    SocialPostDTO toggleActive(String id);
    SocialPostDTO recordClick(String id);
}
