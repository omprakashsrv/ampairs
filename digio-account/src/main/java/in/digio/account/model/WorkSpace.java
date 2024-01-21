package in.digio.account.model;

import in.digio.acls.model.AbstractSecuredEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;


@Builder
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "workspace")
public class WorkSpace extends AbstractSecuredEntity {


    private String name;

    @Override
    public String obtainIdPrefix() {
        return "WSP";
    }

    @SuppressWarnings("JpaAttributeTypeInspection")
    @Override
    public AbstractSecuredEntity getParent() {
        return null;
    }

    @Override
    public String getAclClass() {
        return this.getClass().getName();
    }
}
