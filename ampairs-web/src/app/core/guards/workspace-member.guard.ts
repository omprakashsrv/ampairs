import {Injectable} from '@angular/core';
import {ActivatedRouteSnapshot, CanActivate, Router} from '@angular/router';
import {Observable, of} from 'rxjs';
import {catchError, map} from 'rxjs/operators';
import {WorkspaceMemberService} from '../services/workspace-member.service';
import {WorkspaceService} from '../services/workspace.service';

@Injectable({
  providedIn: 'root'
})
export class WorkspaceMemberGuard implements CanActivate {

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
        // Check if user has permission to view members
        const canViewMembers = roleResponse?.can_view_members;

        if (!canViewMembers) {
          // Insufficient permissions, redirect to dashboard with error message
          this.router.navigate(['/w', currentWorkspace.slug, 'dashboard'], {
            queryParams: {error: 'insufficient-permissions'}
          });
          return false;
        }

        // Check specific route permissions
        const routePath = route.routeConfig?.path;
        if (routePath === 'members') {
          return this.checkMemberManagementPermissions(roleResponse);
        }

        return true;
      }),
      catchError(error => {
        console.error('Failed to check member permissions:', error);
        // Error checking permissions, redirect to dashboard
        this.router.navigate(['/w', currentWorkspace.slug, 'dashboard'], {
          queryParams: {error: 'permission-check-failed'}
        });
        return of(false);
      })
    );
  }

  private checkMemberManagementPermissions(roleResponse: any): boolean {
    // User needs at least view members permission
    if (!roleResponse.can_view_members) {
      return false;
    }

    // For member management, user should have one of these permissions:
    // - is_owner or is_admin (full management access)
    // - can_invite_members (for inviting new members)
    // - can_manage_workspace (for workspace-level member management)
    const hasManagementPermission =
      roleResponse.is_owner ||
      roleResponse.is_admin ||
      roleResponse.can_invite_members ||
      roleResponse.can_manage_workspace;

    return hasManagementPermission;
  }
}
