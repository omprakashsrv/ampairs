import {Component, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormBuilder, FormControl, FormGroup, FormsModule, ReactiveFormsModule} from '@angular/forms';
import {MatTableDataSource, MatTableModule} from '@angular/material/table';
import {MatPaginator, MatPaginatorModule, PageEvent} from '@angular/material/paginator';
import {MatSort, MatSortModule} from '@angular/material/sort';
import {MatCardModule} from '@angular/material/card';
import {MatButtonModule} from '@angular/material/button';
import {MatIconModule} from '@angular/material/icon';
import {MatMenuModule} from '@angular/material/menu';
import {MatChipsModule} from '@angular/material/chips';
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatInputModule} from '@angular/material/input';
import {MatSelectModule} from '@angular/material/select';
import {MatCheckboxModule} from '@angular/material/checkbox';
import {MatProgressSpinnerModule} from '@angular/material/progress-spinner';
import {MatSnackBar, MatSnackBarModule} from '@angular/material/snack-bar';
import {MatDialog, MatDialogModule} from '@angular/material/dialog';
import {MatTooltipModule} from '@angular/material/tooltip';
import {MatDividerModule} from '@angular/material/divider';
import {MatBadgeModule} from '@angular/material/badge';
import {Subject} from 'rxjs';
import {debounceTime, distinctUntilChanged, startWith, takeUntil} from 'rxjs/operators';
import {SelectionModel} from '@angular/cdk/collections';
import {Router} from '@angular/router';

import {WorkspaceMemberService} from '../../../core/services/workspace-member.service';
import {WorkspaceService} from '../../../core/services/workspace.service';
import {
  canManageRole,
  getRoleColor,
  getRoleDescription,
  MemberFilters,
  MemberListResponse,
  MemberSortOptions,
  MemberStatus,
  PagedMemberResponse,
  WorkspaceMemberRole
} from '../../../core/models/workspace-member.interface';
import {MemberDetailDialogComponent} from './member-detail-dialog/member-detail-dialog.component';
import {CreateInvitationDialogComponent} from '../workspace-invitations/create-invitation-dialog/create-invitation-dialog.component';

@Component({
  selector: 'app-workspace-members',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatMenuModule,
    MatChipsModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatCheckboxModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatDialogModule,
    MatTooltipModule,
    MatDividerModule,
    MatBadgeModule
  ],
  templateUrl: './workspace-members.component.html',
  styleUrl: './workspace-members.component.scss'
})
export class WorkspaceMembersComponent implements OnInit, OnDestroy {
  @ViewChild(MatPaginator) paginator!: MatPaginator;
  @ViewChild(MatSort) sort!: MatSort;
  // Data sources and selection
  dataSource = new MatTableDataSource<MemberListResponse>([]);
  selection = new SelectionModel<MemberListResponse>(true, []);
  // Loading states
  isLoading = false;
  isLoadingStats = false;
  // Current workspace
  currentWorkspaceId = '';
  currentUserRole: WorkspaceMemberRole = 'MEMBER';
  // Pagination and sorting
  totalElements = 0;
  pageSize = 20;
  pageIndex = 0;
  sortBy = 'joinedAt';
  sortDirection: 'asc' | 'desc' = 'desc';
  // Filters and search
  filterForm!: FormGroup;
  searchControl = new FormControl('');
  // Available options
  roles: WorkspaceMemberRole[] = ['OWNER', 'ADMIN', 'MANAGER', 'MEMBER', 'GUEST', 'VIEWER'];
  statuses: MemberStatus[] = ['ACTIVE', 'INACTIVE', 'PENDING', 'SUSPENDED'];
  // Table configuration
  displayedColumns: string[] = [
    'select',
    'avatar',
    'name',
    'email',
    'role',
    'status',
    'joined_at',
    'last_activity',
    'actions'
  ];
  // Statistics
  memberStats: any = {};
  // UI state
  selectedMembers: MemberListResponse[] = [];
  bulkActionMenuOpen = false;
  private destroy$ = new Subject<void>();

  constructor(
    private memberService: WorkspaceMemberService,
    private workspaceService: WorkspaceService,
    private formBuilder: FormBuilder,
    private snackBar: MatSnackBar,
    private dialog: MatDialog,
    private router: Router
  ) {
    this.initializeFilterForm();
  }

