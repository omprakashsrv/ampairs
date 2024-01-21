
package in.digio.acls.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.security.acls.model.Permission;


@AllArgsConstructor
@Builder
@Getter
public class PermissionVO {
    private Boolean principal;
    private String userId;
    private Permission permission;
}
