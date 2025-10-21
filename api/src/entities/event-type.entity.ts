import { Column, Entity, ManyToMany, PrimaryGeneratedColumn } from 'typeorm';
import { Event } from './event.entity.js';

@Entity('event_types')
export class EventType {
  @PrimaryGeneratedColumn()
  id!: number;

  @Column({ unique: true })
  name!: string;

  @ManyToMany(() => Event, (event: Event) => event.types)
  events!: Array<Event>;
}
