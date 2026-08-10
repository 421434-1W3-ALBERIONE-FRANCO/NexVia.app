import { TripRepository, Trip } from '../repositories/tripRepository';
import { TruckRepository } from '../repositories/truckRepository';
import { ConfigRepository } from '../repositories/configRepository';
import { AuditService } from './auditService';
import { AppError } from '../middleware/errorHandler';

export class TripService {
  // Haversine formula to calculate distance between two points
  static calculateDistance(lat1: number, lng1: number, lat2: number, lng2: number): number {
    const R = 6371; // Earth radius in km
    const dLat = ((lat2 - lat1) * Math.PI) / 180;
    const dLng = ((lng2 - lng1) * Math.PI) / 180;
    const a =
      Math.sin(dLat / 2) * Math.sin(dLat / 2) +
      Math.cos((lat1 * Math.PI) / 180) *
        Math.cos((lat2 * Math.PI) / 180) *
        Math.sin(dLng / 2) *
        Math.sin(dLng / 2);
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    const distance = R * c;
    return Math.round(distance * 100) / 100;
  }

  static async calculatePrice(
    distanciaKm: number,
    toneladas: number | null,
    tipoTarifa: string
  ): Promise<{ precio: number; tarifaUnitaria: number }> {
    const config = await ConfigRepository.get();

    let precio = 0;
    let tarifaUnitaria = 0;

    if (tipoTarifa === 'por_km') {
      tarifaUnitaria = config.tarifa_por_km;
      precio = distanciaKm * tarifaUnitaria;
    } else if (tipoTarifa === 'por_tonelada' && toneladas) {
      tarifaUnitaria = config.tarifa_por_tonelada;
      precio = toneladas * tarifaUnitaria;
    } else if (tipoTarifa === 'mixta' && toneladas) {
      const precioPorKm = distanciaKm * config.tarifa_por_km;
      const precioPorTonelada = toneladas * config.tarifa_por_tonelada;
      precio = precioPorKm + precioPorTonelada;
      tarifaUnitaria = config.tarifa_por_km; // Store base rate
    } else {
      throw new AppError(400, 'Invalid pricing configuration', 'INVALID_PRICING');
    }

    return {
      precio: Math.round(precio * 100) / 100,
      tarifaUnitaria: Math.round(tarifaUnitaria * 100) / 100,
    };
  }

  static async create(
    usuarioId: string,
    data: {
      origen_lat: number;
      origen_lng: number;
      destino_lat: number;
      destino_lng: number;
      toneladas?: number;
      tipo_tarifa: string;
      carga?: string;
    },
    clientIp: string,
    userAgent: string
  ): Promise<Trip> {
    // Validate coordinates
    if (
      Math.abs(data.origen_lat - data.destino_lat) < 0.001 &&
      Math.abs(data.origen_lng - data.destino_lng) < 0.001
    ) {
      throw new AppError(400, 'Origin and destination must be different', 'INVALID_COORDINATES');
    }

    // Calculate distance
    const distanciaKm = this.calculateDistance(
      data.origen_lat,
      data.origen_lng,
      data.destino_lat,
      data.destino_lng
    );

    if (distanciaKm < 0.1) {
      throw new AppError(400, 'Distance too short', 'INVALID_DISTANCE');
    }

    // Calculate price
    const { precio, tarifaUnitaria } = await this.calculatePrice(
      distanciaKm,
      data.toneladas ?? null,
      data.tipo_tarifa
    );

    // Create trip
    const trip = await TripRepository.create({
      usuario_id: usuarioId,
      origen_lat: data.origen_lat,
      origen_lng: data.origen_lng,
      destino_lat: data.destino_lat,
      destino_lng: data.destino_lng,
      distancia_km: distanciaKm,
      toneladas: data.toneladas ?? null,
      tipo_tarifa: data.tipo_tarifa,
      precio,
      tarifa_unitaria: tarifaUnitaria,
      carga: data.carga ?? null,
    });

    // Log to audit
    await AuditService.log('create', 'viaje', {
      userId: usuarioId,
      entityId: trip.id,
      details: {
        distancia_km: distanciaKm,
        precio,
      },
    });

    return trip;
  }

  static async getMyTrips(
    usuarioId: string,
    page: number = 1,
    limit: number = 10
  ): Promise<{ trips: Trip[]; total: number }> {
    const offset = (page - 1) * limit;
    const trips = await TripRepository.findByUsuarioId(usuarioId, limit, offset);
    const total = await TripRepository.countByUsuarioId(usuarioId);

    return { trips, total };
  }

