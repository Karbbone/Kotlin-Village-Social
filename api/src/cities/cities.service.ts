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

  async findAll(params?: {
    q?: string;
    postalCode?: string;
    limit?: number;
    offset?: number;
  }): Promise<City[]> {
    const qb = this.cityRepo
      .createQueryBuilder('c')
      .select(['c.id', 'c.name', 'c.postalCode'])
      .orderBy('c.name', 'ASC');

    if (params?.q) {
      qb.andWhere('c.name LIKE :q', { q: `${params.q}%` });
    }
    if (params?.postalCode) {
      qb.andWhere('c.postalCode LIKE :cp', { cp: `${params.postalCode}%` });
    }
    if (typeof params?.limit === 'number') {
      qb.take(Math.max(0, Math.min(10000, params.limit)));
    }
    if (typeof params?.offset === 'number') {
      qb.skip(Math.max(0, params.offset));
    }
    return qb.getMany();
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
