import { MigrationInterface, QueryRunner } from "typeorm";

export class InitSchema1761040154200 implements MigrationInterface {
    name = 'InitSchema1761040154200'

    public async up(queryRunner: QueryRunner): Promise<void> {
        await queryRunner.query(`CREATE TABLE \`event_photos\` (\`id\` int NOT NULL AUTO_INCREMENT, \`url\` varchar(255) NOT NULL, \`eventId\` int NULL, PRIMARY KEY (\`id\`)) ENGINE=InnoDB`);
        await queryRunner.query(`CREATE TABLE \`event_types\` (\`id\` int NOT NULL AUTO_INCREMENT, \`name\` varchar(255) NOT NULL, UNIQUE INDEX \`IDX_d5110ab69f4aacfe41fecdf4fc\` (\`name\`), PRIMARY KEY (\`id\`)) ENGINE=InnoDB`);
        await queryRunner.query(`CREATE TABLE \`events\` (\`id\` int NOT NULL AUTO_INCREMENT, \`title\` varchar(200) NOT NULL, \`description\` text NOT NULL, \`location\` varchar(255) NOT NULL, \`date\` datetime NOT NULL, \`createdAt\` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6), \`updatedAt\` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6), \`cityId\` int NOT NULL, \`creatorId\` int NOT NULL, INDEX \`IDX_217a680273e6f360857e9c5326\` (\`date\`), PRIMARY KEY (\`id\`)) ENGINE=InnoDB`);
        await queryRunner.query(`CREATE TABLE \`cities\` (\`id\` int NOT NULL AUTO_INCREMENT, \`name\` varchar(255) NOT NULL, \`createdAt\` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6), \`updatedAt\` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6), UNIQUE INDEX \`IDX_a0ae8d83b7d32359578c486e7f\` (\`name\`), PRIMARY KEY (\`id\`)) ENGINE=InnoDB`);
        await queryRunner.query(`CREATE TABLE \`users\` (\`id\` int NOT NULL AUTO_INCREMENT, \`email\` varchar(255) NOT NULL, \`passwordHash\` varchar(255) NOT NULL, \`displayName\` varchar(100) NOT NULL, \`createdAt\` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6), \`updatedAt\` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6), UNIQUE INDEX \`IDX_97672ac88f789774dd47f7c8be\` (\`email\`), PRIMARY KEY (\`id\`)) ENGINE=InnoDB`);
        await queryRunner.query(`CREATE TABLE \`events_types\` (\`eventsId\` int NOT NULL, \`eventTypesId\` int NOT NULL, INDEX \`IDX_c789de4327df8972998fde6c9b\` (\`eventsId\`), INDEX \`IDX_f851d86391925334f3db272aea\` (\`eventTypesId\`), PRIMARY KEY (\`eventsId\`, \`eventTypesId\`)) ENGINE=InnoDB`);
        await queryRunner.query(`CREATE TABLE \`users_cities\` (\`usersId\` int NOT NULL, \`citiesId\` int NOT NULL, INDEX \`IDX_14b8d39906620de17ca20691c9\` (\`usersId\`), INDEX \`IDX_1effea7b1c4a2afa0d925780e5\` (\`citiesId\`), PRIMARY KEY (\`usersId\`, \`citiesId\`)) ENGINE=InnoDB`);
        await queryRunner.query(`ALTER TABLE \`event_photos\` ADD CONSTRAINT \`FK_55bdd1c48a98ec17e50cf8e9630\` FOREIGN KEY (\`eventId\`) REFERENCES \`events\`(\`id\`) ON DELETE CASCADE ON UPDATE NO ACTION`);
        await queryRunner.query(`ALTER TABLE \`events\` ADD CONSTRAINT \`FK_712790b5c3b1e6d859c0987c4f5\` FOREIGN KEY (\`cityId\`) REFERENCES \`cities\`(\`id\`) ON DELETE NO ACTION ON UPDATE NO ACTION`);
        await queryRunner.query(`ALTER TABLE \`events\` ADD CONSTRAINT \`FK_c621508a2b84ae21d3f971cdb47\` FOREIGN KEY (\`creatorId\`) REFERENCES \`users\`(\`id\`) ON DELETE NO ACTION ON UPDATE NO ACTION`);
        await queryRunner.query(`ALTER TABLE \`events_types\` ADD CONSTRAINT \`FK_c789de4327df8972998fde6c9b9\` FOREIGN KEY (\`eventsId\`) REFERENCES \`events\`(\`id\`) ON DELETE CASCADE ON UPDATE CASCADE`);
        await queryRunner.query(`ALTER TABLE \`events_types\` ADD CONSTRAINT \`FK_f851d86391925334f3db272aead\` FOREIGN KEY (\`eventTypesId\`) REFERENCES \`event_types\`(\`id\`) ON DELETE NO ACTION ON UPDATE NO ACTION`);
        await queryRunner.query(`ALTER TABLE \`users_cities\` ADD CONSTRAINT \`FK_14b8d39906620de17ca20691c9a\` FOREIGN KEY (\`usersId\`) REFERENCES \`users\`(\`id\`) ON DELETE CASCADE ON UPDATE CASCADE`);
        await queryRunner.query(`ALTER TABLE \`users_cities\` ADD CONSTRAINT \`FK_1effea7b1c4a2afa0d925780e54\` FOREIGN KEY (\`citiesId\`) REFERENCES \`cities\`(\`id\`) ON DELETE NO ACTION ON UPDATE NO ACTION`);
    }

