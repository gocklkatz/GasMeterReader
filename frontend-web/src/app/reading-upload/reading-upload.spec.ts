import { TestBed, ComponentFixture } from '@angular/core/testing';
import { vi } from 'vitest';
import { of, throwError } from 'rxjs';
import { ReadingUploadComponent, FileEntry } from './reading-upload';
import { ReadingsService, Reading } from '../readings.service';

const mockReading: Reading = { id: 1, timestamp: '2026-02-17T08:22:00Z', imagePath: '2026/02/17/img.jpg' };

function makeEntry(overrides: Partial<FileEntry> = {}): FileEntry {
  return {
    file: new File(['img'], 'meter.jpg', { type: 'image/jpeg' }),
    timestamp: '2026-02-17T08:22',
    preview: null,
    loading: false,
    result: null,
    error: null,
    ...overrides,
  };
}

describe('ReadingUploadComponent', () => {
  let component: ReadingUploadComponent;
  let fixture: ComponentFixture<ReadingUploadComponent>;
  let readingsService: { createReading: ReturnType<typeof vi.fn> };

  beforeEach(async () => {
    readingsService = { createReading: vi.fn() };

    await TestBed.configureTestingModule({
      imports: [ReadingUploadComponent],
      providers: [{ provide: ReadingsService, useValue: readingsService }],
    }).compileComponents();

    fixture = TestBed.createComponent(ReadingUploadComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('starts with an empty entry list', () => {
    expect(component.entries()).toHaveLength(0);
  });

  describe('onFileSelected', () => {
    it('adds an image file to entries', () => {
      const file = new File(['img'], 'meter.jpg', { type: 'image/jpeg' });
      component.onFileSelected({ target: { files: [file], value: '' } } as unknown as Event);
      expect(component.entries()).toHaveLength(1);
      expect(component.entries()[0].file).toBe(file);
    });

    it('adds multiple files at once', () => {
      const f1 = new File(['img'], 'a.jpg', { type: 'image/jpeg' });
      const f2 = new File(['img'], 'b.jpg', { type: 'image/jpeg' });
      component.onFileSelected({ target: { files: [f1, f2], value: '' } } as unknown as Event);
      expect(component.entries()).toHaveLength(2);
    });

    it('ignores non-image files', () => {
      const file = new File(['data'], 'data.csv', { type: 'text/csv' });
      component.onFileSelected({ target: { files: [file], value: '' } } as unknown as Event);
      expect(component.entries()).toHaveLength(0);
    });

    it('clears already-uploaded entries when new files are added', () => {
      const done = makeEntry({ file: new File(['img'], 'old.jpg', { type: 'image/jpeg' }), result: mockReading });
      component.entries.set([done]);
      const newFile = new File(['img'], 'new.jpg', { type: 'image/jpeg' });
      component.onFileSelected({ target: { files: [newFile], value: '' } } as unknown as Event);
      expect(component.entries().find(e => e.file === done.file)).toBeUndefined();
      expect(component.entries()).toHaveLength(1);
      expect(component.entries()[0].file).toBe(newFile);
    });

    it('skips duplicate files', () => {
      const f1 = new File(['img'], 'meter.jpg', { type: 'image/jpeg' });
      const f2 = new File(['img'], 'meter.jpg', { type: 'image/jpeg' });
      component.onFileSelected({ target: { files: [f1], value: '' } } as unknown as Event);
      component.onFileSelected({ target: { files: [f2], value: '' } } as unknown as Event);
      expect(component.entries()).toHaveLength(1);
    });
  });

  describe('timestampFromFilename', () => {
    it('parses date and time from IMG_YYYYMMDD_HHMMSS filename', () => {
      const file = new File(['img'], 'IMG_20260217_082215.jpg', { type: 'image/jpeg' });
      component.onFileSelected({ target: { files: [file], value: '' } } as unknown as Event);
      expect(component.entries()[0].timestamp).toBe('2026-02-17T08:22');
    });

    it('falls back to current date when filename does not match', () => {
      const file = new File(['img'], 'photo.jpg', { type: 'image/jpeg' });
      const today = new Date().toISOString().slice(0, 10);
      component.onFileSelected({ target: { files: [file], value: '' } } as unknown as Event);
      expect(component.entries()[0].timestamp).toContain(today);
    });
  });

  describe('onDragOver', () => {
    it('calls preventDefault', () => {
      const event = { preventDefault: vi.fn() } as unknown as DragEvent;
      component.onDragOver(event);
      expect(event.preventDefault).toHaveBeenCalled();
    });
  });

  describe('onDrop', () => {
    it('adds image files from the drag event', () => {
      const file = new File(['img'], 'meter.jpg', { type: 'image/jpeg' });
      component.onDrop({ preventDefault: vi.fn(), dataTransfer: { files: [file] } } as unknown as DragEvent);
      expect(component.entries()).toHaveLength(1);
      expect(component.entries()[0].file).toBe(file);
    });

    it('ignores non-image files', () => {
      component.onDrop({
        preventDefault: vi.fn(),
        dataTransfer: { files: [new File(['data'], 'data.csv', { type: 'text/csv' })] },
      } as unknown as DragEvent);
      expect(component.entries()).toHaveLength(0);
    });

    it('accepts multiple image files simultaneously', () => {
      const files = [
        new File(['img'], 'a.jpg', { type: 'image/jpeg' }),
        new File(['img'], 'b.png', { type: 'image/png' }),
      ];
      component.onDrop({ preventDefault: vi.fn(), dataTransfer: { files } } as unknown as DragEvent);
      expect(component.entries()).toHaveLength(2);
    });
  });

  describe('removeEntry', () => {
    it('removes the targeted entry', () => {
      const e1 = makeEntry({ file: new File(['img'], 'a.jpg', { type: 'image/jpeg' }) });
      const e2 = makeEntry({ file: new File(['img'], 'b.jpg', { type: 'image/jpeg' }) });
      component.entries.set([e1, e2]);
      component.removeEntry(e1);
      expect(component.entries()).toHaveLength(1);
      expect(component.entries()[0].file).toBe(e2.file);
    });
  });

  describe('updateTimestamp', () => {
    it('updates only the timestamp of the given entry', () => {
      const entry = makeEntry();
      component.entries.set([entry]);
      component.updateTimestamp(entry, '2026-03-01T10:00');
      expect(component.entries()[0].timestamp).toBe('2026-03-01T10:00');
    });
  });

  describe('submitAll', () => {
    it('does nothing when entries list is empty', () => {
      component.submitAll();
      expect(readingsService.createReading).not.toHaveBeenCalled();
    });

    it('calls createReading for each pending entry with an ISO timestamp', () => {
      const e1 = makeEntry({ file: new File(['img'], 'a.jpg', { type: 'image/jpeg' }), timestamp: '2026-02-17T08:22' });
      const e2 = makeEntry({ file: new File(['img'], 'b.jpg', { type: 'image/jpeg' }), timestamp: '2026-02-18T09:00' });
      component.entries.set([e1, e2]);
      readingsService.createReading.mockReturnValue(of(mockReading));

      component.submitAll();

      expect(readingsService.createReading).toHaveBeenCalledTimes(2);
      expect(readingsService.createReading).toHaveBeenCalledWith(e1.file, new Date('2026-02-17T08:22').toISOString());
      expect(readingsService.createReading).toHaveBeenCalledWith(e2.file, new Date('2026-02-18T09:00').toISOString());
    });

    it('skips entries that already have a result', () => {
      const pending = makeEntry({ file: new File(['img'], 'a.jpg', { type: 'image/jpeg' }) });
      const done = makeEntry({ file: new File(['img'], 'b.jpg', { type: 'image/jpeg' }), result: mockReading });
      component.entries.set([pending, done]);
      readingsService.createReading.mockReturnValue(of(mockReading));

      component.submitAll();

      expect(readingsService.createReading).toHaveBeenCalledTimes(1);
      expect(readingsService.createReading).toHaveBeenCalledWith(pending.file, expect.any(String));
      expect(component.entries().find(e => e.file === done.file)).toBeDefined();
    });

    it('sets result on the correct entry after success', () => {
      const entry = makeEntry();
      component.entries.set([entry]);
      readingsService.createReading.mockReturnValue(of(mockReading));

      component.submitAll();

      expect(component.entries()[0].result).toEqual(mockReading);
      expect(component.entries()[0].loading).toBe(false);
    });

    it('sets error on the correct entry after failure', () => {
      const entry = makeEntry();
      component.entries.set([entry]);
      readingsService.createReading.mockReturnValue(throwError(() => new Error('Network error')));

      component.submitAll();

      expect(component.entries()[0].error).toBe('Network error');
      expect(component.entries()[0].loading).toBe(false);
    });

    it('sets loading to true while the request is in flight', () => {
      const entry = makeEntry();
      component.entries.set([entry]);
      let loadingDuringCall = false;
      readingsService.createReading.mockImplementation(() => {
        loadingDuringCall = component.entries()[0].loading;
        return of(mockReading);
      });

      component.submitAll();

      expect(loadingDuringCall).toBe(true);
      expect(component.entries()[0].loading).toBe(false);
    });
  });

  describe('computed signals', () => {
    it('hasPending is true when there are entries without results', () => {
      component.entries.set([makeEntry()]);
      expect(component.hasPending()).toBe(true);
    });

    it('hasPending is false when all entries have results', () => {
      component.entries.set([makeEntry({ result: mockReading })]);
      expect(component.hasPending()).toBe(false);
    });

    it('isAnyLoading is true when at least one entry is loading', () => {
      component.entries.set([makeEntry({ loading: true })]);
      expect(component.isAnyLoading()).toBe(true);
    });

    it('pendingCount reflects only entries without a result', () => {
      component.entries.set([
        makeEntry({ file: new File(['img'], 'a.jpg', { type: 'image/jpeg' }) }),
        makeEntry({ file: new File(['img'], 'b.jpg', { type: 'image/jpeg' }), result: mockReading }),
      ]);
      expect(component.pendingCount()).toBe(1);
    });
  });
});
