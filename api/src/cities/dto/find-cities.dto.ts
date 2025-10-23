export class FindCitiesDto {
  q?: string; // recherche par préfixe du nom
  postalCode?: string; // filtre exact ou préfixe de CP
  limit?: number; // défaut: toutes si non fourni
  offset?: number; // défaut: 0
}
