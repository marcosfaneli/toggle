import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from './api-config';

export type ToggleValueType = 'STRING' | 'NUMBER';

export interface ToggleValue {
  type: ToggleValueType;
  raw: string;
}

export interface Toggle {
  id: string;
  name: string;
  maintainer: string;
  enabled: boolean;
  version: number;
  updatedAt: string;
  value?: ToggleValue | null;
}

export interface PagedToggleResponse {
  content: Toggle[];
  number: number;
  size: number;
  first: boolean;
  last: boolean;
}

export interface ToggleWriteRequest {
  maintainer?: string;
  enabled?: boolean;
  value?: ToggleValue | null;
}

export interface CreateToggleRequest extends ToggleWriteRequest {
  name: string;
  maintainer: string;
  enabled: boolean;
}

export interface ToggleFilters {
  maintainer?: string;
  enabled?: boolean;
  page?: number;
  size?: number;
}

@Injectable({ providedIn: 'root' })
export class ToggleApiService {
  private readonly http = inject(HttpClient);
  private readonly apiBaseUrl = inject(API_BASE_URL);

  list(filters: ToggleFilters): Observable<PagedToggleResponse> {
    let params = new HttpParams()
      .set('page', filters.page ?? 0)
      .set('size', filters.size ?? 20);

    if (filters.maintainer) {
      params = params.set('maintainer', filters.maintainer);
    }

    if (filters.enabled !== undefined) {
      params = params.set('enabled', filters.enabled);
    }

    return this.http.get<PagedToggleResponse>(`${this.apiBaseUrl}/toggles`, { params });
  }

  getByName(name: string): Observable<Toggle> {
    return this.http.get<Toggle>(`${this.apiBaseUrl}/toggles/${encodeURIComponent(name)}`);
  }

  create(request: CreateToggleRequest): Observable<Toggle> {
    return this.http.post<Toggle>(`${this.apiBaseUrl}/toggles`, request);
  }

  update(name: string, request: ToggleWriteRequest): Observable<Toggle> {
    return this.http.patch<Toggle>(`${this.apiBaseUrl}/toggles/${encodeURIComponent(name)}`, request);
  }
}
