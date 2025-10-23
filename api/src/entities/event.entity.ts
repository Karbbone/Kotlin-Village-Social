import {
  Column,
  CreateDateColumn,
  Entity,
  Index,
  ManyToOne,
  OneToMany,
  PrimaryGeneratedColumn,
  UpdateDateColumn,
} from 'typeorm';
import { EventPhoto } from './event-photo.entity.js';
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

  // Nom de la ville directement stocké, plus de relation à une entité City
  @Index('IDX_events_city')
  @Column({ length: 255 })
  city!: string;

  @ManyToOne(() => User, (user: User) => user.events, { nullable: false })
  creator!: User;

  // Type d'événement stocké en clair (pas de relation)
  @Index('IDX_events_type')
  @Column({ length: 100 })
  type!: string;

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
