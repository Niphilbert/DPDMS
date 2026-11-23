import { Injectable, signal } from '@angular/core';

/** Short messages at the bottom of the screen ("Saved", "Account deactivated"). */
@Injectable({ providedIn: 'root' })
export class ToastService {
  readonly messages = signal<{ id: number; text: string }[]>([]);
  private next = 1;

  show(text: string, ms = 3500): void {
    const id = this.next++;
    this.messages.update((m) => [...m, { id, text }]);
    setTimeout(() => this.messages.update((m) => m.filter((x) => x.id !== id)), ms);
  }
}
