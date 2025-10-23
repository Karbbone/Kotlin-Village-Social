import {
  Controller,
  Delete,
  Get,
  HttpCode,
  Param,
  ParseIntPipe,
  Post,
  Query,
} from '@nestjs/common';
import { City } from '../entities/city.entity';
import { CitiesService } from './cities.service.js';

@Controller('cities')
export class CitiesController {
  constructor(private readonly citiesService: CitiesService) {}

  // GET /cities
  @Get()
  findAll(
    @Query('q') q?: string,
    @Query('postalCode') postalCode?: string,
    @Query('limit') limit?: string,
    @Query('offset') offset?: string,
  ): Promise<City[]> {
    return this.citiesService.findAll({
      q,
      postalCode,
      limit: limit ? Number(limit) : undefined,
      offset: offset ? Number(offset) : undefined,
    });
  }

  // GET /cities/users/:userId -> toutes les villes d'un utilisateur
  @Get('users/:userId')
  async findByUser(
    @Param('userId', ParseIntPipe) userId: number,
  ): Promise<City[]> {
    return this.citiesService.findAllForUser(userId);
  }

  // POST /cities/:cityId/users/:userId -> lier une ville à un utilisateur
  @Post(':cityId/users/:userId')
  @HttpCode(204)
  async link(
    @Param('cityId', ParseIntPipe) cityId: number,
    @Param('userId', ParseIntPipe) userId: number,
  ): Promise<void> {
    await this.citiesService.linkCityToUser(userId, cityId);
  }

  // DELETE /cities/:cityId/users/:userId -> délier une ville d'un utilisateur
  @Delete(':cityId/users/:userId')
  @HttpCode(204)
  async unlink(
    @Param('cityId', ParseIntPipe) cityId: number,
    @Param('userId', ParseIntPipe) userId: number,
  ): Promise<void> {
    await this.citiesService.unlinkCityFromUser(userId, cityId);
  }
}
