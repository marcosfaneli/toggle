import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ClientApiService, ClientInstance, ClientStatus } from '../../core/client-api.service';
import { finalize, timeout } from 'rxjs';

type StatusFilter = 'ALL' | ClientStatus;

@Component({
  selector: 'app-clients-page',
  imports: [CommonModule, FormsModule],
  templateUrl: './clients-page.html'
})
export class ClientsPage implements OnInit {
  private readonly clientApi = inject(ClientApiService);
  private readonly changeDetector = inject(ChangeDetectorRef);

  clients: ClientInstance[] = [];
  serviceName = '';
  selectedStatus: StatusFilter = 'ALL';
  loading = false;
  loaded = false;
  error = '';

  ngOnInit(): void {
    this.loadClients();
  }

  loadClients(): void {
    this.loading = true;
    this.error = '';
    this.clientApi.list({
      serviceName: this.serviceName.trim() || undefined,
      status: this.selectedStatus === 'ALL' ? undefined : this.selectedStatus
    }).pipe(
      timeout(10000),
      finalize(() => {
        this.loading = false;
        this.changeDetector.detectChanges();
      })
    ).subscribe({
      next: clients => {
        this.clients = clients;
        this.loaded = true;
      },
      error: err => {
        this.error = this.errorMessage(err);
        this.loaded = true;
      }
    });
  }

  resetFilters(): void {
    this.serviceName = '';
    this.selectedStatus = 'ALL';
    this.loadClients();
  }

  trackByPublicId(_: number, client: ClientInstance): string {
    return client.publicId;
  }

  private errorMessage(err: unknown): string {
    if (typeof err === 'object' && err !== null && 'error' in err) {
      const body = (err as { error?: { detail?: string; title?: string } }).error;
      return body?.detail || body?.title || 'Request failed.';
    }
    return 'Request failed.';
  }
}
