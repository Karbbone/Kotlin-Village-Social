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
import { CreateTypeDto } from './dto/create-type.dto.js';
import { FindEventsDto } from './dto/find-events.dto.js';
import { EventsService } from './events.service.js';

@Controller('events')
export class EventsController {
  constructor(private readonly eventsService: EventsService) {}

  @Get('cities/:cityId')
  async findByCity(
    @Param('cityId', ParseIntPipe) cityId: number,
    @Query() query: FindEventsDto,
  ) {
    const upcomingOnly = query.upcomingOnly !== false;
    return this.eventsService.findAll({
      cityId,
      upcomingOnly,
      sort: query.sort ?? 'asc',
      limit: query.limit ?? 20,
      offset: query.offset ?? 0,
    });
  }

  @UseGuards(JwtAuthGuard)
  @Post('cities/:cityId')
  async createInCity(
    @Param('cityId', ParseIntPipe) cityId: number,
    @Body() body: CreateEventDto,
    @Req() req: { user?: { userId: number } },
  ) {
    const creatorId = req.user!.userId;
    return this.eventsService.createInCity(cityId, creatorId, body);
  }

  // Create an event type (protected)
  @UseGuards(JwtAuthGuard)
  @Get('types')
  findAllTypes() {
    return this.eventsService.findAllTypes();
  }

  // Create an event type (protected)
  @UseGuards(JwtAuthGuard)
  @Post('types')
  async createType(@Body() body: CreateTypeDto) {
    return this.eventsService.createType(body.name);
  }

  // Past events of current user (only creator can access)
  @UseGuards(JwtAuthGuard)
  @Get('me/past')
  async findMyPast(@Req() req: { user?: { userId: number } }) {
    const userId = req.user!.userId;
    return this.eventsService.findPastByCreator(userId);
  }

  @Get(':id')
  async findOne(
    @Param('id', ParseIntPipe) id: number,
    @Req() req: { user?: { id: number } },
  ) {
    const requesterId = req.user?.id;
    return this.eventsService.findOne(id, requesterId);
  }
}
