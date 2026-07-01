import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { finalize, timeout } from 'rxjs';
import {
  CreateToggleRequest,
  PagedToggleConsumerResponse,
  Toggle,
  ToggleConsumer,
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
  consumersLoading = false;
  saving = false;
  error = '';
  consumersError = '';
  form: ToggleForm = this.emptyForm();
  consumers: ToggleConsumer[] = [];
  consumersPage = 0;
  readonly consumersPageSize = 20;
  consumersFirst = true;
  consumersLast = true;
  loaded = false;
  private toggleName = '';

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
    this.loaded = false;
    this.toggleName = name;

    this.toggleApi.getByName(name).pipe(
      timeout(10000),
      finalize(() => {
        this.loading = false;
        this.changeDetector.detectChanges();
      })
    ).subscribe({
      next: toggle => {
        this.form = this.formFromToggle(toggle);
        this.loaded = true;
        this.loadConsumersPage(name, 0);
      },
      error: err => {
        this.error = this.errorMessage(err);
        this.loaded = true;
      }
    });
  }

  loadConsumers(page = this.consumersPage): void {
    if (!this.toggleName) {
      return;
    }

    this.loadConsumersPage(this.toggleName, page);
  }

  previousConsumersPage(): void {
    if (!this.consumersFirst) {
      this.loadConsumersPage(this.toggleName, this.consumersPage - 1);
    }
  }

  nextConsumersPage(): void {
    if (!this.consumersLast) {
      this.loadConsumersPage(this.toggleName, this.consumersPage + 1);
    }
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

  trackByConsumer(_: number, consumer: ToggleConsumer): string {
    return `${consumer.serviceName}:${consumer.instanceId}:${consumer.consumeMode}`;
  }

  private loadConsumersPage(name: string, page: number): void {
    this.consumersLoading = true;
    this.consumersError = '';
    this.consumersPage = page;

    this.toggleApi.getConsumers(name, page, this.consumersPageSize).pipe(
      timeout(10000),
      finalize(() => {
        this.consumersLoading = false;
        this.changeDetector.detectChanges();
      })
    ).subscribe({
      next: response => this.applyConsumersResponse(response),
      error: err => {
        this.consumersError = this.errorMessage(err);
        this.consumers = [];
        this.consumersFirst = true;
        this.consumersLast = true;
      }
    });
  }

  private applyConsumersResponse(response: PagedToggleConsumerResponse): void {
    this.consumers = response.content;
    this.consumersPage = response.number;
    this.consumersFirst = response.first;
    this.consumersLast = response.last;
  }

  private errorMessage(err: unknown): string {
    if (typeof err === 'object' && err !== null && 'error' in err) {
      const body = (err as { error?: { detail?: string; title?: string } }).error;
      return body?.detail || body?.title || 'Request failed.';
    }
    return 'Request failed.';
  }
}
