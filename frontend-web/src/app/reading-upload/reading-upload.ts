import { Component, signal, inject, computed } from '@angular/core';
import { ReadingsService, Reading } from '../readings.service';

export interface FileEntry {
  file: File;
  timestamp: string;
  preview: string | null;
  loading: boolean;
  result: Reading | null;
  error: string | null;
}

@Component({
  selector: 'app-reading-upload',
  templateUrl: './reading-upload.html',
  styleUrl: './reading-upload.scss'
})
export class ReadingUploadComponent {
  private readingsService = inject(ReadingsService);

  entries = signal<FileEntry[]>([]);

  hasPending = computed(() => this.entries().some(e => !e.loading && !e.result));
  isAnyLoading = computed(() => this.entries().some(e => e.loading));
  pendingCount = computed(() => this.entries().filter(e => !e.loading && !e.result).length);

  private fileKey(file: File): string {
    return `${file.name}-${file.size}-${file.lastModified}`;
  }

  private timestampFromFilename(filename: string): string {
    const match = filename.match(/(\d{4})(\d{2})(\d{2})_(\d{2})(\d{2})(\d{2})/);
    if (match) {
      const [, year, month, day, hour, minute] = match;
      return `${year}-${month}-${day}T${hour}:${minute}`;
    }
    return new Date().toISOString().slice(0, 16);
  }

  private addFiles(files: File[]): void {
    this.entries.update(prev => prev.filter(e => !e.result));

    const existingKeys = new Set(this.entries().map(e => this.fileKey(e.file)));
    const newEntries: FileEntry[] = files
      .filter(f => f.type.startsWith('image/') && !existingKeys.has(this.fileKey(f)))
      .map(file => ({
        file,
        timestamp: this.timestampFromFilename(file.name),
        preview: null,
        loading: false,
        result: null,
        error: null,
      }));

    if (newEntries.length === 0) return;

    this.entries.update(prev => [...prev, ...newEntries]);

    newEntries.forEach(entry => {
      const reader = new FileReader();
      reader.onload = () => {
        this.entries.update(prev =>
          prev.map(e => e.file === entry.file ? { ...e, preview: reader.result as string } : e)
        );
      };
      reader.readAsDataURL(entry.file);
    });
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.addFiles(Array.from(input.files ?? []));
    input.value = '';
  }

  onDragOver(event: DragEvent): void {
    event.preventDefault();
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    this.addFiles(Array.from(event.dataTransfer?.files ?? []));
  }

  updateTimestamp(entry: FileEntry, value: string): void {
    this.entries.update(prev =>
      prev.map(e => e.file === entry.file ? { ...e, timestamp: value } : e)
    );
  }

  removeEntry(entry: FileEntry): void {
    this.entries.update(prev => prev.filter(e => e.file !== entry.file));
  }

  submitAll(): void {
    this.entries()
      .filter(e => !e.loading && !e.result)
      .forEach(entry => {
        this.entries.update(prev =>
          prev.map(e => e.file === entry.file ? { ...e, loading: true, error: null } : e)
        );

        const isoTimestamp = new Date(entry.timestamp).toISOString();

        this.readingsService.createReading(entry.file, isoTimestamp).subscribe({
          next: reading => {
            this.entries.update(prev =>
              prev.map(e => e.file === entry.file ? { ...e, loading: false, result: reading } : e)
            );
          },
          error: err => {
            this.entries.update(prev =>
              prev.map(e => e.file === entry.file ? { ...e, loading: false, error: err.message ?? 'Upload failed' } : e)
            );
          }
        });
      });
  }
}
