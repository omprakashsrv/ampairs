package in.digio.account.model.authorization;

import in.digio.core.model.BaseDomain;
import jakarta.persistence.Entity;
import lombok.*;

/**
 * Created by Madhav Singh on 13/12/23
 */

@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
public class RolePermission extends BaseDomain {

    public static final String ID_PREFIX = "RPID";

    private String roleId;
    private String permissionId;

    @Override
    public String obtainIdPrefix() {
        return ID_PREFIX;
    }
}
