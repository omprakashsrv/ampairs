

package in.digio.acls.model;

import in.digio.core.model.OwnableBaseDomain;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

/**
 * {@link AbstractSecuredEntity} represents an entity access to which may be
 * restricted via ACL (access control list) security layer. {@link AbstractSecuredEntity}
 * implies that permissions may be set for current object or may be inherited from
 * a parent {@link AbstractSecuredEntity}. Common rule is that only users with admin role
 * are allowed to manage entities without a parent ("root" entities), but this behaviour
 * may be overriden in security layer.
 */
@Getter
@Setter
@MappedSuperclass
public abstract class AbstractSecuredEntity extends OwnableBaseDomain {

    /**
     * Flag indicating, whether item is locked from changes or not
     */
    @Transient
    private boolean locked = false;

    /**
     * @return a parent {@link AbstractSecuredEntity} to inherit permissions,
     * that are not set for current entity.
     */
    public abstract AbstractSecuredEntity getParent();

    public void clearParent() {
        // nothing by default
    }

    /**
     * @return {@link String} to which an entity belongs
     */
    public abstract String getAclClass();

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AbstractSecuredEntity that = (AbstractSecuredEntity) o;
        return Objects.equals(getAclClass(), that.getAclClass()) &&
                Objects.equals(getId(), that.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getAclClass(), getId());
    }
}
