import {
  ForbiddenException,
  Injectable,
  NotFoundException,
} from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { In, Repository } from 'typeorm';
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
    @InjectRepository(EventType)
    private readonly eventTypeRepo: Repository<EventType>,
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
    // Inclure les types dans les résultats
    qb.leftJoinAndSelect('event.types', 'types');
    // Limiter les colonnes du creator pour ne retourner que id + displayName
    qb.select([
      'event',
      'photos',
      'creator.id',
      'creator.displayName',
      'types',
    ]);

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
    // Étape 1: récupérer uniquement les métadonnées nécessaires (date et creatorId)
    const meta = await this.eventRepo
      .createQueryBuilder('event')
      .leftJoin('event.creator', 'creator')
      .where('event.id = :id', { id })
      .select('event.id', 'event_id')
      .addSelect('event.date', 'event_date')
      .addSelect('creator.id', 'creator_id')
      .getRawOne<{
        event_id?: number;
        event_date?: Date;
        creator_id?: number;
      }>();

    if (!meta) throw new NotFoundException('Event not found');

    const now = new Date();
    const eventDate = new Date(meta.event_date as unknown as string);
    const creatorId = meta.creator_id;
    if (eventDate < now && requesterId !== creatorId) {
      throw new ForbiddenException('Past event: access restricted to creator');
    }

    // Étape 2: retourner l'événement sans inclure la relation 'creator'
    const ev = await this.eventRepo.findOne({
      where: { id },
      relations: ['photos', 'types'],
    });
    if (!ev) throw new NotFoundException('Event not found');
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
      types?: string[];
      type?: string; // compat
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

    // Normaliser la liste des types
    const fromArray = Array.isArray(payload.types) ? payload.types : [];
    const fromSingle = payload.type ? [payload.type] : [];
    const rawTypes = fromArray.length > 0 ? fromArray : fromSingle;
    const typeNames = Array.from(
      new Set(rawTypes.map((t) => t.trim()).filter((t) => t.length > 0)),
    );

    // Récupérer les types existants
    const existing = typeNames.length
      ? await this.eventTypeRepo.find({ where: { name: In(typeNames) } })
      : [];
    const existingNames = new Set(existing.map((e) => e.name));
    const toCreate = typeNames
      .filter((n) => !existingNames.has(n))
      .map((name) => this.eventTypeRepo.create({ name }));
    const created = toCreate.length
      ? await this.eventTypeRepo.save(toCreate)
      : [];
    const allTypes = [...existing, ...created];

    const ev = this.eventRepo.create({
      title: payload.title,
      description: payload.description,
      location: payload.location,
      date,
      city: cityName,
      creator,
      photos,
      types: allTypes,
    });
    const saved = await this.eventRepo.save(ev);
    return this.findOne(saved.id, creatorUserId);
  }

  async findPastByCreator(userId: number) {
    const qb = this.eventRepo
      .createQueryBuilder('event')
      .leftJoinAndSelect('event.photos', 'photos')
      .leftJoinAndSelect('event.creator', 'creator')
      .leftJoinAndSelect('event.types', 'types')
      .select([
        'event', // toutes les colonnes de l’event
        'photos', // id + url des photos
        'creator.id', // uniquement ces 2 colonnes du creator
        'creator.displayName',
        'types',
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
  async findByCityAndTypeOptional(filters: {
    cityName?: string;
    types?: string[];
    type?: string;
  }) {
    const qb = this.eventRepo
      .createQueryBuilder('event')
      .leftJoinAndSelect('event.photos', 'photos')
      .leftJoinAndSelect('event.creator', 'creator')
      .leftJoinAndSelect('event.types', 'types')
      .select([
        'event',
        'photos',
        'creator.id',
        'creator.displayName',
        'types',
      ]);

    const { cityName, types, type } = filters ?? {};

    if (cityName) {
      qb.andWhere('event.city = :city', { city: cityName });
    }

    // Construire la liste de noms de types à filtrer (ANY)
    let baseTypes: string[] = [];
    if (Array.isArray(types)) {
      baseTypes = types;
    } else if (type) {
      baseTypes = [type];
    }
    const names = Array.from(
      new Set(baseTypes.map((t) => t.trim()).filter((t) => t.length > 0)),
    );

    if (names.length > 0) {
      qb.andWhere('types.name IN (:...names)', { names }).distinct(true);
    }

    if (cityName || names.length > 0) {
      qb.orderBy('event.date', 'DESC');
      return qb.getMany();
    }

    qb.orderBy('event.createdAt', 'DESC').take(5);
    return qb.getMany();
  }

  // Plus de gestion de types (entité supprimée)
}
