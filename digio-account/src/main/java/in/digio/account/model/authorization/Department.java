package in.digio.account.model.authorization;

import in.digio.core.model.BaseDomain;
import jakarta.persistence.Entity;
import lombok.*;

/**
 * Created by Madhav Singh on 13/12/23
 */

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class Department extends BaseDomain {

    public static final String ID_PREFIX = "DPT";

    private String departmentName;
    private String ownerId;

    @Override
    public String obtainIdPrefix() {
        return ID_PREFIX;
    }
}
