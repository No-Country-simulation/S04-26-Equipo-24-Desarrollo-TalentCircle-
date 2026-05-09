package com.talentcircle.domain.port.out;

import com.talentcircle.domain.model.CommunityActivity;
import java.util.List;

public interface CommunityClientPort {
    List<CommunityActivity> fetchTopPosts(String apiUrl, String apiKey, int limit);
    List<CommunityActivity> fetchTopQuestions(String apiUrl, String apiKey, int limit);
    List<CommunityActivity> fetchTopResources(String apiUrl, String apiKey, int limit);
    boolean validateConnection(String apiUrl, String apiKey);
}
