import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';
import { query, closeDatabase } from '../config/database';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

interface Migration {
  name: string;
  up: string;
}

async function getMigrations(): Promise<Migration[]> {
  const migrationsDir = __dirname;
  const files = fs.readdirSync(migrationsDir)
    .filter(file => file.match(/^\d{3}_.+\.sql$/))
    .sort();

  return files.map(file => ({
    name: file.replace('.sql', ''),
    up: fs.readFileSync(path.join(migrationsDir, file), 'utf-8')
  }));
}

async function hasExecutedMigration(name: string): Promise<boolean> {
  try {
    const result = await query(
      'SELECT 1 FROM _migrations WHERE name = $1',
      [name]
    );
    return result.rows.length > 0;
  } catch {
    return false;
  }
}

async function runMigrations() {
  try {
    // Ensure _migrations table exists
    await query(`
      CREATE TABLE IF NOT EXISTS _migrations (
        id SERIAL PRIMARY KEY,
        name TEXT UNIQUE NOT NULL,
        executed_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
      )
    `);

    const migrations = await getMigrations();
    let executedCount = 0;

    for (const migration of migrations) {
      const hasRun = await hasExecutedMigration(migration.name);

      if (!hasRun) {
        console.log(`🔄 Running migration: ${migration.name}`);
        try {
          await query(migration.up);
          await query(
            'INSERT INTO _migrations (name) VALUES ($1)',
            [migration.name]
          );
          console.log(`✅ Completed: ${migration.name}`);
          executedCount++;
        } catch (error) {
          console.error(`❌ Failed: ${migration.name}`, error);
          throw error;
        }
      } else {
        console.log(`⏭️  Already ran: ${migration.name}`);
      }
    }

    if (executedCount === 0) {
      console.log('✨ All migrations already executed');
    } else {
      console.log(`\n🎉 Successfully executed ${executedCount} migration(s)`);
    }
  } catch (error) {
    console.error('❌ Migration failed:', error);
    process.exit(1);
  } finally {
    await closeDatabase();
  }
}

runMigrations();
