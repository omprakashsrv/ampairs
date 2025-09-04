import {Component, OnInit} from '@angular/core';
import {Router} from '@angular/router';
import {AbstractControl, FormBuilder, FormGroup, ReactiveFormsModule, Validators} from '@angular/forms';
import {CommonModule} from '@angular/common';
import {MatCardModule} from '@angular/material/card';
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatInputModule} from '@angular/material/input';
import {MatSelectModule} from '@angular/material/select';
import {MatButtonModule} from '@angular/material/button';
import {MatIconModule} from '@angular/material/icon';
import {MatProgressSpinnerModule} from '@angular/material/progress-spinner';
import {MatSnackBar, MatSnackBarModule} from '@angular/material/snack-bar';
import {MatStepperModule} from '@angular/material/stepper';
import {MatTooltipModule} from '@angular/material/tooltip';
import {MatToolbarModule} from '@angular/material/toolbar';
import {debounceTime, distinctUntilChanged, switchMap} from 'rxjs/operators';
import {CreateWorkspaceRequest, WorkspaceService} from '../../../core/services/workspace.service';

@Component({
  selector: 'app-workspace-create',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatStepperModule,
    MatTooltipModule,
    MatToolbarModule
  ],
  templateUrl: './workspace-create.component.html',
  styleUrl: './workspace-create.component.scss'
})
export class WorkspaceCreateComponent implements OnInit {
  workspaceForm: FormGroup;
  step1FormGroup: FormGroup;
  step2FormGroup: FormGroup;
  step3FormGroup: FormGroup;
  isLoading = false;
  isCheckingSlug = false;
  slugAvailable = true;
  currentStep = 0;
  totalSteps = 3;

  workspaceTypes = [
    {value: 'BUSINESS', label: 'Business', description: 'For companies and organizations', icon: 'business'},
    {value: 'PERSONAL', label: 'Personal', description: 'For individual use', icon: 'person'},
    {value: 'TEAM', label: 'Team', description: 'For small teams and groups', icon: 'group'},
    {value: 'ENTERPRISE', label: 'Enterprise', description: 'For large organizations', icon: 'corporate_fare'}
  ];

  timezones = [
    {value: 'UTC', label: 'UTC (Coordinated Universal Time)'},
    {value: 'America/New_York', label: 'Eastern Time (ET)'},
    {value: 'America/Chicago', label: 'Central Time (CT)'},
    {value: 'America/Denver', label: 'Mountain Time (MT)'},
    {value: 'America/Los_Angeles', label: 'Pacific Time (PT)'},
    {value: 'Europe/London', label: 'London (GMT)'},
    {value: 'Europe/Paris', label: 'Paris (CET)'},
    {value: 'Asia/Tokyo', label: 'Tokyo (JST)'},
    {value: 'Asia/Shanghai', label: 'Shanghai (CST)'},
    {value: 'Asia/Kolkata', label: 'India (IST)'},
    {value: 'Australia/Sydney', label: 'Sydney (AEST)'}
  ];

  languages = [
    {value: 'en', label: 'English'},
    {value: 'es', label: 'Spanish'},
    {value: 'fr', label: 'French'},
    {value: 'de', label: 'German'},
    {value: 'it', label: 'Italian'},
    {value: 'pt', label: 'Portuguese'},
    {value: 'ru', label: 'Russian'},
    {value: 'ja', label: 'Japanese'},
    {value: 'ko', label: 'Korean'},
    {value: 'zh', label: 'Chinese'},
    {value: 'hi', label: 'Hindi'},
    {value: 'ar', label: 'Arabic'}
  ];

