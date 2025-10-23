export class CreateEventDto {
  title!: string;
  description!: string;
  location!: string;
  // ISO string date (e.g., 2025-11-01T18:00:00.000Z)
  date!: string;
  // Nom de la ville (si vous préférez l'envoyer dans le body plutôt que dans l'URL)
  city?: string;
  // Type d'événement (ex: "Sport", "Concert")
  type!: string;
  // Optional: array of photo URLs to create EventPhoto entries
  photoUrls?: string[];
}
