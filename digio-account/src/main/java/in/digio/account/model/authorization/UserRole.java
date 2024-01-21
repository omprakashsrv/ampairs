package in.digio.account.model.authorization;

import in.digio.core.model.BaseDomain;
import jakarta.persistence.Entity;
import lombok.*;

/**
 * Created by Madhav Singh on 13/12/23
 */

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
public class UserRole extends BaseDomain {

    private static final String ID_PREFIX = "URID";

    private String userId;
    private String roleId;

    @Override
    public String obtainIdPrefix() {
        return ID_PREFIX;
    }
}

