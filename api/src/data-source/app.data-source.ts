import 'dotenv/config';
import { DataSource } from 'typeorm';

// Valeurs par défaut alignées avec le docker-compose.yml
const DB_HOST = process.env.DB_HOST ?? 'localhost'; // si l'API tourne dans Docker, utiliser 'mysql'
const DB_PORT = parseInt(process.env.DB_PORT ?? '3306', 10);
const DB_USER = process.env.DB_USER ?? 'app_user';
const DB_PASSWORD = process.env.DB_PASSWORD ?? 'app_password';
const DB_NAME = process.env.DB_NAME ?? 'app_db';

const AppDataSource = new DataSource({
  type: 'mysql',
  host: DB_HOST,
  port: DB_PORT,
  username: DB_USER,
  password: DB_PASSWORD,
  database: DB_NAME,
  synchronize: false,
  logging: false,
  entities: ['dist/entities/*.entity.js'],
  migrations: ['dist/migrations/*.js'],
});

export default AppDataSource;
