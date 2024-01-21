package in.digio.account.model.authorization;

import in.digio.core.model.BaseDomain;
import jakarta.persistence.Entity;
import lombok.*;

/**
 * Created by Madhav Singh on 13/12/23
 */

@EqualsAndHashCode(callSuper = true)
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class UserTeam extends BaseDomain {

    public static final String ID_PREFIX = "UTID";
    private String teamId;
    private String userId;

    @Override
    public String obtainIdPrefix() {
        return ID_PREFIX;
    }
}
