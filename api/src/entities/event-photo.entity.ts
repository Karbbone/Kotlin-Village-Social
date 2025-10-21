import { Column, Entity, ManyToOne, PrimaryGeneratedColumn } from 'typeorm';
import { Event } from './event.entity.js';

@Entity('event_photos')
export class EventPhoto {
  @PrimaryGeneratedColumn()
  id!: number;

  @Column()
  url!: string;

  @ManyToOne(() => Event, (event: Event) => event.photos, {
    onDelete: 'CASCADE',
  })
  event!: Event;
}
