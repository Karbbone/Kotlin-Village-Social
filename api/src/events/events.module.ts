import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { EventPhoto } from '../entities/event-photo.entity.js';
import { EventType } from '../entities/event-type.entity.js';
import { Event } from '../entities/event.entity.js';
import { User } from '../entities/user.entity.js';
import { EventsController } from './events.controller.js';
import { EventsService } from './events.service.js';

@Module({
  imports: [TypeOrmModule.forFeature([Event, User, EventType, EventPhoto])],
  controllers: [EventsController],
  providers: [EventsService],
  exports: [EventsService],
})
export class EventsModule {}
