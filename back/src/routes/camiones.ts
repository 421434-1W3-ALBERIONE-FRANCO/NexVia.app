import { Router, Request, Response } from 'express';
import { z } from 'zod';
import { TruckService } from '../services/truckService';
import { TruckRepository } from '../repositories/truckRepository';
import { asyncHandler, AppError } from '../middleware/errorHandler';
import { requireAuth, requireRole } from '../middleware/auth';
import {
  UpdateTruckLocationSchema,
  UpdateTruckSchema,
  GetAvailableTrucksSchema,
} from '../types/schemas';

const router = Router();

// GET /api/v1/camiones/disponibles — Get available trucks
router.get(
  '/disponibles',
  requireAuth,
  asyncHandler(async (req: Request, res: Response) => {
    const validation = GetAvailableTrucksSchema.safeParse(req.query);
    if (!validation.success) {
      throw new AppError(400, 'Validation failed', 'VALIDATION_ERROR');
    }

    const { trucks, total } = await TruckService.getAvailable(
      validation.data.page,
      validation.data.limit
    );

    res.json({
      trucks: trucks.map(TruckRepository.toResponse),
      pagination: {
        page: validation.data.page,
        limit: validation.data.limit,
        total,
        pages: Math.ceil(total / validation.data.limit),
      },
    });
  })
);

// GET /api/v1/camiones/mi-camion — Get my truck (chofer)
router.get(
  '/mi-camion',
  requireAuth,
  requireRole('chofer'),
  asyncHandler(async (req: Request, res: Response) => {
    const truck = await TruckService.getMyTruck(req.user!.id);

    res.json({
      truck: TruckRepository.toResponse(truck),
    });
  })
);

// POST /api/v1/camiones/:id/ubicacion — Update GPS location
router.post(
  '/:id/ubicacion',
  requireAuth,
  requireRole('chofer'),
  asyncHandler(async (req: Request, res: Response) => {
    const validation = UpdateTruckLocationSchema.safeParse(req.body);
    if (!validation.success) {
      throw new AppError(400, 'Validation failed', 'VALIDATION_ERROR');
    }

    const truck = await TruckService.updateLocation(
      req.params.id,
      validation.data.lat,
      validation.data.lng,
      req.user!.id
    );

    res.json({
      message: 'Location updated successfully',
      truck: TruckRepository.toResponse(truck),
    });
  })
);

// PUT /api/v1/camiones/:id — Update truck (admin)
router.put(
  '/:id',
  requireAuth,
  requireRole('admin'),
  asyncHandler(async (req: Request, res: Response) => {
    const validation = UpdateTruckSchema.safeParse(req.body);
    if (!validation.success) {
      throw new AppError(400, 'Validation failed', 'VALIDATION_ERROR');
    }

    const truck = await TruckService.update(req.params.id, validation.data, req.user!.id);

    res.json({
      message: 'Truck updated successfully',
      truck: TruckRepository.toResponse(truck),
    });
  })
);

// DELETE /api/v1/camiones/:id — Soft delete (admin)
router.delete(
  '/:id',
  requireAuth,
  requireRole('admin'),
  asyncHandler(async (req: Request, res: Response) => {
    await TruckService.softDelete(req.params.id, req.user!.id);

    res.json({
      message: 'Truck deleted successfully',
    });
  })
);

export default router;
