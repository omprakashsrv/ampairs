package in.digio.account.repository;


import in.digio.account.model.WorkSpace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WorkSpaceRepository extends JpaRepository<WorkSpace, Integer> {

    Optional<WorkSpace> findById(String id);

}