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
import {MatDatepickerModule} from '@angular/material/datepicker';
import {MatNativeDateModule} from '@angular/material/core';
import {Subject} from 'rxjs';
import {debounceTime, distinctUntilChanged, startWith, takeUntil} from 'rxjs/operators';
import {SelectionModel} from '@angular/cdk/collections';

import {WorkspaceInvitationService} from '../../../core/services/workspace-invitation.service';
import {WorkspaceService} from '../../../core/services/workspace.service';
import {
  canCancelInvitation,
  canResendInvitation,
  DeliveryStatus,
  getDeliveryStatusColor,
  getDeliveryStatusDescription,
  getInvitationStatusColor,
  getInvitationStatusDescription,
  getInvitationTimeRemaining,
  InvitationFilters,
  InvitationListResponse,
  InvitationSortOptions,
  InvitationStatistics,
  InvitationStatus,
  isInvitationExpired,
  PagedInvitationResponse,
  WorkspaceMemberRole
} from '../../../core/models/workspace-invitation.interface';

@Component({
  selector: 'app-workspace-invitations',
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
    MatBadgeModule,
    MatDatepickerModule,
    MatNativeDateModule
  ],
  templateUrl: './workspace-invitations.component.html',
  styleUrl: './workspace-invitations.component.scss'
})
export class WorkspaceInvitationsComponent implements OnInit, OnDestroy {
  @ViewChild(MatPaginator) paginator!: MatPaginator;
  @ViewChild(MatSort) sort!: MatSort;
  // Data sources and selection
  dataSource = new MatTableDataSource<InvitationListResponse>([]);
  selection = new SelectionModel<InvitationListResponse>(true, []);
  // Loading states
  isLoading = false;
  isLoadingStats = false;
  // Current workspace
  currentWorkspaceId = '';
  // Pagination and sorting
  totalElements = 0;
  pageSize = 20;
  pageIndex = 0;
  sortBy = 'createdAt';
  sortDirection: 'asc' | 'desc' = 'desc';
  // Filters and search
  filterForm!: FormGroup;
  searchControl = new FormControl('');
  // Available options
  roles: WorkspaceMemberRole[] = ['OWNER', 'ADMIN', 'MANAGER', 'MEMBER', 'GUEST', 'VIEWER'];
  statuses: InvitationStatus[] = ['PENDING', 'SENT', 'DELIVERED', 'OPENED', 'ACCEPTED', 'DECLINED', 'EXPIRED', 'CANCELLED', 'FAILED'];
  deliveryStatuses: DeliveryStatus[] = ['PENDING', 'SENDING', 'SENT', 'DELIVERED', 'BOUNCED', 'FAILED', 'BLOCKED'];
  // Table configuration
  displayedColumns: string[] = [
    'select',
    'email',
    'role',
    'status',
    'delivery_status',
    'inviter',
    'created_at',
    'expires_at',
    'reminder_count',
    'actions'
  ];
  // Statistics
  invitationStats: InvitationStatistics | null = null;
  // UI state
  selectedInvitations: InvitationListResponse[] = [];
  private destroy$ = new Subject<void>();

  constructor(
    private invitationService: WorkspaceInvitationService,
    private workspaceService: WorkspaceService,
    private formBuilder: FormBuilder,
    private snackBar: MatSnackBar,
    private dialog: MatDialog
  ) {
    this.initializeFilterForm();
  }

