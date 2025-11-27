import {
  ConflictException,
  Injectable,
  NotFoundException,
} from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { EventType } from '../entities/event-type.entity.js';

@Injectable()
export class EventTypesService {
  constructor(
    @InjectRepository(EventType)
    private readonly eventTypeRepo: Repository<EventType>,
  ) {}

  /**
   * Récupérer tous les types d'événements disponibles
   */
  async findAll(): Promise<EventType[]> {
    return this.eventTypeRepo.find({
      order: { name: 'ASC' },
    });
  }

  /**
   * Créer un nouveau type d'événement
   */
  async create(name: string): Promise<EventType> {
    // Vérifier si le type existe déjà
    const existing = await this.eventTypeRepo.findOne({ where: { name } });
    if (existing) {
      throw new ConflictException(
        `Event type "${name}" already exists in the system`,
      );
    }

    const eventType = this.eventTypeRepo.create({ name });
    return this.eventTypeRepo.save(eventType);
  }

  /**
   * Supprimer un type d'événement
   */
  async delete(name: string): Promise<void> {
    const eventType = await this.eventTypeRepo.findOne({ where: { name } });
    if (!eventType) {
      throw new NotFoundException(`Event type "${name}" not found`);
    }

    await this.eventTypeRepo.remove(eventType);
  }
}
