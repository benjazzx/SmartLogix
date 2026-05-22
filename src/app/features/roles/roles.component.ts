import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RolService } from '../usuarios/usuario.service';
import { Rol } from '../../shared/models/models';

@Component({
  selector: 'app-roles',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './roles.component.html',
})
export class RolesComponent implements OnInit {
  roles: Rol[] = [];

  constructor(private rolService: RolService, private cdr: ChangeDetectorRef) {}

  ngOnInit(): void {
    this.rolService.getAll().subscribe();
    this.rolService.roles$.subscribe(r => {
      this.roles = r;
      this.cdr.markForCheck();
    });
  }
}
