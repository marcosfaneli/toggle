import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { finalize, timeout } from 'rxjs';
import { Toggle, ToggleApiService } from '../../core/toggle-api.service';

type EnabledFilter = 'ALL' | 'ENABLED' | 'DISABLED';

@Component({
  selector: 'app-toggles-page',
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './toggles-page.html'
})
export class TogglesPage implements OnInit {
  private readonly toggleApi = inject(ToggleApiService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly changeDetector = inject(ChangeDetectorRef);

  toggles: Toggle[] = [];
  selectedMaintainer = '';
  selectedEnabled: EnabledFilter = 'ALL';
  page = 0;
  readonly pageSize = 20;
  first = true;
  last = true;
  loading = false;
  loaded = false;
  error = '';
  notice = '';

  ngOnInit(): void {
    this.route.queryParamMap.subscribe(() => this.loadToggles(0));
  }

  loadToggles(page = this.page): void {
    this.loading = true;
    this.error = '';
    this.page = page;

    this.toggleApi.list({
      maintainer: this.selectedMaintainer || undefined,
      enabled: this.enabledFilterValue(),
      page: this.page,
      size: this.pageSize
    }).pipe(
      timeout(10000),
      finalize(() => {
        this.loading = false;
        this.changeDetector.detectChanges();
      })
    ).subscribe({
      next: response => {
        this.toggles = response.content;
        this.first = response.first;
        this.last = response.last;
        this.page = response.number;
        this.loaded = true;
      },
      error: err => {
        this.error = this.errorMessage(err);
        this.loaded = true;
      }
    });
  }

  applyFilters(): void {
    this.loadToggles(0);
  }

  resetFilters(): void {
    this.selectedMaintainer = '';
    this.selectedEnabled = 'ALL';
    this.loadToggles(0);
  }

  nextPage(): void {
    if (!this.last) {
      this.loadToggles(this.page + 1);
    }
  }

  previousPage(): void {
    if (!this.first) {
      this.loadToggles(this.page - 1);
    }
  }

  edit(toggle: Toggle): void {
    this.router.navigate(['/toggles', toggle.name, 'edit']);
  }

  valueLabel(toggle: Toggle): string {
    return toggle.value ? `${toggle.value.type}: ${toggle.value.raw}` : 'None';
  }

  trackByName(_: number, toggle: Toggle): string {
    return toggle.name;
  }

  private enabledFilterValue(): boolean | undefined {
    if (this.selectedEnabled === 'ENABLED') {
      return true;
    }
    if (this.selectedEnabled === 'DISABLED') {
      return false;
    }
    return undefined;
  }

  private errorMessage(err: unknown): string {
    if (typeof err === 'object' && err !== null && 'error' in err) {
      const body = (err as { error?: { detail?: string; title?: string } }).error;
      return body?.detail || body?.title || 'Request failed.';
    }
    return 'Request failed.';
  }
}