  constructor(
    private fb: FormBuilder,
    private workspaceService: WorkspaceService,
    private router: Router,
    private snackBar: MatSnackBar
  ) {
    this.workspaceForm = this.fb.group({
      name: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
      slug: ['', [Validators.required, Validators.pattern(/^[a-z0-9-]+$/), Validators.minLength(3), Validators.maxLength(50)]],
      description: ['', [Validators.maxLength(500)]],
      workspace_type: ['BUSINESS', [Validators.required]],
      timezone: ['Asia/Kolkata', [Validators.required]],
      language: ['en', [Validators.required]]
    });

    // Initialize separate form groups for stepper validation
    this.step1FormGroup = this.fb.group({
      name: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
      slug: ['', [Validators.required, Validators.pattern(/^[a-z0-9-]+$/), Validators.minLength(3), Validators.maxLength(50)]],
      workspace_type: ['BUSINESS', [Validators.required]]
    });

    this.step2FormGroup = this.fb.group({
      description: ['', [Validators.maxLength(500)]]
    });

    this.step3FormGroup = this.fb.group({
      timezone: ['Asia/Kolkata', [Validators.required]],
      language: ['en', [Validators.required]]
    });

    // Sync the separate form groups with the main form
    this.syncFormGroups();
  }

  // Form field getters for template
  get nameField() {
    return this.step1FormGroup.get('name') || this.workspaceForm.get('name');
  }

  get slugField() {
    return this.step1FormGroup.get('slug') || this.workspaceForm.get('slug');
  }

  get descriptionField() {
    return this.step2FormGroup.get('description') || this.workspaceForm.get('description');
  }

  get workspaceTypeField() {
    return this.step1FormGroup.get('workspace_type') || this.workspaceForm.get('workspace_type');
  }

  get timezoneField() {
    return this.step3FormGroup.get('timezone') || this.workspaceForm.get('timezone');
  }

  get languageField() {
    return this.step3FormGroup.get('language') || this.workspaceForm.get('language');
  }

  ngOnInit(): void {
    // Auto-generate slug from name
    this.step1FormGroup.get('name')?.valueChanges.subscribe(name => {
      if (name && !this.step1FormGroup.get('slug')?.dirty) {
        const slug = this.generateSlug(name);
        this.step1FormGroup.get('slug')?.setValue(slug);
      }
    });

    // Check slug availability when changed
    this.step1FormGroup.get('slug')?.valueChanges.pipe(
      debounceTime(500),
      distinctUntilChanged(),
      switchMap(slug => {
        if (slug && slug.length >= 3) {
          this.isCheckingSlug = true;
          return this.workspaceService.checkSlugAvailability(slug);
        }
        return [];
      })
    ).subscribe({
      next: (available) => {
        this.slugAvailable = available;
        this.isCheckingSlug = false;

        if (!available) {
          this.step1FormGroup.get('slug')?.setErrors({unavailable: true});
          this.workspaceForm.get('slug')?.setErrors({unavailable: true});
        }
      },
      error: () => {
        this.isCheckingSlug = false;
      }
    });
  }

  async onSubmit(): Promise<void> {
    if (this.workspaceForm.valid && !this.isLoading && this.slugAvailable) {
      this.isLoading = true;

      const workspaceData: CreateWorkspaceRequest = {
        name: this.workspaceForm.get('name')?.value.trim(),
        slug: this.workspaceForm.get('slug')?.value.trim(),
        description: this.workspaceForm.get('description')?.value?.trim() || undefined,
        workspace_type: this.workspaceForm.get('workspace_type')?.value,
        timezone: this.workspaceForm.get('timezone')?.value,
        language: this.workspaceForm.get('language')?.value
      };

      try {
        const workspace = await this.workspaceService.createWorkspace(workspaceData);
        this.isLoading = false;

        // Set as current workspace and navigate to home
        this.workspaceService.setCurrentWorkspace(workspace);

        this.snackBar.open(`Workspace "${workspace.name}" created successfully!`, 'Close', {
          duration: 4000,
          panelClass: ['success-snackbar']
        });

        this.router.navigate(['/w', workspace.slug]);
      } catch (error: any) {
        this.isLoading = false;
        console.error('Failed to create workspace:', error);
        this.showError(error.message || 'Failed to create workspace. Please try again.');
      }
    } else {
      this.markFormGroupTouched();
    }
  }

