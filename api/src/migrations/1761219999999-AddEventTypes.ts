import { MigrationInterface, QueryRunner } from 'typeorm';

export class AddEventTypes1761219999999 implements MigrationInterface {
  name = 'AddEventTypes1761219999999';

  public async up(queryRunner: QueryRunner): Promise<void> {
    await queryRunner.query(
      'CREATE TABLE `event_types` (`name` varchar(100) NOT NULL, PRIMARY KEY (`name`)) ENGINE=InnoDB',
    );
    await queryRunner.query(
      'CREATE TABLE `events_types` (`eventId` int NOT NULL, `typeName` varchar(100) NOT NULL, PRIMARY KEY (`eventId`, `typeName`)) ENGINE=InnoDB',
    );
    await queryRunner.query(
      'ALTER TABLE `events_types` ADD CONSTRAINT `FK_events_types_event` FOREIGN KEY (`eventId`) REFERENCES `events`(`id`) ON DELETE CASCADE ON UPDATE NO ACTION',
    );
    await queryRunner.query(
      'ALTER TABLE `events_types` ADD CONSTRAINT `FK_events_types_type` FOREIGN KEY (`typeName`) REFERENCES `event_types`(`name`) ON DELETE CASCADE ON UPDATE NO ACTION',
    );
    // Supprimer l'ancienne colonne type si elle existe
    const hasTypeColumn = await queryRunner.hasColumn('events', 'type');
    if (hasTypeColumn) {
      await queryRunner.query('ALTER TABLE `events` DROP COLUMN `type`');
    }
  }

  public async down(queryRunner: QueryRunner): Promise<void> {
    // Ré-ajouter la colonne type (fallback)
    const hasTypeColumn = await queryRunner.hasColumn('events', 'type');
    if (!hasTypeColumn) {
      await queryRunner.query(
        'ALTER TABLE `events` ADD `type` varchar(100) NOT NULL',
      );
    }
    await queryRunner.query(
      'ALTER TABLE `events_types` DROP FOREIGN KEY `FK_events_types_type`',
    );
    await queryRunner.query(
      'ALTER TABLE `events_types` DROP FOREIGN KEY `FK_events_types_event`',
    );
    await queryRunner.query('DROP TABLE `events_types`');
    await queryRunner.query('DROP TABLE `event_types`');
  }
}
