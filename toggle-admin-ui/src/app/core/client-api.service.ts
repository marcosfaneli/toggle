import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from './api-config';

export type ClientStatus = 'ACTIVE' | 'INACTIVE';

export interface ClientSubscription {
  toggleName: string;
  consumeMode: 'LOCAL_CACHE' | 'REMOTE_ALWAYS';
}

export interface ClientInstance {
  publicId: string;
  serviceName: string;
  instanceId: string;
  podName: string;
  namespace: string;
  callbackUrl: string;
  status: ClientStatus;
  registeredAt: string;
  subscriptions: ClientSubscription[];
}

export interface ClientFilters {
  serviceName?: string;
  status?: ClientStatus;
}

@Injectable({ providedIn: 'root' })
export class ClientApiService {
  private readonly http = inject(HttpClient);
  private readonly apiBaseUrl = inject(API_BASE_URL);

  list(filters: ClientFilters): Observable<ClientInstance[]> {
    let params = new HttpParams();

    if (filters.serviceName) {
      params = params.set('serviceName', filters.serviceName);
    }

    if (filters.status) {
      params = params.set('status', filters.status);
    }

    return this.http.get<ClientInstance[]>(`${this.apiBaseUrl}/clients`, { params });
  }
}
