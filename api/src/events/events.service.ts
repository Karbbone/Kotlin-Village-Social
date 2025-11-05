import {
  ForbiddenException,
  Injectable,
  NotFoundException,
} from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { Event } from '../entities/event.entity.js';
import { User } from '../entities/user.entity.js';

@Injectable()
export class EventsService {
  constructor(
    @InjectRepository(Event)
    private readonly eventRepo: Repository<Event>,
    @InjectRepository(User)
    private readonly userRepo: Repository<User>,
  ) {}

  async findAll(opts: {
    city?: string;
    upcomingOnly?: boolean;
    sort?: 'asc' | 'desc';
    limit?: number;
    offset?: number;
  }) {
    const qb = this.eventRepo
      .createQueryBuilder('event')
      .leftJoinAndSelect('event.creator', 'creator')
      .leftJoinAndSelect('event.photos', 'photos');

    if (opts.city) {
      qb.andWhere('event.city = :city', { city: opts.city });
    }

    if (opts.upcomingOnly !== false) {
      qb.andWhere('event.date >= :now', { now: new Date().toISOString() });
    }

    qb.orderBy('event.date', opts.sort === 'desc' ? 'DESC' : 'ASC');

    if (typeof opts.offset === 'number') qb.skip(opts.offset);
    if (typeof opts.limit === 'number') qb.take(opts.limit);

    return qb.getMany();
  }

  // Méthode supprimée: plus de villes rattachées à l'utilisateur

  async findByUser(userId: number, requesterId?: number, includePast = false) {
    const qb = this.eventRepo
      .createQueryBuilder('event')
      .leftJoinAndSelect('event.creator', 'creator')
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
      relations: ['creator', 'photos'],
    });
    if (!ev) throw new NotFoundException('Event not found');

    const now = new Date();
    if (ev.date < now && requesterId !== ev.creator.id) {
      throw new ForbiddenException('Past event: access restricted to creator');
    }

    return ev;
  }

  async createInCity(
    cityName: string,
    creatorUserId: number,
    payload: {
      title: string;
      description: string;
      location: string;
      date: string; // ISO string
      type: string;
      photoUrls?: string[];
    },
  ) {
    const creator = await this.userRepo.findOne({
      where: { id: creatorUserId },
    });
    if (!creator) throw new NotFoundException('User not found');

    const date = new Date(payload.date);
    if (Number.isNaN(date.getTime())) {
      throw new NotFoundException('Invalid date');
    }

    // Prepare photos if provided (cascading save from Event -> EventPhoto)
    const photos = (payload.photoUrls ?? []).map((url) => ({ url }));

    const ev = this.eventRepo.create({
      title: payload.title,
      description: payload.description,
      location: payload.location,
      date,
      city: cityName,
      type: payload.type,
      creator,
      photos,
    });
    const saved = await this.eventRepo.save(ev);
    return this.findOne(saved.id, creatorUserId);
  }

  async findPastByCreator(userId: number) {
    const qb = this.eventRepo
      .createQueryBuilder('event')
      .leftJoinAndSelect('event.photos', 'photos')
      .leftJoinAndSelect('event.creator', 'creator')
      .select([
        'event', // toutes les colonnes de l’event
        'photos', // id + url des photos
        'creator.id', // uniquement ces 2 colonnes du creator
        'creator.displayName',
      ])
      .andWhere('creator.id = :userId', { userId })
      .andWhere('event.date < :now', { now: new Date().toISOString() })
      .orderBy('event.date', 'DESC');

    return qb.getMany();
  }

  /**
   * Recherche par paramètres optionnels dans l'URL: /events/:cityName?/:type?
   * - Si aucun paramètre n'est fourni, on retourne les 5 derniers événements créés
   * - Sinon on filtre sur city et/ou type et on retourne tous les résultats triés par date DESC
   */
  async findByCityAndTypeOptional(cityName?: string, type?: string) {
    const qb = this.eventRepo
      .createQueryBuilder('event')
      .leftJoinAndSelect('event.photos', 'photos')
      .leftJoinAndSelect('event.creator', 'creator')
      .select(['event', 'photos', 'creator.id', 'creator.displayName']);

    if (cityName) {
      qb.andWhere('event.city = :city', { city: cityName });
    }
    if (type) {
      qb.andWhere('event.type = :type', { type });
    }

    if (cityName || type) {
      qb.orderBy('event.date', 'DESC');
      return qb.getMany();
    }

    qb.orderBy('event.createdAt', 'DESC').take(5);
    return qb.getMany();
  }

  // Plus de gestion de types (entité supprimée)
}
