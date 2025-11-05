import {
  Body,
  Controller,
  Get,
  Param,
  ParseIntPipe,
  Post,
  Query,
  Req,
  UseGuards,
} from '@nestjs/common';
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard.js';
import { CreateEventDto } from './dto/create-event.dto.js';
import { FindEventsDto } from './dto/find-events.dto.js';
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
  async createInCity(
    @Param('cityName') cityName: string,
    @Body() body: CreateEventDto,
    @Req() req: { user?: { userId: number } },
  ) {
    const creatorId = req.user!.userId;
    return this.eventsService.createInCity(cityName, creatorId, body);
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
}
