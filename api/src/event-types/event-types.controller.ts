import {
  Body,
  Controller,
  Delete,
  Get,
  Param,
  Post,
  UseGuards,
} from '@nestjs/common';
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard.js';
import { EventTypesService } from './event-types.service.js';

@Controller('event-types')
export class EventTypesController {
  constructor(private readonly eventTypesService: EventTypesService) {}

  /**
   * Lister tous les types d'événements disponibles
   * GET /event-types
   */
  @Get()
  async findAll() {
    return this.eventTypesService.findAll();
  }

  /**
   * Créer un nouveau type d'événement dans le référentiel
   * POST /event-types
   * Body: { "name": "Concert" }
   */
  @UseGuards(JwtAuthGuard)
  @Post()
  async create(@Body() body: { name: string }) {
    return this.eventTypesService.create(body.name);
  }

  /**
   * Supprimer un type d'événement du référentiel
   * DELETE /event-types/:name
   */
  @UseGuards(JwtAuthGuard)
  @Delete(':name')
  async delete(@Param('name') name: string) {
    await this.eventTypesService.delete(name);
    return { message: 'Event type deleted successfully' };
  }
}
