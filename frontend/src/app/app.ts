import { Component, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { ToastService } from './core/toast.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  template: `
    <router-outlet />
    <div class="toasts" aria-live="polite">
      @for (t of toasts.messages(); track t.id) { <div class="toast">{{ t.text }}</div> }
    </div>
  `,
})
export class App {
  protected readonly toasts = inject(ToastService);
}
