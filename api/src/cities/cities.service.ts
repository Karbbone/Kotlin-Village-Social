import { ConflictException, Injectable } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { City } from '../entities/city.entity.js';

@Injectable()
export class CitiesService {
  constructor(
    @InjectRepository(City)
    private readonly cityRepo: Repository<City>,
  ) {}

  findAll() {
    return this.cityRepo.find({ order: { name: 'ASC' } });
  }

  async create(name: string) {
    const exists = await this.cityRepo.findOne({ where: { name } });
    if (exists) {
      throw new ConflictException('City name already exists');
    }
    const city = this.cityRepo.create({ name });
    return this.cityRepo.save(city);
  }
}
