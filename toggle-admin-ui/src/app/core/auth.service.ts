import { Injectable } from '@angular/core';

export type AuthRole = 'ADMIN' | 'MAINTAINER' | 'VIEWER';

interface AuthConfig {
  mode: 'none' | 'oidc';
  issuer?: string;
  clientId?: string;
  redirectUri?: string;
  postLogoutRedirectUri?: string;
  scope?: string;
}

interface TokenSet {
  accessToken: string;
  idToken?: string;
  refreshToken?: string;
  expiresAt: number;
}

interface JwtClaims {
  name?: string;
  preferred_username?: string;
  email?: string;
  realm_access?: {
    roles?: string[];
  };
  resource_access?: Record<string, {
    roles?: string[];
  }>;
}

const TOKEN_STORAGE_KEY = 'switchboard_auth_tokens';
const STATE_STORAGE_KEY = 'switchboard_auth_state';
const VERIFIER_STORAGE_KEY = 'switchboard_auth_code_verifier';
const RETURN_URL_STORAGE_KEY = 'switchboard_auth_return_url';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private config: AuthConfig = { mode: 'none' };
  private tokens: TokenSet | null = null;
  private claims: JwtClaims | null = null;

  async initialize(): Promise<void> {
    this.config = await this.loadConfig();
    this.tokens = this.readTokens();
    this.claims = this.tokens ? decodeClaims(this.tokens.accessToken) : null;

    if (!this.requiresAuth()) {
      return;
    }

    if (this.hasAuthorizationCode()) {
      await this.completeLogin();
      return;
    }

    if (this.tokens && this.isExpired(this.tokens)) {
      await this.refreshOrClear();
    }
  }

  requiresAuth(): boolean {
    return this.config.mode === 'oidc';
  }

  isAuthenticated(): boolean {
    return !this.requiresAuth() || (!!this.tokens && !this.isExpired(this.tokens));
  }

  accessToken(): string | null {
    if (!this.tokens || this.isExpired(this.tokens)) {
      return null;
    }
    return this.tokens.accessToken;
  }

  displayName(): string {
    return this.claims?.name
      || this.claims?.preferred_username
      || this.claims?.email
      || 'Authenticated user';
  }

  roles(): AuthRole[] {
    if (!this.requiresAuth()) {
      return ['ADMIN', 'MAINTAINER', 'VIEWER'];
    }

    const rawRoles = new Set<string>([
      ...(this.claims?.realm_access?.roles ?? []),
      ...(this.config.clientId ? this.claims?.resource_access?.[this.config.clientId]?.roles ?? [] : [])
    ]);
    const roles: AuthRole[] = [];

    if (rawRoles.has('switchboard-admin')) {
      roles.push('ADMIN');
    }
    if (rawRoles.has('switchboard-maintainer')) {
      roles.push('MAINTAINER');
    }
    if (rawRoles.has('switchboard-viewer')) {
      roles.push('VIEWER');
    }

    return roles;
  }

  hasAnyRole(roles: AuthRole[]): boolean {
    if (!this.requiresAuth()) {
      return true;
    }

    const userRoles = new Set(this.roles());
    return roles.some(role => userRoles.has(role));
  }

  login(returnUrl = location.pathname + location.search): void {
    if (!this.requiresAuth()) {
      return;
    }

    void this.redirectToAuthorizationEndpoint(returnUrl);
  }

  logout(): void {
    const idToken = this.tokens?.idToken;
    this.clearTokens();

    if (!this.requiresAuth()) {
      location.assign('/');
      return;
    }

    const logoutUrl = new URL(`${this.issuer()}/protocol/openid-connect/logout`);
    logoutUrl.searchParams.set('post_logout_redirect_uri', this.postLogoutRedirectUri());
    if (idToken) {
      logoutUrl.searchParams.set('id_token_hint', idToken);
    }
    location.assign(logoutUrl.toString());
  }

  private async loadConfig(): Promise<AuthConfig> {
    try {
      const response = await fetch('/auth-config.json', { cache: 'no-store' });
      if (!response.ok) {
        return { mode: 'none' };
      }
      return await response.json() as AuthConfig;
    } catch {
      return { mode: 'none' };
    }
  }

  private hasAuthorizationCode(): boolean {
    const params = new URLSearchParams(location.search);
    return params.has('code') && params.has('state');
  }

  private async completeLogin(): Promise<void> {
    const params = new URLSearchParams(location.search);
    const state = params.get('state');
    const expectedState = sessionStorage.getItem(STATE_STORAGE_KEY);
    const verifier = sessionStorage.getItem(VERIFIER_STORAGE_KEY);

    if (!state || state !== expectedState || !verifier) {
      this.clearLoginState();
      this.clearTokens();
      throw new Error('Invalid OIDC callback state.');
    }

    const code = params.get('code');
    if (!code) {
      throw new Error('Missing OIDC authorization code.');
    }

    const body = new URLSearchParams({
      grant_type: 'authorization_code',
      client_id: this.clientId(),
      code,
      redirect_uri: this.redirectUri(),
      code_verifier: verifier
    });

    const tokenResponse = await this.requestTokens(body);
    this.storeTokenResponse(tokenResponse);

    const returnUrl = sessionStorage.getItem(RETURN_URL_STORAGE_KEY) || '/toggles';
    this.clearLoginState();
    history.replaceState(null, '', returnUrl);
  }

  private async redirectToAuthorizationEndpoint(returnUrl: string): Promise<void> {
    const state = randomString();
    const verifier = randomString();
    const challenge = await pkceChallenge(verifier);

    sessionStorage.setItem(STATE_STORAGE_KEY, state);
    sessionStorage.setItem(VERIFIER_STORAGE_KEY, verifier);
    sessionStorage.setItem(RETURN_URL_STORAGE_KEY, returnUrl);

    const authorizeUrl = new URL(`${this.issuer()}/protocol/openid-connect/auth`);
    authorizeUrl.searchParams.set('response_type', 'code');
    authorizeUrl.searchParams.set('client_id', this.clientId());
    authorizeUrl.searchParams.set('redirect_uri', this.redirectUri());
    authorizeUrl.searchParams.set('scope', this.config.scope || 'openid profile email');
    authorizeUrl.searchParams.set('state', state);
    authorizeUrl.searchParams.set('code_challenge', challenge);
    authorizeUrl.searchParams.set('code_challenge_method', 'S256');

    location.assign(authorizeUrl.toString());
  }

  private async refreshOrClear(): Promise<void> {
    if (!this.tokens?.refreshToken) {
      this.clearTokens();
      return;
    }

    try {
      const body = new URLSearchParams({
        grant_type: 'refresh_token',
        client_id: this.clientId(),
        refresh_token: this.tokens.refreshToken
      });
      const tokenResponse = await this.requestTokens(body);
      this.storeTokenResponse(tokenResponse);
    } catch {
      this.clearTokens();
    }
  }

  private async requestTokens(body: URLSearchParams): Promise<Record<string, unknown>> {
    const response = await fetch(`${this.issuer()}/protocol/openid-connect/token`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded'
      },
      body
    });

    if (!response.ok) {
      throw new Error('OIDC token request failed.');
    }

    return await response.json() as Record<string, unknown>;
  }

  private storeTokenResponse(response: Record<string, unknown>): void {
    const accessToken = String(response['access_token'] ?? '');
    if (!accessToken) {
      throw new Error('OIDC response did not include an access token.');
    }

    const expiresIn = Number(response['expires_in'] ?? 300);
    this.tokens = {
      accessToken,
      idToken: response['id_token'] ? String(response['id_token']) : undefined,
      refreshToken: response['refresh_token'] ? String(response['refresh_token']) : undefined,
      expiresAt: Date.now() + Math.max(expiresIn - 30, 1) * 1000
    };
    this.claims = decodeClaims(accessToken);
    localStorage.setItem(TOKEN_STORAGE_KEY, JSON.stringify(this.tokens));
  }

  private readTokens(): TokenSet | null {
    const value = localStorage.getItem(TOKEN_STORAGE_KEY);
    if (!value) {
      return null;
    }

    try {
      return JSON.parse(value) as TokenSet;
    } catch {
      return null;
    }
  }

  private isExpired(tokens: TokenSet): boolean {
    return Date.now() >= tokens.expiresAt;
  }

  private clearTokens(): void {
    this.tokens = null;
    this.claims = null;
    localStorage.removeItem(TOKEN_STORAGE_KEY);
  }

  private clearLoginState(): void {
    sessionStorage.removeItem(STATE_STORAGE_KEY);
    sessionStorage.removeItem(VERIFIER_STORAGE_KEY);
    sessionStorage.removeItem(RETURN_URL_STORAGE_KEY);
  }

  private issuer(): string {
    if (!this.config.issuer) {
      throw new Error('OIDC issuer is not configured.');
    }
    return this.config.issuer.replace(/\/$/, '');
  }

  private clientId(): string {
    if (!this.config.clientId) {
      throw new Error('OIDC clientId is not configured.');
    }
    return this.config.clientId;
  }

  private redirectUri(): string {
    return this.config.redirectUri || `${location.origin}/`;
  }

  private postLogoutRedirectUri(): string {
    return this.config.postLogoutRedirectUri || `${location.origin}/`;
  }
}

function decodeClaims(token: string): JwtClaims | null {
  const parts = token.split('.');
  if (parts.length < 2) {
    return null;
  }

  try {
    const payload = parts[1].replace(/-/g, '+').replace(/_/g, '/');
    const json = decodeURIComponent(escape(atob(payload.padEnd(Math.ceil(payload.length / 4) * 4, '='))));
    return JSON.parse(json) as JwtClaims;
  } catch {
    return null;
  }
}

function randomString(): string {
  const bytes = new Uint8Array(32);
  crypto.getRandomValues(bytes);
  return base64Url(bytes);
}

async function pkceChallenge(verifier: string): Promise<string> {
  const data = new TextEncoder().encode(verifier);
  const digest = await crypto.subtle.digest('SHA-256', data);
  return base64Url(new Uint8Array(digest));
}

function base64Url(bytes: Uint8Array): string {
  let value = '';
  for (const byte of bytes) {
    value += String.fromCharCode(byte);
  }
  return btoa(value)
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=+$/, '');
}
