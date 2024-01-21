
package in.digio.acls.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class AclSecuredEntry {

    private AbstractSecuredEntity entity;
    private List<AclPermissionEntry> permissions = new ArrayList<>();

    public AclSecuredEntry(AbstractSecuredEntity entity) {
        this.entity = entity;
    }

    public void addPermission(AclPermissionEntry entry) {
        permissions.add(entry);
    }
}
