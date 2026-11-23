import { Component } from '@angular/core';
import { ICON } from '../../core/nav-icons';

/** The page used by sign-in, register and confirm-email: a branded panel on the left, the form card on the right. */
@Component({
  selector: 'app-auth-layout',
  template: `
    <div class="login">
      <section class="login-brand">
        <div class="brand">
          <span class="mk" aria-hidden="true">
            <svg class="ico" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
              <path [attr.d]="icons.layers" />
            </svg>
          </span>
          <div><b>ITZETech Group</b><small>Digital Project Delivery</small></div>
        </div>
        <div>
          <h2>From first request to a website that stays healthy.</h2>
          <p>One place for requests, quotes, projects, invoices and care of your websites after they go live.</p>
        </div>
        <ul>
          <li><span class="fi" aria-hidden="true"><svg class="ico" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path [attr.d]="icons.check" /></svg></span>Clients follow their project and approve work</li>
          <li><span class="fi" aria-hidden="true"><svg class="ico" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path [attr.d]="icons.clock" /></svg></span>Developers see their tasks and log their hours</li>
          <li><span class="fi" aria-hidden="true"><svg class="ico" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path [attr.d]="icons.reports" /></svg></span>Managers and administrators see the whole delivery</li>
        </ul>
        <div class="login-foot">ITZETech Group · Digital Project Delivery Management System</div>
      </section>
      <section class="login-main">
        <div class="auth-card">
          <ng-content />
        </div>
      </section>
    </div>
  `,
})
export class AuthLayoutComponent {
  protected readonly icons = ICON;
}
