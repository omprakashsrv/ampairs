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
public class Permission extends BaseDomain {
    private static final String ID_PREFIX = "PER";

    private String title;
    private PermissionType type;
    private String category;


    @Override
    public String obtainIdPrefix() {
        return ID_PREFIX;
    }
}
