import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ReadingsService } from './readings.service';

describe('ReadingsService', () => {
  let service: ReadingsService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(ReadingsService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('createReading', () => {
    it('POSTs to /readings with FormData containing image and timestamp', () => {
      const file = new File(['content'], 'meter.jpg', { type: 'image/jpeg' });
      const timestamp = '2026-02-17T08:22:15.000Z';
      const mockReading = { id: 1, timestamp, imagePath: '2026/02/17/meter.jpg' };

      service.createReading(file, timestamp).subscribe((r) => {
        expect(r).toEqual(mockReading);
      });

      const req = httpTesting.expectOne('http://localhost:8080/readings');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toBeInstanceOf(FormData);
      expect(req.request.body.get('image')).toBe(file);
      expect(req.request.body.get('timestamp')).toBe(timestamp);
      req.flush(mockReading);
    });

    it('propagates HTTP error responses to the caller', () => {
      const file = new File(['content'], 'meter.jpg', { type: 'image/jpeg' });
      let caughtError: unknown;

      service.createReading(file, '2026-02-17T08:22:15.000Z').subscribe({
        error: (e) => (caughtError = e),
      });

      const req = httpTesting.expectOne('http://localhost:8080/readings');
      req.flush('Internal Server Error', { status: 500, statusText: 'Internal Server Error' });

      expect(caughtError).toBeTruthy();
    });
  });

  describe('getAllReadings', () => {
    it('GETs from /readings and returns the list', () => {
      const mockReadings = [
        { id: 1, timestamp: '2026-02-17T08:22:00Z', imagePath: '2026/02/17/a.jpg' },
        { id: 2, timestamp: '2026-02-18T09:00:00Z', imagePath: '2026/02/18/b.jpg' },
      ];

      service.getAllReadings().subscribe((r) => {
        expect(r).toEqual(mockReadings);
      });

      const req = httpTesting.expectOne('http://localhost:8080/readings');
      expect(req.request.method).toBe('GET');
      req.flush(mockReadings);
    });

    it('propagates HTTP error responses to the caller', () => {
      let caughtError: unknown;

      service.getAllReadings().subscribe({ error: (e) => (caughtError = e) });

      const req = httpTesting.expectOne('http://localhost:8080/readings');
      req.flush('Not Found', { status: 404, statusText: 'Not Found' });

      expect(caughtError).toBeTruthy();
    });
  });

  describe('getImageUrl', () => {
    it('constructs the correct image URL from a relative path', () => {
      expect(service.getImageUrl('2026/02/17/abc.jpg')).toBe(
        'http://localhost:8080/images/2026/02/17/abc.jpg',
      );
    });
  });
});
