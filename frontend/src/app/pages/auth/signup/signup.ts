import { HttpErrorResponse } from '@angular/common/http';
import { AfterViewInit, Component, ElementRef, inject, signal, viewChild } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { LucideLock, LucideMail, LucideUser } from '@lucide/angular';
import { AuthService } from '../../../core/services/auth.service';
import { GoogleIdentityService } from '../../../core/services/google-identity.service';
import { NotificationService } from '../../../core/services/notification.service';
import { ProblemDetail } from '../../../core/models/problem-detail.model';
import { AuthContainer } from '../../../shared/components/auth-container/auth-container';

@Component({
  selector: 'app-signup',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, LucideUser, LucideMail, LucideLock, AuthContainer],
  templateUrl: './signup.html',
  styleUrl: './signup.scss',
})
export class Signup implements AfterViewInit {
  private readonly formBuilder = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly googleIdentityService = inject(GoogleIdentityService);
  private readonly notificationService = inject(NotificationService);
  private readonly router = inject(Router);

  private readonly googleButton = viewChild<ElementRef<HTMLElement>>('googleButton');

  protected readonly form = this.formBuilder.group({
    fullName: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8)]],
  });

  protected readonly submitting = signal(false);
  protected readonly errorMessage = signal<string | null>(null);

  ngAfterViewInit(): void {
    const container = this.googleButton()?.nativeElement;
    if (!container) {
      return;
    }

    this.googleIdentityService
      .renderButton(container, (idToken) => this.onGoogleCredential(idToken))
      .catch(() => this.errorMessage.set('Unable to load Google Sign-In. Please try again.'));
  }

  protected onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const { fullName, email, password } = this.form.getRawValue();
    this.errorMessage.set(null);
    this.submitting.set(true);

    this.authService.register({ nome: fullName!, email: email!, senha: password! }).subscribe({
      next: () => {
        this.notificationService.success('Account created successfully! Log in to continue.');
        this.router.navigateByUrl('/login');
      },
      error: (error: HttpErrorResponse) => {
        const problem = error.error as ProblemDetail | null;
        this.errorMessage.set(problem?.detail ?? '');
        this.submitting.set(false);
      },
    });
  }

  private onGoogleCredential(idToken: string): void {
    this.errorMessage.set(null);
    this.authService.loginWithGoogle(idToken).subscribe({
      next: () => this.router.navigateByUrl('/'),
      error: (error: HttpErrorResponse) => {
        const problem = error.error as ProblemDetail | null;
        this.errorMessage.set(problem?.detail ?? 'Unable to create the account. Please try again.');
        this.submitting.set(false);
      },
    });
  }
}
