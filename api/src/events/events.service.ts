import {
  ForbiddenException,
  Injectable,
  NotFoundException,
} from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import * as crypto from 'crypto';
import * as Minio from 'minio';
import { In, Repository } from 'typeorm';
import { EventPhoto } from '../entities/event-photo.entity.js';
import { EventType } from '../entities/event-type.entity.js';
import { Event } from '../entities/event.entity.js';
import { User } from '../entities/user.entity.js';

@Injectable()
export class EventsService {
  private minioClient: Minio.Client;
  private readonly bucketName = 'event-photos';

  constructor(
    @InjectRepository(Event)
    private readonly eventRepo: Repository<Event>,
    @InjectRepository(User)
    private readonly userRepo: Repository<User>,
    @InjectRepository(EventType)
    private readonly eventTypeRepo: Repository<EventType>,
    @InjectRepository(EventPhoto)
    private readonly photoRepo: Repository<EventPhoto>,
  ) {
    // Initialiser le client MinIO
    this.minioClient = new Minio.Client({
      endPoint: process.env.MINIO_ENDPOINT || 'localhost',
      port: parseInt(process.env.MINIO_PORT || '9000'),
      useSSL: process.env.MINIO_USE_SSL === 'true',
      accessKey: process.env.MINIO_ACCESS_KEY || 'minioadmin',
      secretKey: process.env.MINIO_SECRET_KEY || 'minioadmin',
    });

    // Créer le bucket s'il n'existe pas
    void this.ensureBucket();
  }

