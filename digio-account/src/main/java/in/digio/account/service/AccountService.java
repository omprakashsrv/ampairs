package in.digio.account.service;

import in.digio.core.multitenancy.TenantContext;
import org.springframework.stereotype.Service;

@Service
public class AccountService {

    public String accountId() {
        return TenantContext.getCurrentTenant();
    }

}
