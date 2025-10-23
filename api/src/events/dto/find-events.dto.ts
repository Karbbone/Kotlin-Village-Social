export class FindEventsDto {
  city?: string;
  // upcomingOnly expected as string in query params, controller will coerce. Default true
  upcomingOnly?: boolean = true;
  sort?: 'asc' | 'desc' = 'asc';
  offset?: number = 0;
  limit?: number = 20;
}
