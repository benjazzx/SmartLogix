import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

function passwordMatch(control: AbstractControl): ValidationErrors | null {
  const a = control.get('nuevaClave')?.value;
  const b = control.get('confirmar')?.value;
  return a && b && a !== b ? { noCoinciden: true } : null;
}

@Component({
  selector: 'app-recuperar',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './recuperar.component.html',
})
export class RecuperarComponent {
  paso: 1 | 2 | 3 = 1;
  correoValidado = '';
  rutValidado = '';

  validarForm: FormGroup;
  claveForm: FormGroup;

  loading = false;
  error = '';
  showNueva = false;
  showConfirmar = false;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router,
  ) {
    this.validarForm = this.fb.group({
      correo: ['', [Validators.required, Validators.email]],
      rut:    ['', [Validators.required, Validators.minLength(7), Validators.maxLength(12)]],
    });

    this.claveForm = this.fb.group({
      nuevaClave: ['', [Validators.required, Validators.minLength(6)]],
      confirmar:  ['', Validators.required],
    }, { validators: passwordMatch });
  }

  onValidar(): void {
    if (this.validarForm.invalid) return;
    this.loading = true;
    this.error = '';
    const { correo, rut } = this.validarForm.value;
    this.authService.validarIdentidad(correo, rut).subscribe({
      next: (res) => {
        this.loading = false;
        if (res.valido) {
          this.correoValidado = correo;
          this.rutValidado = rut;
          this.paso = 2;
        } else {
          this.error = 'No se encontró una cuenta con ese correo y RUT. Verifica los datos.';
        }
      },
      error: () => {
        this.loading = false;
        this.error = 'Error al conectar con el servidor. Intenta nuevamente.';
      },
    });
  }

  onCambiarClave(): void {
    if (this.claveForm.invalid) return;
    this.loading = true;
    this.error = '';
    this.authService.cambiarClave(this.correoValidado, this.rutValidado, this.claveForm.value.nuevaClave).subscribe({
      next: () => {
        this.loading = false;
        this.paso = 3;
      },
      error: () => {
        this.loading = false;
        this.error = 'No se pudo actualizar la contraseña. Intenta nuevamente.';
      },
    });
  }
}
