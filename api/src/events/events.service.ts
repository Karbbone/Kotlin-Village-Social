import {
  ConflictException,
  ForbiddenException,
  Injectable,
  NotFoundException,
} from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { In, Repository } from 'typeorm';
import { City } from '../entities/city.entity.js';
import { EventType } from '../entities/event-type.entity.js';
import { Event } from '../entities/event.entity.js';
import { User } from '../entities/user.entity.js';

@Injectable()
export class EventsService {
  constructor(
    @InjectRepository(Event)
    private readonly eventRepo: Repository<Event>,
    @InjectRepository(User)
    private readonly userRepo: Repository<User>,
    @InjectRepository(City)
    private readonly cityRepo: Repository<City>,
    @InjectRepository(EventType)
    private readonly typeRepo: Repository<EventType>,
  ) {}

  async findAll(opts: {
    cityId?: number;
    upcomingOnly?: boolean;
    sort?: 'asc' | 'desc';
    limit?: number;
    offset?: number;
  }) {
    const qb = this.eventRepo
      .createQueryBuilder('event')
      .leftJoinAndSelect('event.city', 'city')
      .leftJoinAndSelect('event.creator', 'creator')
      .leftJoinAndSelect('event.types', 'types')
      .leftJoinAndSelect('event.photos', 'photos');

    if (opts.cityId) {
      qb.andWhere('city.id = :cityId', { cityId: opts.cityId });
    }

    if (opts.upcomingOnly !== false) {
      qb.andWhere('event.date >= :now', { now: new Date().toISOString() });
    }

    qb.orderBy('event.date', opts.sort === 'desc' ? 'DESC' : 'ASC');

    if (typeof opts.offset === 'number') qb.skip(opts.offset);
    if (typeof opts.limit === 'number') qb.take(opts.limit);

    return qb.getMany();
  }

  async findForUserAttachedCities(
    userId: number,
    opts: {
      upcomingOnly?: boolean;
      sort?: 'asc' | 'desc';
      limit?: number;
      offset?: number;
    },
  ) {
    const user = await this.userRepo.findOne({
      where: { id: userId },
      relations: ['attachedCities'],
    });
    if (!user) throw new NotFoundException('User not found');

    const cityIds = user.attachedCities?.map((c) => c.id) ?? [];
    if (cityIds.length === 0) return [];

    const qb = this.eventRepo
      .createQueryBuilder('event')
      .leftJoinAndSelect('event.city', 'city')
      .leftJoinAndSelect('event.creator', 'creator')
      .leftJoinAndSelect('event.types', 'types')
      .leftJoinAndSelect('event.photos', 'photos')
      .andWhere('city.id IN (:...ids)', { ids: cityIds });

    if (opts.upcomingOnly !== false) {
      qb.andWhere('event.date >= :now', { now: new Date().toISOString() });
    }

    qb.orderBy('event.date', opts.sort === 'desc' ? 'DESC' : 'ASC');
    if (typeof opts.offset === 'number') qb.skip(opts.offset);
    if (typeof opts.limit === 'number') qb.take(opts.limit);

    return qb.getMany();
  }

  async findByUser(userId: number, requesterId?: number, includePast = false) {
    const qb = this.eventRepo
      .createQueryBuilder('event')
      .leftJoinAndSelect('event.city', 'city')
      .leftJoinAndSelect('event.creator', 'creator')
      .leftJoinAndSelect('event.types', 'types')
      .leftJoinAndSelect('event.photos', 'photos')
      .andWhere('creator.id = :userId', { userId });

    if (!includePast || requesterId !== userId) {
      qb.andWhere('event.date >= :now', { now: new Date().toISOString() });
    }

    qb.orderBy('event.date', 'ASC');
    return qb.getMany();
  }

  async findOne(id: number, requesterId?: number) {
    const ev = await this.eventRepo.findOne({
      where: { id },
      relations: ['city', 'creator', 'types', 'photos'],
    });
    if (!ev) throw new NotFoundException('Event not found');

    const now = new Date();
    if (ev.date < now && requesterId !== ev.creator.id) {
      throw new ForbiddenException('Past event: access restricted to creator');
    }

    return ev;
  }

  async createInCity(
    cityId: number,
    creatorUserId: number,
    payload: {
      title: string;
      description: string;
      location: string;
      date: string; // ISO string
      typesIds?: number[];
      photoUrls?: string[];
    },
  ) {
    const city = await this.cityRepo.findOne({ where: { id: cityId } });
    if (!city) throw new NotFoundException('City not found');

    const creator = await this.userRepo.findOne({
      where: { id: creatorUserId },
    });
    if (!creator) throw new NotFoundException('User not found');

    const date = new Date(payload.date);
    if (Number.isNaN(date.getTime())) {
      throw new NotFoundException('Invalid date');
    }

    // Load event types if provided
    let types: EventType[] = [];
    if (payload.typesIds && payload.typesIds.length > 0) {
      types = await this.typeRepo.find({ where: { id: In(payload.typesIds) } });
    }

    // Prepare photos if provided (cascading save from Event -> EventPhoto)
    const photos = (payload.photoUrls ?? []).map((url) => ({ url }));

    const ev = this.eventRepo.create({
      title: payload.title,
      description: payload.description,
      location: payload.location,
      date,
      city,
      creator,
      types,
      photos,
    });
    const saved = await this.eventRepo.save(ev);
    return this.findOne(saved.id, creatorUserId);
  }

  async findPastByCreator(userId: number) {
    const qb = this.eventRepo
      .createQueryBuilder('event')
      .leftJoinAndSelect('event.city', 'city')
      .leftJoinAndSelect('event.creator', 'creator')
      .leftJoinAndSelect('event.types', 'types')
      .leftJoinAndSelect('event.photos', 'photos')
      .andWhere('creator.id = :userId', { userId })
      .andWhere('event.date < :now', { now: new Date().toISOString() })
      .orderBy('event.date', 'DESC');

    return qb.getMany();
  }

  async createType(name: string) {
    const exists = await this.typeRepo.findOne({ where: { name } });
    if (exists) {
      throw new ConflictException('Event type already exists');
    }
    const type = this.typeRepo.create({ name });
    return this.typeRepo.save(type);
  }

  async findAllTypes() {
    return this.typeRepo.find({ order: { name: 'ASC' } });
  }
}
