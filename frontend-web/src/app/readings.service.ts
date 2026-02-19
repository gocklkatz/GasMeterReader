import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Reading {
  id: number;
  timestamp: string;
  imagePath: string;
}

@Injectable({ providedIn: 'root' })
export class ReadingsService {
  private http = inject(HttpClient);
  private apiBase = 'http://localhost:8080';
  private baseUrl = `${this.apiBase}/readings`;

  createReading(image: File, timestamp: string): Observable<Reading> {
    const formData = new FormData();
    formData.append('image', image);
    formData.append('timestamp', timestamp);
    return this.http.post<Reading>(this.baseUrl, formData);
  }

  getAllReadings(): Observable<Reading[]> {
    return this.http.get<Reading[]>(this.baseUrl);
  }

  getImageUrl(imagePath: string): string {
    return `${this.apiBase}/images/${imagePath}`;
  }
}
