import { TestBed, ComponentFixture } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { vi } from 'vitest';
import { of, throwError, NEVER } from 'rxjs';
import { ReadingsBrowseComponent } from './readings-browse';
import { ReadingsService, Reading } from '../readings.service';

const mockReadings: Reading[] = [
  { id: 1, timestamp: '2026-02-17T08:22:00Z', imagePath: '2026/02/17/a.jpg' },
  { id: 2, timestamp: '2026-02-18T09:00:00Z', imagePath: '2026/02/18/b.jpg' },
];

describe('ReadingsBrowseComponent', () => {
  let component: ReadingsBrowseComponent;
  let fixture: ComponentFixture<ReadingsBrowseComponent>;
  let readingsService: { getAllReadings: ReturnType<typeof vi.fn>; getImageUrl: ReturnType<typeof vi.fn> };

  beforeEach(async () => {
    readingsService = {
      getAllReadings: vi.fn(),
      getImageUrl: vi.fn((path: string) => `http://localhost:8080/images/${path}`),
    };

    await TestBed.configureTestingModule({
      imports: [ReadingsBrowseComponent],
      providers: [
        { provide: ReadingsService, useValue: readingsService },
        provideRouter([]),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ReadingsBrowseComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    readingsService.getAllReadings.mockReturnValue(of([]));
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });

  it('starts in loading state before the request completes', () => {
    readingsService.getAllReadings.mockReturnValue(NEVER);
    fixture.detectChanges();
    expect(component.loading()).toBe(true);
    expect(component.error()).toBeNull();
  });

  it('populates readings and clears loading on success', () => {
    readingsService.getAllReadings.mockReturnValue(of(mockReadings));
    fixture.detectChanges();
    expect(component.readings()).toEqual(mockReadings);
    expect(component.loading()).toBe(false);
  });

  it('sets error and clears loading on failure', () => {
    readingsService.getAllReadings.mockReturnValue(throwError(() => new Error('Network error')));
    fixture.detectChanges();
    expect(component.error()).toBe('Network error');
    expect(component.loading()).toBe(false);
  });

  it('has an empty readings list when the API returns none', () => {
    readingsService.getAllReadings.mockReturnValue(of([]));
    fixture.detectChanges();
    expect(component.readings()).toHaveLength(0);
  });

  it('imageUrl delegates to the service', () => {
    readingsService.getAllReadings.mockReturnValue(of([]));
    fixture.detectChanges();
    const url = component.imageUrl('2026/02/17/a.jpg');
    expect(readingsService.getImageUrl).toHaveBeenCalledWith('2026/02/17/a.jpg');
    expect(url).toBe('http://localhost:8080/images/2026/02/17/a.jpg');
  });

  it('calls getAllReadings on init', () => {
    readingsService.getAllReadings.mockReturnValue(of([]));
    fixture.detectChanges();
    expect(readingsService.getAllReadings).toHaveBeenCalledTimes(1);
  });
});