  static async getAvailable(page: number = 1, limit: number = 10): Promise<{ trips: Trip[]; total: number }> {
    const offset = (page - 1) * limit;
    const trips = await TripRepository.findAvailable(limit, offset);
    const total = await TripRepository.countAvailable();

    return { trips, total };
  }

  static async acceptTrip(
    tripId: string,
    choferId: string,
    clientIp: string,
    userAgent: string
  ): Promise<Trip> {
    // Get truck for this driver
    const truck = await TruckRepository.findByUserId(choferId);
    if (!truck) {
      throw new AppError(400, 'Driver has no truck assigned', 'NO_TRUCK');
    }

    if (truck.estado !== 'disponible') {
      throw new AppError(400, 'Truck is not available', 'TRUCK_UNAVAILABLE');
    }

    // Get trip to check it's available
    const trip = await TripRepository.findById(tripId);
    if (!trip) {
      throw new AppError(404, 'Trip not found', 'TRIP_NOT_FOUND');
    }

    if (trip.estado !== 'solicitado' || trip.chofer_id !== null) {
      throw new AppError(400, 'Trip is no longer available', 'TRIP_UNAVAILABLE');
    }

    // Accept trip (transactional)
    const acceptedTrip = await TripRepository.acceptTrip(tripId, choferId, truck.id);

    // Update truck to ocupado
    await TruckRepository.updateEstado(truck.id, 'ocupado');

    // Log to audit
    await AuditService.log('update', 'viaje', {
      userId: choferId,
      entityId: tripId,
      details: {
        action: 'accept',
        camion_id: truck.id,
      },
    });

    return acceptedTrip;
  }

  static async cancelTrip(
    tripId: string,
    userId: string,
    reason: string,
    userRole: string,
    clientIp: string,
    userAgent: string
  ): Promise<Trip> {
    const trip = await TripRepository.findById(tripId);
    if (!trip) {
      throw new AppError(404, 'Trip not found', 'TRIP_NOT_FOUND');
    }

    // Verify ownership/permission
    if (userRole !== 'admin' && trip.usuario_id !== userId && trip.chofer_id !== userId) {
      throw new AppError(403, 'Not authorized to cancel this trip', 'UNAUTHORIZED');
    }

    if (trip.estado === 'completado' || trip.estado === 'cancelado') {
      throw new AppError(400, 'Cannot cancel completed or already cancelled trip', 'INVALID_STATE');
    }

    // Cancel trip
    const cancelledTrip = await TripRepository.updateState(tripId, 'cancelado', reason);

    // If truck was assigned, mark as disponible
    if (trip.camion_id) {
      await TruckRepository.updateEstado(trip.camion_id, 'disponible');
    }

    // Log to audit
    await AuditService.log('update', 'viaje', {
      userId,
      entityId: tripId,
      details: {
        action: 'cancel',
        reason,
      },
    });

    return cancelledTrip;
  }

  static async markEnRoute(tripId: string, choferId: string): Promise<Trip> {
    const trip = await TripRepository.findById(tripId);
    if (!trip) {
      throw new AppError(404, 'Trip not found', 'TRIP_NOT_FOUND');
    }

    if (trip.chofer_id !== choferId) {
      throw new AppError(403, 'Not authorized', 'UNAUTHORIZED');
    }

    if (trip.estado !== 'aceptado') {
      throw new AppError(400, 'Trip must be accepted first', 'INVALID_STATE');
    }

    const updatedTrip = await TripRepository.updateState(tripId, 'en_camino');

    await AuditService.log('update', 'viaje', {
      userId: choferId,
      entityId: tripId,
      details: {
        action: 'en_camino',
      },
    });

    return updatedTrip;
  }

  static async markCompleted(tripId: string, choferId: string): Promise<Trip> {
    const trip = await TripRepository.findById(tripId);
    if (!trip) {
      throw new AppError(404, 'Trip not found', 'TRIP_NOT_FOUND');
    }

    if (trip.chofer_id !== choferId) {
      throw new AppError(403, 'Not authorized', 'UNAUTHORIZED');
    }

    if (trip.estado !== 'en_camino') {
      throw new AppError(400, 'Trip must be en_camino first', 'INVALID_STATE');
    }

    const completedTrip = await TripRepository.updateState(tripId, 'completado');

    // Mark truck as disponible
    if (trip.camion_id) {
      await TruckRepository.updateEstado(trip.camion_id, 'disponible');
    }

    await AuditService.log('update', 'viaje', {
      userId: choferId,
      entityId: tripId,
      details: {
        action: 'completado',
      },
    });

    return completedTrip;
  }
}
