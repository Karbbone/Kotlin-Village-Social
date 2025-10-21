import {
  Column,
  CreateDateColumn,
  Entity,
  Index,
  JoinTable,
  ManyToMany,
  ManyToOne,
  OneToMany,
  PrimaryGeneratedColumn,
  UpdateDateColumn,
} from 'typeorm';
import { City } from './city.entity.js';
import { EventPhoto } from './event-photo.entity.js';
import { EventType } from './event-type.entity.js';
import { User } from './user.entity.js';

@Entity('events')
export class Event {
  @PrimaryGeneratedColumn()
  id!: number;

  @Column({ length: 200 })
  title!: string;

  @Column({ type: 'text' })
  description!: string;

  @Column({ length: 255 })
  location!: string;

  @Index()
  @Column({ type: 'datetime' })
  date!: Date;

  // Rattachements
  @ManyToOne(() => City, (city: City) => city.events, { nullable: false })
  city!: City;

  @ManyToOne(() => User, (user: User) => user.events, { nullable: false })
  creator!: User;

  // Types multiples
  @ManyToMany(() => EventType, (type: EventType) => type.events, {
    cascade: true,
  })
  @JoinTable({ name: 'events_types' })
  types!: Array<EventType>;

  // Photos multiples
  @OneToMany(() => EventPhoto, (photo: EventPhoto) => photo.event, {
    cascade: true,
  })
  photos!: Array<EventPhoto>;

  @CreateDateColumn()
  createdAt!: Date;

  @UpdateDateColumn()
  updatedAt!: Date;
}