    public async down(queryRunner: QueryRunner): Promise<void> {
        await queryRunner.query(`ALTER TABLE \`users_cities\` DROP FOREIGN KEY \`FK_1effea7b1c4a2afa0d925780e54\``);
        await queryRunner.query(`ALTER TABLE \`users_cities\` DROP FOREIGN KEY \`FK_14b8d39906620de17ca20691c9a\``);
        await queryRunner.query(`ALTER TABLE \`events_types\` DROP FOREIGN KEY \`FK_f851d86391925334f3db272aead\``);
        await queryRunner.query(`ALTER TABLE \`events_types\` DROP FOREIGN KEY \`FK_c789de4327df8972998fde6c9b9\``);
        await queryRunner.query(`ALTER TABLE \`events\` DROP FOREIGN KEY \`FK_c621508a2b84ae21d3f971cdb47\``);
        await queryRunner.query(`ALTER TABLE \`events\` DROP FOREIGN KEY \`FK_712790b5c3b1e6d859c0987c4f5\``);
        await queryRunner.query(`ALTER TABLE \`event_photos\` DROP FOREIGN KEY \`FK_55bdd1c48a98ec17e50cf8e9630\``);
        await queryRunner.query(`DROP INDEX \`IDX_1effea7b1c4a2afa0d925780e5\` ON \`users_cities\``);
        await queryRunner.query(`DROP INDEX \`IDX_14b8d39906620de17ca20691c9\` ON \`users_cities\``);
        await queryRunner.query(`DROP TABLE \`users_cities\``);
        await queryRunner.query(`DROP INDEX \`IDX_f851d86391925334f3db272aea\` ON \`events_types\``);
        await queryRunner.query(`DROP INDEX \`IDX_c789de4327df8972998fde6c9b\` ON \`events_types\``);
        await queryRunner.query(`DROP TABLE \`events_types\``);
        await queryRunner.query(`DROP INDEX \`IDX_97672ac88f789774dd47f7c8be\` ON \`users\``);
        await queryRunner.query(`DROP TABLE \`users\``);
        await queryRunner.query(`DROP INDEX \`IDX_a0ae8d83b7d32359578c486e7f\` ON \`cities\``);
        await queryRunner.query(`DROP TABLE \`cities\``);
        await queryRunner.query(`DROP INDEX \`IDX_217a680273e6f360857e9c5326\` ON \`events\``);
        await queryRunner.query(`DROP TABLE \`events\``);
        await queryRunner.query(`DROP INDEX \`IDX_d5110ab69f4aacfe41fecdf4fc\` ON \`event_types\``);
        await queryRunner.query(`DROP TABLE \`event_types\``);
        await queryRunner.query(`DROP TABLE \`event_photos\``);
    }

}
