import { MigrationInterface, QueryRunner } from 'typeorm';

export class IndexCities1761204363837 implements MigrationInterface {
  name = 'IndexCities1761204363837';

  public async up(queryRunner: QueryRunner): Promise<void> {
    await queryRunner.query(
      `CREATE INDEX \`IDX_cities_name\` ON \`cities\` (\`name\`)`,
    );
    await queryRunner.query(
      `CREATE INDEX \`IDX_cities_postalCode\` ON \`cities\` (\`postalCode\`)`,
    );
  }

  public async down(queryRunner: QueryRunner): Promise<void> {
    await queryRunner.query(
      `DROP INDEX \`IDX_cities_postalCode\` ON \`cities\``,
    );
    await queryRunner.query(`DROP INDEX \`IDX_cities_name\` ON \`cities\``);
  }
}
