import 'dotenv/config';
import AppDataSource from '../data-source/app.data-source.js';
import { City } from '../entities/city.entity.js';

type Commune = {
  nom: string;
  codesPostaux: string[];
};

async function fetchAllCommunes(): Promise<Commune[]> {
  const url =
    'https://geo.api.gouv.fr/communes?fields=nom,codesPostaux&format=json&geometry=centre';
  const res = await fetch(url);
  if (!res.ok) {
    throw new Error(`HTTP ${res.status} en appelant ${url}`);
  }
  return (await res.json()) as Commune[];
}

async function main() {
  try {
    console.log('Initialisation de la connexion DB...');
    await AppDataSource.initialize();
    const cityRepo = AppDataSource.getRepository(City);

    console.log('Récupération des communes depuis geo.api.gouv.fr...');
    const communes = await fetchAllCommunes();
    console.log(`Communes récupérées: ${communes.length}`);

    // Construire une liste (nom, code postal). Une commune peut avoir plusieurs CP => une entrée par CP.
    const seen = new Set<string>();
    const values: Array<Pick<City, 'name' | 'postalCode'>> = [];
    for (const c of communes) {
      for (const cp of c.codesPostaux ?? []) {
        const key = `${c.nom}#${cp}`;
        if (seen.has(key)) continue;
        seen.add(key);
        values.push({ name: c.nom, postalCode: cp });
      }
    }

    console.log(`Total d'entrées Ville à insérer: ${values.length}`);

    // Nettoyer la table pour éviter les doublons lors du seed.
    console.log(
      'Nettoyage de la table cities (DELETE + CASCADE sur users_cities)...',
    );
    // TRUNCATE est interdit si des FKs pointent sur la table. Utiliser DELETE pour déclencher le CASCADE.
    await cityRepo.createQueryBuilder().delete().execute();

    // Insertions par lots pour de meilleures perfs.
    const chunkSize = 1000;
    for (let i = 0; i < values.length; i += chunkSize) {
      const chunk = values.slice(i, i + chunkSize);
      await cityRepo
        .createQueryBuilder()
        .insert()
        .into(City)
        .values(chunk)
        .execute();
      console.log(
        `Insérés: ${Math.min(i + chunkSize, values.length)} / ${values.length}`,
      );
    }

    console.log('Seed des villes terminé avec succès.');
    await AppDataSource.destroy();
  } catch (err) {
    console.error('Erreur pendant le seed des villes:', err);
    try {
      if (AppDataSource.isInitialized) await AppDataSource.destroy();
    } catch {
      // ignore cleanup errors
    }
    process.exit(1);
  }
}

void main();
