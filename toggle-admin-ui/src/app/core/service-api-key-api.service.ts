import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from './api-config';

export interface ServiceApiKey {
  id: string;
  serviceName: string;
  name: string;
  status: 'ACTIVE' | 'REVOKED';
  createdAt: string;
  lastUsedAt?: string | null;
  revokedAt?: string | null;
}

export interface CreateServiceApiKeyRequest {
  serviceName: string;
  name: string;
}

export interface CreatedServiceApiKey {
  id: string;
  serviceName: string;
  name: string;
  apiKey: string;
  createdAt: string;
}

@Injectable({ providedIn: 'root' })
export class ServiceApiKeyApiService {
  private readonly http = inject(HttpClient);
  private readonly apiBaseUrl = inject(API_BASE_URL);

  list(): Observable<ServiceApiKey[]> {
    return this.http.get<ServiceApiKey[]>(`${this.apiBaseUrl}/service-api-keys`);
  }

  create(request: CreateServiceApiKeyRequest): Observable<CreatedServiceApiKey> {
    return this.http.post<CreatedServiceApiKey>(`${this.apiBaseUrl}/service-api-keys`, request);
  }

  revoke(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiBaseUrl}/service-api-keys/${encodeURIComponent(id)}`);
  }
}
