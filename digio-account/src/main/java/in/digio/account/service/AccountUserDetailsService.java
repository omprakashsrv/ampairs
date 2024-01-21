package in.digio.account.service;

import in.digio.account.model.WorkSpace;
import in.digio.account.repository.WorkSpaceRepository;
import in.digio.auth.model.user.User;
import in.digio.auth.repository.UserRepository;
import in.digio.core.multitenancy.TenantContext;
import lombok.AllArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;


@Service
@AllArgsConstructor
public class AccountUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final WorkSpaceRepository workSpaceRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findById(username).orElse(null);
        if (user == null) {
            throw new UsernameNotFoundException("User not found");
        }
        TenantContext.setCurrentTenant(user.getId());
        List<WorkSpace> workSpaces = workSpaceRepository.findAll();
        if (!workSpaces.isEmpty()) {
            List<SimpleGrantedAuthority> authorities = new ArrayList<>();
            authorities.add(new SimpleGrantedAuthority("ROLE_OWNER"));
            user.setAuthorities(authorities);
        }
        return user;
    }
}
