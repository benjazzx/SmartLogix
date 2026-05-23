import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { catchError, of } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Estado, TipoDeEstado } from '../../shared/models/models';

@Component({
  selector: 'app-estados',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './estados.component.html',
})
export class EstadosComponent implements OnInit {
  estados: Estado[] = [];
  tiposDeEstado: TipoDeEstado[] = [];
  activeTab: 'estados' | 'tipos' = 'estados';
  showModal = false;
  form!: FormGroup;

  private readonly estadosUrl    = environment.services.estados;
  private readonly tiposUrl      = environment.services.tiposEstado;

  private mockTipos: TipoDeEstado[] = [
    { id: 'tipo-1', nombre: 'Usuario',   descripcion: 'Estados del ciclo de vida del usuario'   },
    { id: 'tipo-2', nombre: 'Producto',  descripcion: 'Estados del ciclo de vida del producto'  },
    { id: 'tipo-3', nombre: 'Orden',     descripcion: 'Estados del ciclo de vida de la orden'   },
    { id: 'tipo-4', nombre: 'Inventario',descripcion: 'Estados del ciclo de vida del inventario'},
  ];

  private mockEstados: Estado[] = [
    { id: 'est-1', nombre: 'Activo',      descripcion: 'Entidad activa y operativa',       tipoDeEstado: this.mockTipos[0] },
    { id: 'est-2', nombre: 'Inactivo',    descripcion: 'Entidad desactivada temporalmente', tipoDeEstado: this.mockTipos[0] },
    { id: 'est-3', nombre: 'Pendiente',   descripcion: 'En espera de procesamiento',        tipoDeEstado: this.mockTipos[2] },
    { id: 'est-4', nombre: 'Procesando',  descripcion: 'En preparación',                   tipoDeEstado: this.mockTipos[2] },
    { id: 'est-5', nombre: 'Aprobado',    descripcion: 'Listo para despacho',               tipoDeEstado: this.mockTipos[2] },
    { id: 'est-6', nombre: 'En tránsito', descripcion: 'En camino al destino',              tipoDeEstado: this.mockTipos[2] },
    { id: 'est-7', nombre: 'Entregado',   descripcion: 'Entregado satisfactoriamente',      tipoDeEstado: this.mockTipos[2] },
    { id: 'est-8', nombre: 'Cancelado',   descripcion: 'Cancelado por cliente o sistema',   tipoDeEstado: this.mockTipos[2] },
  ];

  constructor(private http: HttpClient, private fb: FormBuilder, private cdr: ChangeDetectorRef) {}

  ngOnInit(): void {
    this.initForm();
    this.http.get<TipoDeEstado[]>(this.tiposUrl).pipe(
      catchError(() => of(this.mockTipos)),
    ).subscribe(t => { this.tiposDeEstado = t; this.cdr.detectChanges(); });

    this.http.get<Estado[]>(this.estadosUrl).pipe(
      catchError(() => of(this.mockEstados)),
    ).subscribe(e => { this.estados = e; this.cdr.detectChanges(); });
  }

  initForm(): void {
    this.form = this.fb.group({
      nombre:      ['', Validators.required],
      descripcion: [''],
      tipoId:      ['', Validators.required],
    });
  }

  openNew(): void { this.initForm(); this.showModal = true; }
  closeModal(): void { this.showModal = false; }

  onSubmit(): void {
    if (this.form.invalid) return;
    const v = this.form.value;
    this.http.post<Estado>(this.estadosUrl, { nombre: v.nombre, descripcion: v.descripcion, tipoId: v.tipoId }).pipe(
      catchError(() => {
        const tipo = this.tiposDeEstado.find(t => t.id === v.tipoId);
        return of({ id: `est-${Date.now()}`, nombre: v.nombre, descripcion: v.descripcion, tipoDeEstado: tipo } as Estado);
      }),
    ).subscribe(created => {
      this.estados = [...this.estados, created];
      this.closeModal();
      this.cdr.detectChanges();
    });
  }

  getEstadosByTipo(tipoId: string): Estado[] {
    return this.estados.filter(e => e.tipoDeEstado?.id === tipoId);
  }
}
