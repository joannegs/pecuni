import { HttpErrorResponse } from '@angular/common/http';
import { AfterViewInit, Component, ElementRef, inject, signal, viewChild } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { LucideLock, LucideMail } from '@lucide/angular';
import { AuthService } from '../../../core/services/auth.service';
import { GoogleIdentityService } from '../../../core/services/google-identity.service';
import { ProblemDetail } from '../../../core/models/problem-detail.model';
import { AuthContainer } from '../../../shared/components/auth-container/auth-container';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, LucideMail, LucideLock, AuthContainer],
  templateUrl: './login.html',
  styleUrl: './login.scss',
})
export class Login implements AfterViewInit {
  private readonly formBuilder = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly googleIdentityService = inject(GoogleIdentityService);
  private readonly router = inject(Router);

  private readonly googleButton = viewChild<ElementRef<HTMLElement>>('googleButton');

  protected readonly form = this.formBuilder.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', Validators.required],
    rememberMe: [false],
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
      .catch(() => this.errorMessage.set('Não foi possível carregar o login com Google.'));
  }

  protected onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const { email, password } = this.form.getRawValue();
    this.errorMessage.set(null);
    this.submitting.set(true);

    this.authService.login({ email: email!, senha: password! }).subscribe({
      next: () => this.router.navigateByUrl('/'),
      error: (error: HttpErrorResponse) => this.handleAuthError(error),
    });
  }

  private onGoogleCredential(idToken: string): void {
    this.errorMessage.set(null);
    this.authService.loginWithGoogle(idToken).subscribe({
      next: () => this.router.navigateByUrl('/'),
      error: (error: HttpErrorResponse) => this.handleAuthError(error),
    });
  }

  private handleAuthError(error: HttpErrorResponse): void {
    const problem = error.error as ProblemDetail | null;
    this.errorMessage.set(problem?.detail ?? 'Não foi possível entrar. Tente novamente.');
    this.submitting.set(false);
  }
}
