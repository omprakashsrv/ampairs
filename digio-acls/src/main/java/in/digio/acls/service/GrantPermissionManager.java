package in.digio.acls.service;

import in.digio.acls.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.acls.model.AccessControlEntry;
import org.springframework.security.acls.model.MutableAcl;
import org.springframework.security.acls.model.Permission;
import org.springframework.security.acls.model.Sid;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static java.util.stream.Collectors.toList;


@Slf4j
@Service
@RequiredArgsConstructor
public class GrantPermissionManager {

    private final JdbcMutableAclServiceImpl aclService;

    @Transactional(propagation = Propagation.REQUIRED)
    public void setPermissionsToEntity(final AbstractSecuredEntity entity, final List<PermissionVO> permissions) {
        if (!permissions.isEmpty()) {
            permissions.forEach(permission ->
                    setPermissionToEntity(entity, permission));
        }
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public AclSecuredEntry setPermissionToEntity(AbstractSecuredEntity entity) {
        return setPermissionToEntity(entity, PermissionVO.builder()
                .permission(AclPermission.OWNER)
                .principal(true).build());
    }

    public AclSecuredEntry setPermissionToEntity(AbstractSecuredEntity entity, PermissionVO grantVO) {
        MutableAcl acl = aclService.getOrCreateObjectIdentity(entity);
        Permission permission = grantVO.getPermission();
        String sidName = grantVO.getUserId();
        if (sidName == null) {
            sidName = SecurityContextHolder.getContext().getAuthentication().getName();
        }
        Sid sid = aclService.createOrGetSid(sidName, grantVO.getPrincipal());
        log.info("Granting permissions. Entity: class={} id={}, name={}, permission: {}" +
                        " (mask: {}). Sid: name={} isPrincipal={}",
                entity.getAclClass(), entity.getId(), entity.getClass().getName(),
                AclPermission.getReadableView(permission.getMask()),
                permission.getMask(), sidName, grantVO.getPrincipal());
        int sidEntryIndex = findSidEntry(acl, sid);
        if (sidEntryIndex != -1) {
            acl.deleteAce(sidEntryIndex);
        }
        acl.insertAce(Math.max(sidEntryIndex, 0), permission, sid, true);
        MutableAcl updatedAcl = aclService.updateAcl(acl);
        AclSecuredEntry aclSecuredEntry = convertAclToEntryForUser(entity, updatedAcl, sid);
        aclService.putInCache(updatedAcl);
        return aclSecuredEntry;
    }

    private int findSidEntry(MutableAcl acl, Sid sid) {
        List<AccessControlEntry> entries = acl.getEntries();
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).getSid().equals(sid)) {
                return i;
            }
        }
        return -1;
    }

    private AclSecuredEntry convertAclToEntryForUser(AbstractSecuredEntity entity, MutableAcl acl,
                                                     Sid sid) {
        AclSid aclSid = new AclSid(sid);
        AclSecuredEntry entry = convertAclToEntry(entity, acl);
        List<AclPermissionEntry> filteredPermissions =
                entry.getPermissions().stream().filter(p -> p.getSid().equals(aclSid))
                        .collect(toList());
        entry.setPermissions(filteredPermissions);
        return entry;
    }

    private AclSecuredEntry convertAclToEntry(AbstractSecuredEntity entity, MutableAcl acl) {
        AclSecuredEntry entry = new AclSecuredEntry(entity);
        acl.getEntries().forEach(aclEntry -> entry.addPermission(
                new AclPermissionEntry(aclEntry.getSid(), aclEntry.getPermission().getMask())));
        return entry;
    }

}
