import { TruckRepository, Truck } from '../repositories/truckRepository';
import { AuditService } from './auditService';
import { AppError } from '../middleware/errorHandler';

export class TruckService {
  static async getAvailable(page: number = 1, limit: number = 10): Promise<{ trucks: Truck[]; total: number }> {
    const offset = (page - 1) * limit;
    const trucks = await TruckRepository.findAvailable(limit, offset);
    const total = await TruckRepository.countAvailable();

    return { trucks, total };
  }

  static async create(userId: string, data: Partial<Truck>): Promise<Truck> {
    // One truck per chofer: if they already registered one, return it (idempotent onboarding).
    const existing = await TruckRepository.findByUserId(userId);
    if (existing) {
      return existing;
    }
    if (!data.patente) {
      throw new AppError(400, 'Patente is required', 'PATENTE_REQUIRED');
    }
    const truck = await TruckRepository.create(userId, data);
    await AuditService.log('create', 'camion', { userId, entityId: truck.id });
    return truck;
  }

  static async getMyTruck(choferId: string): Promise<Truck> {
    const truck = await TruckRepository.findByUserId(choferId);
    if (!truck) {
      throw new AppError(404, 'Truck not found', 'TRUCK_NOT_FOUND');
    }
    return truck;
  }

  static async updateLocation(truckId: string, lat: number, lng: number, choferId: string): Promise<Truck> {
    const truck = await TruckRepository.findById(truckId);
    if (!truck) {
      throw new AppError(404, 'Truck not found', 'TRUCK_NOT_FOUND');
    }

    if (truck.user_id !== choferId) {
      throw new AppError(403, 'Not authorized', 'UNAUTHORIZED');
    }

    // Validate coordinates
    if (lat < -90 || lat > 90 || lng < -180 || lng > 180) {
      throw new AppError(400, 'Invalid coordinates', 'INVALID_COORDINATES');
    }

    const updatedTruck = await TruckRepository.updateLocation(truckId, lat, lng);

    await AuditService.log('update', 'camion', {
      userId: choferId,
      entityId: truckId,
      details: {
        action: 'location_update',
        lat,
        lng,
      },
    });

    return updatedTruck;
  }

  static async update(truckId: string, data: Partial<Truck>, adminId: string): Promise<Truck> {
    const truck = await TruckRepository.findById(truckId);
    if (!truck) {
      throw new AppError(404, 'Truck not found', 'TRUCK_NOT_FOUND');
    }

    const updatedTruck = await TruckRepository.update(truckId, data);

    await AuditService.log('update', 'camion', {
      userId: adminId,
      entityId: truckId,
      details: {
        updated_fields: Object.keys(data),
      },
    });

    return updatedTruck;
  }

  static async softDelete(truckId: string, adminId: string): Promise<void> {
    const truck = await TruckRepository.findById(truckId);
    if (!truck) {
      throw new AppError(404, 'Truck not found', 'TRUCK_NOT_FOUND');
    }

    await TruckRepository.softDelete(truckId);

    await AuditService.log('delete', 'camion', {
      userId: adminId,
      entityId: truckId,
    });
  }
}
