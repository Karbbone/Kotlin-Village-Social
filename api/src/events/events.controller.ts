import {
  Body,
  Controller,
  Delete,
  Get,
  Param,
  ParseIntPipe,
  Patch,
  Post,
  Query,
  Req,
  UploadedFiles,
  UseGuards,
  UseInterceptors,
} from '@nestjs/common';
import { FilesInterceptor } from '@nestjs/platform-express';
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard.js';
import { CreateEventDto } from './dto/create-event.dto.js';
import { FindEventsDto } from './dto/find-events.dto.js';
import { UpdateEventDto } from './dto/update-event.dto.js';
import { EventsService } from './events.service.js';

@Controller('events')
export class EventsController {
  constructor(private readonly eventsService: EventsService) {}

  @Get('cities/:cityName')
  async findByCity(
    @Param('cityName') cityName: string,
    @Query() query: FindEventsDto,
  ) {
    const upcomingOnly = query.upcomingOnly !== false;
    return this.eventsService.findAll({
      city: cityName,
      upcomingOnly,
      sort: query.sort ?? 'asc',
      limit: query.limit ?? 20,
      offset: query.offset ?? 0,
    });
  }

  @UseGuards(JwtAuthGuard)
  @Post('cities/:cityName')
  @UseInterceptors(FilesInterceptor('photos', 10))
  async createInCity(
    @Param('cityName') cityName: string,
    @Body() body: CreateEventDto,
    @UploadedFiles() photos: Express.Multer.File[],
    @Req() req: { user?: { userId: number } },
  ) {
    const creatorId = req.user!.userId;
    return this.eventsService.createInCity(cityName, creatorId, body, photos);
  }

  // Plus de routes pour les types: on stocke un simple string

  // Past events of current user (only creator can access)
  @UseGuards(JwtAuthGuard)
  @Get('me/past')
  async findMyPast(@Req() req: { user?: { userId: number } }) {
    const userId = req.user!.userId;
    return this.eventsService.findPastByCreator(userId);
  }

  // Recherche via body: POST /events/search { cityName?: string; types?: string[] }
  // - Si aucun champ: retourne les 5 derniers événements créés
  // - Si cityName et/ou types fournis: filtre en conséquence (types = ANY des valeurs)
  @Post('search')
  async searchByCityAndType(
    @Body() body: { cityName?: string; types?: string[]; type?: string },
  ) {
    const { cityName, types, type } = body ?? {};
    return await this.eventsService.findByCityAndTypeOptional({
      cityName,
      types,
      type,
    });
  }

  // Détail par id (validation numérique via ParseIntPipe)
  @Get(':id')
  async findOne(
    @Param('id', new ParseIntPipe()) id: number,
    @Req() req: { user?: { id: number } },
  ) {
    const requesterId = req.user?.id;
    return this.eventsService.findOne(id, requesterId);
  }

  // Update d'un événement (seul le créateur peut modifier)
  @UseGuards(JwtAuthGuard)
  @Patch(':id')
  @UseInterceptors(FilesInterceptor('photos', 10))
  async updateEvent(
    @Param('id', new ParseIntPipe()) id: number,
    @Body() body: UpdateEventDto,
    @UploadedFiles() photos: Express.Multer.File[],
    @Req() req: { user?: { userId: number } },
  ) {
    const userId = req.user!.userId;
    return this.eventsService.updateEvent(id, userId, body, photos);
  }

  // Suppression d'un événement (seul le créateur peut supprimer)
  @UseGuards(JwtAuthGuard)
  @Delete(':id')
  async deleteEvent(
    @Param('id', new ParseIntPipe()) id: number,
    @Req() req: { user?: { userId: number } },
  ) {
    const userId = req.user!.userId;
    await this.eventsService.deleteEvent(id, userId);
    return { message: 'Event deleted successfully' };
  }

  // Supprimer une photo
  @UseGuards(JwtAuthGuard)
  @Delete('photos/:photoId')
  async deletePhoto(
    @Param('photoId', ParseIntPipe) photoId: number,
    @Req() req: { user?: { userId: number } },
  ) {
    const userId = req.user!.userId;
    await this.eventsService.deletePhoto(photoId, userId);
    return { message: 'Photo deleted successfully' };
  }
}
