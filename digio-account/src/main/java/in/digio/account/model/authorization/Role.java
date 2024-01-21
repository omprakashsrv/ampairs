package in.digio.account.model.authorization;

import in.digio.auth.model.user.RoleType;
import in.digio.core.model.BaseDomain;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.*;

/**
 * Created by Madhav Singh on 13/12/23
 */

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role extends BaseDomain {

    public static final String ID_PREFIX = "ROLE";

    @Enumerated(EnumType.STRING)
    private RoleType roleName;

    private String accessLevel;


    @Override
    public String obtainIdPrefix() {
        return ID_PREFIX;
    }
}
