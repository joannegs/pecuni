import { Injectable, signal } from '@angular/core';

export type NotificationType = 'success' | 'error';

export interface Notification {
  id: number;
  type: NotificationType;
  message: string;
}

const DEFAULT_DURATION_MS = 4000;

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private nextId = 0;

  readonly notifications = signal<Notification[]>([]);

  success(message: string, durationMs = DEFAULT_DURATION_MS): void {
    this.show('success', message, durationMs);
  }

  error(message: string, durationMs = DEFAULT_DURATION_MS): void {
    this.show('error', message, durationMs);
  }

  dismiss(id: number): void {
    this.notifications.update((current) => current.filter((notification) => notification.id !== id));
  }

  private show(type: NotificationType, message: string, durationMs: number): void {
    const id = this.nextId++;
    this.notifications.update((current) => [...current, { id, type, message }]);
    setTimeout(() => this.dismiss(id), durationMs);
  }
}
