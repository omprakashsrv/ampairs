package in.digio.account.controller;

import in.digio.account.dto.TeamResponse;
import in.digio.account.service.AuthorizationService;
import in.digio.auth.dto.authorization.request.TeamRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by Madhav Singh on 13/12/23
 */

@RestController
@RequestMapping(value = "/user", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class UserManagementController {

    private final AuthorizationService authorizationService;

    @PostMapping(value = "/team/create", produces = MediaType.APPLICATION_JSON_VALUE)
    public TeamResponse createAndUpdateTeam(@RequestBody TeamRequest teamRequest) {
        return authorizationService.createTeam(teamRequest);
    }

}
