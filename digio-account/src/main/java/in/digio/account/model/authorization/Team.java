package in.digio.account.model.authorization;

import in.digio.core.model.BaseDomain;
import jakarta.persistence.Entity;
import lombok.*;

/**
 * Created by Madhav Singh on 11/12/23
 */

@Builder
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class Team extends BaseDomain {

    public static final String ID_PREFIX = "TEAM";

    private String teamName;
    private String ownerId;
    private String departmentId;

//    private transient Department department;


    @Override
    public String obtainIdPrefix() {
        return ID_PREFIX;
    }
}
