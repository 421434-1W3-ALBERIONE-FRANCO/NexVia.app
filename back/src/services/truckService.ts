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
