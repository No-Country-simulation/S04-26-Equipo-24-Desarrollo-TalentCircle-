package com.talentcircle.domain.port.out;

import com.talentcircle.domain.model.User;
import java.util.List;
import java.util.Optional;

public interface UserRepository {
    User save(User user);
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    Optional<User> findById(String id);
    List<User> findAll();
    void delete(User user);
}
