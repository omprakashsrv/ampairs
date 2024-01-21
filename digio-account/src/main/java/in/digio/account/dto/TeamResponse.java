package in.digio.account.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import in.digio.account.model.authorization.Team;
import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;

/**
 * Created by Madhav Singh on 13/12/23
 */

@Getter
@Setter
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class TeamResponse {

    private String teamId;
    private String teamName;
    private String departmentId;
    private String ownerId;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public TeamResponse(Team team) {
        this.teamId = team.getId();
        this.teamName = team.getTeamName();
        this.departmentId = team.getDepartmentId();
        this.ownerId = team.getOwnerId();
        this.createdAt = team.getCreatedAt();
        this.updatedAt = team.getUpdatedAt();
    }

}
