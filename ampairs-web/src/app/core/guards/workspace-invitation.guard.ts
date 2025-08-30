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
    // User needs invitation management permissions
    // Check multiple permission sources as invitations might be controlled by:
    // 1. Direct invitation permission (can_invite_members)
    // 2. Workspace management permissions (can_manage_workspace)
    // 3. Role hierarchy (owners and admins typically can manage invitations)

    const canInviteMembers = roleResponse.can_invite_members;
    const canManageWorkspace = roleResponse.can_manage_workspace;
    const isOwner = roleResponse.is_owner;
    const isAdmin = roleResponse.is_admin;

    // Grant access if user has any of these permissions:
    // - Direct invitation permission
    // - Workspace management permission
    // - Owner or Admin role
    return canInviteMembers || canManageWorkspace || isOwner || isAdmin;
  }
}
