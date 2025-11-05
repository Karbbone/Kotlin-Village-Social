import { Entity, ManyToMany, PrimaryColumn } from 'typeorm';
import { Event } from './event.entity.js';

@Entity('event_types')
export class EventType {
  @PrimaryColumn({ length: 100 })
  name!: string;

  @ManyToMany((): typeof Event => Event, (event: Event) => event.types)
  events!: Array<Event>;
}
