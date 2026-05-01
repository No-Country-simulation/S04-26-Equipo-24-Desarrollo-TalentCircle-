package com.talentcircle.domain.port.out;

import com.talentcircle.domain.model.Publication;

public interface LinkedInClientPort {
    String publishPost(String content);
    Publication.PublicationStatus checkStatus(String externalPostId);
    boolean validateConnection(String accessToken);
}
