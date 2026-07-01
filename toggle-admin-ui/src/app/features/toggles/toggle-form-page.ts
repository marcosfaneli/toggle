import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { finalize, timeout } from 'rxjs';
import {
  CreateToggleRequest,
  Toggle,
  ToggleApiService,
  ToggleValueType,
  ToggleWriteRequest
} from '../../core/toggle-api.service';

interface ToggleForm {
  name: string;
  maintainer: string;
  enabled: boolean;
  hasValue: boolean;
  valueType: ToggleValueType;
  valueRaw: string;
}

@Component({
  selector: 'app-toggle-form-page',
  imports: [CommonModule, FormsModule],
  templateUrl: './toggle-form-page.html'
})
export class ToggleFormPage implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly toggleApi = inject(ToggleApiService);
  private readonly changeDetector = inject(ChangeDetectorRef);

  mode: 'create' | 'edit' = 'create';
  loading = false;
  saving = false;
  error = '';
  form: ToggleForm = this.emptyForm();

  ngOnInit(): void {
    const name = this.route.snapshot.paramMap.get('name');
    if (name) {
      this.mode = 'edit';
      this.loadToggle(name);
    }
  }

  save(): void {
    this.error = '';

    if (!this.form.name.trim() || !this.form.maintainer.trim()) {
      this.error = 'Name and maintainer are required.';
      return;
    }

    if (this.form.hasValue && !this.form.valueRaw.trim()) {
      this.error = 'Value is required when a type is selected.';
      return;
    }

    this.saving = true;
    const operation = this.mode === 'create'
      ? this.toggleApi.create(this.createRequest())
      : this.toggleApi.update(this.form.name, this.updateRequest());

    operation.subscribe({
      next: () => this.router.navigate(['/toggles'], {
        queryParams: { refreshedAt: Date.now() }
      }),
      error: err => {
        this.saving = false;
        this.error = this.errorMessage(err);
      }
    });
  }

  cancel(): void {
    this.router.navigate(['/toggles']);
  }

  private loadToggle(name: string): void {
    this.loading = true;
    this.error = '';

    this.toggleApi.getByName(name).pipe(
      timeout(10000),
      finalize(() => {
        this.loading = false;
        this.changeDetector.detectChanges();
      })
    ).subscribe({
      next: toggle => {
        this.form = this.formFromToggle(toggle);
      },
      error: err => {
        this.error = this.errorMessage(err);
      }
    });
  }

  private createRequest(): CreateToggleRequest {
    return {
      name: this.form.name.trim(),
      maintainer: this.form.maintainer.trim(),
      enabled: this.form.enabled,
      value: this.form.hasValue ? {
        type: this.form.valueType,
        raw: this.form.valueRaw.trim()
      } : undefined
    };
  }

  private updateRequest(): ToggleWriteRequest {
    return {
      maintainer: this.form.maintainer.trim(),
      enabled: this.form.enabled,
      value: this.form.hasValue ? {
        type: this.form.valueType,
        raw: this.form.valueRaw.trim()
      } : null
    };
  }

  private emptyForm(): ToggleForm {
    return {
      name: '',
      maintainer: '',
      enabled: false,
      hasValue: false,
      valueType: 'STRING',
      valueRaw: ''
    };
  }

  private formFromToggle(toggle: Toggle): ToggleForm {
    return {
      name: toggle.name,
      maintainer: toggle.maintainer,
      enabled: toggle.enabled,
      hasValue: !!toggle.value,
      valueType: (toggle.value?.type ?? 'STRING') as ToggleValueType,
      valueRaw: toggle.value?.raw ?? ''
    };
  }

  private errorMessage(err: unknown): string {
    if (typeof err === 'object' && err !== null && 'error' in err) {
      const body = (err as { error?: { detail?: string; title?: string } }).error;
      return body?.detail || body?.title || 'Request failed.';
    }
    return 'Request failed.';
  }
}
