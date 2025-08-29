import {Injectable} from '@angular/core';
import {ActivatedRouteSnapshot, CanActivate, Router} from '@angular/router';
import {Observable, of} from 'rxjs';
import {catchError, map} from 'rxjs/operators';
import {WorkspaceMemberService} from '../services/workspace-member.service';
import {WorkspaceService} from '../services/workspace.service';

@Injectable({
  providedIn: 'root'
})
export class WorkspaceInvitationGuard implements CanActivate {

  constructor(
    private memberService: WorkspaceMemberService,
    private workspaceService: WorkspaceService,
    private router: Router
  ) {
  }

  canActivate(route: ActivatedRouteSnapshot): Observable<boolean> {
    const currentWorkspace = this.workspaceService.getCurrentWorkspace();

    if (!currentWorkspace) {
      // No workspace context, redirect to workspace selection
      this.router.navigate(['/workspaces']);
      return of(false);
    }

    // Get current user's role and permissions in the workspace
    return this.memberService.getCurrentUserRole(currentWorkspace.id).pipe(
      map(roleResponse => {
        // Check if user has permission to manage invitations
        const canManageInvitations = this.checkInvitationPermissions(roleResponse);

        if (!canManageInvitations) {
          // Insufficient permissions, redirect to dashboard with error message
          this.router.navigate(['/w', currentWorkspace.slug, 'dashboard'], {
            queryParams: {error: 'insufficient-permissions-invitations'}
          });
          return false;
        }

        return true;
      }),
      catchError(error => {
        console.error('Failed to check invitation permissions:', error);
        // Error checking permissions, redirect to dashboard
        this.router.navigate(['/w', currentWorkspace.slug, 'dashboard'], {
          queryParams: {error: 'permission-check-failed'}
        });
        return of(false);
      })
    );
  }

  private checkInvitationPermissions(roleResponse: any): boolean {
    const memberPermissions = roleResponse.permissions.member_management;
    const workspacePermissions = roleResponse.permissions.workspace_management;

    // User needs invitation management permissions
    // Check multiple permission sources as invitations might be controlled by:
    // 1. Member management permissions (can_invite_members)
    // 2. Workspace management permissions (can_manage_workspace)
    // 3. Role hierarchy (admins and owners typically can manage invitations)

    const canInviteMembers = memberPermissions.can_invite_members;
    const canManageWorkspace = workspacePermissions.can_manage_workspace;
    const canManageMembers = memberPermissions.can_manage_members;

    // User role hierarchy check
    const roleHierarchy = roleResponse.role_hierarchy;
    const isAdminOrAbove = roleHierarchy.is_owner || roleHierarchy.is_admin || roleHierarchy.is_manager;

    // Grant access if user has any of these permissions:
    // - Direct invitation permission
    // - Member management permission
    // - Workspace management permission
    // - Admin role or above
    return canInviteMembers || canManageMembers || canManageWorkspace || isAdminOrAbove;
  }
}
