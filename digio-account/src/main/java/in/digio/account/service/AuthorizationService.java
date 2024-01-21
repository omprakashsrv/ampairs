package in.digio.account.service;

import in.digio.account.dto.TeamResponse;
import in.digio.account.model.authorization.Team;
import in.digio.account.repository.TeamRepository;
import in.digio.auth.dto.authorization.request.TeamRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Created by Madhav Singh on 13/12/23
 */

@Component
@RequiredArgsConstructor
public class AuthorizationService {

    private final TeamRepository teamRepository;


    @Transactional
    public TeamResponse createTeam(TeamRequest teamRequest) {
        try {
//            TODO: Validate team request
            Optional<Team> team = teamRepository.findById(teamRequest.getTeamId());
            if (team.isPresent()) {
                // Update Team name and add users
            }
            Team teamResponse = teamRepository.save(Team.builder().teamName(teamRequest.getTeamName()).build());
            return new TeamResponse(teamResponse);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
