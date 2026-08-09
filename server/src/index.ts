import express, { Request, Response } from 'express';
import helmet from 'helmet';
import { config } from './config/env';
import { corsMiddleware } from './middleware/cors';
import { errorHandler } from './middleware/errorHandler';
import { apiRateLimit } from './middleware/rateLimit';
import { initRedis } from './config/redis';

const app = express();

// Security
app.use(helmet());

// Middleware
app.use(express.json({ limit: '10mb' }));
app.use(express.urlencoded({ limit: '10mb', extended: true }));
app.use(corsMiddleware);

// Client IP
app.use((req: Request, res: Response, next: Function) => {
  req.clientIp = (
    req.headers['x-forwarded-for'] as string ||
    req.socket.remoteAddress ||
    ''
  ).split(',')[0].trim();
  req.userAgent = req.headers['user-agent'];
  next();
});

// Health check
app.get('/health', (req: Request, res: Response) => {
  res.json({
    status: 'ok',
    timestamp: new Date().toISOString(),
    version: '1.0.0',
  });
});

app.get('/api/v1/health', (req: Request, res: Response) => {
  res.json({
    status: 'ok',
    database: 'connected',
    redis: 'checking',
    timestamp: new Date().toISOString(),
  });
});

// API Routes (to be added in subsequent phases)
// import authRoutes from './routes/auth';
// import viajesRoutes from './routes/viajes';
// import camionesRoutes from './routes/camiones';
// import adminRoutes from './routes/admin';

// app.use('/api/v1/auth', authRoutes);
// app.use('/api/v1/viajes', viajesRoutes);
// app.use('/api/v1/camiones', camionesRoutes);
// app.use('/api/v1/admin', adminRoutes);

// Rate limiting (general)
app.use('/api/', apiRateLimit);

// 404
app.use('*', (req: Request, res: Response) => {
  res.status(404).json({
    error: {
      message: 'Not found',
      code: 'NOT_FOUND',
      statusCode: 404,
    },
  });
});

// Error handling (must be last)
app.use(errorHandler);

// Initialize and start server
async function start() {
  try {
    console.log(`🚀 NEXVIA Backend v1.0.0`);
    console.log(`📝 Environment: ${config.NODE_ENV}`);

    // Initialize Redis
    initRedis();

    const PORT = config.PORT;
    app.listen(PORT, () => {
      console.log(`✅ Server listening on http://localhost:${PORT}`);
      console.log(`📡 API: http://localhost:${PORT}/api/v1`);
      console.log(`💊 Health: http://localhost:${PORT}/health`);
    });
  } catch (error) {
    console.error('❌ Failed to start server:', error);
    process.exit(1);
  }
}

start();
