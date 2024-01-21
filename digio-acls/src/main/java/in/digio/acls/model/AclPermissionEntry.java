

package in.digio.acls.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.acls.model.Sid;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AclPermissionEntry {

    private AclSid sid;
    private Integer mask;

    public AclPermissionEntry(Sid sid, Integer mask) {
        this.sid = new AclSid(sid);
        this.mask = mask;
    }
}
