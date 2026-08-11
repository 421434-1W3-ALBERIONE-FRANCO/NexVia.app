import { query } from '../config/database';

export interface Truck {
  id: string;
  user_id: string | null;
  patente: string;
  patente_acoplado: string | null;
  transporte_nombre: string | null;
  transporte_cuit: string | null;
  chofer_nombre: string;
  chofer_cuit: string | null;
  telefono: string | null;
  capacidad_kg: number | null;
  lat: number;
  lng: number;
  estado: 'disponible' | 'ocupado' | 'inactivo';
  is_deleted: boolean;
  created_at: Date;
  updated_at: Date;
}

export class TruckRepository {
  static async findById(id: string): Promise<Truck | null> {
    const result = await query(
      'SELECT * FROM camiones WHERE id = $1 AND is_deleted = FALSE',
      [id]
    );
    return result.rows[0] || null;
  }

  static async findByUserId(userId: string): Promise<Truck | null> {
    const result = await query(
      'SELECT * FROM camiones WHERE user_id = $1 AND is_deleted = FALSE',
      [userId]
    );
    return result.rows[0] || null;
  }

  static async findAvailable(limit: number = 20, offset: number = 0): Promise<Truck[]> {
    const result = await query(
      `SELECT * FROM camiones
       WHERE estado = 'disponible' AND is_deleted = FALSE
       ORDER BY created_at DESC LIMIT $1 OFFSET $2`,
      [limit, offset]
    );
    return result.rows;
  }

  static async findByEstado(estado: 'disponible' | 'ocupado' | 'inactivo'): Promise<Truck[]> {
    const result = await query(
      'SELECT * FROM camiones WHERE estado = $1 AND is_deleted = FALSE ORDER BY created_at DESC',
      [estado]
    );
    return result.rows;
  }

  static async updateLocation(id: string, lat: number, lng: number): Promise<Truck> {
    const result = await query(
      `UPDATE camiones
       SET lat = $1, lng = $2, updated_at = CURRENT_TIMESTAMP
       WHERE id = $3 AND is_deleted = FALSE
       RETURNING *`,
      [lat, lng, id]
    );

    if (result.rows.length === 0) throw new Error('Truck not found');
    return result.rows[0];
  }

  static async updateEstado(id: string, estado: 'disponible' | 'ocupado' | 'inactivo'): Promise<Truck> {
    const result = await query(
      `UPDATE camiones
       SET estado = $1, updated_at = CURRENT_TIMESTAMP
       WHERE id = $2 AND is_deleted = FALSE
       RETURNING *`,
      [estado, id]
    );

    if (result.rows.length === 0) throw new Error('Truck not found');
    return result.rows[0];
  }

  static async update(id: string, data: Partial<Truck>): Promise<Truck> {
    const updates: string[] = [];
    const values: any[] = [];
    let paramCount = 1;

    Object.entries(data).forEach(([key, value]) => {
      if (
        key !== 'id' &&
        key !== 'is_deleted' &&
        key !== 'created_at' &&
        key !== 'updated_at' &&
        value !== undefined
      ) {
        updates.push(`${key} = $${paramCount++}`);
        values.push(value);
      }
    });

    if (updates.length === 0) {
      const result = await query('SELECT * FROM camiones WHERE id = $1', [id]);
      if (result.rows.length === 0) throw new Error('Truck not found');
      return result.rows[0];
    }

    updates.push(`updated_at = CURRENT_TIMESTAMP`);
    values.push(id);

    const result = await query(
      `UPDATE camiones SET ${updates.join(', ')} WHERE id = $${paramCount} AND is_deleted = FALSE RETURNING *`,
      values
    );

    if (result.rows.length === 0) throw new Error('Truck not found');
    return result.rows[0];
  }

  static async softDelete(id: string): Promise<void> {
    const result = await query(
      'UPDATE camiones SET is_deleted = TRUE, updated_at = CURRENT_TIMESTAMP WHERE id = $1',
      [id]
    );

    if (result.rowCount === 0) throw new Error('Truck not found');
  }

  static async countAvailable(): Promise<number> {
    const result = await query(
      "SELECT COUNT(*) as count FROM camiones WHERE estado = 'disponible' AND is_deleted = FALSE"
    );
    return parseInt(result.rows[0].count);
  }

  static async create(userId: string, data: Partial<Truck>): Promise<Truck> {
    const result = await query(
      `INSERT INTO camiones
         (user_id, patente, patente_acoplado, transporte_nombre, transporte_cuit,
          chofer_nombre, chofer_cuit, telefono, capacidad_kg, lat, lng, estado)
       VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12)
       RETURNING *`,
      [
        userId,
        data.patente,
        data.patente_acoplado ?? null,
        data.transporte_nombre ?? null,
        data.transporte_cuit ?? null,
        data.chofer_nombre,
        data.chofer_cuit ?? null,
        data.telefono ?? null,
        data.capacidad_kg ?? null,
        data.lat ?? 0,
        data.lng ?? 0,
        data.estado ?? 'disponible',
      ]
    );
    return result.rows[0];
  }

  static toResponse(truck: Truck) {
    return {
      id: truck.id,
      user_id: truck.user_id,
      patente: truck.patente,
      patente_acoplado: truck.patente_acoplado,
      transporte_nombre: truck.transporte_nombre,
      chofer_nombre: truck.chofer_nombre,
      telefono: truck.telefono,
      capacidad_kg: truck.capacidad_kg,
      lat: truck.lat,
      lng: truck.lng,
      estado: truck.estado,
      created_at: truck.created_at,
      updated_at: truck.updated_at,
    };
  }
}
