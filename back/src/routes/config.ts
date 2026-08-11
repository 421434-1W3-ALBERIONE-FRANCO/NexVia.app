import { Router, Request, Response } from 'express';
import { ConfigRepository } from '../repositories/configRepository';
import { asyncHandler } from '../middleware/errorHandler';
import { requireAuth } from '../middleware/auth';

const router = Router();

// GET /api/v1/config — read public configuration (any authenticated user).
// Tarifas and zona are needed by productores to price trips and center the map.
router.get(
  '/',
  requireAuth,
  asyncHandler(async (req: Request, res: Response) => {
    const config = await ConfigRepository.get();
    res.json({ config: ConfigRepository.toResponse(config) });
  })
);

export default router;
