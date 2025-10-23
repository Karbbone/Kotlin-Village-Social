import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import 'dotenv/config';
import { AppController } from './app.controller';
import { AppService } from './app.service';
import { AuthModule } from './auth/auth.module';
import { CitiesModule } from './cities/cities.module';
import { EventPhoto } from './entities/event-photo.entity';
import { Event } from './entities/event.entity';
import { User } from './entities/user.entity';
import { EventsModule } from './events/events.module';

@Module({
  imports: [
    TypeOrmModule.forRoot({
      type: 'mysql',
      host: process.env.DB_HOST ?? 'localhost',
      port: Number.parseInt(process.env.DB_PORT ?? '3306', 10),
      username: process.env.DB_USER ?? 'app_user',
      password: process.env.DB_PASSWORD ?? 'app_password',
      database: process.env.DB_NAME ?? 'app_db',
      synchronize: (process.env.DB_SYNC ?? 'false').toLowerCase() === 'true',
      autoLoadEntities: true,
      logging: false,
      entities: [User, Event, EventPhoto],
    }),
    TypeOrmModule.forFeature([User, Event, EventPhoto]),
    AuthModule,
    EventsModule,
    CitiesModule,
  ],
  controllers: [AppController],
  providers: [AppService],
})
export class AppModule {}
