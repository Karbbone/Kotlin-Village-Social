import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { EventType } from '../entities/event-type.entity.js';
import { EventTypesController } from './event-types.controller.js';
import { EventTypesService } from './event-types.service.js';

@Module({
  imports: [TypeOrmModule.forFeature([EventType])],
  controllers: [EventTypesController],
  providers: [EventTypesService],
})
export class EventTypesModule {}
