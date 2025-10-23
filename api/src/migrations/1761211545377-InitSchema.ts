import { MigrationInterface, QueryRunner } from "typeorm";

export class InitSchema1761211545377 implements MigrationInterface {
    name = 'InitSchema1761211545377'

    public async up(queryRunner: QueryRunner): Promise<void> {
        await queryRunner.query(`CREATE TABLE \`cities\` (\`id\` int NOT NULL AUTO_INCREMENT, \`name\` varchar(255) NOT NULL, \`postalCode\` varchar(20) NOT NULL, \`createdAt\` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6), \`updatedAt\` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6), PRIMARY KEY (\`id\`)) ENGINE=InnoDB`);
        await queryRunner.query(`CREATE TABLE \`users_cities\` (\`userId\` int NOT NULL, \`cityId\` int NOT NULL, INDEX \`IDX_53e34ea660cf6a0579e25171c4\` (\`userId\`), INDEX \`IDX_0fe05ee5194ff247a1d17a7f3f\` (\`cityId\`), PRIMARY KEY (\`userId\`, \`cityId\`)) ENGINE=InnoDB`);
        await queryRunner.query(`ALTER TABLE \`users_cities\` ADD CONSTRAINT \`FK_53e34ea660cf6a0579e25171c43\` FOREIGN KEY (\`userId\`) REFERENCES \`users\`(\`id\`) ON DELETE CASCADE ON UPDATE CASCADE`);
        await queryRunner.query(`ALTER TABLE \`users_cities\` ADD CONSTRAINT \`FK_0fe05ee5194ff247a1d17a7f3ff\` FOREIGN KEY (\`cityId\`) REFERENCES \`cities\`(\`id\`) ON DELETE NO ACTION ON UPDATE NO ACTION`);
    }

    public async down(queryRunner: QueryRunner): Promise<void> {
        await queryRunner.query(`ALTER TABLE \`users_cities\` DROP FOREIGN KEY \`FK_0fe05ee5194ff247a1d17a7f3ff\``);
        await queryRunner.query(`ALTER TABLE \`users_cities\` DROP FOREIGN KEY \`FK_53e34ea660cf6a0579e25171c43\``);
        await queryRunner.query(`DROP INDEX \`IDX_0fe05ee5194ff247a1d17a7f3f\` ON \`users_cities\``);
        await queryRunner.query(`DROP INDEX \`IDX_53e34ea660cf6a0579e25171c4\` ON \`users_cities\``);
        await queryRunner.query(`DROP TABLE \`users_cities\``);
        await queryRunner.query(`DROP TABLE \`cities\``);
    }

}
