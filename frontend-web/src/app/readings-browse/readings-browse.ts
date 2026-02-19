import { Component, OnInit, signal, inject } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ReadingsService, Reading } from '../readings.service';

@Component({
  selector: 'app-readings-browse',
  imports: [DatePipe, RouterLink],
  templateUrl: './readings-browse.html',
  styleUrl: './readings-browse.scss'
})
export class ReadingsBrowseComponent implements OnInit {
  private readingsService = inject(ReadingsService);

  readings = signal<Reading[]>([]);
  loading = signal(true);
  error = signal<string | null>(null);

  ngOnInit(): void {
    this.readingsService.getAllReadings().subscribe({
      next: readings => {
        this.readings.set(readings);
        this.loading.set(false);
      },
      error: err => {
        this.error.set(err.message ?? 'Failed to load readings');
        this.loading.set(false);
      }
    });
  }

  imageUrl(imagePath: string): string {
    return this.readingsService.getImageUrl(imagePath);
  }
}
