export class EventResponseDto {
  id!: number;
  title!: string;
  description!: string;
  location!: string;
  date!: string;
  city!: string;
  creator!: { id: number; displayName: string };
  type!: string;
  photos!: Array<{ id: number; url?: string }>;
  createdAt!: string;
}