  ngOnInit(): void {
    this.setupCurrentWorkspace();
    this.setupSearchSubscription();
    this.setupFilterSubscription();
    this.loadInvitations();
    this.loadInvitationStatistics();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadInvitations(): void {
    if (!this.currentWorkspaceId) {
      this.showError('No workspace selected');
      return;
    }

    this.isLoading = true;

    const filters = this.getCurrentFilters();
    const sortOptions: InvitationSortOptions = {
      sort_by: this.sortBy as any,
      sort_direction: this.sortDirection
    };

    this.invitationService.searchInvitations(filters, sortOptions, this.pageIndex, this.pageSize)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response: PagedInvitationResponse) => {
          this.dataSource.data = response.content;
          this.totalElements = response.total_elements;
          this.isLoading = false;
          this.selection.clear();
        },
        error: (error) => {
          this.isLoading = false;
          console.error('Failed to load invitations:', error);
          this.showError('Failed to load workspace invitations');
        }
      });
  }

  loadInvitationStatistics(): void {
    if (!this.currentWorkspaceId) {
      return;
    }

    this.isLoadingStats = true;
    this.invitationService.getInvitationStatistics(this.currentWorkspaceId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (stats) => {
          this.invitationStats = stats;
          this.isLoadingStats = false;
        },
        error: (error) => {
          this.isLoadingStats = false;
          console.error('Failed to load invitation statistics:', error);
        }
      });
  }

  onPageChange(event: PageEvent): void {
    this.pageIndex = event.pageIndex;
    this.pageSize = event.pageSize;
    this.loadInvitations();
  }

  onSortChange(sortState: any): void {
    this.sortBy = sortState.active;
    this.sortDirection = sortState.direction;
    this.loadInvitations();
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
    this.updateSelectedInvitations();
  }

  toggleRow(row: InvitationListResponse): void {
    this.selection.toggle(row);
    this.updateSelectedInvitations();
  }

  // Invitation actions
  viewInvitationDetails(invitation: InvitationListResponse): void {
    console.log('View invitation details:', invitation);
    // TODO: Implement invitation detail view
  }

  resendInvitation(invitation: InvitationListResponse): void {
    if (!canResendInvitation(invitation.status, invitation.reminder_count)) {
      this.showError('Cannot resend this invitation');
      return;
    }

    this.invitationService.resendInvitation(invitation.id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (result) => {
          this.showSuccess(`Invitation resent to ${invitation.email}`);
          this.loadInvitations();
          this.loadInvitationStatistics();
        },
        error: (error) => {
          console.error('Failed to resend invitation:', error);
          this.showError('Failed to resend invitation');
        }
      });
  }

  cancelInvitation(invitation: InvitationListResponse): void {
    if (!canCancelInvitation(invitation.status)) {
      this.showError('Cannot cancel this invitation');
      return;
    }

    if (confirm(`Are you sure you want to cancel the invitation for ${invitation.email}?`)) {
      this.invitationService.cancelInvitation(invitation.id)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: () => {
            this.showSuccess(`Invitation cancelled for ${invitation.email}`);
            this.loadInvitations();
            this.loadInvitationStatistics();
          },
          error: (error) => {
            console.error('Failed to cancel invitation:', error);
            this.showError('Failed to cancel invitation');
          }
        });
    }
  }

  deleteInvitation(invitation: InvitationListResponse): void {
    if (confirm(`Are you sure you want to permanently delete the invitation for ${invitation.email}?`)) {
      this.invitationService.deleteInvitation(invitation.id)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: () => {
            this.showSuccess(`Invitation deleted for ${invitation.email}`);
            this.loadInvitations();
            this.loadInvitationStatistics();
          },
          error: (error) => {
            console.error('Failed to delete invitation:', error);
            this.showError('Failed to delete invitation');
          }
        });
    }
  }

  // Bulk actions
  bulkCancelInvitations(): void {
    const invitationEmails = this.selectedInvitations.map(i => i.email).join(', ');

    if (confirm(`Are you sure you want to cancel ${this.selectedInvitations.length} invitations (${invitationEmails})?`)) {
      const invitationIds = this.selectedInvitations.map(i => i.id);

      this.invitationService.bulkCancelInvitations(invitationIds)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: (result) => {
            this.showSuccess(`Cancelled ${result.cancelled_count} invitations successfully`);
            this.selection.clear();
            this.loadInvitations();
            this.loadInvitationStatistics();
          },
          error: (error) => {
            console.error('Failed to cancel invitations:', error);
            this.showError('Failed to cancel selected invitations');
          }
        });
    }
  }

  bulkResendInvitations(): void {
    // Filter only resendable invitations
    const resendableInvitations = this.selectedInvitations.filter(inv =>
      canResendInvitation(inv.status, inv.reminder_count)
    );

    if (resendableInvitations.length === 0) {
      this.showError('None of the selected invitations can be resent');
      return;
    }

    const promises = resendableInvitations.map(invitation =>
      this.invitationService.resendInvitation(invitation.id).toPromise()
    );

    Promise.allSettled(promises).then(results => {
      const successful = results.filter(r => r.status === 'fulfilled').length;
      const failed = results.filter(r => r.status === 'rejected').length;

      if (successful > 0) {
        this.showSuccess(`Resent ${successful} invitations successfully`);
      }
      if (failed > 0) {
        this.showError(`Failed to resend ${failed} invitations`);
      }

      this.selection.clear();
      this.loadInvitations();
      this.loadInvitationStatistics();
    });
  }

  // Export functionality
  exportInvitations(format: 'CSV' | 'EXCEL' = 'CSV'): void {
    const filters = this.getCurrentFilters();

    this.invitationService.exportInvitations(format, filters)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (blob) => {
          const url = window.URL.createObjectURL(blob);
          const link = document.createElement('a');
          link.href = url;
          link.download = `workspace-invitations-${new Date().toISOString().split('T')[0]}.${format.toLowerCase()}`;
          link.click();
          window.URL.revokeObjectURL(url);
          this.showSuccess('Invitations data exported successfully');
        },
        error: (error) => {
          console.error('Failed to export invitations:', error);
          this.showError('Failed to export invitations data');
        }
      });
  }

  // Utility methods
  getInvitationStatusColor(status: InvitationStatus): string {
    return getInvitationStatusColor(status);
  }

  getInvitationStatusDescription(status: InvitationStatus): string {
    return getInvitationStatusDescription(status);
  }

  getDeliveryStatusColor(status: DeliveryStatus): string {
    return getDeliveryStatusColor(status);
  }

  getDeliveryStatusDescription(status: DeliveryStatus): string {
    return getDeliveryStatusDescription(status);
  }

  getInvitationTimeRemaining(expiresAt: string): string {
    return getInvitationTimeRemaining(expiresAt);
  }

  isExpired(expiresAt: string): boolean {
    return isInvitationExpired(expiresAt);
  }

  canResend(invitation: InvitationListResponse): boolean {
    return canResendInvitation(invitation.status, invitation.reminder_count);
  }

  canCancel(invitation: InvitationListResponse): boolean {
    return canCancelInvitation(invitation.status);
  }

  formatDate(dateString: string): string {
    return new Date(dateString).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  // Navigation methods
  createNewInvitation(): void {
    console.log('Navigate to create invitation');
    // TODO: Implement navigation to invitation creation
  }

  viewMembers(): void {
    console.log('Navigate to members');
    // TODO: Implement navigation to members component
  }

  // Reset filters
  resetFilters(): void {
    this.filterForm.reset({
      status: 'ALL',
      role: 'ALL',
      delivery_status: 'ALL',
      search_query: '',
      start_date: '',
      end_date: ''
    });
    this.searchControl.setValue('');
    this.pageIndex = 0;
  }

  private initializeFilterForm(): void {
    this.filterForm = this.formBuilder.group({
      status: ['ALL'],
      role: ['ALL'],
      delivery_status: ['ALL'],
      search_query: [''],
      start_date: [''],
      end_date: ['']
    });
  }

  private setupCurrentWorkspace(): void {
    const currentWorkspace = this.workspaceService.getCurrentWorkspace();
    if (currentWorkspace) {
      this.currentWorkspaceId = currentWorkspace.id;
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
      this.loadInvitations();
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
      this.loadInvitations();
    });
  }

  private getCurrentFilters(): InvitationFilters {
    const formValue = this.filterForm.value;
    const filters: InvitationFilters = {
      status: formValue.status,
      role: formValue.role,
      delivery_status: formValue.delivery_status,
      search_query: formValue.search_query || undefined
    };

    if (formValue.start_date || formValue.end_date) {
      filters.date_range = {
        start_date: formValue.start_date ? formValue.start_date.toISOString().split('T')[0] : '',
        end_date: formValue.end_date ? formValue.end_date.toISOString().split('T')[0] : ''
      };
    }

    return filters;
  }

  private updateSelectedInvitations(): void {
    this.selectedInvitations = this.selection.selected;
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
}
