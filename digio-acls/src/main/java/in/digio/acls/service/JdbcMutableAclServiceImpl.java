package in.digio.acls.service;

import in.digio.acls.model.AbstractSecuredEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.acls.domain.GrantedAuthoritySid;
import org.springframework.security.acls.domain.ObjectIdentityImpl;
import org.springframework.security.acls.domain.PrincipalSid;
import org.springframework.security.acls.jdbc.JdbcMutableAclService;
import org.springframework.security.acls.jdbc.LookupStrategy;
import org.springframework.security.acls.model.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class JdbcMutableAclServiceImpl extends JdbcMutableAclService {

    private final AclCache aclCache;

    public JdbcMutableAclServiceImpl(DataSource dataSource, LookupStrategy lookupStrategy,
                                     AclCache aclCache) {
        super(dataSource, lookupStrategy, aclCache);
        this.aclCache = aclCache;
        this.setClassIdentityQuery("SELECT @@IDENTITY");
        this.setSidIdentityQuery("SELECT @@IDENTITY");
        this.setAclClassIdSupported(true);
    }

    @Transactional
    public MutableAcl getOrCreateObjectIdentity(AbstractSecuredEntity securedEntity) {
        ObjectIdentity identity = new ObjectIdentityImpl(securedEntity.getClass(), securedEntity.getId());
        if (retrieveObjectIdentityPrimaryKey(identity) != null) {
            Acl acl = readAclById(identity);
            Assert.isInstanceOf(MutableAcl.class, acl, "error mutable acl return");
            return (MutableAcl) acl;
        } else {
            MutableAcl acl = createAcl(identity);
            if (securedEntity.getParent() != null && securedEntity.getParent().getId() != null) {
                MutableAcl parentAcl = getOrCreateObjectIdentity(securedEntity.getParent());
                acl.setParent(parentAcl);
                updateAcl(acl);
            }
            return acl;
        }
    }

    public Map<ObjectIdentity, Acl> getObjectIdentities(Set<AbstractSecuredEntity> securedEntities) {
        List<ObjectIdentity> objectIdentities = securedEntities.stream()
                .map(ObjectIdentityImpl::new)
                .collect(Collectors.toList());
        return readAclsById(objectIdentities);
    }

    @Transactional
    public Sid createOrGetSid(String userName, boolean isPrincipal) {
        createOrRetrieveSidPrimaryKey(userName, isPrincipal, true);
        return isPrincipal ? new PrincipalSid(userName) : new GrantedAuthoritySid(userName);
    }

    public Sid getSid(String user, boolean isPrincipal) {
        Assert.notNull(createOrRetrieveSidPrimaryKey(user, isPrincipal, false), "error user not found");
        return isPrincipal ? new PrincipalSid(user) : new GrantedAuthoritySid(user);
    }

    public Long getSidId(String user, boolean isPrincipal) {
        Sid sid = isPrincipal ? new PrincipalSid(user) : new GrantedAuthoritySid(user);
        return createOrRetrieveSidPrimaryKey(sid, false);
    }

    public MutableAcl getAcl(AbstractSecuredEntity securedEntity) {
        try {
            ObjectIdentity identity = new ObjectIdentityImpl(securedEntity);
            Acl acl = readAclById(identity);
            Assert.isInstanceOf(MutableAcl.class, acl, "error mutable acl return");
            return (MutableAcl) acl;
        } catch (NotFoundException e) {
            log.debug(e.getMessage());
            return null;
        }
    }

    @Transactional
    public void getOrCreateObjectIdentityWithParent(AbstractSecuredEntity entity,
                                                    AbstractSecuredEntity parent) {
        MutableAcl acl = getOrCreateObjectIdentity(entity);
        if ((parent == null || parent.getId() == null) && acl.getParentAcl() == null) {
            return;
        }
        if (parent == null || parent.getId() == null) {
            acl.setParent(null);
            updateAcl(acl);
        } else if (acl.getParentAcl() == null
                || acl.getParentAcl().getObjectIdentity().getIdentifier() != parent.getId()) {
            MutableAcl parentAcl = getOrCreateObjectIdentity(parent);
            acl.setParent(parentAcl);
            updateAcl(acl);
        }
    }

    @Transactional
    public void changeOwner(final AbstractSecuredEntity entity, final String owner) {
        final MutableAcl aclFolder = getOrCreateObjectIdentity(entity);
        aclFolder.setOwner(createOrGetSid(owner, true));
        updateAcl(aclFolder);
    }

    public void putInCache(final MutableAcl acl) {
        aclCache.putInCache(acl);
    }

}
