import {
  Column,
  CreateDateColumn,
  Entity,
  ManyToMany,
  OneToMany,
  PrimaryGeneratedColumn,
  UpdateDateColumn,
} from 'typeorm';
import { Event } from './event.entity.js';
import { User } from './user.entity.js';

@Entity('cities')
export class City {
  @PrimaryGeneratedColumn()
  id!: number;

  @Column({ unique: true })
  name!: string;

  // Utilisateurs rattachés à la ville
  @ManyToMany(() => User, (user: User) => user.attachedCities)
  followers!: Array<User>;

  // Evénements dans cette ville
  @OneToMany(() => Event, (event: Event) => event.city)
  events!: Array<Event>;

  @CreateDateColumn()
  createdAt!: Date;

  @UpdateDateColumn()
  updatedAt!: Date;
}
