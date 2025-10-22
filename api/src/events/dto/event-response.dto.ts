export class EventResponseDto {
  id!: number;
  title!: string;
  description!: string;
  location!: string;
  date!: string;
  city!: { id: number; name: string };
  creator!: { id: number; displayName: string };
  types!: Array<{ id: number; label: string }>;
  photos!: Array<{ id: number; url?: string }>;
  createdAt!: string;
}
