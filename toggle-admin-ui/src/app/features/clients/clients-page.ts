import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ClientApiService, ClientInstance, ClientStatus } from '../../core/client-api.service';

type StatusFilter = 'ALL' | ClientStatus;

@Component({
  selector: 'app-clients-page',
  imports: [CommonModule, FormsModule],
  templateUrl: './clients-page.html'
})
export class ClientsPage implements OnInit {
  private readonly clientApi = inject(ClientApiService);

  clients: ClientInstance[] = [];
  serviceName = '';
  selectedStatus: StatusFilter = 'ALL';
  loading = false;
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
    }).subscribe({
      next: clients => {
        this.clients = clients;
        this.loading = false;
      },
      error: err => {
        this.error = this.errorMessage(err);
        this.loading = false;
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