  private async ensureBucket() {
    try {
      const exists = await this.minioClient.bucketExists(this.bucketName);
      if (!exists) {
        await this.minioClient.makeBucket(this.bucketName, 'us-east-1');
        // Rendre le bucket public en lecture
        const policy = {
          Version: '2012-10-17',
          Statement: [
            {
              Effect: 'Allow',
              Principal: { AWS: ['*'] },
              Action: ['s3:GetObject'],
              Resource: [`arn:aws:s3:::${this.bucketName}/*`],
            },
          ],
        };
        await this.minioClient.setBucketPolicy(
          this.bucketName,
          JSON.stringify(policy),
        );
      }
    } catch (error) {
      console.error('Error ensuring MinIO bucket:', error);
    }
  }

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
    },
    photos?: Express.Multer.File[],
  ) {
    const creator = await this.userRepo.findOne({
      where: { id: creatorUserId },
    });
    if (!creator) throw new NotFoundException('User not found');

    const date = new Date(payload.date);
    if (Number.isNaN(date.getTime())) {
      throw new NotFoundException('Invalid date');
    }

    // Normaliser la liste des types
    const typeNames = Array.from(
      new Set(
        (payload.types ?? []).map((t) => t.trim()).filter((t) => t.length > 0),
      ),
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

    // Créer l'événement sans les photos d'abord
    const ev = this.eventRepo.create({
      title: payload.title,
      description: payload.description,
      location: payload.location,
      date,
      city: cityName,
      creator,
      types: allTypes,
    });
    const saved = await this.eventRepo.save(ev);

    // Uploader les photos vers MinIO si fournies
    if (photos && photos.length > 0) {
      for (const file of photos) {
        await this.uploadPhotoForEvent(saved.id, file);
      }
    }

    return this.findOne(saved.id, creatorUserId);
  }

  /**
   * Upload une photo pour un événement (méthode interne)
   */
  private async uploadPhotoForEvent(
    eventId: number,
    file: Express.Multer.File,
  ): Promise<EventPhoto> {
    const originalName: string = file.originalname || 'photo.jpg';
    const parts = originalName.split('.');
    const fileExt = parts.length > 1 ? parts[parts.length - 1] : 'jpg';
    const fileName = `${eventId}/${crypto.randomUUID()}.${fileExt}`;

    // Upload vers MinIO
    await this.minioClient.putObject(
      this.bucketName,
      fileName,

      file.buffer,

      file.size || 0,
      {
        'Content-Type': file.mimetype || 'image/jpeg',
      },
    );

    // Construire l'URL publique
    // Utiliser MINIO_PUBLIC_ENDPOINT si défini (pour émulateur Android: 10.0.2.2)
    const publicEndpoint =
      process.env.MINIO_PUBLIC_ENDPOINT ||
      process.env.MINIO_ENDPOINT ||
      'localhost';
    const url = `http://${publicEndpoint}:${process.env.MINIO_PORT || '9000'}/${this.bucketName}/${fileName}`;

    // Créer l'entrée en DB
    const photo = this.photoRepo.create({
      url,
      event: { id: eventId } as Event,
    });
    return this.photoRepo.save(photo);
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
   * - Si aucun paramètre n'est fourni, on retourne les 5 derniers événements à venir
   * - Sinon on filtre sur city et/ou type et on retourne tous les résultats à venir triés par date ASC
   * - Par défaut, ne retourne que les événements futurs
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

    // Filtrer uniquement les événements futurs par défaut
    qb.andWhere('event.date >= :now', { now: new Date().toISOString() });

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
      qb.orderBy('event.date', 'ASC');
      return qb.getMany();
    }

    qb.orderBy('event.date', 'ASC').take(5);
    return qb.getMany();
  }

  async updateEvent(
    eventId: number,
    userId: number,
    payload: {
      title?: string;
      description?: string;
      location?: string;
      date?: string;
      types?: string[];
    },
    photos?: Express.Multer.File[],
  ) {
    // Vérifier que l'événement existe et que l'utilisateur est le créateur
    const event = await this.eventRepo.findOne({
      where: { id: eventId },
      relations: ['creator', 'types'],
    });
    if (!event) throw new NotFoundException('Event not found');
    if (event.creator.id !== userId) {
      throw new ForbiddenException('Only the creator can update this event');
    }

    // Mettre à jour les champs simples
    if (payload.title !== undefined) event.title = payload.title;
    if (payload.description !== undefined)
      event.description = payload.description;
    if (payload.location !== undefined) event.location = payload.location;
    if (payload.date !== undefined) {
      const newDate = new Date(payload.date);
      if (Number.isNaN(newDate.getTime())) {
        throw new NotFoundException('Invalid date');
      }
      event.date = newDate;
    }

    // Mettre à jour les types si fournis
    if (Array.isArray(payload.types)) {
      const typeNames = Array.from(
        new Set(payload.types.map((t) => t.trim()).filter((t) => t.length > 0)),
      );
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
      event.types = [...existing, ...created];
    }

    await this.eventRepo.save(event);

    // Ajouter les nouvelles photos si fournies
    if (photos && photos.length > 0) {
      for (const file of photos) {
        await this.uploadPhotoForEvent(eventId, file);
      }
    }

    return this.findOne(eventId, userId);
  }

  async deleteEvent(eventId: number, userId: number) {
    const event = await this.eventRepo.findOne({
      where: { id: eventId },
      relations: ['creator', 'photos'],
    });
    if (!event) throw new NotFoundException('Event not found');
    if (event.creator.id !== userId) {
      throw new ForbiddenException('Only the creator can delete this event');
    }

    // Supprimer les photos de MinIO avant de supprimer l'événement
    for (const photo of event.photos) {
      try {
        const urlParts = photo.url.split('/');
        const fileName = urlParts.slice(-2).join('/'); // eventId/uuid.ext
        await this.minioClient.removeObject(this.bucketName, fileName);
      } catch (error) {
        console.error('Error deleting photo from MinIO:', error);
      }
    }

    await this.eventRepo.remove(event);
  }

  /**
   * Supprimer une photo d'un événement
   */
  async deletePhoto(photoId: number, userId: number) {
    const photo = await this.photoRepo.findOne({
      where: { id: photoId },
      relations: ['event', 'event.creator'],
    });
    if (!photo) throw new NotFoundException('Photo not found');

    // Vérifier que l'utilisateur est le créateur de l'événement
    if (photo.event.creator.id !== userId) {
      throw new ForbiddenException('Only the creator can delete photos');
    }

    // Extraire le nom du fichier depuis l'URL
    const urlParts = photo.url.split('/');
    const fileName = urlParts.slice(-2).join('/'); // eventId/uuid.ext

    // Supprimer de MinIO
    try {
      await this.minioClient.removeObject(this.bucketName, fileName);
    } catch (error) {
      console.error('Error deleting from MinIO:', error);
    }

    // Supprimer de la DB
    await this.photoRepo.remove(photo);
  }
}
