export class CreateEventDto {
  title!: string;
  description!: string;
  location!: string;
  // ISO string date (e.g., 2025-11-01T18:00:00.000Z)
  date!: string;
  // Nom de la ville (si vous préférez l'envoyer dans le body plutôt que dans l'URL)
  city?: string;
  // Types d'événement (ex: ["Sport", "Concert"])
  types?: string[];
}
