package in.digio.account.filter;

import in.digio.account.dto.WorkSpaceRequest;
import in.digio.account.dto.WorkSpaceResponse;
import in.digio.account.service.WorkSpaceService;
import in.digio.auth.model.user.User;
import in.digio.core.multitenancy.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static in.digio.auth.config.SecurityConfiguration.*;

@Order(2)
@Component
@RequiredArgsConstructor
public class TenantFilter extends OncePerRequestFilter {

    private final WorkSpaceService workSpaceService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        if (Arrays.stream(OPEN_APIS).anyMatch(api -> request.getServletPath().contains(api)) || request.getServletPath().contains(USER_APIS)) {
            filterChain.doFilter(request, response);
            return;
        }
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        TenantContext.setCurrentTenant(user.getId());

        if (request.getServletPath().contains(ACCOUNT_APIS)) {
            filterChain.doFilter(request, response);
        } else {
            // Set current workspace id.
            List<WorkSpaceResponse> workSpaces = workSpaceService.getWorkSpaces();
            if (workSpaces.isEmpty()) {
                String fullName = user.getFullName();
                WorkSpaceResponse workSpace = workSpaceService.createWorkSpace(WorkSpaceRequest.builder().name(fullName.isEmpty() ? "Default WorkSpace" : fullName).build());
                TenantContext.setCurrentTenant(workSpace.getId());
            } else {
                TenantContext.setCurrentTenant(workSpaces.get(0).getId());
            }
            filterChain.doFilter(request, response);
        }
        TenantContext.unload();
    }
}
