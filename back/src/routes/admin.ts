import { Router, Request, Response } from 'express';
import { z } from 'zod';
import { AdminService } from '../services/adminService';
import { ConfigRepository } from '../repositories/configRepository';
import { asyncHandler, AppError } from '../middleware/errorHandler';
import { requireAuth, requireRole } from '../middleware/auth';
import { UpdateConfigSchema, GetAuditLogSchema } from '../types/schemas';

const router = Router();

// GET /api/v1/admin/usuarios — List users (admin)
router.get(
  '/usuarios',
  requireAuth,
  requireRole('admin'),
  asyncHandler(async (req: Request, res: Response) => {
    const page = parseInt(req.query.page as string) || 1;
    const limit = parseInt(req.query.limit as string) || 25;

    const { users, total } = await AdminService.listUsers(page, limit);

    res.json({
      users,
      pagination: {
        page,
        limit,
        total,
        pages: Math.ceil(total / limit),
      },
    });
  })
);

// GET /api/v1/admin/usuarios/:id — Get user details (admin)
router.get(
  '/usuarios/:id',
  requireAuth,
  requireRole('admin'),
  asyncHandler(async (req: Request, res: Response) => {
    const user = await AdminService.getUserDetails(req.params.id);

    res.json(user);
  })
);

// GET /api/v1/admin/configuracion — Get configuration
router.get(
  '/configuracion',
  requireAuth,
  requireRole('admin'),
  asyncHandler(async (req: Request, res: Response) => {
    const config = await ConfigRepository.get();

    res.json({
      config: ConfigRepository.toResponse(config),
    });
  })
);

// PUT /api/v1/admin/configuracion — Update configuration (admin)
router.put(
  '/configuracion',
  requireAuth,
  requireRole('admin'),
  asyncHandler(async (req: Request, res: Response) => {
    const validation = UpdateConfigSchema.safeParse(req.body);
    if (!validation.success) {
      throw new AppError(400, 'Validation failed', 'VALIDATION_ERROR');
    }

    const config = await AdminService.updateConfiguration(validation.data, req.user!.id);

    res.json({
      message: 'Configuration updated successfully',
      config: ConfigRepository.toResponse(config),
    });
  })
);

// GET /api/v1/admin/audit-log — Get audit log (admin)
router.get(
  '/audit-log',
  requireAuth,
  requireRole('admin'),
  asyncHandler(async (req: Request, res: Response) => {
    const validation = GetAuditLogSchema.safeParse(req.query);
    if (!validation.success) {
      throw new AppError(400, 'Validation failed', 'VALIDATION_ERROR');
    }

    const { logs, total } = await AdminService.getAuditLog({
      user_id: validation.data.user_id,
      action: validation.data.action,
      page: validation.data.page,
      limit: validation.data.limit,
    });

    res.json({
      logs,
      pagination: {
        page: validation.data.page,
        limit: validation.data.limit,
        total,
        pages: Math.ceil(total / validation.data.limit),
      },
    });
  })
);

export default router;
