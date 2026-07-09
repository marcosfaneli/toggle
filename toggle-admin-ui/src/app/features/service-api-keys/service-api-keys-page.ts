import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { finalize, timeout } from 'rxjs';
import {
  CreatedServiceApiKey,
  ServiceApiKey,
  ServiceApiKeyApiService
} from '../../core/service-api-key-api.service';

@Component({
  selector: 'app-service-api-keys-page',
  imports: [CommonModule, FormsModule],
  templateUrl: './service-api-keys-page.html'
})
export class ServiceApiKeysPage implements OnInit {
  private readonly api = inject(ServiceApiKeyApiService);
  private readonly changeDetector = inject(ChangeDetectorRef);

  keys: ServiceApiKey[] = [];
  form = {
    serviceName: '',
    name: ''
  };
  loading = false;
  creating = false;
  error = '';
  createdKey: CreatedServiceApiKey | null = null;

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.error = '';

    this.api.list().pipe(
      timeout(10000),
      finalize(() => {
        this.loading = false;
        this.changeDetector.detectChanges();
      })
    ).subscribe({
      next: keys => this.keys = keys,
      error: err => this.error = this.errorMessage(err)
    });
  }

  create(): void {
    this.error = '';
    this.createdKey = null;

    if (!this.form.serviceName.trim() || !this.form.name.trim()) {
      this.error = 'Service and name are required.';
      return;
    }

    this.creating = true;
    this.api.create({
      serviceName: this.form.serviceName.trim(),
      name: this.form.name.trim()
    }).pipe(
      finalize(() => {
        this.creating = false;
        this.changeDetector.detectChanges();
      })
    ).subscribe({
      next: created => {
        this.createdKey = created;
        this.form = { serviceName: '', name: '' };
        this.load();
      },
      error: err => this.error = this.errorMessage(err)
    });
  }

  revoke(key: ServiceApiKey): void {
    this.error = '';
    this.api.revoke(key.id).subscribe({
      next: () => this.load(),
      error: err => this.error = this.errorMessage(err)
    });
  }

  trackById(_: number, key: ServiceApiKey): string {
    return key.id;
  }

  private errorMessage(err: unknown): string {
    if (typeof err === 'object' && err !== null && 'error' in err) {
      const body = (err as { error?: { detail?: string; title?: string } }).error;
      return body?.detail || body?.title || 'Request failed.';
    }
    return 'Request failed.';
  }
}
