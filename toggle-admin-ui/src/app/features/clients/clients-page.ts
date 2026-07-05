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
  page = 0;
  readonly pageSize = 20;
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
        this.clampPage();
        this.loaded = true;
      },
      error: err => {
        this.error = this.errorMessage(err);
        this.loaded = true;
      }
    });
  }

  applyFilters(): void {
    this.page = 0;
    this.loadClients();
  }

  resetFilters(): void {
    this.serviceName = '';
    this.selectedStatus = 'ALL';
    this.applyFilters();
  }

  nextPage(): void {
    if (!this.last) {
      this.page += 1;
    }
  }

  previousPage(): void {
    if (!this.first) {
      this.page -= 1;
    }
  }

  get pagedClients(): ClientInstance[] {
    const start = this.page * this.pageSize;
    return this.clients.slice(start, start + this.pageSize);
  }

  get first(): boolean {
    return this.page === 0;
  }

  get last(): boolean {
    return this.page >= this.totalPages - 1;
  }

  get totalPages(): number {
    return Math.max(Math.ceil(this.clients.length / this.pageSize), 1);
  }

  get visibleStart(): number {
    return this.clients.length === 0 ? 0 : this.page * this.pageSize + 1;
  }

  get visibleEnd(): number {
    return Math.min((this.page + 1) * this.pageSize, this.clients.length);
  }

  trackByPublicId(_: number, client: ClientInstance): string {
    return client.publicId;
  }

  private clampPage(): void {
    if (this.page >= this.totalPages) {
      this.page = this.totalPages - 1;
    }
  }

  private errorMessage(err: unknown): string {
    if (typeof err === 'object' && err !== null && 'error' in err) {
      const body = (err as { error?: { detail?: string; title?: string } }).error;
      return body?.detail || body?.title || 'Request failed.';
    }
    return 'Request failed.';
  }
}
