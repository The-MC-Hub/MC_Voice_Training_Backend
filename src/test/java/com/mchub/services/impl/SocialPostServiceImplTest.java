package com.mchub.services.impl;

import com.mchub.dto.SaveSocialPostRequest;
import com.mchub.dto.SocialPostDTO;
import com.mchub.exception.AppException;
import com.mchub.models.SocialPost;
import com.mchub.repositories.SocialPostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for SocialPostServiceImpl. Also documents a known style finding
 * from the audit (Remaining_Modules_Audit_Report.md 3.3): all 4 not-found
 * sites in this class throw a raw RuntimeException instead of
 * AppException(RESOURCE_NOT_FOUND), so GlobalExceptionHandler maps them to
 * HTTP 500 instead of 404. Not fixed here — QA reports, does not fix
 * production code.
 */
@ExtendWith(MockitoExtension.class)
class SocialPostServiceImplTest {

    @Mock private SocialPostRepository socialPostRepository;

    private SocialPostServiceImpl service;

    private static final String POST_ID = "post-001";

    @BeforeEach
    void setUp() {
        service = new SocialPostServiceImpl(socialPostRepository);
    }

    private SaveSocialPostRequest request() {
        SaveSocialPostRequest req = new SaveSocialPostRequest();
        req.setImage("img.jpg");
        req.setDescription("desc");
        req.setFbLink("https://fb.com/post");
        req.setSortOrder(1);
        req.setActive(true);
        return req;
    }

    @Nested
    @DisplayName("getActivePosts / getAllPostsAdmin")
    class Listing {

        @Test
        @DisplayName("getActivePosts returns only active posts ordered by sortOrder desc")
        void returnsOnlyActivePosts() {
            SocialPost post = SocialPost.builder().id(POST_ID).active(true).build();
            when(socialPostRepository.findByActiveTrueOrderBySortOrderDesc()).thenReturn(List.of(post));

            List<SocialPostDTO> result = service.getActivePosts();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).isActive()).isTrue();
        }

        @Test
        @DisplayName("getAllPostsAdmin returns all posts regardless of active state")
        void returnsAllPostsForAdmin() {
            when(socialPostRepository.findAllByOrderBySortOrderDesc())
                    .thenReturn(List.of(SocialPost.builder().id("p1").active(true).build(),
                            SocialPost.builder().id("p2").active(false).build()));

            List<SocialPostDTO> result = service.getAllPostsAdmin();

            assertThat(result).hasSize(2);
        }
    }

    @Nested
    @DisplayName("createPost / updatePost")
    class CreateAndUpdate {

        @Test
        @DisplayName("createPost maps request fields onto a new SocialPost")
        void createsPostFromRequest() {
            when(socialPostRepository.save(any(SocialPost.class))).thenAnswer(inv -> {
                SocialPost p = inv.getArgument(0);
                p.setId("new-id");
                return p;
            });

            SocialPostDTO result = service.createPost(request());

            assertThat(result.getImage()).isEqualTo("img.jpg");
            assertThat(result.getFbLink()).isEqualTo("https://fb.com/post");
        }

        @Test
        @DisplayName("updatePost overwrites all mutable fields")
        void updatesExistingPost() {
            SocialPost existing = SocialPost.builder().id(POST_ID).image("old.jpg").active(false).build();
            when(socialPostRepository.findById(POST_ID)).thenReturn(Optional.of(existing));
            when(socialPostRepository.save(any(SocialPost.class))).thenAnswer(inv -> inv.getArgument(0));

            SocialPostDTO result = service.updatePost(POST_ID, request());

            assertThat(result.getImage()).isEqualTo("img.jpg");
            assertThat(result.isActive()).isTrue();
        }

        @Test
        @DisplayName("updatePost throws for unknown id")
        void updateThrowsForUnknownId() {
            when(socialPostRepository.findById("missing")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updatePost("missing", request()))
                    .isInstanceOf(RuntimeException.class)
                    .isNotInstanceOf(AppException.class);
        }
    }

    @Nested
    @DisplayName("deletePost")
    class DeletePost {

        @Test
        @DisplayName("deletes when the post exists")
        void deletesWhenExists() {
            when(socialPostRepository.existsById(POST_ID)).thenReturn(true);

            service.deletePost(POST_ID);

            verify(socialPostRepository).deleteById(POST_ID);
        }

        @Test
        @DisplayName("throws instead of silently no-op-ing for an unknown id")
        void throwsForUnknownId() {
            when(socialPostRepository.existsById("missing")).thenReturn(false);

            assertThatThrownBy(() -> service.deletePost("missing")).isInstanceOf(RuntimeException.class);

            verify(socialPostRepository, never()).deleteById(any());
        }
    }

    @Nested
    @DisplayName("toggleActive")
    class ToggleActive {

        @Test
        @DisplayName("flips active from true to false")
        void flipsActiveTrueToFalse() {
            SocialPost post = SocialPost.builder().id(POST_ID).active(true).build();
            when(socialPostRepository.findById(POST_ID)).thenReturn(Optional.of(post));
            when(socialPostRepository.save(any(SocialPost.class))).thenAnswer(inv -> inv.getArgument(0));

            SocialPostDTO result = service.toggleActive(POST_ID);

            assertThat(result.isActive()).isFalse();
        }

        @Test
        @DisplayName("flips active from false to true")
        void flipsActiveFalseToTrue() {
            SocialPost post = SocialPost.builder().id(POST_ID).active(false).build();
            when(socialPostRepository.findById(POST_ID)).thenReturn(Optional.of(post));
            when(socialPostRepository.save(any(SocialPost.class))).thenAnswer(inv -> inv.getArgument(0));

            SocialPostDTO result = service.toggleActive(POST_ID);

            assertThat(result.isActive()).isTrue();
        }
    }

    @Nested
    @DisplayName("recordClick")
    class RecordClick {

        @Test
        @DisplayName("increments clickCount by 1")
        void incrementsClickCount() {
            SocialPost post = SocialPost.builder().id(POST_ID).clickCount(4).build();
            when(socialPostRepository.findById(POST_ID)).thenReturn(Optional.of(post));
            when(socialPostRepository.save(any(SocialPost.class))).thenAnswer(inv -> inv.getArgument(0));

            SocialPostDTO result = service.recordClick(POST_ID);

            assertThat(result.getClickCount()).isEqualTo(5);
        }
    }
}
