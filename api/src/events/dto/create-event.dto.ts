export class CreateEventDto {
  title!: string;
  description!: string;
  location!: string;
  // ISO string date (e.g., 2025-11-01T18:00:00.000Z)
  date!: string;
  // Optional: array of EventType ids to attach
  typesIds?: number[];
  // Optional: array of photo URLs to create EventPhoto entries
  photoUrls?: string[];
}
