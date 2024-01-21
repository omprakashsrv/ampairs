

package in.digio.acls.service;

import org.springframework.security.acls.domain.DefaultPermissionFactory;
import org.springframework.stereotype.Service;

@Service
public class AclPermissionFactory extends DefaultPermissionFactory {

    public AclPermissionFactory() {
        super(AclPermission.class);
    }
}