  goBack(): void {
    this.router.navigate(['/workspaces']);
  }

  generateSlug(name: string): string {
    return name
      .toLowerCase()
      .replace(/[^a-z0-9\s-]/g, '') // Remove special characters
      .replace(/\s+/g, '-') // Replace spaces with hyphens
      .replace(/-+/g, '-') // Replace multiple hyphens with single
      .replace(/^-|-$/g, '') // Remove leading/trailing hyphens
      .substring(0, 50); // Limit length
  }

  getSlugPreview(): string {
    const slug = this.workspaceForm.get('slug')?.value;
    return slug ? `ampairs.com/workspace/${slug}` : 'ampairs.com/workspace/your-workspace';
  }

  isStepCompleted(step: number): boolean {
    switch (step) {
      case 0: // Basic Info
        const basicControls = ['name', 'slug', 'workspace_type'];
        return basicControls.every(control =>
          this.workspaceForm.get(control)?.valid && this.slugAvailable
        );
      case 1: // Details
        return this.workspaceForm.get('description')?.valid !== false;
      case 2: // Preferences
        const prefControls = ['timezone', 'language'];
        return prefControls.every(control => this.workspaceForm.get(control)?.valid);
      default:
        return false;
    }
  }

  nextStep(): void {
    if (this.currentStep < 2 && this.isStepCompleted(this.currentStep)) {
      this.currentStep++;
    }
  }

  prevStep(): void {
    if (this.currentStep > 0) {
      this.currentStep--;
    }
  }

  // M3 Stepper helper methods
  getStepFormGroup(stepIndex: number): AbstractControl {
    switch (stepIndex) {
      case 0:
        return this.step1FormGroup;
      case 1:
        return this.step2FormGroup;
      case 2:
        return this.step3FormGroup;
      default:
        return this.step1FormGroup;
    }
  }

  isStepValid(stepIndex: number): boolean {
    return this.isStepCompleted(stepIndex);
  }

  getSlugIcon(): string {
    if (this.isCheckingSlug) return 'autorenew';
    if (this.slugAvailable) return 'check_circle';
    return 'error';
  }

  getSlugIconClass(): string {
    if (this.isCheckingSlug) return 'slug-checking';
    if (this.slugAvailable) return 'slug-available';
    return 'slug-unavailable';
  }

  private markFormGroupTouched(): void {
    Object.keys(this.workspaceForm.controls).forEach(key => {
      const control = this.workspaceForm.get(key);
      control?.markAsTouched();
    });
  }

  private showError(message: string): void {
    this.snackBar.open(message, 'Close', {
      duration: 5000,
      panelClass: ['error-snackbar']
    });
  }

  // Sync form groups to keep values in sync
  private syncFormGroups(): void {
    // Step 1 to main form
    this.step1FormGroup.valueChanges.subscribe(values => {
      this.workspaceForm.patchValue(values, {emitEvent: false});
    });

    // Step 2 to main form
    this.step2FormGroup.valueChanges.subscribe(values => {
      this.workspaceForm.patchValue(values, {emitEvent: false});
    });

    // Step 3 to main form
    this.step3FormGroup.valueChanges.subscribe(values => {
      this.workspaceForm.patchValue(values, {emitEvent: false});
    });

    // Main form to step forms (for initialization)
    this.workspaceForm.valueChanges.subscribe(values => {
      this.step1FormGroup.patchValue({
        name: values.name,
        slug: values.slug,
        workspace_type: values.workspace_type
      }, {emitEvent: false});

      this.step2FormGroup.patchValue({
        description: values.description
      }, {emitEvent: false});

      this.step3FormGroup.patchValue({
        timezone: values.timezone,
        language: values.language
      }, {emitEvent: false});
    });
  }
}
