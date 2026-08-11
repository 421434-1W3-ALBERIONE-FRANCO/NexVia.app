import { query } from '../config/database';

export interface Trip {
  id: string;
  usuario_id: string;
  chofer_id: string | null;
  camion_id: string | null;
  origen_lat: number;
  origen_lng: number;
  destino_lat: number;
  destino_lng: number;
  distancia_km: number;
  toneladas: number | null;
  tipo_tarifa: string;
  precio: number;
  tarifa_unitaria: number;
  carga: string | null;
  estado: 'solicitado' | 'aceptado' | 'en_camino' | 'completado' | 'cancelado';
  version: number;
  solicitado_at: Date;
  aceptado_at: Date | null;
  en_camino_at: Date | null;
  completado_at: Date | null;
  cancelado_at: Date | null;
  cancelacion_razon: string | null;
  created_at: Date;
  updated_at: Date;
}

export class TripRepository {
  static async create(data: {
    usuario_id: string;
    origen_lat: number;
    origen_lng: number;
    destino_lat: number;
    destino_lng: number;
    distancia_km: number;
    toneladas: number | null;
    tipo_tarifa: string;
    precio: number;
    tarifa_unitaria: number;
    carga: string | null;
  }): Promise<Trip> {
    const result = await query(
      `INSERT INTO viajes (
        usuario_id, origen_lat, origen_lng, destino_lat, destino_lng,
        distancia_km, toneladas, tipo_tarifa, precio, tarifa_unitaria, carga
      ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11)
      RETURNING *`,
      [
        data.usuario_id,
        data.origen_lat,
        data.origen_lng,
        data.destino_lat,
        data.destino_lng,
        data.distancia_km,
        data.toneladas,
        data.tipo_tarifa,
        data.precio,
        data.tarifa_unitaria,
        data.carga,
      ]
    );
    return result.rows[0];
  }

  static async findById(id: string): Promise<Trip | null> {
    const result = await query('SELECT * FROM viajes WHERE id = $1', [id]);
    return result.rows[0] || null;
  }

  static async findByUsuarioId(usuarioId: string, limit: number = 20, offset: number = 0): Promise<Trip[]> {
    const result = await query(
      'SELECT * FROM viajes WHERE usuario_id = $1 ORDER BY created_at DESC LIMIT $2 OFFSET $3',
      [usuarioId, limit, offset]
    );
    return result.rows;
  }

  static async findAvailable(limit: number = 20, offset: number = 0): Promise<Trip[]> {
    const result = await query(
      `SELECT * FROM viajes
       WHERE estado = 'solicitado' AND chofer_id IS NULL
       ORDER BY created_at DESC LIMIT $1 OFFSET $2`,
      [limit, offset]
    );
    return result.rows;
  }

  static async findByChofer(choferId: string, limit: number = 20, offset: number = 0): Promise<Trip[]> {
    const result = await query(
      `SELECT * FROM viajes
       WHERE chofer_id = $1
       ORDER BY created_at DESC LIMIT $2 OFFSET $3`,
      [choferId, limit, offset]
    );
    return result.rows;
  }

  static async acceptTrip(tripId: string, choferId: string, camionId: string): Promise<Trip> {
    const client = await query('SELECT * FROM viajes WHERE id = $1 FOR UPDATE', [tripId]);
    if (!client.rows[0]) throw new Error('Trip not found');

    const trip = client.rows[0];
    if (trip.estado !== 'solicitado' || trip.chofer_id !== null) {
      throw new Error('Trip already accepted or not available');
    }

    const result = await query(
      `UPDATE viajes
       SET chofer_id = $1, camion_id = $2, estado = 'aceptado',
           aceptado_at = CURRENT_TIMESTAMP, version = version + 1,
           updated_at = CURRENT_TIMESTAMP
       WHERE id = $3 AND estado = 'solicitado' AND chofer_id IS NULL
       RETURNING *`,
      [choferId, camionId, tripId]
    );

    if (result.rows.length === 0) {
      throw new Error('Failed to accept trip (concurrent modification)');
    }

    return result.rows[0];
  }

  static async updateState(
    tripId: string,
    newState: 'en_camino' | 'completado' | 'cancelado',
    reason?: string
  ): Promise<Trip> {
    let updateFields = `estado = $2, ${newState}_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP`;
    let params: any[] = [tripId, newState];

    if (newState === 'cancelado' && reason) {
      updateFields += `, cancelacion_razon = $3`;
      params.push(reason);
    }

    const result = await query(
      `UPDATE viajes SET ${updateFields}, version = version + 1
       WHERE id = $1 RETURNING *`,
      params
    );

    if (result.rows.length === 0) throw new Error('Trip not found');
    return result.rows[0];
  }

  static async countByUsuarioId(usuarioId: string): Promise<number> {
    const result = await query(
      'SELECT COUNT(*) as count FROM viajes WHERE usuario_id = $1',
      [usuarioId]
    );
    return parseInt(result.rows[0].count);
  }

  static async countAvailable(): Promise<number> {
    const result = await query(
      "SELECT COUNT(*) as count FROM viajes WHERE estado = 'solicitado' AND chofer_id IS NULL"
    );
    return parseInt(result.rows[0].count);
  }

  static toResponse(trip: Trip) {
    return {
      id: trip.id,
      usuario_id: trip.usuario_id,
      chofer_id: trip.chofer_id,
      camion_id: trip.camion_id,
      origen_lat: trip.origen_lat,
      origen_lng: trip.origen_lng,
      destino_lat: trip.destino_lat,
      destino_lng: trip.destino_lng,
      // Postgres returns DECIMAL as strings; coerce to numbers for the client.
      distancia_km: trip.distancia_km != null ? Number(trip.distancia_km) : null,
      toneladas: trip.toneladas != null ? Number(trip.toneladas) : null,
      tipo_tarifa: trip.tipo_tarifa,
      precio: trip.precio != null ? Number(trip.precio) : null,
      estado: trip.estado,
      created_at: trip.created_at,
      updated_at: trip.updated_at,
    };
  }
}
