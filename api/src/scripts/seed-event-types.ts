import 'dotenv/config';
import { In } from 'typeorm';
import AppDataSource from '../data-source/app.data-source.js';
import { EventType } from '../entities/event-type.entity.js';

const TYPE_NAMES = [
  'Spectacle',
  'Concert, spectacle musical',
  'Activité de loisirs',
  'Exposition, musée',
  'Visite, balade',
  'Conférence, débat',
  'Sport',
];

async function main() {
  try {
    console.log('Initialisation de la connexion DB...');
    await AppDataSource.initialize();
    const repo = AppDataSource.getRepository(EventType);

    // Nettoyer les noms (éviter doublons accidentels)
    const names = Array.from(
      new Set(TYPE_NAMES.map((n) => n.trim()).filter((n) => n.length > 0)),
    );

    console.log(`Types attendus: ${names.length}`);
    const existing = await repo.find({ where: { name: In(names) } });
    const existingNames = new Set(existing.map((e) => e.name));

    const toCreate = names
      .filter((n) => !existingNames.has(n))
      .map((name) => repo.create({ name }));

    if (toCreate.length > 0) {
      await repo.save(toCreate);
      console.log(`Créés: ${toCreate.length} nouveaux types.`);
    } else {
      console.log('Aucun nouveau type à créer.');
    }

    console.log('Seed des types terminé avec succès.');
    await AppDataSource.destroy();
  } catch (err) {
    console.error('Erreur pendant le seed des types:', err);
    try {
      if (AppDataSource.isInitialized) await AppDataSource.destroy();
    } catch {
      // ignore
    }
    process.exit(1);
  }
}

void main();
