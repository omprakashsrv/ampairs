package in.digio.acls.config;

import in.digio.acls.service.JdbcMutableAclServiceImpl;
import in.digio.auth.model.user.RoleType;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.acls.AclPermissionEvaluator;
import org.springframework.security.acls.domain.*;
import org.springframework.security.acls.jdbc.BasicLookupStrategy;
import org.springframework.security.acls.jdbc.JdbcMutableAclService;
import org.springframework.security.acls.jdbc.LookupStrategy;
import org.springframework.security.acls.model.AclCache;
import org.springframework.security.acls.model.PermissionGrantingStrategy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import javax.sql.DataSource;


@RequiredArgsConstructor
@AutoConfiguration
public class AclSecurityConfiguration {

    private final ApplicationContext context;
    private final PermissionFactory permissionFactory;
    private final DataSource masterDataSource;
    private final CacheManager cacheManager;

    @Bean
    public MethodSecurityExpressionHandler methodSecurityExpressionHandler() {
        DefaultMethodSecurityExpressionHandler expressionHandler =
                new DefaultMethodSecurityExpressionHandler();
        expressionHandler.setPermissionEvaluator(permissionEvaluator());
        expressionHandler.setRoleHierarchy(roleHierarchy());
        expressionHandler.setApplicationContext(context);
        return expressionHandler;
    }

    @Bean
    public RoleHierarchy roleHierarchy() {
        RoleHierarchyImpl roleHierarchy = new RoleHierarchyImpl();
        roleHierarchy.setHierarchy(RoleType.ADMIN.name() + " > " +
                RoleType.MANAGER.name() + " > " +
                RoleType.USER.name());
        return roleHierarchy;
    }

    @Bean
    public PermissionEvaluator permissionEvaluator() {
        AclPermissionEvaluator evaluator = new AclPermissionEvaluator(aclService());
        evaluator.setPermissionFactory(permissionFactory);
        return evaluator;
    }

    @Bean
    public PermissionGrantingStrategy permissionGrantingStrategy() {
        return new DefaultPermissionGrantingStrategy(auditLogger());
    }

    @Bean
    public AclAuthorizationStrategy aclAuthorizationStrategy() {
        return new AclAuthorizationStrategyImpl(new SimpleGrantedAuthority(RoleType.ADMIN.name()));
    }

    @Bean
    public AuditLogger auditLogger() {
        return new ConsoleAuditLogger();
    }

    @Bean
    public AclCache aclCache() {
        return new SpringCacheBasedAclCache(
                cacheManager.getCache(CacheConfiguration.ACL_CACHE),
                permissionGrantingStrategy(), aclAuthorizationStrategy());
    }

    @Bean
    public LookupStrategy lookupStrategy() {
        BasicLookupStrategy basicLookupStrategy = new BasicLookupStrategy(masterDataSource, aclCache(), aclAuthorizationStrategy(), permissionGrantingStrategy());
        basicLookupStrategy.setAclClassIdSupported(true);
        basicLookupStrategy.setPermissionFactory(permissionFactory);
        return basicLookupStrategy;
    }

    @Bean
    public JdbcMutableAclService aclService() {
        return new JdbcMutableAclServiceImpl(masterDataSource, lookupStrategy(), aclCache());
    }

}
