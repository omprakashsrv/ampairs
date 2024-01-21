package in.digio.account.repository;

import in.digio.account.model.authorization.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Created by Madhav Singh on 13/12/23
 */

@Repository
public interface TeamRepository extends JpaRepository<Team, Integer> {

    Optional<Team> findById(String id);

    Optional<Team> findByIdAndOwnerId(String id, String ownerId);

}
