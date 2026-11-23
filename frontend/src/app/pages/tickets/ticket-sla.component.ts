import { DatePipe } from '@angular/common';
import { Component, computed, input } from '@angular/core';
import { SLA_STATE, slaText } from '../../core/format';
import { TicketSummary } from '../../core/models';

/**
 * The answer deadline of a ticket: a badge (on track, at risk, deadline missed or met), the time left or "breached by",
 * and the deadline itself. The text never depends on colour alone.
 */
@Component({
  selector: 'app-ticket-sla',
  imports: [DatePipe],
  template: `
    <span class="sla">
      @if (badge(); as b) { <span class="pill" [class]="b.cls">{{ b.label }}</span> }
      @if (text(); as t) { <span class="t" [class.bad]="late()">{{ t }}</span> }
      @else if (ticket().status === 'CLOSED' && !ticket().slaState) { <span class="muted small">Closed</span> }
    </span>
    @if (showDue()) { <div class="l2 muted small">Deadline {{ ticket().dueAt | date: 'd MMM, HH:mm' }}</div> }
  `,
})
export class TicketSlaComponent {
  readonly ticket = input.required<TicketSummary>();
  readonly showDue = input(true);

  readonly badge = computed(() => {
    const s = this.ticket().slaState;
    return s ? SLA_STATE[s] : null;
  });
  readonly late = computed(() => this.ticket().minutesLeft !== null && this.ticket().minutesLeft! < 0);
  readonly text = computed(() => {
    const t = this.ticket();
    return t.minutesLeft === null ? '' : slaText(t.minutesLeft);
  });
}
