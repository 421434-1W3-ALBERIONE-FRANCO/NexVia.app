import { query } from '../config/database';

export interface Config {
  id: string;
  zona_nombre: string;
  centro_lat: number;
  centro_lng: number;
  tarifa_por_km: number;
  tarifa_por_tonelada: number;
  created_at: Date;
  updated_at: Date;
}

export class ConfigRepository {
  static async get(): Promise<Config> {
    const result = await query('SELECT * FROM configuracion LIMIT 1');
    if (result.rows.length === 0) {
      return this.createDefault();
    }
    return result.rows[0];
  }

  static async update(data: Partial<Config>): Promise<Config> {
    const updates: string[] = [];
    const values: any[] = [];
    let paramCount = 1;

    Object.entries(data).forEach(([key, value]) => {
      if (
        key !== 'id' &&
        key !== 'created_at' &&
        key !== 'updated_at' &&
        value !== undefined
      ) {
        updates.push(`${key} = $${paramCount++}`);
        values.push(value);
      }
    });

    if (updates.length === 0) {
      return this.get();
    }

    updates.push('updated_at = CURRENT_TIMESTAMP');
    const result = await query(
      `UPDATE configuracion SET ${updates.join(', ')} RETURNING *`,
      values
    );

    return result.rows[0];
  }

  private static async createDefault(): Promise<Config> {
    const result = await query(
      `INSERT INTO configuracion (zona_nombre, centro_lat, centro_lng, tarifa_por_km, tarifa_por_tonelada)
       VALUES ('Default Zone', -32.4341, -63.2433, 500, 1000)
       RETURNING *`
    );
    return result.rows[0];
  }

  static toResponse(config: Config) {
    return {
      id: config.id,
      zona_nombre: config.zona_nombre,
      centro_lat: config.centro_lat,
      centro_lng: config.centro_lng,
      tarifa_por_km: config.tarifa_por_km,
      tarifa_por_tonelada: config.tarifa_por_tonelada,
      updated_at: config.updated_at,
    };
  }
}