  ngOnInit(): void {
    this.setupCurrentWorkspace();
    this.setupSearchSubscription();
    this.setupFilterSubscription();
    this.loadMembers();
    this.loadMemberStatistics();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadMembers(): void {
    if (!this.currentWorkspaceId) {
      this.showError('No workspace selected');
      return;
    }

    this.isLoading = true;

    const filters = this.getCurrentFilters();
    const sortOptions: MemberSortOptions = {
      sort_by: this.sortBy as any,
      sort_direction: this.sortDirection
    };

    this.memberService.searchMembers(filters, sortOptions, this.pageIndex, this.pageSize)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response: PagedMemberResponse) => {
          this.dataSource.data = response.content;
          this.totalElements = response.total_elements;
          this.isLoading = false;
          this.selection.clear();
        },
        error: (error) => {
          this.isLoading = false;
          console.error('Failed to load members:', error);
          this.showError('Failed to load workspace members');
        }
      });
  }


  loadMemberStatistics(): void {
    if (!this.currentWorkspaceId) {
      return;
    }

    this.isLoadingStats = true;
    this.memberService.getMemberStatistics(this.currentWorkspaceId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (stats) => {
          this.memberStats = stats;
          this.isLoadingStats = false;
        },
        error: (error) => {
          this.isLoadingStats = false;
          console.error('Failed to load member statistics:', error);
        }
      });
  }

  onPageChange(event: PageEvent): void {
    this.pageIndex = event.pageIndex;
    this.pageSize = event.pageSize;
    this.loadMembers();
  }

  onSortChange(sortState: any): void {
    this.sortBy = sortState.active;
    this.sortDirection = sortState.direction;
    this.loadMembers();
  }

  // Selection methods
  isAllSelected(): boolean {
    const numSelected = this.selection.selected.length;
    const numRows = this.dataSource.data.length;
    return numSelected === numRows;
  }

  toggleAllRows(): void {
    if (this.isAllSelected()) {
      this.selection.clear();
    } else {
      this.dataSource.data.forEach(row => this.selection.select(row));
    }
    this.updateSelectedMembers();
  }

  toggleRow(row: MemberListResponse): void {
    this.selection.toggle(row);
    this.updateSelectedMembers();
  }

  // Member actions
  viewMemberDetails(member: MemberListResponse): void {
    this.openMemberDetailDialog(member);
  }

  editMemberRole(member: MemberListResponse): void {
    this.openMemberDetailDialog(member, true);
  }

  private openMemberDetailDialog(member: MemberListResponse, startEditing: boolean = false): void {
    const dialogRef = this.dialog.open(MemberDetailDialogComponent, {
      width: '700px',
      maxWidth: '90vw',
      maxHeight: '90vh',
      panelClass: 'member-detail-dialog-container',
      data: {
        member: member,
        currentUserRole: this.currentUserRole,
        workspaceId: this.currentWorkspaceId
      }
    });

    // If startEditing is true, trigger edit mode after dialog opens
    if (startEditing) {
      dialogRef.afterOpened().subscribe(() => {
        const componentInstance = dialogRef.componentInstance as MemberDetailDialogComponent;
        if (componentInstance) {
          setTimeout(() => componentInstance.onEdit(), 100);
        }
      });
    }

    // Handle dialog close
    dialogRef.afterClosed().subscribe(result => {
      if (result?.updated) {
        // Refresh the members list
        this.loadMembers();
        this.loadMemberStatistics();
        
        if (result.member) {
          this.showSuccess('Member updated successfully');
        } else {
          this.showSuccess('Member removed successfully');
        }
      }
    });
  }

  updateMemberStatus(member: MemberListResponse, newStatus: MemberStatus): void {
    this.memberService.updateMemberStatus(member.id, newStatus)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.showSuccess(`Member status updated to ${newStatus.toLowerCase()}`);
          this.loadMembers();
        },
        error: (error) => {
          console.error('Failed to update member status:', error);
          this.showError('Failed to update member status');
        }
      });
  }

  removeMember(member: MemberListResponse): void {
    const memberName = this.getMemberName(member);
    if (confirm(`Are you sure you want to remove ${memberName} from this workspace?`)) {
      this.memberService.removeMember(this.currentWorkspaceId, member.id)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: () => {
            this.showSuccess('Member removed successfully');
            this.loadMembers();
            this.loadMemberStatistics();
          },
          error: (error) => {
            console.error('Failed to remove member:', error);
            this.showError('Failed to remove member');
          }
        });
    }
  }

  // Bulk actions
  bulkUpdateRole(newRole: WorkspaceMemberRole): void {
    const memberIds = this.selectedMembers.map(m => m.id);

    this.memberService.bulkUpdateMembers({
      member_ids: memberIds,
      role: newRole,
      notify_members: true
    }).pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (result) => {
          this.showSuccess(`Updated ${result.updated_count} members to ${newRole} role`);
          this.selection.clear();
          this.loadMembers();
          this.loadMemberStatistics();
        },
        error: (error) => {
          console.error('Failed to update members:', error);
          this.showError('Failed to update selected members');
        }
      });
  }

  bulkUpdateStatus(newStatus: MemberStatus): void {
    const memberIds = this.selectedMembers.map(m => m.id);

    this.memberService.bulkUpdateMembers({
      member_ids: memberIds,
      status: newStatus,
      notify_members: true
    }).pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (result) => {
          this.showSuccess(`Updated ${result.updated_count} members to ${newStatus.toLowerCase()} status`);
          this.selection.clear();
          this.loadMembers();
          this.loadMemberStatistics();
        },
        error: (error) => {
          console.error('Failed to update members:', error);
          this.showError('Failed to update selected members');
        }
      });
  }

  bulkRemoveMembers(): void {
    const memberNames = this.selectedMembers.map(m => this.getMemberName(m)).join(', ');

    if (confirm(`Are you sure you want to remove ${this.selectedMembers.length} members (${memberNames}) from this workspace?`)) {
      const memberIds = this.selectedMembers.map(m => m.id);

      this.memberService.bulkRemoveMembers(memberIds)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: (result) => {
            this.showSuccess(`Removed ${result.removed_count} members successfully`);
            this.selection.clear();
            this.loadMembers();
            this.loadMemberStatistics();
          },
          error: (error) => {
            console.error('Failed to remove members:', error);
            this.showError('Failed to remove selected members');
          }
        });
    }
  }

  // Export functionality
  exportMembers(format: 'CSV' | 'EXCEL' = 'CSV'): void {
    const filters = this.getCurrentFilters();

    this.memberService.exportMembers(format, filters)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (blob) => {
          const url = window.URL.createObjectURL(blob);
          const link = document.createElement('a');
          link.href = url;
          link.download = `workspace-members-${new Date().toISOString().split('T')[0]}.${format.toLowerCase()}`;
          link.click();
          window.URL.revokeObjectURL(url);
          this.showSuccess('Members data exported successfully');
        },
        error: (error) => {
          console.error('Failed to export members:', error);
          this.showError('Failed to export members data');
        }
      });
  }

  // Utility methods
  getMemberName(member: MemberListResponse): string {
    return `${member.first_name} ${member.last_name}`.trim() || member.email;
  }

  getRoleColor(role: WorkspaceMemberRole): string {
    return getRoleColor(role);
  }

  getRoleDescription(role: WorkspaceMemberRole): string {
    return getRoleDescription(role);
  }

  canManageMember(memberRole: WorkspaceMemberRole): boolean {
    return canManageRole(this.currentUserRole, memberRole);
  }

  getStatusColor(status: MemberStatus): string {
    switch (status) {
      case 'ACTIVE':
        return 'success';
      case 'INACTIVE':
        return 'warn';
      case 'PENDING':
        return 'accent';
      case 'SUSPENDED':
        return 'error';
      default:
        return 'basic';
    }
  }

  formatLastActivity(lastActivity?: string): string {
    if (!lastActivity) return 'No recent activity';

    const now = new Date();
    const activityDate = new Date(lastActivity);
    const diffMs = now.getTime() - activityDate.getTime();
    const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24));
    const diffHours = Math.floor(diffMs / (1000 * 60 * 60));
    const diffMinutes = Math.floor(diffMs / (1000 * 60));

    if (diffDays > 0) {
      return `${diffDays} day${diffDays > 1 ? 's' : ''} ago`;
    } else if (diffHours > 0) {
      return `${diffHours} hour${diffHours > 1 ? 's' : ''} ago`;
    } else if (diffMinutes > 0) {
      return `${diffMinutes} minute${diffMinutes > 1 ? 's' : ''} ago`;
    } else {
      return 'Just now';
    }
  }

  formatJoinedDate(joinedAt: string): string {
    return new Date(joinedAt).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric'
    });
  }

  // Navigation methods
  inviteNewMembers(): void {
    const dialogRef = this.dialog.open(CreateInvitationDialogComponent, {
      width: '800px',
      maxWidth: '90vw',
      maxHeight: '90vh',
      panelClass: 'create-invitation-dialog-container',
      data: {
        workspaceId: this.currentWorkspaceId,
        currentUserRole: this.currentUserRole
      }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result?.created) {
        // Refresh the members list and show success
        this.loadMembers();
        this.loadMemberStatistics();
        
        this.showSuccess(`${result.count || 1} invitation${(result.count || 1) > 1 ? 's' : ''} sent successfully`);
      }
    });
  }

  viewInvitations(): void {
    const currentWorkspace = this.workspaceService.getCurrentWorkspace();
    if (currentWorkspace) {
      this.router.navigate(['/w', currentWorkspace.slug, 'invitations']);
    }
  }

  // Reset filters
  resetFilters(): void {
    this.filterForm.reset({
      role: 'ALL',
      status: 'ALL',
      search_query: ''
    });
    this.searchControl.setValue('');
    this.pageIndex = 0;
  }

  private initializeFilterForm(): void {
    this.filterForm = this.formBuilder.group({
      role: ['ALL'],
      status: ['ALL'],
      search_query: ['']
    });
  }

  private setupCurrentWorkspace(): void {
    const currentWorkspace = this.workspaceService.getCurrentWorkspace();
    if (currentWorkspace) {
      this.currentWorkspaceId = currentWorkspace.id;
      
      // Get current user's role in workspace
      this.memberService.getCurrentUserRole(this.currentWorkspaceId).subscribe({
        next: (roleResponse) => {
          // Derive role from permissions since backend returns simplified format
          if (roleResponse.is_owner) {
            this.currentUserRole = 'OWNER';
          } else if (roleResponse.is_admin) {
            this.currentUserRole = 'ADMIN';
          } else if (roleResponse.can_invite_members) {
            this.currentUserRole = 'MANAGER';
          } else if (roleResponse.can_view_members) {
            this.currentUserRole = 'MEMBER';
          } else {
            this.currentUserRole = 'VIEWER';
          }
        },
        error: (error) => {
          console.error('Failed to load current user role:', error);
          this.showError('Failed to load user permissions');
        }
      });
    } else {
      this.showError('No workspace selected');
    }
  }

  private setupSearchSubscription(): void {
    this.searchControl.valueChanges.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      takeUntil(this.destroy$)
    ).subscribe(searchValue => {
      this.filterForm.patchValue({search_query: searchValue || ''});
      this.pageIndex = 0;
      this.loadMembers();
    });
  }

  private setupFilterSubscription(): void {
    this.filterForm.valueChanges.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      startWith(this.filterForm.value),
      takeUntil(this.destroy$)
    ).subscribe(() => {
      this.pageIndex = 0;
      this.loadMembers();
    });
  }

  private getCurrentFilters(): MemberFilters {
    const formValue = this.filterForm.value;
    return {
      role: formValue.role,
      status: formValue.status,
      search_query: formValue.search_query || undefined
    };
  }

  private updateSelectedMembers(): void {
    this.selectedMembers = this.selection.selected;
  }

  // Utility methods for UI feedback
  private showSuccess(message: string): void {
    this.snackBar.open(message, 'Close', {
      duration: 5000,
      panelClass: ['success-snackbar']
    });
  }

  private showError(message: string): void {
    this.snackBar.open(message, 'Close', {
      duration: 8000,
      panelClass: ['error-snackbar']
    });
  }

  getDisplayName(member: any): string {
    if (!member) return 'Unknown';
    
    // Check if member has a direct name property
    if (member.name) return member.name;
    
    // Check if member has first_name and last_name
    if (member.first_name || member.last_name) {
      return `${member.first_name || ''} ${member.last_name || ''}`.trim();
    }
    
    // Check if member has a nested user object with name info
    if (member.user) {
      if (member.user.name) return member.user.name;
      if (member.user.firstName || member.user.lastName) {
        return `${member.user.firstName || ''} ${member.user.lastName || ''}`.trim();
      }
      if (member.user.first_name || member.user.last_name) {
        return `${member.user.first_name || ''} ${member.user.last_name || ''}`.trim();
      }
    }
    
    // Fallback to email or id
    return member.email || member.user_id || member.id || 'Unknown';
  }

  // Track by function for ngFor performance
  trackByMemberId(index: number, member: MemberListResponse): string {
    return member.id;
  }

  // Refresh data
  refresh(): void {
    this.loadMembers();
    this.loadMemberStatistics();
  }
}
