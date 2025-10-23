import {
  Column,
  CreateDateColumn,
  Entity,
  JoinTable,
  ManyToMany,
  OneToMany,
  PrimaryGeneratedColumn,
  UpdateDateColumn,
} from 'typeorm';
import { City } from './city.entity.js';
import { Event } from './event.entity.js';

@Entity('users')
export class User {
  @PrimaryGeneratedColumn()
  id!: number;

  @Column({ unique: true })
  email!: string;

  @Column()
  passwordHash!: string;

  @Column({ length: 100 })
  displayName!: string;

  // Villes associées à l'utilisateur (Many-to-Many)
  @ManyToMany(() => City, (city: City) => city.users, { cascade: false })
  @JoinTable({
    name: 'users_cities',
    joinColumn: { name: 'userId', referencedColumnName: 'id' },
    inverseJoinColumn: { name: 'cityId', referencedColumnName: 'id' },
  })
  cities!: Array<City>;

  // Evénements créés par l'utilisateur
  @OneToMany(() => Event, (event: Event) => event.creator)
  events!: Array<Event>;

  @CreateDateColumn()
  createdAt!: Date;

  @UpdateDateColumn()
  updatedAt!: Date;
}
