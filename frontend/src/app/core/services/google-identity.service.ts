import { Injectable } from '@angular/core';
import { environment } from '../../../environments/environment';

const SCRIPT_SRC = 'https://accounts.google.com/gsi/client';

interface GoogleAccountsId {
  initialize(config: { client_id: string; callback: (response: { credential: string }) => void }): void;
  renderButton(parent: HTMLElement, options: { theme: string; size: string; width?: number }): void;
}

declare const google: { accounts: { id: GoogleAccountsId } } | undefined;

/**
 * Thin wrapper around Google Identity Services (GIS) — loaded from Google's
 * CDN at runtime, not an npm package (there isn't an official one for GIS).
 */
@Injectable({ providedIn: 'root' })
export class GoogleIdentityService {
  private scriptLoadPromise: Promise<void> | null = null;

  async renderButton(container: HTMLElement, onCredential: (idToken: string) => void): Promise<void> {
    await this.loadScript();

    google!.accounts.id.initialize({
      client_id: environment.googleClientId,
      callback: (response) => onCredential(response.credential),
    });
    google!.accounts.id.renderButton(container, { theme: 'outline', size: 'large', width: 320 });
  }

  private loadScript(): Promise<void> {
    if (typeof google !== 'undefined') {
      return Promise.resolve();
    }

    if (!this.scriptLoadPromise) {
      this.scriptLoadPromise = new Promise((resolve, reject) => {
        const script = document.createElement('script');
        script.src = SCRIPT_SRC;
        script.async = true;
        script.defer = true;
        script.onload = () => resolve();
        script.onerror = () => reject(new Error('Falha ao carregar o Google Identity Services.'));
        document.head.appendChild(script);
      });
    }

    return this.scriptLoadPromise;
  }
}
