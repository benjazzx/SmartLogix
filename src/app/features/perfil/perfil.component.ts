import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { catchError, of, switchMap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Usuario, Region, Comuna } from '../../shared/models/models';

@Component({
  selector: 'app-perfil',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './perfil.component.html',
})
export class PerfilComponent implements OnInit {
  usuario: Usuario | null = null;
  regiones: Region[] = [];
  comunas: Comuna[] = [];
  form!: FormGroup;
  guardando = false;
  guardadoExitoso = false;

  constructor(
    private readonly http: HttpClient,
    private readonly fb: FormBuilder,
    private readonly cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.initForm();
    this.http.get<Region[]>(environment.services.regiones).pipe(
      catchError(() => of([])),
    ).subscribe(r => { this.regiones = r; this.cdr.detectChanges(); });

    this.http.get<Usuario>(`${environment.services.usuarios}/me`).pipe(
      catchError(() => of(null)),
    ).subscribe(u => {
      this.usuario = u;
      if (u?.direccion) {
        this.prefillarDireccion(u);
      }
      this.cdr.detectChanges();
    });
  }

  private prefillarDireccion(u: Usuario): void {
    const d = u.direccion;
    if (!d) return;
    const regionId = d.comuna?.region?.id ?? '';
    this.form.patchValue({ regionId, calle: d.calle ?? '', numero: d.numero ?? '', codigoPostal: d.codigoPostal ?? '' });
    if (!regionId) return;
    this.http.get<Comuna[]>(`${environment.services.comunas}/por-region/${regionId}`).pipe(
      catchError(() => of([])),
    ).subscribe(c => {
      this.comunas = c;
      this.form.patchValue({ comunaId: d.comuna?.id ?? '' });
      this.cdr.detectChanges();
    });
  }

  private initForm(): void {
    this.form = this.fb.group({
      regionId:     ['', Validators.required],
      comunaId:     ['', Validators.required],
      calle:        ['', Validators.required],
      numero:       ['', Validators.required],
      codigoPostal: [''],
    });
  }

  onRegionChange(): void {
    const regionId = this.form.get('regionId')?.value;
    this.comunas = [];
    this.form.patchValue({ comunaId: '' });
    if (!regionId) return;
    this.http.get<Comuna[]>(`${environment.services.comunas}/por-region/${regionId}`).pipe(
      catchError(() => of([])),
    ).subscribe(c => { this.comunas = c; this.cdr.detectChanges(); });
  }

  onSubmit(): void {
    if (this.form.invalid || this.guardando) return;
    this.guardando = true;
    const v = this.form.value;
    const dirDto = { calle: v.calle, numero: v.numero, codigoPostal: v.codigoPostal || null, comunaId: v.comunaId };

    this.http.post<{ id: string }>(environment.services.direcciones, dirDto).pipe(
      catchError(() => of(null)),
      switchMap(dir => {
        if (!dir?.id) return of(null);
        return this.http.put<Usuario>(`${environment.services.usuarios}/me`, { direccionId: dir.id }).pipe(
          catchError(() => of(null)),
        );
      }),
    ).subscribe(result => {
      this.guardando = false;
      if (result) {
        this.usuario = result;
        this.guardadoExitoso = true;
        setTimeout(() => { this.guardadoExitoso = false; this.cdr.detectChanges(); }, 4000);
      }
      this.cdr.detectChanges();
    });
  }

  getDireccionTexto(): string {
    const d = this.usuario?.direccion;
    if (!d) return '—';
    const partes = [d.calle, d.numero ? `N°${d.numero}` : null, d.comuna?.nombre, d.comuna?.region?.nombre].filter(Boolean);
    return partes.join(', ');
  }

  getIniciales(u: Usuario | null): string {
    return ((u?.nombre?.[0] ?? '') + (u?.apellido?.[0] ?? '')).toUpperCase() || '?';
  }

  getRolBadge(rol?: string): string {
    const map: Record<string, string> = {
      admin: 'bg-red-100 text-red-800', bodeguero: 'bg-blue-100 text-blue-800',
      transportista: 'bg-yellow-100 text-yellow-800', cliente: 'bg-green-100 text-green-800',
    };
    return map[rol ?? ''] ?? 'bg-gray-100 text-gray-600';
  }
}
