package com.talentcircle.adapter.out.linkedin;

import com.talentcircle.domain.model.Publication;
import com.talentcircle.domain.port.out.LinkedInClientPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LinkedInClientAdapter implements LinkedInClientPort {

    @Value("${app.linkedin.access-token:}")
    private final String accessToken;

    @Value("${app.linkedin.person-id:}")
    private final String personId;

    public LinkedInClientAdapter(@Value("${app.linkedin.access-token:}") String accessToken,
                                 @Value("${app.linkedin.person-id:}") String personId) {
        this.accessToken = accessToken;
        this.personId = personId;
    }

    @Override
    public String publishPost(String content) {
        // POST /ugcPosts
        // Body: { "author": "urn:li:person:{personId}", "lifecycleState": "PUBLISHED", ... }
        throw new RuntimeException("LinkedIn adapter not fully implemented yet");
    }

    @Override
    public Publication.PublicationStatus checkStatus(String externalPostId) {
        // GET /ugcPosts/{externalPostId}
        throw new RuntimeException("Not implemented yet");
    }

    @Override
    public boolean validateConnection(String accessToken) {
        // TODO: Implement with proper HTTP client
        return false;
    }
}
