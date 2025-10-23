import { Injectable, NotFoundException } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { City } from '../entities/city.entity.js';
import { User } from '../entities/user.entity.js';

@Injectable()
export class CitiesService {
  constructor(
    @InjectRepository(City) private readonly cityRepo: Repository<City>,
    @InjectRepository(User) private readonly userRepo: Repository<User>,
  ) {}

  findAll(): Promise<City[]> {
    return this.cityRepo.find({ order: { name: 'ASC' } });
  }

  async findAllForUser(userId: number): Promise<City[]> {
    const user = await this.userRepo.findOne({
      where: { id: userId },
      relations: ['cities'],
    });
    if (!user) throw new NotFoundException('User not found');
    return user.cities ?? [];
  }

  async linkCityToUser(userId: number, cityId: number): Promise<void> {
    const [user, city] = await Promise.all([
      this.userRepo.findOne({ where: { id: userId }, relations: ['cities'] }),
      this.cityRepo.findOne({ where: { id: cityId } }),
    ]);
    if (!user) throw new NotFoundException('User not found');
    if (!city) throw new NotFoundException('City not found');

    const already = (user.cities ?? []).some((c) => c.id === city.id);
    if (!already) {
      user.cities = [...(user.cities ?? []), city];
      await this.userRepo.save(user);
    }
  }

  async unlinkCityFromUser(userId: number, cityId: number): Promise<void> {
    const user = await this.userRepo.findOne({
      where: { id: userId },
      relations: ['cities'],
    });
    if (!user) throw new NotFoundException('User not found');

    const before = user.cities?.length ?? 0;
    user.cities = (user.cities ?? []).filter((c) => c.id !== cityId);
    const after = user.cities.length;
    if (after !== before) {
      await this.userRepo.save(user);
    }
  }
}
