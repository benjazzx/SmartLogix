import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { InventarioService } from './inventario.service';
import {
  Bodega, BodegaRequest,
  Pasillo, PasilloRequest,
  Estante, EstanteRequest,
  EstPasi, EstPasiRequest,
} from '../../shared/models/models';

@Component({
  selector: 'app-inventario',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule],
  templateUrl: './inventario.component.html',
})
export class InventarioComponent implements OnInit {
  activeTab: 'bodegas' | 'pasillos' | 'estantes' | 'estpasi' = 'bodegas';

  bodegas:   Bodega[]  = [];
  pasillos:  Pasillo[] = [];
  estantes:  Estante[] = [];
  estPasiList: EstPasi[] = [];

  showModal = false;
  modalTipo: 'bodega' | 'pasillo' | 'estante' | 'estpasi' = 'bodega';
  editandoEstPasiId: number | null = null;

  bodegaForm!:  FormGroup;
  pasilloForm!: FormGroup;
  estanteForm!: FormGroup;
  estPasiForm!: FormGroup;

  constructor(
    private inventarioService: InventarioService,
    private fb: FormBuilder,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.initForms();
    this.inventarioService.getBodegas().subscribe();
    this.inventarioService.getPasillos().subscribe();
    this.inventarioService.getEstantes().subscribe();
    this.inventarioService.getEstPasi().subscribe();

    this.inventarioService.bodegas$.subscribe(b   => { this.bodegas      = b;  this.cdr.detectChanges(); });
    this.inventarioService.pasillos$.subscribe(p  => { this.pasillos     = p;  this.cdr.detectChanges(); });
    this.inventarioService.estantes$.subscribe(e  => { this.estantes     = e;  this.cdr.detectChanges(); });
    this.inventarioService.estPasi$.subscribe(ep  => { this.estPasiList  = ep; this.cdr.detectChanges(); });
  }

  initForms(): void {
    this.bodegaForm = this.fb.group({
      nombre:        ['', Validators.required],
      direccion:     [''],
      ciudad:        [''],
      pais:          ['Chile'],
      capacidadTotal:[0, Validators.min(0)],
    });
    this.pasilloForm = this.fb.group({
      codigo:       ['', Validators.required],
      descripcion:  [''],
      numeroOrden:  [1, Validators.min(1)],
      idBodega:     [null, Validators.required],
    });
    this.estanteForm = this.fb.group({
      codigo:            ['', Validators.required],
      descripcion:       [''],
      numNiveles:        [1, [Validators.required, Validators.min(1)]],
      capacidadPorNivel: [0, Validators.min(0)],
      idPasillo:         [null, Validators.required],
    });
    this.estPasiForm = this.fb.group({
      idEstante:   [null, Validators.required],
      idPasillo:   [null, Validators.required],
      posicion:    [''],
      numeroFila:  [null],
      ocupacionPct:[null, [Validators.min(0), Validators.max(100)]],
      habilitada:  [true],
      observaciones:[''],
    });
  }

  openNewBodega():   void { this.modalTipo = 'bodega';   this.bodegaForm.reset({ pais: 'Chile', capacidadTotal: 0 }); this.showModal = true; }
  openNewPasillo():  void { this.modalTipo = 'pasillo';  this.pasilloForm.reset({ numeroOrden: 1 });                   this.showModal = true; }
  openNewEstante():  void { this.modalTipo = 'estante';  this.estanteForm.reset({ numNiveles: 1, capacidadPorNivel: 0 }); this.showModal = true; }
  openNewEstPasi():  void {
    this.modalTipo = 'estpasi';
    this.editandoEstPasiId = null;
    this.estPasiForm.reset({ habilitada: true });
    this.showModal = true;
  }
  openEditEstPasi(ep: EstPasi): void {
    this.modalTipo = 'estpasi';
    this.editandoEstPasiId = ep.idEstPasi ?? null;
    this.estPasiForm.patchValue({
      idEstante:    ep.idEstante,
      idPasillo:    ep.idPasillo,
      posicion:     ep.posicion ?? '',
      numeroFila:   ep.numeroFila ?? null,
      ocupacionPct: ep.ocupacionPct ?? null,
      habilitada:   ep.habilitada ?? true,
      observaciones:ep.observaciones ?? '',
    });
    this.showModal = true;
  }

  closeModal(): void {
    this.showModal = false;
    this.editandoEstPasiId = null;
  }

  onSubmitBodega(): void {
    if (this.bodegaForm.invalid) return;
    const dto: BodegaRequest = this.bodegaForm.value;
    this.inventarioService.createBodega(dto).subscribe();
    this.closeModal();
  }

  onSubmitPasillo(): void {
    if (this.pasilloForm.invalid) return;
    const dto: PasilloRequest = this.pasilloForm.value;
    this.inventarioService.createPasillo(dto).subscribe();
    this.closeModal();
  }

  onSubmitEstante(): void {
    if (this.estanteForm.invalid) return;
    const { idPasillo, ...rest } = this.estanteForm.value as { idPasillo: number } & EstanteRequest;
    const dto: EstanteRequest = rest;
    this.inventarioService.createEstante(dto).subscribe({
      next: created => {
        if (idPasillo && created?.idEstante) {
          this.inventarioService.createEstPasiLink(created.idEstante, idPasillo).subscribe();
        }
        this.closeModal();
      },
      error: () => this.closeModal(),
    });
  }

  onSubmitEstPasi(): void {
    if (this.estPasiForm.invalid) return;
    const v = this.estPasiForm.value;
    const dto: EstPasiRequest = {
      idEstante:    +v.idEstante,
      idPasillo:    +v.idPasillo,
      posicion:     v.posicion || undefined,
      numeroFila:   v.numeroFila ? +v.numeroFila : undefined,
      ocupacionPct: v.ocupacionPct != null ? +v.ocupacionPct : undefined,
      habilitada:   v.habilitada,
      observaciones:v.observaciones || undefined,
    };
    if (this.editandoEstPasiId != null) {
      this.inventarioService.updateEstPasi(this.editandoEstPasiId, dto).subscribe();
    } else {
      this.inventarioService.createEstPasi(dto).subscribe();
    }
    this.closeModal();
  }

  deleteBodega(id: number):   void { if (!confirm('¿Eliminar esta bodega?'))    return; this.inventarioService.deleteBodega(id).subscribe(); }
  deletePasillo(id: number):  void { if (!confirm('¿Eliminar este pasillo?'))   return; this.inventarioService.deletePasillo(id).subscribe(); }
  deleteEstante(id: number):  void { if (!confirm('¿Eliminar este estante?'))   return; this.inventarioService.deleteEstante(id).subscribe(); }
  deleteEstPasi(id: number):  void { if (!confirm('¿Eliminar esta asignación?'))return; this.inventarioService.deleteEstPasi(id).subscribe(); }

  toggleBodega(b: Bodega): void {
    if (b.idBodega == null) return;
    this.inventarioService.toggleBodega(b.idBodega).subscribe();
  }

  getPasillosDeBodega(idBodega: number): Pasillo[] {
    return this.pasillos.filter(p => p.idBodega === idBodega);
  }

  getOcupacionColor(pct?: number): string {
    if (pct == null) return 'text-gray-400';
    if (pct >= 80) return 'text-red-600';
    if (pct >= 50) return 'text-yellow-600';
    return 'text-green-600';
  }
}
