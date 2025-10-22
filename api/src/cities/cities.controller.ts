import { Body, Controller, Get, Post, UseGuards } from '@nestjs/common';
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard.js';
import { CitiesService } from './cities.service.js';
import { CreateCityDto } from './dto/create-city.dto.js';

@Controller('cities')
export class CitiesController {
  constructor(private readonly citiesService: CitiesService) {}

  @Get()
  findAll() {
    return this.citiesService.findAll();
  }

  @UseGuards(JwtAuthGuard)
  @Post()
  create(@Body() body: CreateCityDto) {
    return this.citiesService.create(body.name);
  }
}
