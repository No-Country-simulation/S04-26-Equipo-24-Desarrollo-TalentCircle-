package com.talentcircle.domain.port.out;

import com.talentcircle.domain.model.CommunitySource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CommunitySourceRepository extends JpaRepository<CommunitySource, String> {
    List<CommunitySource> findAllByActiveTrue();
    void deleteByActiveFalse();
}
