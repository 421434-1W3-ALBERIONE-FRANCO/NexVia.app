import { Router, Request, Response } from 'express';
import { z } from 'zod';
import { TripService } from '../services/tripService';
import { TripRepository } from '../repositories/tripRepository';
import { asyncHandler, AppError } from '../middleware/errorHandler';
import { requireAuth, requireRole } from '../middleware/auth';
import {
  CreateTripSchema,
  CancelTripSchema,
  GetAvailableTripsSchema,
} from '../types/schemas';

const router = Router();

// POST /api/v1/viajes — Create trip (usuario)
router.post(
  '/',
  requireAuth,
  asyncHandler(async (req: Request, res: Response) => {
    const validation = CreateTripSchema.safeParse(req.body);
    if (!validation.success) {
      throw new AppError(400, 'Validation failed', 'VALIDATION_ERROR');
    }

    const trip = await TripService.create(
      req.user!.id,
      validation.data,
      req.clientIp || '',
      req.userAgent || ''
    );

    res.status(201).json({
      message: 'Trip created successfully',
      trip: TripRepository.toResponse(trip),
    });
  })
);

// GET /api/v1/viajes/mis-viajes — Get my trips
router.get(
  '/mis-viajes',
  requireAuth,
  asyncHandler(async (req: Request, res: Response) => {
    const page = parseInt(req.query.page as string) || 1;
    const limit = parseInt(req.query.limit as string) || 10;

    const { trips, total } = await TripService.getMyTrips(req.user!.id, page, limit);

    res.json({
      trips: trips.map(TripRepository.toResponse),
      pagination: {
        page,
        limit,
        total,
        pages: Math.ceil(total / limit),
      },
    });
  })
);

// GET /api/v1/viajes/disponibles — Get available trips (chofer)
router.get(
  '/disponibles',
  requireAuth,
  requireRole('chofer'),
  asyncHandler(async (req: Request, res: Response) => {
    const validation = GetAvailableTripsSchema.safeParse(req.query);
    if (!validation.success) {
      throw new AppError(400, 'Validation failed', 'VALIDATION_ERROR');
    }

    const { trips, total } = await TripService.getAvailable(
      validation.data.page,
      validation.data.limit
    );

    res.json({
      trips: trips.map(TripRepository.toResponse),
      pagination: {
        page: validation.data.page,
        limit: validation.data.limit,
        total,
        pages: Math.ceil(total / validation.data.limit),
      },
    });
  })
);

// POST /api/v1/viajes/:id/aceptar — Accept trip (chofer, TRANSACTIONAL)
router.post(
  '/:id/aceptar',
  requireAuth,
  requireRole('chofer'),
  asyncHandler(async (req: Request, res: Response) => {
    const trip = await TripService.acceptTrip(
      req.params.id,
      req.user!.id,
      req.clientIp || '',
      req.userAgent || ''
    );

    res.json({
      message: 'Trip accepted successfully',
      trip: TripRepository.toResponse(trip),
    });
  })
);

// POST /api/v1/viajes/:id/cancelar — Cancel trip
router.post(
  '/:id/cancelar',
  requireAuth,
  asyncHandler(async (req: Request, res: Response) => {
    const validation = CancelTripSchema.safeParse(req.body);
    if (!validation.success) {
      throw new AppError(400, 'Validation failed', 'VALIDATION_ERROR');
    }

    const trip = await TripService.cancelTrip(
      req.params.id,
      req.user!.id,
      validation.data.razon,
      req.user!.role,
      req.clientIp || '',
      req.userAgent || ''
    );

    res.json({
      message: 'Trip cancelled successfully',
      trip: TripRepository.toResponse(trip),
    });
  })
);

// POST /api/v1/viajes/:id/en-camino — Mark en route
router.post(
  '/:id/en-camino',
  requireAuth,
  requireRole('chofer'),
  asyncHandler(async (req: Request, res: Response) => {
    const trip = await TripService.markEnRoute(req.params.id, req.user!.id);

    res.json({
      message: 'Trip marked as en route',
      trip: TripRepository.toResponse(trip),
    });
  })
);

// POST /api/v1/viajes/:id/completar — Mark completed
router.post(
  '/:id/completar',
  requireAuth,
  requireRole('chofer'),
  asyncHandler(async (req: Request, res: Response) => {
    const trip = await TripService.markCompleted(req.params.id, req.user!.id);

    res.json({
      message: 'Trip completed successfully',
      trip: TripRepository.toResponse(trip),
    });
  })
);

export default router;
